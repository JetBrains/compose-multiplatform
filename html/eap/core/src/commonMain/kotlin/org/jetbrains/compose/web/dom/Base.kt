/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.dom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.browser.dom.Element
import kotlinx.browser.dom.HTMLStyleElement
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.css.CSSRuleDeclarationList
import org.jetbrains.compose.web.css.utils.serializeRules


// Match the HTML parser's ASCII-only attribute-name normalization in both render paths.
internal fun Map<String, String>.containsAttribute(name: String): Boolean =
    keys.any { it.asciiLowercase() == name }

internal interface ComposeHtmlContext {
    val supportsDomElementAccess: Boolean

    fun <TElement : Element> elementBuilder(tagName: String): ElementBuilder<TElement>

    @Composable
    fun <TElement : Element> TagElement(
        elementBuilder: ElementBuilder<TElement>,
        applyAttrs: (AttrsScope<TElement>.() -> Unit)?,
        content: (@Composable ElementScope<TElement>.() -> Unit)?,
    )

    @Composable
    fun <TElement : Element> RawTextElement(
        tagName: String,
        applyAttrs: (AttrsScope<TElement>.() -> Unit)?,
        content: RawTextContent,
    )

    @Composable
    fun TextElement(value: String)

    @Composable
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
fun <TElement : Element> TagElement(
    elementBuilder: ElementBuilder<TElement>,
    applyAttrs: (AttrsScope<TElement>.() -> Unit)?,
    content: (@Composable ElementScope<TElement>.() -> Unit)?,
) {
    LocalComposeHtmlContext.current.TagElement(
        elementBuilder = elementBuilder,
        applyAttrs = applyAttrs,
        content = content,
    )
}

/**
 * @param tagName - the name of the tag that needs to be created.
 * It's best to use constant values for [tagName].
 * If variable [tagName] needed, consider wrapping TagElement calls into an if...else:
 *
 * ```
 *      if (useDiv) TagElement("div", ...) else TagElement("span", ...)
 * ```
 */
@Composable
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
            content = content,
        )
    }
}

@Composable
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
        content = cssRules.serializeRules().joinToString("\n"),
    )

internal class RawTextContent private constructor(
    val tagName: String,
    val text: String,
) {
    fun validateAttributes(attributes: Map<String, String>) {
        if (tagName == "script") {
            require(attributes.keys.none { it.equals("src", ignoreCase = true) }) {
                "Inline <script> content cannot be combined with a src attribute"
            }
        }
    }

    companion object {
        fun create(tagName: String, content: String): RawTextContent {
            val normalizedTagName = tagName.asciiLowercase()
            val normalizedContent = content.normalizeHtmlInputCharacters()
            requireValidRawTextContent(normalizedTagName, normalizedContent)
            return RawTextContent(normalizedTagName, normalizedContent)
        }
    }
}

internal fun String.normalizeHtmlInputCharacters(): String =
    if ('\r' in this || '\u0000' in this) {
        replace("\r\n", "\n")
            .replace('\r', '\n')
            .replace('\u0000', '\uFFFD')
    } else {
        this
    }

private fun requireValidRawTextContent(tagName: String, content: String) {
    val endTag = Regex("</$tagName(?=[\\t\\n\\u000C\\r />])", RegexOption.IGNORE_CASE)
    require(!endTag.containsMatchIn(content)) {
        "Raw text for <$tagName> must not contain a </$tagName end tag"
    }

    if (tagName == "script") {
        val scriptStart = Regex("<script(?=[\\t\\n\\u000C\\r />])", RegexOption.IGNORE_CASE)
        var escapedStart = content.indexOf("<!--")
        while (escapedStart >= 0) {
            // Include the opener's dashes: <!--> also exits the escaped state.
            val escapedEnd = content.indexOf("-->", escapedStart + 2)
            val script = scriptStart.find(content, escapedStart + 4)
            require(script == null || (escapedEnd >= 0 && script.range.first > escapedEnd)) {
                "Raw text for <script> must not contain a <script tag inside <!-- escaped text"
            }
            if (escapedEnd < 0) break
            escapedStart = content.indexOf("<!--", escapedEnd + 3)
        }
    }
}
