/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests.css

import org.jetbrains.compose.web.testutils.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.HTMLElement
import org.w3c.dom.get
import kotlin.test.Test
import kotlin.test.assertEquals

class CSSListStyleTests {
    @Test
    fun listStyleImage() = runTest {
        composition {
            Div({
                style {
                    listStyleImage("none")
                }
            })
            Div({
                style {
                    listStyleImage("url('starsolid.gif')")
                }
            })
            Div({
                style {
                    listStyleImage("linear-gradient(to left bottom, red, blue)")
                }
            })

        }

        assertEquals("none", nextChild().style.listStyleImage)
        assertEquals("url(\"starsolid.gif\")", nextChild().style.listStyleImage)
        assertEquals("linear-gradient(to left bottom, red, blue)", nextChild().style.listStyleImage)
    }

    @Test
    fun listStylePosition() = runTest {
        composition {
            Div({
                style {
                    listStylePosition("inside")
                }
            })
            Div({
                style {
                    listStylePosition("outside")
                }
            })
        }

        assertEquals("inside", nextChild().style.listStylePosition)
        assertEquals("outside", nextChild().style.listStylePosition)
    }


    @Test
    fun listStyleType() = runTest {
        composition {
            Div({
                style {
                    listStyleType("armenian")
                }
            })
            Div({
                style {
                    listStyleType("disc")
                }
            })
            Div({
                style {
                    listStyleType("circle")
                }
            })
            Div({
                style {
                    listStyleType("square")
                }
            })
        }

        assertEquals("armenian", nextChild().style.listStyleType)
        assertEquals("disc", nextChild().style.listStyleType)
        assertEquals("circle", nextChild().style.listStyleType)
        assertEquals("square", nextChild().style.listStyleType)
    }

    @Test
    fun listStyle() = runTest {
        composition {
            Div({
                style {
                    listStyle("georgian inside")
                }
            })
        }

        nextChild().style.apply {
            assertEquals("inside", listStylePosition)
            assertEquals("georgian", listStyleType)
        }
    }

}
