package org.jetbrains.compose.web.dom

import androidx.compose.runtime.AbstractApplier

// Adapts all HTML nodes to single node
internal class StringHtmlNodeWrapper(
    val node: StringHtmlNode
) {
    private fun requireElement(operation: String): StringHtmlElementNode =
        node as? StringHtmlElementNode
            ?: error("Cannot perform '$operation' on a text node")

    fun insert(index: Int, nodeWrapper: StringHtmlNodeWrapper) {
        requireElement("insert").children.add(index, nodeWrapper.node)
    }

    fun remove(index: Int, count: Int) {
        val children = requireElement("remove").children
        repeat(count) {
            children.removeAt(index)
        }
    }

    fun move(from: Int, to: Int, count: Int) {
        if (count == 0 || from == to) return

        val children = requireElement("move").children
        val movedNodes = children.subList(from, from + count).toList()
        repeat(count) {
            children.removeAt(from)
        }

        val destination = if (to > from) to - count else to
        children.addAll(destination, movedNodes)
    }

    fun clear() {
        requireElement("clear").children.clear()
    }

    fun updateAttributes(attributes: StringHtmlAttributes) {
        requireElement("update attributes").updateAttributes(attributes)
    }

    fun updateText(value: String) {
        val textNode = node as? StringHtmlTextNode
            ?: error("Cannot update text on an element node")
        textNode.text = value
    }

    fun updateRawText(value: RawTextContent) {
        val rawTextNode = node as? StringHtmlRawTextNode
            ?: error("Cannot update raw text on a non-raw-text node")
        rawTextNode.content = value
    }
}

internal class StringHtmlApplier(
    root: StringHtmlNodeWrapper
) : AbstractApplier<StringHtmlNodeWrapper>(root) {
    override fun insertTopDown(index: Int, instance: StringHtmlNodeWrapper) {
        // The string tree is assembled bottom-up.
    }

    override fun insertBottomUp(index: Int, instance: StringHtmlNodeWrapper) {
        current.insert(index, instance)
    }

    override fun remove(index: Int, count: Int) {
        current.remove(index, count)
    }

    override fun move(from: Int, to: Int, count: Int) {
        current.move(from, to, count)
    }

    override fun onClear() {
        root.clear()
    }
}
