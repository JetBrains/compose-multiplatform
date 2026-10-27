/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.dom

// HTML rules shared with the compiler plugin.

@PublishedApi
internal const val HtmlNamespace = "http://www.w3.org/1999/xhtml"

// HTML parsing merges adjacent non-empty text nodes. This comment preserves their boundary.
internal const val HydrationTextBoundaryMarker = "c"

internal fun StringBuilder.appendHydrationTextBoundaryMarker() {
    append("<!--").append(HydrationTextBoundaryMarker).append("-->")
}

internal const val AsciiWhitespaceCharacters = "\t\n\u000C\r "

internal fun isValidHtmlTagName(name: String): Boolean =
    name.firstOrNull()?.isAsciiLetter() == true &&
            name.none { it in AsciiWhitespaceCharacters || it in InvalidHtmlTagNameCharacters }

private const val InvalidHtmlTagNameCharacters = "\u0000/>"

private fun Char.isAsciiLetter(): Boolean = this in 'A'..'Z' || this in 'a'..'z'

internal fun htmlTextEscapeReplacement(character: Char): String? = when (character) {
    '\u0000' -> throw IllegalArgumentException("HTML text must not contain NUL (U+0000)")
    '&' -> "&amp;"
    '<' -> "&lt;"
    '>' -> "&gt;"
    '\r' -> "&#13;"
    else -> null
}

internal fun htmlAttributeEscapeReplacement(character: Char): String? = when (character) {
    '\u0000' -> throw IllegalArgumentException("HTML attribute values must not contain NUL (U+0000)")
    '&' -> "&amp;"
    '<' -> "&lt;"
    '>' -> "&gt;"
    '\r' -> "&#13;"
    '"' -> "&quot;"
    '\n' -> "&#10;"
    '\t' -> "&#9;"
    else -> null
}

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

// Prepared containers need an ordinary end tag and the normal text-writing path.
internal fun supportsPreparedHtmlAttributes(tagName: String): Boolean =
    tagName != "noscript" && !isHtmlRawTextElement(tagName, HtmlNamespace) && !isHtmlVoidElement(tagName, HtmlNamespace)

// HTML parsing discards the first LF in these elements.
private val HtmlLeadingNewlineElementNames = setOf("pre", "textarea", "listing")

internal fun isHtmlLeadingNewlineElement(tagName: String, namespace: String): Boolean =
    namespace == HtmlNamespace && tagName in HtmlLeadingNewlineElementNames

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

private val HtmlTableChildNames = mapOf(
    "table" to setOf("caption", "colgroup", "thead", "tbody", "tfoot", "script", "style", "template"),
    "thead" to setOf("tr", "script", "style", "template"),
    "tbody" to setOf("tr", "script", "style", "template"),
    "tfoot" to setOf("tr", "script", "style", "template"),
    "tr" to setOf("td", "th", "script", "style", "template"),
    "colgroup" to setOf("col", "template"),
)

internal fun isHtmlParserStableTableElement(parentTagName: String?, childTagName: String?): Boolean =
    HtmlTableChildNames[parentTagName]?.let { childTagName in it } ?: true

internal fun requireHtmlParserStableTableElement(parentTagName: String?, childTagName: String?) {
    if (isHtmlParserStableTableElement(parentTagName, childTagName)) return

    val suggestion = when {
        parentTagName == "table" && childTagName == "tr" -> "wrap rows in Tbody { }"
        parentTagName == "table" && (childTagName == "td" || childTagName == "th") ->
            "wrap rows in Tbody { } and cells in Tr { }"

        parentTagName == "table" && childTagName == "col" -> "wrap columns in Colgroup { }"
        parentTagName in setOf("tbody", "thead", "tfoot") &&
                (childTagName == "td" || childTagName == "th") -> "wrap cells in Tr { }"

        else -> "use explicit table sections, rows and cells, or move this content outside the table"
    }
    throw IllegalArgumentException(
        "Element <$childTagName> cannot be serialized directly inside <$parentTagName>; " +
                "the HTML parser would insert wrappers, move or discard content. $suggestion",
    )
}

internal fun isHtmlParserStableTableText(parentTagName: String?, text: String): Boolean =
    parentTagName !in HtmlTableChildNames || text.all { it in AsciiWhitespaceCharacters }

internal fun requireHtmlParserStableTableText(parentTagName: String?, text: String) {
    require(isHtmlParserStableTableText(parentTagName, text)) {
        "Non-whitespace text cannot be serialized directly inside <$parentTagName>; " +
                "the HTML parser would move it outside the table. " +
                "Wrap text in Td { } or Th { } inside Tr { }, or move it outside the table"
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

internal fun String.rawTextTagIndex(
    tagName: String,
    closing: Boolean,
    startIndex: Int = 0,
    endIndex: Int = length,
): Int {
    var index = indexOf('<', startIndex)
    while (index >= 0 && index < endIndex) {
        val nameStart = index + if (closing) 2 else 1
        val delimiterIndex = nameStart + tagName.length
        if (
            delimiterIndex < length &&
            (!closing || this[index + 1] == '/') &&
            matchesRawTextTagName(nameStart, tagName) &&
            this[delimiterIndex] in "\t\n\u000C\r />"
        ) {
            return index
        }
        index = indexOf('<', index + 1)
    }
    return -1
}

internal fun isValidRawTextContent(tagName: String, content: String): Boolean =
    invalidRawTextContentReason(tagName, content) == null

internal fun requireValidRawTextContent(tagName: String, content: String) {
    val reason = invalidRawTextContentReason(tagName, content) ?: return
    throw IllegalArgumentException(reason)
}

private fun invalidRawTextContentReason(tagName: String, content: String): String? {
    if (tagName !in HtmlRawTextElementNames) {
        return "Raw text content is not supported for <$tagName>"
    }
    if (content.rawTextTagIndex(tagName, closing = true) >= 0) {
        return "Raw text for <$tagName> must not contain a </$tagName end tag"
    }

    if (tagName == "script") {
        var escapedStart = content.indexOf("<!--")
        while (escapedStart >= 0) {
            // Include the opener's dashes: <!--> also exits the escaped state.
            val escapedEnd = content.indexOf("-->", escapedStart + 2)
            // Limit the tag scan to this segment so each character is checked at most once.
            val script = content.rawTextTagIndex(
                tagName = "script",
                closing = false,
                startIndex = escapedStart + 4,
                endIndex = if (escapedEnd < 0) content.length else escapedEnd + 1,
            )
            if (script >= 0) {
                return "Raw text for <script> must not contain a <script tag inside <!-- escaped text"
            }
            if (escapedEnd < 0) break
            escapedStart = content.indexOf("<!--", escapedEnd + 3)
        }
    }
    return null
}

internal fun escapeHtml(value: String, attribute: Boolean): String = buildString {
    for (character in value) {
        val replacement =
            if (attribute) htmlAttributeEscapeReplacement(character) else htmlTextEscapeReplacement(character)
        if (replacement != null) append(replacement) else append(character)
    }
}
