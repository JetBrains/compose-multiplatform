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

        assertEquals("100px", nextChild().style.width)
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

        assertEquals("90px", nextChild().style.height)
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

        assertEquals("border-box", nextChild().style.boxSizing)
        assertEquals("content-box", nextChild().style.boxSizing)
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

        assertEquals("thin", nextChild().style.outlineWidth)
        assertEquals("medium", nextChild().style.outlineWidth)
        assertEquals("thick", nextChild().style.outlineWidth)
        assertEquals("8px", nextChild().style.outlineWidth)
        assertEquals("0.1em", (root.children[4] as HTMLElement).style.outlineWidth)
    }

    @Test
    fun outlineColor() = runTest {
        composition {
            Div({
                style {
                    outlineColor(Color.red)
                }
            })
            Div({
                style {
                    outlineColor(Color("#32a1ce"))
                }
            })
        }

        assertEquals("red", nextChild().style.outlineColor)
        assertEquals("rgb(50, 161, 206)", nextChild().style.outlineColor)
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

        assertEquals("dotted", nextChild().style.outlineStyle)
        assertEquals("dashed", nextChild().style.outlineStyle)
        assertEquals("solid", nextChild().style.outlineStyle)
        assertEquals("double", nextChild().style.outlineStyle)
        assertEquals("groove", nextChild().style.outlineStyle)
        assertEquals("ridge", nextChild().style.outlineStyle)
        assertEquals("outset", nextChild().style.outlineStyle)
        assertEquals("inset", nextChild().style.outlineStyle)
    }

    @Test
    fun outlineOneValue() = runTest {
        composition {
            Div({ style { outline("dotted") } })
        }

        assertEquals("dotted", nextChild().style.outlineStyle)
    }

    @Test
    fun outlineTwoValues() = runTest {
        composition {
            Div({ style { outline("#f66", "dashed") } })
            Div({ style { outline("inset", "thick") } })
            Div({ style { outline("ridge", 3.px) } })
        }

        nextChild().let {
            assertEquals("rgb(255, 102, 102)", it.style.outlineColor)
            assertEquals("dashed", it.style.outlineStyle)
        }

        nextChild().let {
            assertEquals("inset", it.style.outlineStyle)
            assertEquals("thick", it.style.outlineWidth)
        }

        nextChild().let {
            assertEquals("ridge", it.style.outlineStyle)
            assertEquals("3px", it.style.outlineWidth)
        }
    }

    @Test
    fun outlineThreeValues() = runTest {
        composition {
            Div({ style { outline(rgb(0, 20, 100), "dashed", "thick") } })
            Div({ style { outline(rgb(0, 100, 20), "double", 4.px) } })
            Div({ style { outline("red", "outset", "thin") } })
            Div({ style { outline("yellow", "inset", 8.px) } })
        }

        assertEquals("rgb(0, 20, 100) dashed thick", nextChild().style.outline)
        assertEquals("rgb(0, 100, 20) double 4px", nextChild().style.outline)
        assertEquals("red outset thin", nextChild().style.outline)
        assertEquals("yellow inset 8px", nextChild().style.outline)
    }

    @Test
    fun minWidth() = runTest {
        composition {
            Div({ style { minWidth(3.5.em) } })
            Div({ style { minWidth(75.percent) } })
            Div({ style { minWidth("max-content") } })
            Div({ style { minWidth("min-content") } })
        }

        assertEquals("3.5em", nextChild().style.minWidth)
        assertEquals("75%", nextChild().style.minWidth)
        assertEquals("max-content", nextChild().style.minWidth)
        assertEquals("min-content", nextChild().style.minWidth)
    }

    @Test
    fun maxWidth() = runTest {
        composition {
            Div({ style { maxWidth(0.5.em) } })
            Div({ style { maxWidth(10.percent) } })
            Div({ style { maxWidth("max-content") } })
            Div({ style { maxWidth("min-content") } })
        }

        assertEquals("0.5em", nextChild().style.maxWidth)
        assertEquals("10%", nextChild().style.maxWidth)
        assertEquals("max-content", nextChild().style.maxWidth)
        assertEquals("min-content", nextChild().style.maxWidth)
    }

    @Test
    fun minHeight() = runTest {
        composition {
            Div({ style { minHeight(5.px) } })
            Div({ style { minHeight(25.percent) } })
            Div({ style { minHeight("max-content") } })
            Div({ style { minHeight("min-content") } })
        }

        assertEquals("5px", nextChild().style.minHeight)
        assertEquals("25%", nextChild().style.minHeight)
        assertEquals("max-content", nextChild().style.minHeight)
        assertEquals("min-content", nextChild().style.minHeight)
    }

    @Test
    fun maxHeight() = runTest {
        composition {
            Div({ style { maxHeight(15.px) } })
            Div({ style { maxHeight(35.percent) } })
            Div({ style { maxHeight("max-content") } })
            Div({ style { maxHeight("min-content") } })
        }

        assertEquals("15px", nextChild().style.maxHeight)
        assertEquals("35%", nextChild().style.maxHeight)
        assertEquals("max-content", nextChild().style.maxHeight)
        assertEquals("min-content", nextChild().style.maxHeight)
    }
}
