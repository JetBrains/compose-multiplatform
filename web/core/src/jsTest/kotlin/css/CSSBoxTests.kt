/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests.css

import org.jetbrains.compose.web.core.tests.asHtmlElement
import org.jetbrains.compose.web.core.tests.runTest
import org.jetbrains.compose.web.css.boxSizing
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.renderComposable
import org.w3c.dom.HTMLElement
import org.w3c.dom.get
import kotlin.test.Test
import kotlin.test.assertEquals

class CSSBoxTests {

    @Test
    fun stylesWidth() = runTest {
        composition {
            Div(
                {
                    style {
                        width(100.px)
                    }
                }
            )
        }

        assertEquals("100px", (root.children[0] as HTMLElement).style.width)
    }

    @Test
    fun stylesHeight() = runTest {
        composition {
            Div(
                {
                    style {
                        height(90.px)
                    }
                }
            )
        }

        assertEquals("90px", (root.children[0] as HTMLElement).style.height)
    }

    @Test
    fun boxSizing() = runTest {
        composition {
            Div(
                {
                    style {
                        boxSizing("border-box")
                    }
                }
            )
            Div(
                {
                    style {
                        boxSizing("content-box")
                    }
                }
            )
        }

        assertEquals("border-box", (root.children[0] as HTMLElement).style.boxSizing)
        assertEquals("content-box", (root.children[1] as HTMLElement).style.boxSizing)
    }


}