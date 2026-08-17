package org.jetbrains.compose.web.dom

import kotlin.test.Test
import kotlin.test.assertEquals

class StringHtmlNodesTest {
    @Test
    fun serializesNestedNodesAndRootSiblings() {
        val root = StringHtmlElementNode.root()
        val div = StringHtmlElementNode("DIV")
        div.children += StringHtmlTextNode("before")
        div.children += StringHtmlElementNode("span").apply {
            children += StringHtmlTextNode("inside")
        }
        root.children += div
        root.children += StringHtmlTextNode("after")

        assertEquals(
            "<div>before<span>inside</span></div>after",
            root.toHtmlString()
        )
    }

    @Test
    fun escapesTextAndAttributeValues() {
        val element = StringHtmlElementNode("div").apply {
            updateAttributes(mapOf("title" to "Tom & \"Jerry\" <3"))
            children += StringHtmlTextNode("Tom & Jerry <3 > 2")
        }

        assertEquals(
            "<div title=\"Tom &amp; &quot;Jerry&quot; &lt;3\">Tom &amp; Jerry &lt;3 &gt; 2</div>",
            element.toHtmlString()
        )
    }

    @Test
    fun serializesVoidElementsWithoutClosingTags() {
        val element = StringHtmlElementNode("BR")

        assertEquals("<br>", element.toHtmlString())
    }
}
