package org.jetbrains.compose.web.dom

import org.jetbrains.compose.web.HydrationProtocolAttributes

// HTML parsing merges adjacent non-empty text nodes. This comment preserves their boundary.
internal const val HydrationTextBoundaryMarker = "c"

// A boolean attribute is true when present, independently of its supplied string value.
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
    this in HtmlBooleanAttributeNames || lowercase() in HtmlBooleanAttributeNames

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
        requireNotNull(tagName).also(::requireValidHtmlTagName).lowercase()
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
            // HTML parsers ASCII-lowercase attribute names. Mirror that behavior so validation and
            // lookup agree with the browser DOM. SVG has no string-rendering path today; revisit
            // this normalization if the renderer gains XML/XHTML or SVG output.
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
            if (!name.isHtmlBooleanAttributeName()) {
                builder.append("=\"")
                builder.appendEscapedAttribute(value)
                builder.append('"')
            }
        }
        builder.append('>')

        // HTML void elements have neither content nor an end tag.
        if (tagName in VoidElementNames) return

        appendChildrenHtmlTo(builder, hydratable)
        builder.append("</").append(tagName).append('>')
    }

    private fun appendChildrenHtmlTo(builder: StringBuilder, hydratable: Boolean) {
        val rendered = children.filterNot(StringHtmlNode::isEmptyText)
        // Appends boundary marker for hydration between two text nodes
        rendered.forEachIndexed { index, child ->
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

private fun String.asciiLowercase(): String = buildString(length) {
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
            else -> append(character)
        }
    }
}
