package org.jetbrains.compose.web.core.tests

import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.renderComposable
import org.w3c.dom.HTMLElement
import org.w3c.dom.get
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StaticComposableTests {
    @Test
    fun emptyComposable() {
        val root = "div".asHtmlElement()
        renderComposable(
            root = root
        ) {}
        assertEquals("<div></div>", root.outerHTML)
    }

    @Test
    fun textChild() {
        val root = "div".asHtmlElement()
        renderComposable(
            root = root
        ) {
            Text("inner text")
        }
        assertEquals("<div>inner text</div>", root.outerHTML)
    }

    @Test
    fun attrs() {
        val root = "div".asHtmlElement()
        renderComposable(
            root = root
        ) {
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

        val el = root.firstChild
        assertTrue(el is HTMLElement, "element not found")

        assertEquals("verySpecial", el.getAttribute("id"))
        assertEquals("some simple classes", el.getAttribute("class"))
        assertEquals("some other data", el.getAttribute("data-val"))
    }

    @Test
    fun styles() {
        val root = "div".asHtmlElement()
        renderComposable(
            root = root
        ) {
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

        assertEquals("opacity: 0.2; color: green;", (root.children[0] as HTMLElement).style.cssText)
    }


    @Test
    fun stylesTop() {
        val root = "div".asHtmlElement()
        renderComposable(
            root = root
        ) {
            Div(
                {
                    style {
                        top(100.px)
                    }
                }
            )
            Div(
                {
                    style {
                        top(100.percent)
                    }
                }
            )
        }

        assertEquals("top: 100px;", (root.children[0] as HTMLElement).style.cssText)
        assertEquals("top: 100%;", (root.children[1] as HTMLElement).style.cssText)
    }

    @Test
    fun stylesBottom() {
        val root = "div".asHtmlElement()
        renderComposable(
            root = root
        ) {
            Div(
                {
                    style {
                        bottom(100.px)
                    }
                }
            )
            Div(
                {
                    style {
                        bottom(100.percent)
                    }
                }
            )
        }

        assertEquals("bottom: 100px;", (root.children[0] as HTMLElement).style.cssText)
        assertEquals("bottom: 100%;", (root.children[1] as HTMLElement).style.cssText)
    }

    @Test
    fun stylesLeft() {
        val root = "div".asHtmlElement()
        renderComposable(
            root = root
        ) {
            Div(
                {
                    style {
                        left(100.px)
                    }
                }
            )
            Div(
                {
                    style {
                        left(100.percent)
                    }
                }
            )
        }

        assertEquals("left: 100px;", (root.children[0] as HTMLElement).style.cssText)
        assertEquals("left: 100%;", (root.children[1] as HTMLElement).style.cssText)
    }

    @Test
    fun stylesRight() {
        val root = "div".asHtmlElement()
        renderComposable(
            root = root
        ) {
            Div(
                {
                    style {
                        right(100.px)
                    }
                }
            )
            Div(
                {
                    style {
                        right(100.percent)
                    }
                }
            )
        }

        assertEquals("right: 100px;", (root.children[0] as HTMLElement).style.cssText)
        assertEquals("right: 100%;", (root.children[1] as HTMLElement).style.cssText)
    }

    @Test
    fun stylesPosition() {
        val root = "div".asHtmlElement()
        val enumValues = Position.values()
        renderComposable(
            root = root
        ) {
            enumValues.forEach { position ->
                Span(
                    {
                        style {
                            position(position)
                        }
                    }
                ) { }
            }
        }

        enumValues.forEachIndexed { index, position ->
            assertEquals(
                "position: ${position.value};",
                (root.children[index] as HTMLElement).style.cssText
            )
        }
    }
}
