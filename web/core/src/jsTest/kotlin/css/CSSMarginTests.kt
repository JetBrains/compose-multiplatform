/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests.css

import org.jetbrains.compose.web.testutils.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import kotlin.test.Test
import kotlin.test.assertEquals

class CSSMarginTests {
    @Test
    fun marginLeft() = runTest {
        composition {
            Div({
                style {
                    marginLeft(5.px)
                }
            })
        }

        assertEquals("5px", nextChild().style.marginLeft)
    }

    @Test
    fun marginTop() = runTest {
        composition {
            Div({
                style {
                    marginTop(15.px)
                }
            })
        }

        assertEquals("15px", nextChild().style.marginTop)
    }

    @Test
    fun marginRight() = runTest {
        composition {
            Div({
                style {
                    marginRight(12.vw)
                }
            })
        }

        assertEquals("12vw", nextChild().style.marginRight)
    }

    @Test
    fun marginBottom() = runTest {
        composition {
            Div({
                style {
                    marginBottom(12.percent)
                }
            })
        }

        assertEquals("12%", nextChild().style.marginBottom)
    }

    @Test
    fun marginWithOneValue() = runTest {
        composition {
            Div({
                style {
                    margin(4.px)
                }
            })
        }

        val el = nextChild().computedStyle

        assertEquals("4px", el.marginTop, "marginTop")
        assertEquals("4px", el.marginRight, "marginRight")
        assertEquals("4px", el.marginBottom, "marginBottom")
        assertEquals("4px", el.marginLeft, "marginLeft")
    }

    @Test
    fun marginWithTwoValues() = runTest {
        composition {
            Div({
                style {
                    margin(4.px, 6.px)
                }
            })
        }

        val el = nextChild().computedStyle

        assertEquals("4px", el.marginTop, "marginTop")
        assertEquals("6px", el.marginRight, "marginRight")
        assertEquals("4px", el.marginBottom, "marginBottom")
        assertEquals("6px", el.marginLeft, "marginLeft")
    }

    @Test
    fun marginWithThreeValues() = runTest {
        composition {
            Div({
                style {
                    margin(4.px, 6.px, 3.px)
                }
            })
        }

        val el = nextChild().computedStyle

        assertEquals("4px", el.marginTop, "marginTop")
        assertEquals("6px", el.marginRight, "marginRight")
        assertEquals("3px", el.marginBottom, "marginBottom")
        assertEquals("6px", el.marginLeft, "marginLeft")
    }

    @Test
    fun marginWithFourValues() = runTest {
        composition {
            Div({
                style {
                    margin(4.px, 6.px, 3.px, 1.px)
                }
            })
        }

        val el = nextChild().computedStyle

        assertEquals("4px", el.marginTop, "marginTop")
        assertEquals("6px", el.marginRight, "marginRight")
        assertEquals("3px", el.marginBottom, "marginBottom")
        assertEquals("1px", el.marginLeft, "marginLeft")
    }

}
