/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

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
import org.jetbrains.compose.web.internal.unsafeCast

/**
 * Writes HTML while composables execute.
 *
 * Each [TagElement] call writes its start tag, composes its children, then writes its end tag.
 * The fields describe the currently open element. Parent state stays in local variables on the
 * call stack.
 */
internal class HtmlStringWriterContext(
    private val hydratable: Boolean,
    private val attrsBuilders: AttrsBuilderPool = AttrsBuilderPool(),
    private val output: StringBuilder = StringBuilder(),
    private val chunkSink: ((StringBuilder) -> Unit)? = null,
    private val chunkSize: Int = 2048,
) : ComposeHtmlContext {
    private val streaming = chunkSink != null

    // Used to trim retained output storage.
    var peakBufferSize: Int = 0
        private set
    private val elementScope = StringElementScope<Element>()
    private var finished = false

    // These elements must keep their content in the shared buffer until their end tag is written.
    private var bufferedElementDepth = 0

    // Reuse attribute channels initialized for each element position across keyed renders.
    private var attrsBuilderIndex = 0

    // A null tag name means that output is being written at the root.
    private var currentTagName: String? = null
    private var currentNamespace = HtmlNamespace
    private var previousSiblingWasText = false

    // The HTML parser drops the first LF in pre, textarea, and listing. Repair it when the
    // first content is written, so these elements can stream without retaining their children.
    private var pendingLeadingNewline = false

    // Don't produce children's HTML, but still execute their composables.
    private var discardingContent = false

    // Raw text is validated at the end tag. Raw-text elements cannot nest, so one buffer suffices.
    private var collectingRawText = false
    private var hasRawTextCall = false
    private val rawText = StringBuilder()

    // Hydrated document renderers require exactly one html root element.
    private var rootChildCount = 0
    private var firstRootChildIsHtml = false

    override val supportsDomElementAccess: Boolean = false

    override fun <TElement : Element> elementBuilder(tagName: String): ElementBuilder<TElement> =
        attrsBuilders.elementBuilder(tagName, HtmlNamespace)

    override fun <TElement : Element> elementBuilderNS(
        tagName: String,
        namespace: String,
    ): ElementBuilder<TElement> = attrsBuilders.elementBuilder(tagName, namespace)

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
        if (!contentOverride.isNullOrEmpty()) writeText(contentOverride, validate)
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
        checkNotFinished()
        when {
            discardingContent -> Unit
            collectingRawText -> {
                // Even an empty text call must go through raw-text validation at the end tag.
                hasRawTextCall = true
                rawText.append(value)
            }

            value.isNotEmpty() -> {
                writeText(value, LocalHtmlValidationMode.current == HtmlValidationMode.Strict)
                flushChunk()
            }
        }
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
            peakBufferSize = output.length
            return output.toString()
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
        require(!collectingRawText) {
            "String rendering does not support element children inside <$currentTagName>"
        }
        if (currentTagName == null) recordRootChild(isHtml = tagName == "html")
        if (validate && currentNamespace == HtmlNamespace) requireHtmlParserStableTableElement(currentTagName, tagName)

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

    private fun writeEndTag(
        tagName: String,
        namespace: String,
        hasScriptSource: Boolean,
        contentStart: Int,
    ) {
        if (hasRawTextCall) {
            val content = RawTextContent.create(tagName, rawText.toString())
            content.validateScriptSource(hasScriptSource)
            output.append(content.text)
            hasRawTextCall = false
            rawText.setLength(0)
        }
        output.appendEndTag(tagName, namespace, contentStart)
    }

    private fun writeText(value: String, validate: Boolean) {
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
    private fun flushChunk(force: Boolean = false) {
        val chunkSink = chunkSink ?: return
        // A buffered element validates its content when its end tag is written.
        if (bufferedElementDepth != 0 || output.isEmpty() || (!force && output.length < chunkSize)) return

        peakBufferSize = maxOf(peakBufferSize, output.length)
        chunkSink(output)
        output.setLength(0)
    }

    private fun recordRootChild(isHtml: Boolean) {
        if (rootChildCount++ == 0) firstRootChildIsHtml = isHtml
    }

    private fun checkNotFinished() {
        check(!finished) { "String rendering does not support HTML emission after composition" }
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
