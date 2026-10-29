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
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.browser.dom.Element
import kotlinx.browser.dom.HTMLStyleElement
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.css.CSSRuleDeclarationList
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi

internal interface ComposeHtmlContext {
    val supportsDomElementAccess: Boolean

    fun <TElement : Element> elementBuilder(tagName: String): ElementBuilder<TElement>

    fun <TElement : Element> elementBuilderNS(
        tagName: String,
        namespace: String,
    ): ElementBuilder<TElement>

    // Forward without adding a restart group per element.
    @Composable
    @NonRestartableComposable
    fun <TElement : Element> TagElement(
        elementBuilder: ElementBuilder<TElement>,
        applyAttrs: (AttrsScope<TElement>.() -> Unit)?,
        rawTextContent: RawTextContent?,
        content: (@Composable ElementScope<TElement>.() -> Unit)?,
    )

    @Composable
    @NonRestartableComposable
    fun <TElement : Element> RawTextElement(
        tagName: String,
        applyAttrs: (AttrsScope<TElement>.() -> Unit)?,
        content: RawTextContent,
    )

    @Composable
    @NonRestartableComposable
    fun TextElement(value: String)

    @Composable
    @NonRestartableComposable
    fun StyleElement(
        applyAttrs: (AttrsScope<HTMLStyleElement>.() -> Unit)?,
        cssRules: CSSRuleDeclarationList,
    )
}

// Define default per platform, e.g. [BrowserComposeHtmlContext] for web
internal expect val DefaultComposeHtmlContext: ComposeHtmlContext

internal val LocalComposeHtmlContext = staticCompositionLocalOf<ComposeHtmlContext> {
    DefaultComposeHtmlContext
}

@Composable
@NonRestartableComposable
@ExplicitGroupsComposable
fun <TElement : Element> TagElement(
    elementBuilder: ElementBuilder<TElement>,
    applyAttrs: (AttrsScope<TElement>.() -> Unit)?,
    content: (@Composable ElementScope<TElement>.() -> Unit)?,
) {
    LocalComposeHtmlContext.current.TagElement(
        elementBuilder = elementBuilder,
        applyAttrs = applyAttrs,
        rawTextContent = null,
        content = content,
    )
}

/**
 * Creates an element identified by both its local [tagName] and [namespace].
 * The tag name is used exactly as supplied.
 */
@Composable
@NonRestartableComposable
@ExplicitGroupsComposable
@ComposeWebInternalApi
fun <TElement : Element> TagElementNS(
    tagName: String,
    namespace: String,
    applyAttrs: (AttrsScope<TElement>.() -> Unit)?,
    content: (@Composable ElementScope<TElement>.() -> Unit)?,
) {
    val context = LocalComposeHtmlContext.current

    key(namespace, tagName) {
        context.TagElement(
            elementBuilder = context.elementBuilderNS(tagName, namespace),
            applyAttrs = applyAttrs,
            rawTextContent = null,
            content = content,
        )
    }
}

/**
 * @param tagName - the name of the tag that needs to be created.
 * Use canonical lowercase spelling for HTML tags. Others can produce mismatches.
 * It's best to use constant values for [tagName].
 * If variable [tagName] needed, consider wrapping TagElement calls into an if...else:
 *
 * ```
 *      if (useDiv) TagElement("div", ...) else TagElement("span", ...)
 * ```
 */
@Composable
@NonRestartableComposable
@ExplicitGroupsComposable
fun <TElement : Element> TagElement(
    tagName: String,
    applyAttrs: (AttrsScope<TElement>.() -> Unit)?,
    content: (@Composable ElementScope<TElement>.() -> Unit)?,
) {
    val context = LocalComposeHtmlContext.current

    key(tagName) {
        context.TagElement(
            elementBuilder = context.elementBuilder(tagName),
            applyAttrs = applyAttrs,
            rawTextContent = null,
            content = content,
        )
    }
}

@Composable
@NonRestartableComposable
internal fun <TElement : Element> RawTextElement(
    tagName: String,
    applyAttrs: (AttrsScope<TElement>.() -> Unit)?,
    content: String,
) {
    val rawTextContent = remember(tagName, content) {
        RawTextContent.create(tagName, content)
    }
    val context = LocalComposeHtmlContext.current

    key(tagName) {
        context.RawTextElement(
            tagName = tagName,
            applyAttrs = applyAttrs,
            content = rawTextContent,
        )
    }
}

internal fun prepareStyleRawTextContent(cssRules: CSSRuleDeclarationList): RawTextContent =
    RawTextContent.create(
        tagName = "style",
        content = buildString {
            cssRules.forEachIndexed { index, rule ->
                if (index != 0) append('\n')
                appendRulePresentation(rule, baseIndent = "", indent = " ", delimiter = "")
            }
        },
    )

internal class RawTextContent private constructor(
    val tagName: String,
    val text: String,
) {
    fun validateAttributes(attributes: Map<String, String>) {
        if (tagName == "script") {
            validateScriptSource(attributes.keys.any { it.equals("src", ignoreCase = true) })
        }
    }

    fun validateScriptSource(hasSource: Boolean) {
        require(tagName != "script" || !hasSource) {
            "Inline <script> content cannot be combined with a src attribute"
        }
    }

    companion object {
        fun create(tagName: String, content: String): RawTextContent {
            val normalizedContent = content.normalizeHtmlInputCharacters()
            requireValidRawTextContent(tagName, normalizedContent)
            return RawTextContent(tagName, normalizedContent)
        }
    }
}
