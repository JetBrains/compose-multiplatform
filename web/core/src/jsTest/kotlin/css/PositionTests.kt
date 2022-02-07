/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests.css

import org.jetbrains.compose.web.core.tests.values
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.testutils.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PositionTests {
    @Test
    fun stylesTop() = runTest {
        composition {
            Div({ style { top(100.px) } })
            Div({ style { top(100.percent) } })
        }

        assertEquals("100px", nextChild().style.top)
        assertEquals("100%", nextChild().style.top)
    }

    @Test
    fun stylesBottom() = runTest {
        composition {
            Div({ style { bottom(100.px) } })
            Div({ style { bottom(100.percent) } })
        }

        assertEquals("100px", nextChild().style.bottom)
        assertEquals("100%", nextChild().style.bottom)
    }

    @Test
    fun stylesLeft() = runTest {
        composition {
            Div({ style { left(100.px) } })
            Div({ style { left(100.percent) } })
        }

        assertEquals("100px", nextChild().style.left)
        assertEquals("100%", nextChild().style.left)
    }

    @Test
    fun stylesRight() = runTest {
        composition {
            Div({ style { right(100.px) } })
            Div({ style { right(100.percent) } })
        }

        assertEquals("100px", nextChild().style.right)
        assertEquals("100%", nextChild().style.right)
    }

    @Test
    fun stylesPosition() = runTest {
        val enumValues = Position.values()

        composition {
            enumValues.forEach { position ->
                Span(
                    {
                        style {
                            position(position)
                        }
                    }
                )
            }
        }

        enumValues.forEach { position ->
            assertEquals(
                "position: ${position.value};",
                nextChild().style.cssText
            )
        }
    }
}