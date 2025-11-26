package org.jetbrains.compose.html2

import kotlin.test.Test
import kotlin.test.assertEquals

class AttrsScopeTest {

    @Test
    fun setAttrIdViaMap() {
        val result = composeHtmlToString {
            Div(attrsScope = {
                attr("id", "my-div")
            }) {
                Text("Hello, my-div!")
            }
        }

        assertEquals("<div id=\"my-div\">Hello, my-div!</div>", result)
    }

    @Test
    fun setAttrIdViaProperty() {
        val result = composeHtmlToString {
            Div(attrsScope = {
                id = "my-div"
            }) {
                Text("Hello, my-div!")
            }
        }

        assertEquals("<div id=\"my-div\">Hello, my-div!</div>", result)
    }

    @Test
    fun setBooleanAttr() {
        val result = composeHtmlToString {
            Div(attrsScope = {
                attr("disabled")
            }) {
                Text("Hello, my-div!")
            }
        }

        assertEquals("<div disabled>Hello, my-div!</div>", result)
    }

}