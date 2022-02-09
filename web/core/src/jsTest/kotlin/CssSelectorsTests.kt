package org.jetbrains.compose.web.core.tests

import kotlinx.browser.window
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.css.selectors.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.testutils.computedStyle
import org.jetbrains.compose.web.testutils.runTest
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLParagraphElement
import org.w3c.dom.HTMLSpanElement
import org.w3c.dom.get
import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("DEPRECATION")
class CssSelectorsTests {

    private object TestSimpleDescendantsSelectorStyleSheet: StyleSheet() {
        val cls1 by style {
            "p" {
                color(Color.red)
            }
        }
    }

    @Test
    fun testPlusOperator() = runTest {
        val selectorScope = object : SelectorsScope {}

        with(selectorScope) {
            assertEquals("h1:hover", (selector("h1") + hover()).toString())

            assertEquals(
                "h1:hover:enabled",
                ((selector("h1") + hover()) + CSSSelector.PseudoClass.enabled).toString()
            )

            assertEquals(
                "h1:hover",
                (selector("h1") + ":hover").toString()
            )

            assertEquals(
                "h1:hover:enabled",
                ((selector("h1") + hover()) + ":enabled").toString()
            )

            assertEquals(
                "h1:hover:enabled",
                (selector("h1") + combine(hover(), selector(":enabled"))).toString()
            )
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
            assertEquals("rgb(255, 0, 0)", pEl.computedStyle.color)
        }

        (root.children[2] as HTMLElement).let { el ->
            assertEquals("rgb(0, 0, 0)", el.computedStyle.color)
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

            assertEquals("rgb(255, 0, 0)", pEl.computedStyle.color)
            assertEquals("rgb(0, 0, 255)", spanInPel.computedStyle.color)
        }

        (root.children[2] as HTMLElement).let { el ->
            val spanEl = el.firstChild as HTMLSpanElement

            assertEquals("rgb(0, 0, 0)", el.computedStyle.color)
            assertEquals("rgb(0, 0, 0)", spanEl.computedStyle.color)
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

            assertEquals("rgb(255, 0, 0)", pEl.computedStyle.color)
            assertEquals("rgb(0, 0, 255)", spanInPel.computedStyle.color)


            val spanInClsEl = el.children[1] as HTMLSpanElement
            assertEquals("rgb(100, 100, 200)", spanInClsEl.computedStyle.color)
        }

        (root.children[2] as HTMLParagraphElement).let { el ->
            val spanEl = el.firstChild as HTMLSpanElement

            assertEquals("rgb(0, 0, 0)", el.computedStyle.color)
            assertEquals("rgb(0, 0, 0)", window.getComputedStyle(spanEl).color)
        }
    }
}
