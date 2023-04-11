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

class CSSOverflowTests {
    @Test
    fun overflowX() = runTest {
        composition {
            Div({
                style {
                    overflowX("visible")
                }
            })
            Div({
                style {
                    overflowX("hidden")
                }
            })
            Div({
                style {
                    overflowX("clip")
                }
            })
            Div({
                style {
                    overflowX("scroll")
                }
            })
            Div({
                style {
                    overflowX("auto")
                }
            })
        }

        assertEquals("visible", nextChild().style.overflowX)
        assertEquals("hidden", nextChild().style.overflowX)
        assertEquals("clip", nextChild().style.overflowX)
        assertEquals("scroll", nextChild().style.overflowX)
        assertEquals("auto", nextChild().style.overflowX)
    }

    @Test
    fun overflowY() = runTest {
        composition {
            Div({
                style {
                    overflowY("visible")
                }
            })
            Div({
                style {
                    overflowY("hidden")
                }
            })
            Div({
                style {
                    overflowY("clip")
                }
            })
            Div({
                style {
                    overflowY("scroll")
                }
            })
            Div({
                style {
                    overflowY("auto")
                }
            })
        }

        assertEquals("visible", nextChild().style.overflowY)
        assertEquals("hidden", nextChild().style.overflowY)
        assertEquals("clip", nextChild().style.overflowY)
        assertEquals("scroll", nextChild().style.overflowY)
        assertEquals("auto", nextChild().style.overflowY)
    }

    @Test
    fun overflow() = runTest {
        composition {
            Div({
                style {
                    overflow("clip")
                }
            })
        }

        val style = nextChild().style
        assertEquals("clip", style.overflowX)
        assertEquals("clip", style.overflowY)
    }


}
