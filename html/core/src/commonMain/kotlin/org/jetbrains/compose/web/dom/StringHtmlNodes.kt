package org.jetbrains.compose.web.dom

// in-memory equivalent of DOM node
internal sealed interface StringHtmlNode {
    fun appendHtmlTo(builder: StringBuilder)
}

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

    fun updateAttributes(attributes: Map<String, String>) {
        attributes.keys.forEach(::requireValidHtmlAttributeName)
        this.attributes.clear()
        this.attributes.putAll(attributes)
    }

    fun toHtmlString(): String = buildString {
        appendHtmlTo(this)
    }

    override fun appendHtmlTo(builder: StringBuilder) {
        val tagName = tagName
        if (tagName == null) { // root
            children.forEach { it.appendHtmlTo(builder) }
            return
        }

        builder.append('<').append(tagName)
        attributes.forEach { (name, value) ->
            builder.append(' ').append(name)
            if (name.lowercase() !in BooleanAttributeNames) {
                builder.append("=\"")
                builder.appendEscapedAttribute(value)
                builder.append('"')
            }
        }
        builder.append('>')

        // HTML void elements have neither content nor an end tag.
        if (tagName in VoidElementNames) return

        children.forEach { it.appendHtmlTo(builder) }
        builder.append("</").append(tagName).append('>')
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

        // A boolean attribute is true when present, independently of its supplied string value.
        private val BooleanAttributeNames = setOf(
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

        fun root(): StringHtmlElementNode = StringHtmlElementNode(
            tagName = null,
            isRoot = true,
        )
    }
}

internal class StringHtmlTextNode(
    var text: String
) : StringHtmlNode {
    fun toHtmlString(): String = buildString {
        appendHtmlTo(this)
    }

    override fun appendHtmlTo(builder: StringBuilder) {
        builder.appendEscapedText(text)
    }
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

private const val AsciiWhitespaceCharacters = "\t\n\u000C\r "
private const val InvalidHtmlTagNameCharacters = "\u0000/>"
private const val InvalidHtmlAttributeNameCharacters = " \"'/>="

private fun Char.isAsciiLetter(): Boolean = this in 'A'..'Z' || this in 'a'..'z'

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
