/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi::class)

// Access shared serialization helpers without exposing them as public API.
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package org.jetbrains.compose.web.dom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ExplicitGroupsComposable
import androidx.compose.runtime.NonRestartableComposable
import kotlinx.browser.dom.Element
import kotlinx.browser.dom.HTMLStyleElement
import org.jetbrains.compose.web.HtmlValidationMode
import org.jetbrains.compose.web.LocalHtmlValidationMode
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.attributes.AttrsScopeBuilder
import org.jetbrains.compose.web.css.CSSRuleDeclarationList
import org.jetbrains.compose.web.css.StyleScope
import org.jetbrains.compose.web.css.StyleScopeBuilder
import org.jetbrains.compose.web.css.toStyleAttributeValue
import org.jetbrains.compose.web.internal.unsafeCast

/**
 * Writes HTML while composables execute.
 *
 * Each [TagElement] call writes its start tag, composes its children, then writes its end tag.
 * The fields describe the currently open element. Parent state stays in local variables on the
 * call stack.
 */
@PublishedApi
internal class HtmlStringWriterContext(
    private val hydratable: Boolean,
    private val attrsBuilders: AttrsBuilderPool = AttrsBuilderPool(),
    @PublishedApi internal val output: StringBuilder = StringBuilder(),
    private val chunkSink: ((StringBuilder) -> Unit)? = null,
    private val chunkSize: Int = 2048,
) : ComposeHtmlContext {
    private val streaming = chunkSink != null

    // Used to trim retained output storage.
    var peakBufferSize: Int = 0
        private set
    private val elementScope = StringElementScope<Element>()
    private var finished = false
    private var immutableOutput: String? = null

    // These elements must keep their content in the shared buffer until their end tag is written.
    private var bufferedElementDepth = 0

    // Reuse attribute channels initialized for each element position across keyed renders.
    private var attrsBuilderIndex = 0

    // A null tag name means that output is being written at the root.
    internal var currentTagName: String? = null
    internal var currentNamespace = HtmlNamespace
    internal var previousSiblingWasText = false

    // The HTML parser drops the first LF in pre, textarea, and listing. Repair it when the
    // first content is written, so these elements can stream without retaining their children.
    internal var pendingLeadingNewline = false

    // Don't produce children's HTML, but still execute their composables.
    internal var discardingContent = false

    // Raw text is validated at the end tag. Raw-text elements cannot nest, so one buffer suffices.
    internal var collectingRawText = false
    internal var hasRawTextCall = false
    internal val rawText = StringBuilder()

    // Hydrated document renderers require exactly one html root element.
    private var rootChildCount = 0
    private var firstRootChildIsHtml = false

    override val supportsDomElementAccess: Boolean = false

    override fun <TElement : Element> elementBuilder(tagName: String): ElementBuilder<TElement> =
        attrsBuilders.elementBuilder(tagName, HtmlNamespace)

    override fun <TElement : Element> elementBuilderNS(tagName: String, namespace: String): ElementBuilder<TElement> =
        attrsBuilders.elementBuilder(tagName, namespace)

    @Composable
    @NonRestartableComposable
    @ExplicitGroupsComposable
    override fun <TElement : Element> TagElement(
        elementBuilder: ElementBuilder<TElement>,
        applyAttrs: (AttrsScope<TElement>.() -> Unit)?,
        rawTextContent: RawTextContent?,
        content: (@Composable ElementScope<TElement>.() -> Unit)?,
    ) {
        checkNotFinished()
        if (discardingContent) {
            content?.invoke(elementScope())
            return
        }

        val tagName = elementBuilder.tagName
        val metadata = elementBuilder as? HtmlTagMetadata
        val namespace = metadata?.namespace ?: HtmlNamespace
        val flags = metadata?.serializationFlags ?: htmlStringTagFlags(tagName, namespace)
        val rawTextElement = flags and RawTextTagFlag != 0

        // Emit any completed chunk before writing this start tag.
        flushChunk()
        val bufferElement = streaming &&
                (rawTextElement || (namespace == HtmlNamespace && tagName == "noscript"))
        if (bufferElement) bufferedElementDepth++

        val validate = LocalHtmlValidationMode.current == HtmlValidationMode.Strict
        val startTag = writeStartTag(
            tagName = tagName,
            namespace = namespace,
            applyAttrs = applyAttrs,
            validate = validate,
            tagNameIsValid = flags and ValidTagFlag != 0,
        )
        val contentOverride = startTag.textContent
        // A raw-text parent rejects child tags, and a void parent returned above.
        // Only the parent's tag and namespace need saving.
        val parentTagName = currentTagName
        val parentNamespace = currentNamespace
        val isVoid = flags and VoidTagFlag != 0
        // Skip unchanged reference stores on the ordinary HTML path.
        if (currentTagName !== tagName) currentTagName = tagName
        if (currentNamespace !== namespace) currentNamespace = namespace
        previousSiblingWasText = false
        pendingLeadingNewline = flags and LeadingNewlineTagFlag != 0
        collectingRawText = rawTextElement

        flushChunk()
        // For buffered elements this index stays valid until appendEndTag inspects the content.
        val contentStart = output.length
        if (!contentOverride.isNullOrEmpty()) appendText(contentOverride, validate)
        discardingContent = isVoid || contentOverride != null
        if (rawTextContent != null && !discardingContent) {
            rawTextContent.validateScriptSource(startTag.hasScriptSource)
            output.append(rawTextContent.text)
        }
        content?.invoke(elementScope())

        if (!isVoid) writeEndTag(tagName, namespace, startTag.hasScriptSource, contentStart)
        if (currentTagName !== parentTagName) currentTagName = parentTagName
        if (currentNamespace !== parentNamespace) currentNamespace = parentNamespace
        previousSiblingWasText = false
        pendingLeadingNewline = false // This element's start tag consumed its parent's first content.
        discardingContent = false
        collectingRawText = false
        if (bufferElement) bufferedElementDepth--
        flushChunk()
    }

    /** Returns the shared buffer, or null when output is discarded or the stored string can be returned directly. */
    @PublishedApi
    internal fun beginStaticHtml(rootTag: String, encoded: String, validate: Boolean): StringBuilder? {
        if (!beginStaticHtmlFragment(rootTag, validate)) return null
        // Return small static pages directly. Buffer mixed or streamed output.
        if (!streaming && currentTagName == null && output.isEmpty()) {
            immutableOutput = encoded
            return null
        }
        return output
    }

    /** Begins one compiler-validated fragment, independently of how its HTML is stored. */
    @PublishedApi
    internal fun beginStaticHtmlFragment(rootTag: String, validate: Boolean): Boolean {
        checkNotFinished()
        if (discardingContent) return false
        beginChild(rootTag, validate)
        previousSiblingWasText = false
        pendingLeadingNewline = false
        return true
    }

    /** Inlines each literal append and streams it through the ordinary buffer. */
    @PublishedApi
    @Suppress("NOTHING_TO_INLINE")
    internal inline fun appendStaticHtmlChunk(encoded: String) {
        output.append(encoded)
        flushChunk()
    }

    /**
     * Only called by compiler plugin. Opens nested elements written as one literal.
     * Only [rootTag] is checked against the unknown parent; [innermost] receives the following content.
     * [closeElement] with the returned parent restores the state from before the region.
     * Keep this and its callees small so the JVM can inline them.
     */
    @PublishedApi
    internal fun openLoweredRegion(
        rootTag: String,
        encoded: String,
        innermost: String,
        validate: Boolean,
        namespace: String = HtmlNamespace,
        leadingNewline: Boolean = false,
    ): Any? {
        checkNotFinished()
        if (discardingContent) return null
        beginChild(rootTag, validate)
        output.append(encoded)
        val parent = enter(innermost, namespace, leadingNewline)
        flushChunk()
        return parent
    }

    /** Only called by compiler plugin. The literal ends with a tag inside the region, so no text boundary is pending. */
    @PublishedApi
    internal fun advanceLoweredRegion(
        encoded: String,
        innermost: String,
        namespace: String = HtmlNamespace,
        leadingNewline: Boolean = false,
    ) {
        checkNotFinished()
        if (discardingContent) return
        output.append(encoded)
        currentTagName = innermost
        currentNamespace = namespace
        previousSiblingWasText = false
        pendingLeadingNewline = leadingNewline
        flushChunk()
    }

    /**
     * Only called by compiler plugin. Begins an element whose attributes are written directly;
     * their names and syntax have already been validated by the compiler.
     */
    @PublishedApi
    internal fun beginMixedElement(tagName: String, validate: Boolean): Boolean {
        checkNotFinished()
        if (discardingContent) return false
        beginChild(tagName, validate)
        return true
    }

    /** Only called by compiler plugin. Appends compiler-validated attribute syntax. */
    @PublishedApi
    internal fun writeMixedLiteral(encoded: String) {
        output.append(encoded)
    }

    /** Only called by compiler plugin. Escapes and writes a dynamic attribute value. */
    @PublishedApi
    internal fun writeMixedAttribute(prefix: String, value: String?, boolean: Boolean) {
        if (value == null) return
        output.append(prefix)
        if (!boolean || value.isNotEmpty()) {
            output.append("=\"")
            output.appendEscapedAttribute(value)
            output.append('"')
        }
    }

    /** Only called by compiler plugin. Writes collected inline styles. */
    @PublishedApi
    internal fun writeMixedStyle(scope: StyleScopeBuilder?) {
        writeMixedAttribute(" style", scope?.toStyleAttributeValue(), false)
    }

    /** Only called by compiler plugin. Applies strict class-token checks. */
    @PublishedApi
    internal fun validateMixedClass(value: String?, validate: Boolean, duplicate: Boolean) {
        if (!validate || value == null) return
        require(!duplicate) { "Class token must not be repeated: \"$value\"" }
        require(value.isNotEmpty()) { "Class token must not be empty" }
        require(value.none { it in AsciiWhitespaceCharacters }) {
            "Class token must not contain ASCII whitespace: \"$value\""
        }
    }

    /** Only called by compiler plugin. Appends one class token. */
    @PublishedApi
    internal fun writeMixedClass(value: String?, previous: Boolean, encoded: Boolean): Boolean {
        if (value == null) return previous
        output.append(if (previous) " " else " class=\"")
        if (encoded) output.append(value) else output.appendEscapedAttribute(value)
        return true
    }

    /** Only called by compiler plugin. Closes a class attribute if any tokens were written. */
    @PublishedApi
    internal fun finishMixedClasses(written: Boolean) {
        if (written) output.append('"')
    }

    /** Only called by compiler plugin. Enters the element after its attributes have been written. */
    @PublishedApi
    internal fun finishMixedElement(
        tagName: String,
        namespace: String = HtmlNamespace,
        leadingNewline: Boolean = false,
    ): Any? {
        output.append('>')
        if (isHtmlVoidElement(tagName, namespace)) {
            previousSiblingWasText = false
            pendingLeadingNewline = false
            flushChunk()
            return null
        }
        val parent = enter(tagName, namespace, leadingNewline)
        flushChunk()
        return parent
    }

    /** Only called by compiler plugin. */
    @PublishedApi
    internal fun closeElement(encoded: String, parent: Any?) {
        checkNotFinished()
        if (discardingContent) return
        output.append(encoded)
        exit(parent)
        flushChunk()
    }

    internal fun beginChild(tagName: String, validate: Boolean) {
        require(!collectingRawText) { "String rendering does not support element children inside <$currentTagName>" }
        if (validate && currentNamespace == HtmlNamespace) requireHtmlParserStableTableElement(currentTagName, tagName)
        if (currentTagName == null) recordRootChild(isHtml = tagName == "html")
        flushChunk()
    }

    internal fun enter(tagName: String, namespace: String, leadingNewline: Boolean): Any? {
        // The identity check keeps String.equals out of compiled region openings so the JVM can inline them.
        val parent = if (currentNamespace === HtmlNamespace || currentNamespace == HtmlNamespace) currentTagName
        else ForeignParent(currentTagName, currentNamespace)
        if (currentTagName !== tagName) currentTagName = tagName
        if (currentNamespace !== namespace) currentNamespace = namespace
        previousSiblingWasText = false
        pendingLeadingNewline = leadingNewline
        return parent
    }

    internal fun exit(parent: Any?) {
        val tagName = if (parent is ForeignParent) parent.tagName else parent as String?
        val namespace = if (parent is ForeignParent) parent.namespace else HtmlNamespace
        if (currentTagName !== tagName) currentTagName = tagName
        if (currentNamespace !== namespace) currentNamespace = namespace
        previousSiblingWasText = false
        pendingLeadingNewline = false // This element's start tag consumed its parent's first content.
    }

    // Ordinary HTML parents stay as tag names, with no allocation on the common path.
    internal class ForeignParent(val tagName: String?, val namespace: String)

    /** Used by compiler-generated calls and ordinary Text rendering. */
    @PublishedApi
    internal fun writeText(value: String, validate: Boolean) {
        checkNotFinished()
        when {
            discardingContent -> Unit
            collectingRawText -> {
                // Empty Text calls still require validation at the end tag.
                hasRawTextCall = true
                rawText.append(value)
            }

            value.isNotEmpty() -> {
                appendText(value, validate)
                flushChunk()
            }
        }
    }

    @Composable
    @NonRestartableComposable
    @ExplicitGroupsComposable
    override fun <TElement : Element> RawTextElement(
        tagName: String,
        applyAttrs: (AttrsScope<TElement>.() -> Unit)?,
        content: RawTextContent,
    ) {
        TagElement(elementBuilder<TElement>(tagName), applyAttrs, rawTextContent = content, content = null)
    }

    @Composable
    @NonRestartableComposable
    @ExplicitGroupsComposable
    override fun TextElement(value: String) {
        writeText(value, LocalHtmlValidationMode.current == HtmlValidationMode.Strict)
    }

    @Composable
    @NonRestartableComposable
    @ExplicitGroupsComposable
    override fun StyleElement(
        applyAttrs: (AttrsScope<HTMLStyleElement>.() -> Unit)?,
        cssRules: CSSRuleDeclarationList,
    ) {
        RawTextElement("style", applyAttrs, prepareStyleRawTextContent(cssRules))
    }

    /** Emits the final chunk or returns the complete HTML string, then rejects further writes. */
    fun finish(requireHtmlDocumentRoot: Boolean): String {
        check(currentTagName == null) { "String rendering finished inside <$currentTagName>" }
        finished = true
        if (requireHtmlDocumentRoot) {
            require(rootChildCount == 1 && firstRootChildIsHtml) {
                "Hydrated document content must produce exactly one html element"
            }
        }
        attrsBuilders.trim(attrsBuilderIndex)
        if (!streaming) {
            peakBufferSize = immutableOutput?.length ?: output.length
            return immutableOutput ?: output.toString()
        }

        flushChunk(force = true)
        return ""
    }

    private fun <TElement : Element> writeStartTag(
        tagName: String,
        namespace: String,
        applyAttrs: (AttrsScope<TElement>.() -> Unit)?,
        validate: Boolean,
        tagNameIsValid: Boolean,
    ): StartTagResult {
        if (validate && !tagNameIsValid) requireValidHtmlTagName(tagName)
        beginChild(tagName, validate)

        val builder = attrsBuilders.builder<TElement>(attrsBuilderIndex++)
        try {
            applyAttrs?.invoke(builder)
            val formState = builder.propertyUpdatesOrEmpty.formControlState(tagName, namespace)
            val hasScriptSource =
                output.appendStartTag(tagName, namespace, builder, validate, formState, attrsBuilders.nameCache)
            pendingLeadingNewline = false // A child tag starts with '<', which needs no LF repair.
            return if (formState?.textContent == null && !hasScriptSource) StartTagResult.Ordinary
            else StartTagResult(formState?.textContent, hasScriptSource)
        } finally {
            builder.reset()
        }
    }

    // Reuse the ordinary result instead of creating a Pair for every tag.
    // Allocation stays predictable when the JVM cannot eliminate temporary objects.
    private class StartTagResult(val textContent: String?, val hasScriptSource: Boolean) {
        companion object {
            // Ordinary tags need neither a text override nor script-source validation.
            val Ordinary = StartTagResult(null, false)
        }
    }

    private fun writeEndTag(tagName: String, namespace: String, hasScriptSource: Boolean, contentStart: Int) {
        if (hasRawTextCall) {
            val content = RawTextContent.create(tagName, rawText.toString())
            content.validateScriptSource(hasScriptSource)
            output.append(content.text)
            hasRawTextCall = false
            rawText.setLength(0)
        }
        output.appendEndTag(tagName, namespace, contentStart)
    }

    internal fun appendText(value: String, validate: Boolean) {
        if (currentTagName == null) recordRootChild(isHtml = false)
        if (validate && currentNamespace == HtmlNamespace) requireHtmlParserStableTableText(currentTagName, value)
        if (pendingLeadingNewline) {
            if (value.first() == '\n') output.append('\n')
            pendingLeadingNewline = false
        }
        // RCDATA decodes escaped text, but treats boundary comments as literal content.
        if (hydratable && previousSiblingWasText && !isHtmlRcdataElement(currentTagName, currentNamespace)) {
            output.appendHydrationTextBoundaryMarker()
        }
        output.appendEscapedText(value)
        previousSiblingWasText = true
    }

    /** Emits the shared buffer at a safe boundary once it reaches the target size. */
    @PublishedApi
    internal fun flushChunk(force: Boolean = false) {
        val chunkSink = chunkSink ?: return
        // A buffered element validates its content when its end tag is written.
        if (bufferedElementDepth != 0 || output.isEmpty() || (!force && output.length < chunkSize)) return

        peakBufferSize = maxOf(peakBufferSize, output.length)
        chunkSink(output)
        output.setLength(0)
    }

    internal fun recordRootChild(isHtml: Boolean) {
        if (rootChildCount++ == 0) firstRootChildIsHtml = isHtml
    }

    internal fun checkNotFinished() {
        check(!finished) { "String rendering does not support HTML emission after composition" }
        immutableOutput?.let {
            output.append(it)
            immutableOutput = null
        }
    }

    private fun <TElement : Element> elementScope(): ElementScope<TElement> = elementScope.unsafeCast()
}

/** Reuses attribute builders by element order without retaining their values between elements. */
internal class AttrsBuilderPool {
    val nameCache = HtmlAttributeNameCache()
    private val builders = mutableListOf<AttrsScopeBuilder<*>>()

    // Reuse immutable metadata for SVG elements and keyed renders. Bound both the number
    // and length of retained dynamic names. Only one active renderer owns this storage.
    private val elementBuilders = HashMap<String, MutableMap<String, StringElementBuilder<*>>>()
    private var elementBuilderCount = 0

    fun <TElement : Element> elementBuilder(tagName: String, namespace: String): ElementBuilder<TElement> {
        elementBuilders[namespace]?.get(tagName)?.let { return it.unsafeCast() }
        val builder = StringElementBuilder<TElement>(tagName, namespace)
        if (elementBuilderCount < 128 && tagName.length <= 128 && namespace.length <= 256) {
            elementBuilders.getOrPut(namespace) { HashMap() }[tagName] = builder
            elementBuilderCount++
        }
        return builder
    }

    fun <TElement : Element> builder(index: Int): AttrsScopeBuilder<TElement> {
        if (index == builders.size) builders.add(AttrsScopeBuilder<Element>())
        return builders[index].unsafeCast()
    }

    /** Drops builders beyond [size] when the last successful render used fewer elements. */
    fun trim(size: Int) {
        if (size < builders.size) builders.subList(size, builders.size).clear()
    }
}

/** Only called by compiler plugin. */
@PublishedApi
internal fun mixedStyle(scope: StyleScopeBuilder?, block: StyleScope.() -> Unit): StyleScopeBuilder =
    (scope ?: StyleScopeBuilder()).apply(block)
