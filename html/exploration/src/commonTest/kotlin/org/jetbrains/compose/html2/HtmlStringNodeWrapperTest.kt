package org.jetbrains.compose.html2

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.jetbrains.compose.html2.internal.HtmlElementStringNode
import org.jetbrains.compose.html2.internal.HtmlTextStringNode
import org.jetbrains.compose.html2.internal.HtmlApplierNodeWrapper
import org.jetbrains.compose.html2.internal.HtmlStringNodeWrapper

class HtmlStringNodeWrapperTest {

    @Test
    fun insert_buildsExpectedChildrenOrder() {
        val rootEl = HtmlElementStringNode("div")
        val root: HtmlApplierNodeWrapper = HtmlStringNodeWrapper(rootEl)

        val a = HtmlStringNodeWrapper(HtmlElementStringNode("span").apply { appendText("A") })
        val b = HtmlStringNodeWrapper(HtmlTextStringNode("B"))
        val c = HtmlStringNodeWrapper(HtmlElementStringNode("i").apply { appendText("C") })

        // insert in order: [a, b, c]
        root.insert(0, a)
        root.insert(1, b)
        root.insert(2, c)

        assertEquals("<div><span>A</span>B<i>C</i></div>", rootEl.toHtmlString())
    }

    @Test
    fun remove_removesRange() {
        val rootEl = HtmlElementStringNode("div")
        val root = HtmlStringNodeWrapper(rootEl)

        root.insert(0, HtmlStringNodeWrapper(HtmlTextStringNode("A")))
        root.insert(1, HtmlStringNodeWrapper(HtmlTextStringNode("B")))
        root.insert(2, HtmlStringNodeWrapper(HtmlTextStringNode("C")))
        root.insert(3, HtmlStringNodeWrapper(HtmlTextStringNode("D")))

        // remove B, C
        root.remove(1, 2)

        assertEquals("<div>AD</div>", rootEl.toHtmlString())
    }

    @Test
    fun move_forwardAdjustsTargetIndex() {
        val rootEl = HtmlElementStringNode("div")
        val root = HtmlStringNodeWrapper(rootEl)

        // Children: [A, B, C, D, E]
        listOf("A","B","C","D","E").forEachIndexed { idx, s ->
            root.insert(idx, HtmlStringNodeWrapper(HtmlTextStringNode(s)))
        }

        // Move [B, C] (from=1,count=2) to just before index 4 (E)
        // After forward move with index adjustment, expected: [A, D, B, C, E]
        root.move(from = 1, to = 4, count = 2)

        assertEquals("<div>ADBCE</div>", rootEl.toHtmlString())
    }

    @Test
    fun move_backwardKeepsOrder() {
        val rootEl = HtmlElementStringNode("div")
        val root = HtmlStringNodeWrapper(rootEl)

        // Children: [1,2,3,4,5]
        listOf("1","2","3","4","5").forEachIndexed { idx, s ->
            root.insert(idx, HtmlStringNodeWrapper(HtmlTextStringNode(s)))
        }

        // Move [4,5] (from=3,count=2) to index 1 -> expected [1,4,5,2,3]
        root.move(from = 3, to = 1, count = 2)

        assertEquals("<div>14523</div>", rootEl.toHtmlString())
    }

    @Test
    fun clear_onElementRemovesAllChildren() {
        val el = HtmlElementStringNode("div")
        el.appendText("x")
        el.appendText("y")
        val wrapper = HtmlStringNodeWrapper(el)
        wrapper.clear()
        assertEquals("<div></div>", el.toHtmlString())
    }

    @Test
    fun textWrapper_disallowsInsert() {
        val textWrapper = HtmlStringNodeWrapper(HtmlTextStringNode("t"))
        val child = HtmlStringNodeWrapper(HtmlTextStringNode("x"))
        val ex = assertFailsWith<IllegalStateException> {
            textWrapper.insert(0, child)
        }
        // Error message should mention the operation and text node
        assertEquals(true, ex.message?.contains("insert") == true)
        assertEquals(true, ex.message?.contains("text node") == true)
    }
}
