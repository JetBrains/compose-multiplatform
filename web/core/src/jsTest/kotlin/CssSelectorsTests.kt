package org.jetbrains.compose.web.core.tests

import kotlinx.browser.window
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.testutils.runTest
import org.w3c.dom.HTMLParagraphElement
import org.w3c.dom.HTMLSpanElement
import org.w3c.dom.get
import kotlin.test.Test
import kotlin.test.assertEquals

class CssSelectorsTests {

    private object TestSimpleDescendantsSelectorStyleSheet: StyleSheet() {
        val cls1 by style {
            "p" {
                color(Color.red)
            }
        }
    }

    @Test
    fun simpleDescendantsSelectorComputedStyleIsCorrect() = runTest {
        composition {
            Style(TestSimpleDescendantsSelectorStyleSheet)

            Div(attrs = {
                classes(TestSimpleDescendantsSelectorStyleSheet.cls1)
            }) {
                P { } // Should be // should be <rgb(255, 0, 0)>
            }

            P {} // should be <rgb(0, 0, 0)>
        }

        root.children[1]!!.let { el ->
            val pEl = el.firstChild as HTMLParagraphElement
            assertEquals("rgb(255, 0, 0)", window.getComputedStyle(pEl).color)
        }

        root.children[2]!!.let { el ->
            val pEl = el
            assertEquals("rgb(0, 0, 0)", window.getComputedStyle(pEl).color)
        }
    }

    private object TestSeveralDescendantsSelectorStyleSheet: StyleSheet() {
        val cls1 by style {
            "p" {
                color(Color.red)

                "span" {
                    color(Color.blue)
                }
            }
            "span" {
                color(rgb(100, 100, 200))
            }
        }
    }

    @Test
    fun descendantsSelectorComputedStyleIsCorrect() = runTest {
        composition {
            Style(TestSeveralDescendantsSelectorStyleSheet)

            Div(attrs = {
                classes(TestSeveralDescendantsSelectorStyleSheet.cls1)
            }) {
                P {  // should be <rgb(255, 0, 0)>
                    Span {  }  // should be <rgb(0, 0, 255)>
                }
            }

            P { // should be <rgb(0, 0, 0)>
                Span {  } // should be <rgb(0, 0, 0)>
            }
        }

        root.children[1]!!.let { el ->
            val pEl = el.firstChild as HTMLParagraphElement
            val spanInPel = pEl.firstChild as HTMLSpanElement

            assertEquals("rgb(255, 0, 0)", window.getComputedStyle(pEl).color)
            assertEquals("rgb(0, 0, 255)", window.getComputedStyle(spanInPel).color)
        }

        root.children[2]!!.let { el ->
            val pEl = el
            val spanEl = pEl.firstChild as HTMLSpanElement

            assertEquals("rgb(0, 0, 0)", window.getComputedStyle(pEl).color)
            assertEquals("rgb(0, 0, 0)", window.getComputedStyle(spanEl).color)
        }
    }

    private object TestDescendantsSelectorOfClassAndOfRoot: StyleSheet() {
        val cls1 by style {
            "p" {
                color(Color.red)

                "span" {
                    color(Color.blue)
                }
            }
            "span" {
                color(rgb(100, 100, 200))
            }
        }
    }

    @Test
    fun descendantsSelectorOfClassAndOfRootComputedStyleAreCorrect() = runTest {
        composition {
            Style(TestDescendantsSelectorOfClassAndOfRoot)

            Div(attrs = {
                classes(TestDescendantsSelectorOfClassAndOfRoot.cls1)
            }) {
                P {  // should be <rgb(255, 0, 0)>
                    Span {  } // should be <rgb(0, 0, 255)>
                }
                Span { } // should be <rgb(100, 100, 200)>
            }

            // Not a desc of "cls1"
            P { // should be <rgb(0, 0, 0)>
                Span {  } // should be <rgb(0, 0, 0)>
            }
        }

        root.children[1]!!.let { el ->
            val pEl = el.firstChild as HTMLParagraphElement
            val spanInPel = pEl.firstChild as HTMLSpanElement

            assertEquals("rgb(255, 0, 0)", window.getComputedStyle(pEl).color)
            assertEquals("rgb(0, 0, 255)", window.getComputedStyle(spanInPel).color)


            val spanInClsEl = el.children[1] as HTMLSpanElement
            assertEquals("rgb(100, 100, 200)", window.getComputedStyle(spanInClsEl).color)
        }

        root.children[2]!!.let { el ->
            val pEl = el
            val spanEl = pEl.firstChild as HTMLSpanElement

            assertEquals("rgb(0, 0, 0)", window.getComputedStyle(pEl).color)
            assertEquals("rgb(0, 0, 0)", window.getComputedStyle(spanEl).color)
        }
    }
}
