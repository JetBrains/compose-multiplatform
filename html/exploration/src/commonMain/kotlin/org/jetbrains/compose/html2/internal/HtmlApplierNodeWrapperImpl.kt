package org.jetbrains.compose.html2.internal

/**
 * A single implementation that can wrap either an element or a text node
 * and translate [HtmlApplier] tree operations to underlying data structures
 * based on [HtmlElementStringNode] and [HtmlTextStringNode].
 */
internal class HtmlStringNodeWrapper private constructor(
    val element: HtmlElementStringNode?,
    val text: HtmlTextStringNode?
) : HtmlApplierNodeWrapper {

    constructor(element: HtmlElementStringNode) : this(element, null)
    constructor(text: HtmlTextStringNode) : this(null, text)

    private inline fun requireElement(op: String): HtmlElementStringNode =
        element ?: error("Cannot perform '$op' on a text node")

    override fun insert(index: Int, nodeWrapper: HtmlApplierNodeWrapper) {
        val e = requireElement("insert")
        val other = nodeWrapper.asChild()
        e.insert(index, other)
    }

    override fun remove(index: Int, count: Int) {
        val e = requireElement("remove")
        if (count <= 0) return
        repeat(count) { e.removeAt(index) }
    }

    override fun move(from: Int, to: Int, count: Int) {
        val e = requireElement("move")
        if (count <= 0 || from == to) return

        // Extract the range [from, from+count) preserving order
        val extracted = ArrayList<HtmlElementStringNode.Child>(count)
        repeat(count) { extracted += e.removeAt(from) }

        // When removing items before the insertion point, the target index shifts left by 'count'
        val insertIndex = if (to > from) to - count else to
        var i = insertIndex
        for (child in extracted) {
            e.insert(i, child)
            i++
        }
    }

    override fun clear() {
        // For elements clear the children; for text there is nothing to clear
        element?.clearChildren()
    }

    // Helpers
    private fun HtmlApplierNodeWrapper.asChild(): HtmlElementStringNode.Child = when (this) {
        is HtmlStringNodeWrapper -> when {
            this.element != null -> HtmlElementStringNode.Child.Element(this.element)
            this.text != null -> HtmlElementStringNode.Child.Text(this.text)
            else -> error("Invalid wrapper state: neither element nor text")
        }
        else -> error("Unknown HtmlApplierNodeWrapper implementation")
    }
}
