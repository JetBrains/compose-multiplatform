package org.jetbrains.compose.web.dom

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StringHtmlNodesTest {
    @Test
    fun rejectsInvalidTagNames() {
        val invalidNames = listOf(
            "",
            "1div",
            "div id",
            "div/",
            "div>",
            "div\u0000",
        )

        invalidNames.forEach { name ->
            assertFailsWith<IllegalArgumentException>(name) {
                StringHtmlElementNode(name)
            }
        }
    }

    @Test
    fun rejectsInvalidAttributeNames() {
        val invalidNames = listOf(
            "",
            "data value",
            "data\u0001value",
            "data\"value",
            "data'value",
            "data/value",
            "data=value",
            "data>value",
            "data\u007Fvalue",
        )

        invalidNames.forEach { name ->
            assertFailsWith<IllegalArgumentException>(name) {
                StringHtmlElementNode("div").updateAttributes(mapOf(name to "value"))
            }
        }
    }

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
    fun normalizesHtmlAttributeNamesAndLooksThemUpCaseInsensitively() {
        val element = StringHtmlElementNode("div").apply {
            updateAttributes(mapOf("DATA-VALUE" to "content"))
        }

        assertEquals("<div data-value=\"content\"></div>", element.toHtmlString())
        assertEquals(true, element.hasAttribute("Data-Value"))
        assertEquals("content", element.attribute("DATA-VALUE"))
    }

    @Test
    fun rejectsAttributeNamesThatDifferOnlyByAsciiCase() {
        val failure = assertFailsWith<IllegalArgumentException> {
            StringHtmlElementNode("div").updateAttributes(
                linkedMapOf("CLASS" to "first", "class" to "second")
            )
        }

        assertEquals(
            "Duplicate HTML attribute names \"CLASS\" and \"class\"",
            failure.message,
        )
    }

    @Test
    fun serializesVoidElementsWithoutClosingTags() {
        val element = StringHtmlElementNode("BR")

        assertEquals("<br>", element.toHtmlString())
    }
}
