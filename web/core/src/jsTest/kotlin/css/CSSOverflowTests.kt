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

        assertEquals("visible", (root.children[0] as HTMLElement).style.overflowX)
        assertEquals("hidden", (root.children[1] as HTMLElement).style.overflowX)
        assertEquals("clip", (root.children[2] as HTMLElement).style.overflowX)
        assertEquals("scroll", (root.children[3] as HTMLElement).style.overflowX)
        assertEquals("auto", (root.children[4] as HTMLElement).style.overflowX)
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

        assertEquals("visible", (root.children[0] as HTMLElement).style.overflowY)
        assertEquals("hidden", (root.children[1] as HTMLElement).style.overflowY)
        assertEquals("clip", (root.children[2] as HTMLElement).style.overflowY)
        assertEquals("scroll", (root.children[3] as HTMLElement).style.overflowY)
        assertEquals("auto", (root.children[4] as HTMLElement).style.overflowY)
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

        val style = (root.children[0] as HTMLElement).style
        assertEquals("clip", style.overflowX)
        assertEquals("clip", style.overflowY)
    }


}