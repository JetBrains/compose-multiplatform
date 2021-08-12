/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests.css

import kotlinx.browser.window
import org.jetbrains.compose.web.core.tests.runTest
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.HTMLElement
import org.w3c.dom.get
import kotlin.test.Test
import kotlin.test.assertEquals

class CSSPaddingTests {
    @Test
    fun paddingLeft() = runTest {
        composition {
            Div({
                style {
                    paddingLeft(5.px)
                }
            })
        }

        assertEquals("5px", (nextChild()).style.paddingLeft)
    }

    @Test
    fun paddingTop() = runTest {
        composition {
            Div({
                style {
                    paddingTop(15.px)
                }
            })
        }

        assertEquals("15px", (nextChild()).style.paddingTop)
    }

    @Test
    fun paddingRight() = runTest {
        composition {
            Div({
                style {
                    paddingRight(12.vw)
                }
            })
        }

        assertEquals("12vw", (nextChild()).style.paddingRight)
    }

    @Test
    fun paddingBottom() = runTest {
        composition {
            Div({
                style {
                    paddingBottom(12.percent)
                }
            })
        }

        assertEquals("12%", (nextChild()).style.paddingBottom)
    }

    @Test
    fun paddingWithOneValue() = runTest {
        composition {
            Div({
                style {
                    padding(4.px)
                }
            })
        }

        val el = window.getComputedStyle(nextChild())

        assertEquals("4px", el.paddingTop, "paddingTop")
        assertEquals("4px", el.paddingRight, "paddingRight")
        assertEquals("4px", el.paddingBottom, "paddingBottom")
        assertEquals("4px", el.paddingLeft, "paddingLeft")
    }

    @Test
    fun paddingWithTwoValues() = runTest {
        composition {
            Div({
                style {
                    padding(4.px, 6.px)
                }
            })
        }

        val el = window.getComputedStyle(nextChild())

        assertEquals("4px", el.paddingTop, "paddingTop")
        assertEquals("6px", el.paddingRight, "paddingRight")
        assertEquals("4px", el.paddingBottom, "paddingBottom")
        assertEquals("6px", el.paddingLeft, "paddingLeft")
    }

    @Test
    fun paddingWithThreeValues() = runTest {
        composition {
            Div({
                style {
                    padding(4.px, 6.px, 3.px)
                }
            })
        }

        val el = window.getComputedStyle(nextChild())

        assertEquals("4px", el.paddingTop, "paddingTop")
        assertEquals("6px", el.paddingRight, "paddingRight")
        assertEquals("3px", el.paddingBottom, "paddingBottom")
        assertEquals("6px", el.paddingLeft, "paddingLeft")
    }

    @Test
    fun paddingWithFourValues() = runTest {
        composition {
            Div({
                style {
                    padding(4.px, 6.px, 3.px, 1.px)
                }
            })
        }

        val el = window.getComputedStyle(nextChild())

        assertEquals("4px", el.paddingTop, "paddingTop")
        assertEquals("6px", el.paddingRight, "paddingRight")
        assertEquals("3px", el.paddingBottom, "paddingBottom")
        assertEquals("1px", el.paddingLeft, "paddingLeft")
    }

}