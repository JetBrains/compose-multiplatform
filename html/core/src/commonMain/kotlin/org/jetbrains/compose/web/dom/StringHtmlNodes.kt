package org.jetbrains.compose.web.dom

// in-memory equivalent of DOM node
internal sealed interface StringHtmlNode {
    fun appendHtmlTo(builder: StringBuilder)
}

internal class StringHtmlElementNode private constructor(
    tagName: String?,
    isRoot: Boolean,
) : StringHtmlNode {
    val tagName: String? = if (isRoot) null else requireNotNull(tagName).lowercase()
    internal val children: MutableList<StringHtmlNode> = mutableListOf()
    private val attributes: MutableMap<String, String> = mutableMapOf()

    constructor(tagName: String) : this(tagName, isRoot = false)

    fun updateAttributes(attributes: Map<String, String>) {
        this.attributes.clear()
        this.attributes.putAll(attributes)
    }

    fun toHtmlString(): String = buildString {
        appendHtmlTo(this)
    }

    override fun appendHtmlTo(builder: StringBuilder) {
        //TODO validate tag & attribute names

        val tagName = tagName
        if (tagName == null) { // root
            children.forEach { it.appendHtmlTo(builder) }
            return
        }

        builder.append('<').append(tagName)
        attributes.forEach { (name, value) ->
            builder.append(' ').append(name).append("=\"")
            builder.appendEscapedAttribute(value)
            builder.append('"')
        }
        builder.append('>')

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
