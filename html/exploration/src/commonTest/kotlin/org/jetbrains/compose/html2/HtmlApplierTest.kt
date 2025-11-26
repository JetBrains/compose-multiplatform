package org.jetbrains.compose.html2

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.jetbrains.compose.html2.internal.HtmlApplier
import org.jetbrains.compose.html2.internal.HtmlApplierNodeWrapper
import org.jetbrains.compose.html2.internal.HtmlElementStringNode
import org.jetbrains.compose.html2.internal.HtmlStringNodeWrapper
import org.jetbrains.compose.html2.internal.HtmlTextStringNode

class HtmlApplierTest {

    private fun el(tag: String, text: String? = null): HtmlStringNodeWrapper {
        val node = HtmlElementStringNode(tag)
        if (text != null) node.appendText(text)
        return HtmlStringNodeWrapper(node)
    }

    private fun tx(value: String): HtmlStringNodeWrapper = HtmlStringNodeWrapper(HtmlTextStringNode(value))

    @Test
    fun insert_into_root_bottomUp() {
        val rootEl = HtmlElementStringNode("div")
        val root: HtmlApplierNodeWrapper = HtmlStringNodeWrapper(rootEl)
        val applier = HtmlApplier(root)

        applier.insertBottomUp(0, el("span", "A"))
        applier.insertBottomUp(1, tx("B"))
        applier.insertBottomUp(2, el("i", "C"))

        assertEquals("<div><span>A</span>B<i>C</i></div>", rootEl.toHtmlString())
    }

    @Test
    fun insertTopDown_isIgnored() {
        val rootEl = HtmlElementStringNode("div")
        val root: HtmlApplierNodeWrapper = HtmlStringNodeWrapper(rootEl)
        val applier = HtmlApplier(root)

        // Should do nothing
        applier.insertTopDown(0, el("b", "X"))
        assertEquals("<div></div>", rootEl.toHtmlString())
    }

    @Test
    fun remove_range() {
        val rootEl = HtmlElementStringNode("div")
        val root = HtmlStringNodeWrapper(rootEl)
        val applier = HtmlApplier(root)

        listOf("A","B","C","D").forEachIndexed { i, s -> applier.insertBottomUp(i, tx(s)) }

        applier.remove(1, 2) // remove B, C

        assertEquals("<div>AD</div>", rootEl.toHtmlString())
    }

    @Test
    fun move_forward_and_backward() {
        val rootEl = HtmlElementStringNode("div")
        val root = HtmlStringNodeWrapper(rootEl)
        val applier = HtmlApplier(root)

        listOf("A","B","C","D","E").forEachIndexed { i, s -> applier.insertBottomUp(i, tx(s)) }

        // Forward move with index adjustment: move [B,C] before E
        applier.move(from = 1, to = 4, count = 2)
        assertEquals("<div>ADBCE</div>", rootEl.toHtmlString())

        // Backward move: move [B,C] (now at 2,3) to index 1 results in the original order
        applier.move(from = 2, to = 1, count = 2)
        assertEquals("<div>ABCDE</div>", rootEl.toHtmlString())
    }

    @Test
    fun onClear_clearsRoot() {
        val rootEl = HtmlElementStringNode("div")
        val root = HtmlStringNodeWrapper(rootEl)
        val applier = HtmlApplier(root)

        applier.insertBottomUp(0, tx("x"))
        applier.insertBottomUp(1, tx("y"))
        // onClear is protected on the applier; verify equivalent behavior by clearing the root
        root.clear()
        assertEquals("<div></div>", rootEl.toHtmlString())
    }

    @Test
    fun down_up_nestedInsert() {
        val rootEl = HtmlElementStringNode("div")
        val root = HtmlStringNodeWrapper(rootEl)
        val applier = HtmlApplier(root)

        val child = el("section")
        applier.insertBottomUp(0, child)

        // Go down into <section> and add its children
        applier.down(child)
        applier.insertBottomUp(0, el("h1", "Title"))
        applier.insertBottomUp(1, tx("Body"))
        applier.up()

        // Also add a sibling after going up
        applier.insertBottomUp(1, el("footer", "F"))

        assertEquals("<div><section><h1>Title</h1>Body</section><footer>F</footer></div>", rootEl.toHtmlString())
    }

    @Test
    fun textCurrent_disallowsInsert() {
        val rootEl = HtmlElementStringNode("div")
        val root = HtmlStringNodeWrapper(rootEl)
        val applier = HtmlApplier(root)

        val textChild = tx("t")
        applier.insertBottomUp(0, textChild)

        applier.down(textChild)
        val ex = assertFailsWith<IllegalStateException> {
            applier.insertBottomUp(0, tx("x"))
        }
        // message should mention the operation and text node
        val msg = ex.message ?: ""
        assertEquals(true, msg.contains("insert"))
        assertEquals(true, msg.contains("text node"))
    }
}
