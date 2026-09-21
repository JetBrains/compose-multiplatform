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

internal class StringHtmlAttributes private constructor(
    val byName: Map<String, String>,
) {
    override fun equals(other: Any?): Boolean =
        this === other || other is StringHtmlAttributes && byName == other.byName

    override fun hashCode(): Int = byName.hashCode()

    companion object {
        fun from(
            attributes: Map<String, String>,
            namespace: String,
            hydrationProtocolAttributes: Set<String>,
            classAttributeValue: String? = null,
            styleAttributeValue: (() -> String?)? = null,
        ): StringHtmlAttributes {
            val serializedAttributes = linkedMapOf<String, String>()
            attributes.forEach { (name, value) ->
                requireValidHtmlAttributeName(name)
                if (namespace != HtmlNamespace) {
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
    }
    internal val children: MutableList<StringHtmlNode> = mutableListOf()
    private val attributes: MutableMap<String, String> = mutableMapOf()

    constructor(
        tagName: String,
        namespace: String = HtmlNamespace,
    ) : this(tagName, namespace, isRoot = false)

    fun updateAttributes(attributes: Map<String, String>) = updateAttributes(
        StringHtmlAttributes.from(
            attributes = attributes,
            namespace = requireElementNamespace(),
            hydrationProtocolAttributes = emptySet(),
        )
    )

    fun updateAttributes(attributes: StringHtmlAttributes) {
        requireElementNamespace()
        this.attributes.clear()
        this.attributes.putAll(attributes.byName)
    }

    fun hasAttribute(name: String): Boolean = attributes.containsKey(name)

    fun attribute(name: String): String? = attributes[name]

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
            // RCDATA decodes escaped text, but treats boundary comments as literal content.
            appendChildrenHtmlTo(builder, hydratable && !isHtmlRcdataElement(tagName, namespace))
        }
        if (namespace == HtmlNamespace && tagName == "noscript") {
            // Render fallback HTML for scripting-disabled browsers, but keep it inside noscript
            // when scripting is enabled and the parser treats the entire contents as raw text.
            require(builder.substring(contentStart).rawTextTagIndex("noscript", closing = true) < 0) {
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
        val validateTableChildren = namespace == HtmlNamespace
        // Appends boundary marker for hydration between two text nodes
        rendered.forEachIndexed { index, child ->
            if (validateTableChildren) {
                requireHtmlParserStableTableChild(tagName, child)
            }
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

    private fun requireElementNamespace(): String =
        checkNotNull(namespace) { "The string-rendering root has no element namespace" }

    companion object {
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
