package org.jetbrains.compose.html2.internal

internal class HtmlElementStringNode(
    val tag: String
) {
    // Attributes support string values and boolean (presence-only) attributes
    private sealed interface AttrValue {
        data class Str(val value: String) : AttrValue
        data object Bool : AttrValue
    }

    // Deterministic containers across KMP via stdlib factory functions
    private val attributes: MutableMap<String, AttrValue> = mutableMapOf()
    private val styles: MutableMap<String, String> = mutableMapOf()
    private val classSet: MutableSet<String> = mutableSetOf()

    sealed interface Child {
        data class Element(val node: HtmlElementStringNode) : Child
        data class Text(val node: HtmlTextStringNode) : Child
    }

    val children: MutableList<Child> = mutableListOf()

    // ---------------------- Convenience: id ----------------------
    var id: String?
        get() = (attributes["id"] as? AttrValue.Str)?.value
        set(value) {
            if (value == null) attributes.remove("id") else attributes["id"] = AttrValue.Str(value)
        }

    // ---------------------- Convenience: classes ----------------------
    fun addClass(vararg names: String) {
        names.forEach { if (it.isNotEmpty()) classSet.add(it) }
    }
    fun removeClass(vararg names: String) {
        names.forEach { classSet.remove(it) }
    }
    fun toggleClass(name: String, force: Boolean? = null): Boolean {
        return if (force == true) {
            classSet.add(name); true
        } else if (force == false) {
            classSet.remove(name); false
        } else {
            if (classSet.contains(name)) { classSet.remove(name); false } else { classSet.add(name); true }
        }
    }
    fun hasClass(name: String): Boolean = classSet.contains(name)
    fun clearClasses() { classSet.clear() }

    // ---------------------- Attributes API ----------------------
    fun attr(name: String, value: String?) {
        if (value == null) attributes.remove(name) else attributes[name] = AttrValue.Str(value)
    }
    fun attr(name: String, present: Boolean) {
        if (present) attributes[name] = AttrValue.Bool else attributes.remove(name)
    }
    fun getAttr(name: String): String? = when (val v = attributes[name]) {
        is AttrValue.Str -> v.value
        is AttrValue.Bool -> ""
        null -> null
    }
    fun hasAttr(name: String): Boolean = attributes.containsKey(name)
    fun removeAttr(name: String) { attributes.remove(name) }

    fun updateAttrs(attrs: Map<String, String>) {
        attributes.clear()
        attrs.forEach {
            if (it.value.isEmpty()) {
                attr(it.key, present = true)
            } else  {
                attr(it.key, value = it.value)
            }
        }
    }

    // ---------------------- Styles API ----------------------
    fun setStyle(name: String, value: String) { styles[name] = value }
    fun getStyle(name: String): String? = styles[name]
    fun removeStyle(name: String) { styles.remove(name) }
    fun clearStyles() { styles.clear() }

    // ---------------------- Children API ----------------------
    fun append(child: Child) { children.add(child) }
    fun appendText(text: String) { children.add(Child.Text(HtmlTextStringNode(text))) }
    fun appendElement(node: HtmlElementStringNode) { children.add(Child.Element(node)) }
    fun insert(index: Int, child: Child) { children.add(index, child) }
    fun removeAt(index: Int): Child = children.removeAt(index)
    fun remove(child: Child): Boolean = children.remove(child)
    fun clearChildren() { children.clear() }
    fun replaceAt(index: Int, child: Child): Child = children.set(index, child)

    // Convenience: innerText replaces all children with a single text node
    var innerText: String?
        get() = if (children.size == 1 && children[0] is Child.Text) (children[0] as Child.Text).node.text else null
        set(value) {
            children.clear()
            if (value != null) children.add(Child.Text(HtmlTextStringNode(value)))
        }

    // ---------------------- Serialization ----------------------
    fun toHtmlString(): String {
        val sb = StringBuilder()
        appendHtml(sb)
        return sb.toString()
    }

    // Append this node's HTML into a shared StringBuilder (no intermediate strings)
    private fun appendHtml(sb: StringBuilder) {
        val isRoot = isCompositionRoot()

        if (!isRoot) {
            sb.append('<').append(tag)

            // merge classes and styles into attributes on the fly for output
            if (classSet.isNotEmpty()) {
                // Deterministic join by insertion order
                val cls = classSet.joinToString(" ")
                sb.append(' ').append("class\u003D\"")
                sb.append(cls)
                sb.append('\"')
            }
            if (styles.isNotEmpty()) {
                val styleStr = styles.entries.joinToString("; ") { (k, v) -> "$k: $v" }
                sb.append(' ').append("style\u003D\"")
                sb.append(styleStr)
                sb.append('\"')
            }

            // other attributes (skip class/style to avoid duplication)
            for ((name, v) in attributes) {
                if (name == "class" || name == "style") continue
                when (v) {
                    is AttrValue.Bool -> sb.append(' ').append(name)
                    is AttrValue.Str -> {
                        sb.append(' ').append(name).append("=\"")
                        sb.append(v.value)
                        sb.append('\"')
                    }
                }
            }
        }

        if (children.isEmpty()) {
            // Follow HTML rules: void elements have no closing tag; non-void must not be self-closing
            if (!isRoot) {
                sb.append('>')
                if (!isVoidTag(tag)) {
                    sb.append("</").append(tag).append('>')
                }
            }
            return
        }

        if (!isRoot) sb.append('>')

        for (c in children) {
            when (c) {
                is Child.Text -> c.node.appendHtml(sb)
                is Child.Element -> c.node.appendHtml(sb)
            }
        }

        if (!isRoot) {
            sb.append("</").append(tag).append('>')
        }
    }

    private fun isVoidTag(name: String): Boolean {
        // The HTML void elements per spec. Comparison is case-insensitive; treat unknown/custom as non-void.
        // List: area, base, br, col, embed, hr, img, input, link, meta, param, source, track, wbr
        return when (name.lowercase()) {
            "area", "base", "br", "col", "embed", "hr", "img", "input",
            "link", "meta", "param", "source", "track", "wbr" -> true
            else -> false
        }
    }

    private fun escapeAttr(s: String): String = buildString(s.length) { appendEscapedAttr(this, s) }
    private fun escapeText(s: String): String = buildString(s.length) { appendEscapedText(this, s) }
    
    private fun isCompositionRoot(): Boolean {
        return tag == "compose-html-root"
    }
    
    companion object {
        fun root() = HtmlElementStringNode("compose-html-root")
    }
}

// Standalone text node that can exist independently of elements
internal class HtmlTextStringNode(
    var text: String
) {
    fun toHtmlString(): String = text

    internal fun appendHtml(sb: StringBuilder) {
        sb.append(text)
    }
}

// Shared escaping helpers to stream directly into a StringBuilder
private fun appendEscapedAttr(sb: StringBuilder, s: String) {
    for (ch in s) when (ch) {
        '\n' -> sb.append("&#10;")
        '\r' -> sb.append("&#13;")
        '\t' -> sb.append("&#9;")
        '"' -> sb.append("&quot;")
        '&' -> sb.append("&amp;")
        '<' -> sb.append("&lt;")
        '>' -> sb.append("&gt;")
        else -> sb.append(ch)
    }
}

private fun appendEscapedText(sb: StringBuilder, s: String) {
    for (ch in s) when (ch) {
        '&' -> sb.append("&amp;")
        '<' -> sb.append("&lt;")
        '>' -> sb.append("&gt;")
        else -> sb.append(ch)
    }
}