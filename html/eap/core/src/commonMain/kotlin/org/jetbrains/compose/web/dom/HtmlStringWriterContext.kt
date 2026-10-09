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
        StringElementBuilder(tagName)

    override fun <TElement : Element> elementBuilderNS(
        tagName: String,
        namespace: String,
    ): ElementBuilder<TElement> = StringElementBuilder(tagName, namespace)

    @Composable
    @NonRestartableComposable
    @ExplicitGroupsComposable
    override fun <TElement : Element> TagElement(
        elementBuilder: ElementBuilder<TElement>,
        applyAttrs: (AttrsScope<TElement>.() -> Unit)?,
        content: (@Composable ElementScope<TElement>.() -> Unit)?,
    ) {
        checkNotFinished()
        if (discardingContent) {
            content?.invoke(elementScope())
            return
        }

        val tagName = elementBuilder.tagName
        val namespace = (elementBuilder as? StringElementBuilder<*>)?.namespace ?: HtmlNamespace

        // Emit any completed chunk before writing this start tag.
        flushChunk()
        val bufferElement = streaming &&
            (isHtmlRawTextElement(tagName, namespace) || (namespace == HtmlNamespace && tagName == "noscript"))
        if (bufferElement) bufferedElementDepth++

        val (hasScriptSource, contentOverride) = writeStartTag(
            tagName = tagName,
            namespace = namespace,
            applyAttrs = applyAttrs,
            validate = LocalHtmlValidationMode.current == HtmlValidationMode.Strict,
        )
        // A raw-text parent rejects child tags, and a void parent returned above.
        // Only the parent's tag and namespace need saving.
        val parentTagName = currentTagName
        val parentNamespace = currentNamespace
        val isVoid = isHtmlVoidElement(tagName, namespace)
        currentTagName = tagName
        currentNamespace = namespace
        previousSiblingWasText = false
        pendingLeadingNewline = isHtmlLeadingNewlineElement(tagName, namespace)
        collectingRawText = isHtmlRawTextElement(tagName, namespace)

        flushChunk()
        // For buffered elements this index stays valid until appendEndTag inspects the content.
        val contentStart = output.length
        if (!contentOverride.isNullOrEmpty()) writeText(contentOverride)
        discardingContent = isVoid || contentOverride != null
        content?.invoke(elementScope())

        if (!isVoid) writeEndTag(tagName, namespace, hasScriptSource, contentStart)
        currentTagName = parentTagName
        currentNamespace = parentNamespace
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
        TagElement(elementBuilder<TElement>(tagName), applyAttrs) {
            TextElement(content.text)
        }
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
                writeText(value)
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
    ): Pair<Boolean, String?> {
        requireValidHtmlTagName(tagName)
        require(!collectingRawText) {
            "String rendering does not support element children inside <$currentTagName>"
        }
        if (currentTagName == null) recordRootChild(isHtml = tagName == "html")
        if (currentNamespace == HtmlNamespace) requireHtmlParserStableTableElement(currentTagName, tagName)

        val builder = attrsBuilders.builder<TElement>(attrsBuilderIndex++)
        try {
            applyAttrs?.invoke(builder)
            val formState = builder.propertyUpdatesOrEmpty.formControlState(tagName, namespace)
            val hasScriptSource = output.appendStartTag(tagName, namespace, builder, validate, formState)
            pendingLeadingNewline = false // A child tag starts with '<', which needs no LF repair.
            return hasScriptSource to formState?.textContent
        } finally {
            builder.reset()
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

    private fun writeText(value: String) {
        if (currentTagName == null) recordRootChild(isHtml = false)
        if (currentNamespace == HtmlNamespace) requireHtmlParserStableTableText(currentTagName, value)
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
    private val builders = mutableListOf<AttrsScopeBuilder<*>>()

    fun <TElement : Element> builder(index: Int): AttrsScopeBuilder<TElement> {
        if (index == builders.size) builders.add(AttrsScopeBuilder<Element>())
        return builders[index].unsafeCast()
    }

    /** Drops builders beyond [size] when the last successful render used fewer elements. */
    fun trim(size: Int) {
        if (size < builders.size) builders.subList(size, builders.size).clear()
    }
}
