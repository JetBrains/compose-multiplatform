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

class CSSBorderTests {

    @Test
    fun borderRadius() = runTest {
        composition {
            Div(
                {
                    style {
                        borderRadius(3.px)
                    }
                }
            )
            Div(
                {
                    style {
                        borderRadius(3.px, 5.px)
                    }
                }
            )
            Div(
                {
                    style {
                        borderRadius(3.px, 5.px, 4.px)
                    }
                }
            )
            Div(
                {
                    style {
                        borderRadius(3.px, 5.px, 4.px, 1.px)
                    }
                }
            )
        }

        assertEquals("3px", (root.children[0] as HTMLElement).style.borderRadius)
        assertEquals("3px 5px", (root.children[1] as HTMLElement).style.borderRadius)
        assertEquals("3px 5px 4px", (root.children[2] as HTMLElement).style.borderRadius)
        assertEquals(
            "3px 5px 4px 1px",
            (root.children[3] as HTMLElement).style.borderRadius    
        )
    }


    @Test
    fun borderWidth() = runTest {
        composition {
            Div(
                {
                    style {
                        borderWidth(2.px)
                    }
                }
            )
            Div(
                {
                    style {
                        borderWidth(3.px, 7.px)
                    }
                }
            )
            Div(
                {
                    style {
                        borderWidth(3.px, 5.px, 4.px)
                    }
                }
            )
            Div(
                {
                    style {
                        borderWidth(3.px, 5.px, 4.px, 2.px)
                    }
                }
            )
        }

        assertEquals("2px", (root.children[0] as HTMLElement).style.borderWidth)
        assertEquals("3px 7px", (root.children[1] as HTMLElement).style.borderWidth)
        assertEquals("3px 5px 4px", (root.children[2] as HTMLElement).style.borderWidth)
        assertEquals(
            "3px 5px 4px 2px",
            (root.children[3] as HTMLElement).style.borderWidth
        )
    }
}