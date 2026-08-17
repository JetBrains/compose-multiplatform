package org.jetbrains.compose.web.dom

import org.jetbrains.compose.web.composeHtmlToString
import kotlin.test.Test
import kotlin.test.assertEquals

class ComposeHtmlToStringTest {
    @Test
    fun rendersNestedElementsAndRootSiblings() {
        val html = composeHtmlToString {
            Div {
                Text("before")
                Span { Text("inside") }
            }
            Text("after")
        }

        assertEquals(
            "<div>before<span>inside</span></div>after",
            html
        )
    }

    @Test
    fun rendersAttributesStylesAndEscapedText() {
        val html = composeHtmlToString {
            Div({
                id("root")
                classes("first", "second")
                style {
                    property("color", "red")
                    property("display", "block", important = true)
                }
                attr("title", "Tom & \"Jerry\"")
            }) {
                Text("Tom & Jerry <3")
            }
        }

        assertEquals(
            "<div id=\"root\" title=\"Tom &amp; &quot;Jerry&quot;\" " +
                "class=\"first second\" " +
                "style=\"color: red; display: block !important\">" +
                "Tom &amp; Jerry &lt;3</div>",
            html
        )
    }
}
