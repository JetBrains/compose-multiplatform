/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests.css

import org.jetbrains.compose.web.core.tests.runTest
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

        assertEquals("none", (root.children[0] as HTMLElement).style.listStyleImage)
        assertEquals("url(\"starsolid.gif\")", (root.children[1] as HTMLElement).style.listStyleImage)
        assertEquals("linear-gradient(to left bottom, red, blue)", (root.children[2] as HTMLElement).style.listStyleImage)
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

        assertEquals("inside", (root.children[0] as HTMLElement).style.listStylePosition)
        assertEquals("outside", (root.children[1] as HTMLElement).style.listStylePosition)
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

        assertEquals("armenian", (root.children[0] as HTMLElement).style.listStyleType)
        assertEquals("disc", (root.children[1] as HTMLElement).style.listStyleType)
        assertEquals("circle", (root.children[2] as HTMLElement).style.listStyleType)
        assertEquals("square", (root.children[3] as HTMLElement).style.listStyleType)
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

        assertEquals("inside", (root.children[0] as HTMLElement).style.listStylePosition)
        assertEquals("georgian", (root.children[0] as HTMLElement).style.listStyleType)
    }

}