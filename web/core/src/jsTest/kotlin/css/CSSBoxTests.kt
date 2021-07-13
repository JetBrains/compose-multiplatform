/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests.css

import org.jetbrains.compose.web.core.tests.asHtmlElement
import org.jetbrains.compose.web.core.tests.runTest
import org.jetbrains.compose.web.css.*
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

    @Test
    fun outlineWidth() = runTest {
        composition {
            Div(
                {
                    style {
                        outlineWidth("thin")
                    }
                }
            )
            Div(
                {
                    style {
                        outlineWidth("medium")
                    }
                }
            )
            Div(
                {
                    style {
                        outlineWidth("thick")
                    }
                }
            )
            Div(
                {
                    style {
                        outlineWidth(8.px)
                    }
                }
            )
            Div(
                {
                    style {
                        outlineWidth(0.1.em)
                    }
                }
            )
        }

        assertEquals("thin", (root.children[0] as HTMLElement).style.outlineWidth)
        assertEquals("medium", (root.children[1] as HTMLElement).style.outlineWidth)
        assertEquals("thick", (root.children[2] as HTMLElement).style.outlineWidth)
        assertEquals("8px", (root.children[3] as HTMLElement).style.outlineWidth)
        assertEquals("0.1em", (root.children[4] as HTMLElement).style.outlineWidth)
    }

    @Test
    fun outlineColor() = runTest {
        composition {
            Div({
                style {
                    outlineColor("red")
                }
            })
            Div({
                style {
                    outlineColor("#32a1ce")
                }
            })
        }

        assertEquals("red", (root.children[0] as HTMLElement).style.outlineColor)
        assertEquals("rgb(50, 161, 206)", (root.children[1] as HTMLElement).style.outlineColor)
    }

    @Test
    fun outlineStyle() = runTest {
        composition {
            Div({ style { outlineStyle("dotted") } })
            Div({ style { outlineStyle("dashed") } })
            Div({ style { outlineStyle("solid") } })
            Div({ style { outlineStyle("double") } })
            Div({ style { outlineStyle("groove") } })
            Div({ style { outlineStyle("ridge") } })
            Div({ style { outlineStyle("outset") } })
            Div({ style { outlineStyle("inset") } })
        }

        assertEquals("dotted", (root.children[0] as HTMLElement).style.outlineStyle)
        assertEquals("dashed", (root.children[1] as HTMLElement).style.outlineStyle)
        assertEquals("solid", (root.children[2] as HTMLElement).style.outlineStyle)
        assertEquals("double", (root.children[3] as HTMLElement).style.outlineStyle)
        assertEquals("groove", (root.children[4] as HTMLElement).style.outlineStyle)
        assertEquals("ridge", (root.children[5] as HTMLElement).style.outlineStyle)
        assertEquals("outset", (root.children[6] as HTMLElement).style.outlineStyle)
        assertEquals("inset", (root.children[7] as HTMLElement).style.outlineStyle)
    }

    @Test
    fun outlineOneValue() = runTest {
        composition {
            Div({ style { outline("dotted") } })
        }

        assertEquals("dotted", (root.children[0] as HTMLElement).style.outlineStyle)
    }

    @Test
    fun outlineTwoValues() = runTest {
        composition {
            Div({ style { outline("#f66", "dashed") } })
            Div({ style { outline("inset", "thick") } })
            Div({ style { outline("ridge", 3.px) } })
        }

        (root.children[0] as HTMLElement).let {
            assertEquals("rgb(255, 102, 102)", it.style.outlineColor)
            assertEquals("dashed", it.style.outlineStyle)
        }

        (root.children[1] as HTMLElement).let {
            assertEquals("inset", it.style.outlineStyle)
            assertEquals("thick", it.style.outlineWidth)
        }

        (root.children[2] as HTMLElement).let {
            assertEquals("ridge", it.style.outlineStyle)
            assertEquals("3px", it.style.outlineWidth)
        }
    }

    @Test
    fun outlineThreeValues() = runTest {
        composition {
            Div({ style { outline(Color.RGB(0, 20, 100), "dashed", "thick") } })
            Div({ style { outline(Color.RGB(0, 100, 20), "double", 4.px) } })
            Div({ style { outline("red", "outset", "thin") } })
            Div({ style { outline("yellow", "inset", 8.px) } })
        }

        assertEquals("rgb(0, 20, 100) dashed thick", (root.children[0] as HTMLElement).style.outline)
        assertEquals("rgb(0, 100, 20) double 4px", (root.children[1] as HTMLElement).style.outline)
        assertEquals("red outset thin", (root.children[2] as HTMLElement).style.outline)
        assertEquals("yellow inset 8px", (root.children[3] as HTMLElement).style.outline)
    }

}