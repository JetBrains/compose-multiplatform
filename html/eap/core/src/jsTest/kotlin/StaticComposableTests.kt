package org.jetbrains.compose.web.core.tests

import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.testutils.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class StaticComposableTests {
    @Test
    fun emptyComposable() = runTest {
        composition {}
        assertEquals("<div></div>", root.outerHTML)
    }

    @Test
    fun textChild() = runTest {
        composition {
            Text("inner text")
        }
        assertEquals("<div>inner text</div>", root.outerHTML)
    }

    @Test
    fun attrs() = runTest {
        composition {
            Div(
                attrs = {
                    classes("some", "simple", "classes")
                    id("special")
                    attr("data-val", "some data")
                    attr("data-val", "some other data")
                    id("verySpecial")
                }
            )
        }

        val el = nextChild()

        assertEquals("verySpecial", el.getAttribute("id"))
        assertEquals("some simple classes", el.getAttribute("class"))
        assertEquals("some other data", el.getAttribute("data-val"))
    }

    @Test
    fun styles() = runTest {
        composition {
            Div(
                {
                    style {
                        opacity(0.3)
                        color(Color.red)
                        opacity(0.2)
                        color(Color.green)
                    }
                }
            )
        }

        assertEquals("opacity: 0.2; color: green;", nextChild().style.cssText)
    }
}
