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
    private val attrsBuilders: AttrsBuilderPool? = null,
    private val output: StringBuilder = StringBuilder(),
    private val sink: ((String) -> Unit)? = null,
    private val chunkSize: Int = 4096,
) : ComposeHtmlContext {
    private val elementScope = StringElementScope<Element>()
    private var finished = false
    // These elements must keep their content in the shared buffer until their end tag is written.
    private var bufferedElementDepth = 0

    // Attribute builders are reused by element position across keyed renders.
    private var attrsBuilderIndex = 0

    // A null tag name means that output is being written at the root.
    private var currentTagName: String? = null
    private var currentNamespace = HtmlNamespace
    private var previousSiblingWasText = false

    // Children of a void element still compose, but must not produce output.
    private var discardingVoidContent = false

    // Raw text is validated at the end tag. Raw-text elements cannot nest, so one buffer suffices.
    private var collectingRawText = false
    private var hasRawTextCall = false
    private val rawText = StringBuilder()

    // renderHydratedDocument requires exactly one html root element.
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
        if (discardingVoidContent) {
            content?.invoke(elementScope())
            return
        }

        val tagName = elementBuilder.tagName
        val namespace = (elementBuilder as? StringElementBuilder<*>)?.namespace ?: HtmlNamespace

        // Emit any completed chunk before writing this start tag.
        flushChunk()
        val bufferElement = sink != null && namespace == HtmlNamespace &&
            (isHtmlRawTextElement(tagName, namespace) || tagName == "noscript" ||
                isHtmlLeadingNewlineElement(tagName, namespace))
        if (bufferElement) bufferedElementDepth++

        val attributes = writeStartTag(
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
        discardingVoidContent = isVoid
        collectingRawText = isHtmlRawTextElement(tagName, namespace)

        flushChunk()
        // For buffered elements this index stays valid until appendEndTag inspects the content.
        val contentStart = output.length
        content?.invoke(elementScope())

        if (!isVoid) writeEndTag(tagName, namespace, attributes, contentStart)
        currentTagName = parentTagName
        currentNamespace = parentNamespace
        previousSiblingWasText = false
        discardingVoidContent = false
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
            discardingVoidContent -> Unit
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
                "renderHydratedDocument content must produce exactly one html element"
            }
        }
        attrsBuilders?.trim(attrsBuilderIndex)
        if (sink == null) return output.toString()

        flushChunk(force = true)
        return ""
    }

    private fun <TElement : Element> writeStartTag(
        tagName: String,
        namespace: String,
        applyAttrs: (AttrsScope<TElement>.() -> Unit)?,
        validate: Boolean,
    ): Map<String, String> {
        requireValidHtmlTagName(tagName)
        require(!collectingRawText) {
            "String rendering does not support element children inside <$currentTagName>"
        }
        if (currentTagName == null) recordRootChild(isHtml = tagName == "html")
        if (currentNamespace == HtmlNamespace) requireHtmlParserStableTableElement(currentTagName, tagName)

        val attrsBuilder = attrsBuilders?.builder<TElement>(attrsBuilderIndex++) ?: AttrsScopeBuilder()
        applyAttrs?.invoke(attrsBuilder)
        // The serialized attributes are a copy, so a pooled builder can be reset right away.
        val attributes = attrsBuilder.stringAttributes(namespace, validate).byName
        if (attrsBuilders != null) attrsBuilder.reset()

        output.appendStartTag(tagName, namespace, attributes)
        return attributes
    }

    private fun writeEndTag(
        tagName: String,
        namespace: String,
        attributes: Map<String, String>,
        contentStart: Int,
    ) {
        if (hasRawTextCall) {
            val content = RawTextContent.create(tagName, rawText.toString())
            content.validateAttributes(attributes)
            output.append(content.text)
            hasRawTextCall = false
            rawText.setLength(0)
        }
        output.appendEndTag(tagName, namespace, contentStart)
    }

    private fun writeText(value: String) {
        if (currentTagName == null) recordRootChild(isHtml = false)
        if (currentNamespace == HtmlNamespace) requireHtmlParserStableTableText(currentTagName, value)
        // RCDATA decodes escaped text, but treats boundary comments as literal content.
        if (hydratable && previousSiblingWasText && !isHtmlRcdataElement(currentTagName, currentNamespace)) {
            output.appendHydrationTextBoundaryMarker()
        }
        output.appendEscapedText(value)
        previousSiblingWasText = true
    }

    /** Emits the shared buffer at a safe boundary once it reaches the target size. */
    private fun flushChunk(force: Boolean = false) {
        val write = sink ?: return
        // A buffered element may inspect or insert into its content when its end tag is written.
        if (bufferedElementDepth != 0 || output.isEmpty() || (!force && output.length < chunkSize)) return

        val chunk = output.toString()
        output.setLength(0)
        write(chunk)
    }

    private fun recordRootChild(isHtml: Boolean) {
        if (rootChildCount++ == 0) firstRootChildIsHtml = isHtml
    }

    private fun checkNotFinished() {
        check(!finished) { "String rendering does not support HTML emission after composition" }
    }

    private fun <TElement : Element> elementScope(): ElementScope<TElement> = elementScope.unsafeCast()
}

/** Reuses attribute builders across keyed renders, indexed by element order, without retaining their values. */
internal class AttrsBuilderPool {
    private val builders = mutableListOf<AttrsScopeBuilder<*>>()

    fun <TElement : Element> builder(index: Int): AttrsScopeBuilder<TElement> {
        if (index == builders.size) builders.add(AttrsScopeBuilder<Element>())
        return builders[index].unsafeCast()
    }

    /** Drops builders beyond [size] that the last render no longer needed. */
    fun trim(size: Int) {
        if (size < builders.size) builders.subList(size, builders.size).clear()
    }
}
