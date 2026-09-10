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
    this in HtmlBooleanAttributeNames || asciiLowercase() in HtmlBooleanAttributeNames

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
    namespace: String?,
    isRoot: Boolean,
) : StringHtmlNode {
    val namespace: String? = if (isRoot) null else requireNotNull(namespace)
    val tagName: String? = if (isRoot) {
        null
    } else {
        requireNotNull(tagName)
            .also(::requireValidHtmlTagName)
            .let { normalizeElementTagName(it, requireNotNull(namespace)) }
    }
    internal val children: MutableList<StringHtmlNode> = mutableListOf()
    private val attributes: MutableMap<String, String> = mutableMapOf()

    constructor(
        tagName: String,
        namespace: String = HtmlNamespace,
    ) : this(tagName, namespace, isRoot = false)

    fun updateAttributes(attributes: Map<String, String>) = updateAttributes(
        StringHtmlAttributes(
            byName = attributes,
            hydrationProtocolAttributes = emptySet(),
        )
    )

    fun updateAttributes(attributes: StringHtmlAttributes) {
        val namespace = requireElementNamespace()
        val normalizedAttributes = mutableMapOf<String, String>()
        requireDistinctHtmlParserAttributeNames(attributes.byName.keys)
        attributes.byName.forEach { (name, value) ->
            requireValidHtmlAttributeName(name)
            // The tokenizer lowercases all names before applying its SVG name adjustments.
            val parserName = name.asciiLowercase()
            val normalizedName = if (namespace == HtmlNamespace) parserName else name
            require(
                parserName !in HydrationProtocolAttributes ||
                    parserName in attributes.hydrationProtocolAttributes
            ) {
                "Attribute \"$name\" is owned by the Compose hydration protocol"
            }
            normalizedAttributes[normalizedName] = value
        }
        this.attributes.clear()
        this.attributes.putAll(normalizedAttributes)
    }

    fun hasAttribute(name: String): Boolean = attributes.containsKey(normalizeAttributeName(name))

    fun attribute(name: String): String? = attributes[normalizeAttributeName(name)]

    fun toHtmlString(hydratable: Boolean = true): String = buildString {
        appendHtmlTo(this, hydratable)
    }

    override fun appendHtmlTo(builder: StringBuilder, hydratable: Boolean) {
        val tagName = tagName
        if (tagName == null) { // root
            appendChildrenHtmlTo(builder, hydratable)
            return
        }
        val namespace = requireElementNamespace()

        builder.append('<').append(tagName)
        attributes.forEach { (name, value) ->
            builder.append(' ').append(name)
            if (namespace != HtmlNamespace || value.isNotEmpty() || '-' in tagName || !name.isHtmlBooleanAttributeName()) {
                builder.append("=\"")
                builder.appendEscapedAttribute(value)
                builder.append('"')
            }
        }
        builder.append('>')

        // HTML void elements have neither content nor an end tag.
        if (namespace == HtmlNamespace && tagName in VoidElementNames) return

        val contentStart = builder.length
        // The parent determines text serialization: script/style and other raw-text elements
        // emit validated text without HTML escaping. Ordinary elements escape their text.
        if (isHtmlRawTextElement(tagName, namespace) && children.isNotEmpty()) {
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
            appendChildrenHtmlTo(builder, hydratable)
        }
        if (namespace == HtmlNamespace && tagName == "noscript") {
            // Render fallback HTML for scripting-disabled browsers, but keep it inside noscript
            // when scripting is enabled and the parser treats the entire contents as raw text.
            require(!NoscriptEndTag.containsMatchIn(builder.substring(contentStart))) {
                "String-rendered <noscript> content must not contain a </noscript end tag"
            }
        }
        // HTML parsing discards the first LF in these elements.
        if (namespace == HtmlNamespace &&
            (tagName == "pre" || tagName == "textarea" || tagName == "listing") &&
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
            requireParserStableChild(child)
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

    private fun requireParserStableChild(child: StringHtmlNode) {
        val childElement = child as? StringHtmlElementNode ?: return
        requireHtmlParserStableChild(
            parentTagName = tagName,
            parentNamespace = namespace,
            childTagName = requireNotNull(childElement.tagName),
            childNamespace = childElement.requireElementNamespace(),
            childAttributeNames = childElement.attributes.keys,
        )
    }

    private fun normalizeAttributeName(name: String): String =
        if (requireElementNamespace() == HtmlNamespace) name.asciiLowercase() else name

    private fun requireElementNamespace(): String =
        checkNotNull(namespace) { "The string-rendering root has no element namespace" }

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
            namespace = null,
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
    append("<!--").append(HydrationTextBoundaryMarker).append("-->")
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
