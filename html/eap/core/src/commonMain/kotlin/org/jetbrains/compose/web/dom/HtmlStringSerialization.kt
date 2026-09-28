/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.dom

import org.jetbrains.compose.web.HydrationProtocolAttributes

// HTML parsing merges adjacent non-empty text nodes. This comment preserves their boundary.
internal const val HydrationTextBoundaryMarker = "c"

private val HtmlRawTextElementNames = setOf(
    "script", "style", "iframe", "xmp", "noembed", "noframes",
)

internal fun isHtmlRawTextElement(tagName: String?, namespace: String?): Boolean =
    namespace == HtmlNamespace && tagName in HtmlRawTextElementNames

internal fun isHtmlRcdataElement(tagName: String?, namespace: String?): Boolean =
    namespace == HtmlNamespace && (tagName == "title" || tagName == "textarea")

private val HtmlVoidElementNames = setOf(
    "area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "param", "source", "track", "wbr",
)

// HTML void elements have no content and no end tag.
internal fun isHtmlVoidElement(tagName: String, namespace: String): Boolean =
    namespace == HtmlNamespace && tagName in HtmlVoidElementNames

// HTML parsing discards the first LF in these elements.
private val HtmlLeadingNewlineElementNames = setOf("pre", "textarea", "listing")

// Only empty boolean attribute values may be minimized without losing their value.
internal val HtmlBooleanAttributeNames = setOf(
    "allowfullscreen",
    "async",
    "autofocus",
    "autoplay",
    "checked",
    "controls",
    "default",
    "defer",
    "disabled",
    "formnovalidate",
    "inert",
    "ismap",
    "itemscope",
    "loop",
    "multiple",
    "muted",
    "nomodule",
    "novalidate",
    "open",
    "playsinline",
    "readonly",
    "required",
    "reversed",
    "selected",
)

internal fun String.isHtmlBooleanAttributeName(): Boolean =
    this in HtmlBooleanAttributeNames

internal class StringHtmlAttributes private constructor(
    val byName: Map<String, String>,
) {
    override fun equals(other: Any?): Boolean =
        this === other || other is StringHtmlAttributes && byName == other.byName

    override fun hashCode(): Int = byName.hashCode()

    companion object {
        private val Empty = StringHtmlAttributes(emptyMap())

        fun from(
            attributes: Map<String, String>,
            namespace: String,
            validate: Boolean,
            hydrationProtocolAttributes: Set<String>,
            classAttributeValue: String? = null,
            styleAttributeValue: (() -> String?)? = null,
        ): StringHtmlAttributes {
            if (attributes.isEmpty() && classAttributeValue == null && styleAttributeValue == null) return Empty

            val serializedAttributes = linkedMapOf<String, String>()
            attributes.forEach { (name, value) ->
                requireValidHtmlAttributeName(name)
                if (validate && namespace != HtmlNamespace) {
                    val duplicateName = serializedAttributes.keys.firstOrNull {
                        it.hasSameHtmlParserAttributeName(name)
                    }
                    require(duplicateName == null) {
                        "Duplicate HTML attribute names \"$duplicateName\" and \"$name\""
                    }
                }
                val protocolName = HydrationProtocolAttributes.firstOrNull {
                    it.equals(name, ignoreCase = true)
                }
                require(
                    protocolName == null || protocolName in hydrationProtocolAttributes
                ) {
                    "Attribute \"$name\" is owned by the Compose hydration protocol"
                }
                serializedAttributes[name] = value
            }

            if ("class" !in serializedAttributes && classAttributeValue != null) {
                serializedAttributes["class"] = classAttributeValue
            }
            if ("style" !in serializedAttributes) {
                styleAttributeValue?.invoke()?.let { value ->
                    serializedAttributes["style"] = value
                }
            }

            return StringHtmlAttributes(serializedAttributes)
        }
    }
}

internal fun StringBuilder.appendStartTag(tagName: String, namespace: String, attributes: Map<String, String>) {
    append('<').append(tagName)
    attributes.forEach { (name, value) ->
        append(' ').append(name)
        if (namespace != HtmlNamespace || value.isNotEmpty() || '-' in tagName || !name.isHtmlBooleanAttributeName()) {
            append("=\"")
            appendEscapedAttribute(value)
            append('"')
        }
    }
    append('>')
}

/** Closes an element whose content was appended after [contentStart]. */
internal fun StringBuilder.appendEndTag(tagName: String, namespace: String, contentStart: Int) {
    if (namespace == HtmlNamespace) {
        if (tagName == "noscript") {
            // Render fallback HTML for scripting-disabled browsers, but keep it inside noscript
            // when scripting is enabled and the parser treats the entire contents as raw text.
            require(substring(contentStart).rawTextTagIndex("noscript", closing = true) < 0) {
                "String-rendered <noscript> content must not contain a </noscript end tag"
            }
        }
        if (tagName in HtmlLeadingNewlineElementNames && length > contentStart && this[contentStart] == '\n') {
            insert(contentStart, '\n')
        }
    }
    append("</").append(tagName).append('>')
}

internal fun StringBuilder.appendHydrationTextBoundaryMarker() {
    append("<!--").append(HydrationTextBoundaryMarker).append("-->")
}

internal fun requireValidHtmlTagName(name: String) {
    require(
        name.firstOrNull()?.isAsciiLetter() == true &&
            name.none { it in AsciiWhitespaceCharacters || it in InvalidHtmlTagNameCharacters }
    ) {
        "Invalid HTML tag name: \"$name\""
    }
}

private fun requireValidHtmlAttributeName(name: String) {
    require(
        name.isNotEmpty() &&
            name.none { it.isISOControl() || it in InvalidHtmlAttributeNameCharacters }
    ) {
        "Invalid HTML attribute name: \"$name\""
    }
}

internal const val AsciiWhitespaceCharacters = "\t\n\u000C\r "
private const val InvalidHtmlTagNameCharacters = "\u0000/>"
private const val InvalidHtmlAttributeNameCharacters = " \"'/>="

private fun Char.isAsciiLetter(): Boolean = this in 'A'..'Z' || this in 'a'..'z'

private fun String.hasSameHtmlParserAttributeName(other: String): Boolean {
    if (length != other.length) return false
    for (index in indices) {
        val first = this[index]
        val second = other[index]
        if (first == second) continue
        if (
            !(first in 'A'..'Z' && second.code == first.code + ('a'.code - 'A'.code)) &&
            !(second in 'A'..'Z' && first.code == second.code + ('a'.code - 'A'.code))
        ) {
            return false
        }
    }
    return true
}

private fun StringBuilder.appendEscapedAttribute(value: String) {
    appendEscaped(value, attribute = true)
}

internal fun StringBuilder.appendEscapedText(value: String) {
    appendEscaped(value, attribute = false)
}

// Append ordinary runs in bulk. Besides avoiding an append per character, this lets the
// JVM copy Latin-1 and UTF-16 runs with its native StringBuilder implementation.
private fun StringBuilder.appendEscaped(value: String, attribute: Boolean) {
    var start = 0
    for (index in value.indices) {
        val replacement = when (value[index]) {
            '\u0000' -> throw IllegalArgumentException(
                if (attribute) "HTML attribute values must not contain NUL (U+0000)"
                else "HTML text must not contain NUL (U+0000)"
            )
            '&' -> "&amp;"
            '<' -> "&lt;"
            '>' -> "&gt;"
            '\r' -> "&#13;"
            '"' -> if (attribute) "&quot;" else null
            '\n' -> if (attribute) "&#10;" else null
            '\t' -> if (attribute) "&#9;" else null
            else -> null
        }
        if (replacement != null) {
            append(value, start, index)
            append(replacement)
            start = index + 1
        }
    }
    append(value, start, value.length)
}
