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

internal fun isHtmlRawTextElement(tagName: String?): Boolean =
    tagName in HtmlRawTextElementNames

internal fun isHtmlRcdataElement(tagName: String?): Boolean =
    tagName == "title" || tagName == "textarea"

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

// in-memory equivalent of DOM node
internal sealed interface StringHtmlNode {
    fun appendHtmlTo(builder: StringBuilder, hydratable: Boolean)
}

internal fun StringHtmlNode.isEmptyText(): Boolean =
    this is StringHtmlTextNode && text.isEmpty()

internal data class StringHtmlAttributes(
    val byName: Map<String, String>,
    val hydrationProtocolAttributes: Set<String>,
)

internal class StringHtmlElementNode private constructor(
    tagName: String?,
    isRoot: Boolean,
) : StringHtmlNode {
    val tagName: String? = if (isRoot) {
        null
    } else {
        requireNotNull(tagName).also(::requireValidHtmlTagName).asciiLowercase()
    }
    internal val children: MutableList<StringHtmlNode> = mutableListOf()
    private val attributes: MutableMap<String, String> = mutableMapOf()

    constructor(tagName: String) : this(tagName, isRoot = false)

    fun updateAttributes(attributes: Map<String, String>) = updateAttributes(
        StringHtmlAttributes(
            byName = attributes,
            hydrationProtocolAttributes = emptySet(),
        )
    )

    fun updateAttributes(attributes: StringHtmlAttributes) {
        val normalizedAttributes = mutableMapOf<String, String>()
        val originalNames = mutableMapOf<String, String>()
        attributes.byName.forEach { (name, value) ->
            requireValidHtmlAttributeName(name)
            // HTML parsers ASCII-lowercase attribute names. Mirror that behavior so serialized
            // output and browser DOM lookup agree.
            val normalizedName = name.asciiLowercase()
            val previousName = originalNames.put(normalizedName, name)
            require(previousName == null) {
                "Duplicate HTML attribute names \"$previousName\" and \"$name\""
            }
            require(
                normalizedName !in HydrationProtocolAttributes ||
                    normalizedName in attributes.hydrationProtocolAttributes
            ) {
                "Attribute \"$name\" is owned by the Compose hydration protocol"
            }
            normalizedAttributes[normalizedName] = value
        }
        this.attributes.clear()
        this.attributes.putAll(normalizedAttributes)
    }

    fun hasAttribute(name: String): Boolean = attributes.containsKey(name.asciiLowercase())

    fun attribute(name: String): String? = attributes[name.asciiLowercase()]

    fun toHtmlString(hydratable: Boolean = true): String = buildString {
        appendHtmlTo(this, hydratable)
    }

    override fun appendHtmlTo(builder: StringBuilder, hydratable: Boolean) {
        val tagName = tagName
        if (tagName == null) { // root
            appendChildrenHtmlTo(builder, hydratable)
            return
        }

        builder.append('<').append(tagName)
        attributes.forEach { (name, value) ->
            builder.append(' ').append(name)
            if (value.isNotEmpty() || '-' in tagName || !name.isHtmlBooleanAttributeName()) {
                builder.append("=\"")
                builder.appendEscapedAttribute(value)
                builder.append('"')
            }
        }
        builder.append('>')

        // HTML void elements have neither content nor an end tag.
        if (tagName in VoidElementNames) return

        val contentStart = builder.length
        // The parent determines text serialization: script/style and other raw-text elements
        // emit validated text without HTML escaping. Ordinary elements escape their text.
        if (isHtmlRawTextElement(tagName) && children.isNotEmpty()) {
            // Validate together so end tags split across children cannot bypass validation.
            val text = children.joinToString("") { child ->
                when (child) {
                    is StringHtmlTextNode -> child.text
                    is StringHtmlRawTextNode -> child.content.text
                    else -> throw IllegalArgumentException(
                        "String rendering does not support element children inside <$tagName>"
                    )
                }
            }
            val content = RawTextContent.create(tagName, text)
            content.validateAttributes(attributes)
            builder.append(content.text)
        } else {
            // RCDATA decodes escaped text, but treats boundary comments as literal content.
            appendChildrenHtmlTo(builder, hydratable && !isHtmlRcdataElement(tagName))
        }
        if (tagName == "noscript") {
            // Render fallback HTML for scripting-disabled browsers, but keep it inside noscript
            // when scripting is enabled and the parser treats the entire contents as raw text.
            require(!NoscriptEndTag.containsMatchIn(builder.substring(contentStart))) {
                "String-rendered <noscript> content must not contain a </noscript end tag"
            }
        }
        // HTML parsing discards the first LF in these elements.
        if ((tagName == "pre" || tagName == "textarea" || tagName == "listing") &&
            builder.length > contentStart && builder[contentStart] == '\n'
        ) {
            builder.insert(contentStart, '\n')
        }
        builder.append("</").append(tagName).append('>')
    }

    private fun appendChildrenHtmlTo(builder: StringBuilder, hydratable: Boolean) {
        val rendered = children.filterNot(StringHtmlNode::isEmptyText)
        // Appends boundary marker for hydration between two text nodes
        rendered.forEachIndexed { index, child ->
            requireHtmlParserStableTableChild(tagName, child)
            child.appendHtmlTo(builder, hydratable)
            if (
                hydratable &&
                child is StringHtmlTextNode &&
                rendered.getOrNull(index + 1) is StringHtmlTextNode
            ) {
                builder.appendHydrationTextBoundaryMarker()
            }
        }
    }

    companion object {
        private val NoscriptEndTag = Regex("</noscript(?=[\\t\\n\\u000C\\r />])", RegexOption.IGNORE_CASE)

        private val VoidElementNames = setOf(
            "area",
            "base",
            "br",
            "col",
            "embed",
            "hr",
            "img",
            "input",
            "link",
            "meta",
            "param",
            "source",
            "track",
            "wbr",
        )

        fun root(): StringHtmlElementNode = StringHtmlElementNode(
            tagName = null,
            isRoot = true,
        )
    }
}

internal class StringHtmlTextNode(
    var text: String
) : StringHtmlNode {
    fun toHtmlString(hydratable: Boolean = true): String = buildString {
        appendHtmlTo(this, hydratable)
    }

    override fun appendHtmlTo(builder: StringBuilder, hydratable: Boolean) {
        builder.appendEscapedText(text)
    }
}

internal class StringHtmlRawTextNode(
    var content: RawTextContent,
) : StringHtmlNode {
    override fun appendHtmlTo(builder: StringBuilder, hydratable: Boolean) {
        builder.append(content.text)
    }
}

private fun StringBuilder.appendHydrationTextBoundaryMarker() {
    append("<!--$HydrationTextBoundaryMarker-->")
}

private fun requireValidHtmlTagName(name: String) {
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

internal fun String.asciiLowercase(): String = buildString(length) {
    this@asciiLowercase.forEach { character ->
        append(if (character in 'A'..'Z') character.lowercaseChar() else character)
    }
}

private fun StringBuilder.appendEscapedAttribute(value: String) {
    require('\u0000' !in value) { "HTML attribute values must not contain NUL (U+0000)" }
    value.forEach { character ->
        when (character) {
            '&' -> append("&amp;")
            '"' -> append("&quot;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '\n' -> append("&#10;")
            '\r' -> append("&#13;")
            '\t' -> append("&#9;")
            else -> append(character)
        }
    }
}

private fun StringBuilder.appendEscapedText(value: String) {
    require('\u0000' !in value) { "HTML text must not contain NUL (U+0000)" }
    value.forEach { character ->
        when (character) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '\r' -> append("&#13;")
            else -> append(character)
        }
    }
}
