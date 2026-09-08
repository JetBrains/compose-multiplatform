/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests.css

import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.paddingBottom
import org.jetbrains.compose.web.css.paddingLeft
import org.jetbrains.compose.web.css.paddingRight
import org.jetbrains.compose.web.css.paddingTop
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.vw
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.testutils.computedStyle
import org.jetbrains.compose.web.testutils.runTest
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

        assertEquals("5px", nextChild().style.paddingLeft)
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

        assertEquals("15px", nextChild().style.paddingTop)
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

        assertEquals("12vw", nextChild().style.paddingRight)
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

        assertEquals("12%", nextChild().style.paddingBottom)
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

        val el = nextChild().computedStyle

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

        val el = nextChild().computedStyle

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

        val el = nextChild().computedStyle

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

        val el = nextChild().computedStyle

        assertEquals("4px", el.paddingTop, "paddingTop")
        assertEquals("6px", el.paddingRight, "paddingRight")
        assertEquals("3px", el.paddingBottom, "paddingBottom")
        assertEquals("1px", el.paddingLeft, "paddingLeft")
    }

}
