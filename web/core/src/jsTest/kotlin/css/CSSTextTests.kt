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

class CSSTextTests {

    @Test
    fun fontSize() = runTest {
        composition {
            Div({
                style {
                    fontSize(20.em)
                }
            })
        }

        assertEquals("20em", nextChild().style.fontSize)
    }

    @Test
    fun fontStyle() = runTest {
        composition {
            Div({
                style {
                    fontStyle("italic")
                }
            })
            Div({
                style {
                    fontStyle("oblique")
                }
            })
        }

        assertEquals("italic", nextChild().style.fontStyle)
        assertEquals("oblique", nextChild().style.fontStyle)
    }

    @Test
    fun fontWeight() = runTest {
        composition {
            Div({
                style {
                    fontWeight("bold")
                }
            })
            Div({
                style {
                    fontWeight("bolder")
                }
            })
            Div({
                style {
                    fontWeight("lighter")
                }
            })
            Div({
                style {
                    fontWeight(100)
                }
            })
            Div({
                style {
                    fontWeight(800)
                }
            })
        }

        assertEquals("bold", nextChild().style.fontWeight)
        assertEquals("bolder", nextChild().style.fontWeight)
        assertEquals("lighter", nextChild().style.fontWeight)
        assertEquals("100", nextChild().style.fontWeight)
        assertEquals("800", nextChild().style.fontWeight)
    }

    @Test
    fun lineHeight() = runTest {
        composition {
            Div({
                style {
                    lineHeight("normal")
                }
            })
            Div({
                style {
                    lineHeight(2.em)
                }
            })
        }

        assertEquals("normal", nextChild().style.lineHeight)
        assertEquals("2em", nextChild().style.lineHeight)
    }

    @Test
    fun letterSpacing() = runTest {
        composition {
            Div({
                style {
                    letterSpacing("normal")
                }
            })
            Div({
                style {
                    letterSpacing(2.em)
                }
            })
        }

        assertEquals("normal", nextChild().style.letterSpacing)
        assertEquals("2em", nextChild().style.letterSpacing)
    }

    @Test
    fun fontFamily() = runTest {
        composition {
            Div({
                style {
                    fontFamily("Gill Sans Extrabold", "sans-serif")
                }
            })
            Div({
                style {
                    fontFamily("sans-serif")
                }
            })
        }

        assertEquals("\"Gill Sans Extrabold\", sans-serif", nextChild().style.fontFamily)
        assertEquals("sans-serif", nextChild().style.fontFamily)
    }

    @Test
    fun font() = runTest {
        composition {
            Div({
                style {
                    font("italic bold .8em/1.2 Arial, sans-serif")
                }
            })
        }

        assertEquals("italic bold 0.8em / 1.2 Arial, sans-serif", nextChild().style.font)
    }

    @Test
    fun textAlign() = runTest {
        composition {
            Div({
                style {
                    textAlign("left")
                }
            })
            Div({
                style {
                    textAlign("right")
                }
            })
            Div({
                style {
                    textAlign("center")
                }
            })
            Div({
                style {
                    textAlign("justify")
                }
            })
            Div({
                style {
                    textAlign("start")
                }
            })
            Div({
                style {
                    textAlign("end")
                }
            })
        }

        assertEquals("left", nextChild().style.textAlign)
        assertEquals("right", nextChild().style.textAlign)
        assertEquals("center", nextChild().style.textAlign)
        assertEquals("justify", nextChild().style.textAlign)
        assertEquals("start", nextChild().style.textAlign)
        assertEquals("end", nextChild().style.textAlign)
    }


    @Test
    fun textDecorationColor() = runTest {
        composition {
            Div({
                style {
                    textDecorationColor(Color.red)
                }
            })
            Div({
                style {
                    textDecorationColor(rgba(0, 200, 20, 0.85))
                }
            })
        }

        assertEquals("red", nextChild().style.textDecorationColor)
        assertEquals("rgba(0, 200, 20, 0.85)", nextChild().style.textDecorationColor)
    }

    @Test
    fun textDecorationStyle() = runTest {
        composition {
            Div({
                style {
                    textDecorationStyle("solid")
                }
            })
            Div({
                style {
                    textDecorationStyle("double")
                }
            })
            Div({
                style {
                    textDecorationStyle("dotted")
                }
            })
            Div({
                style {
                    textDecorationStyle("dashed")
                }
            })
            Div({
                style {
                    textDecorationStyle("wavy")
                }
            })
        }

        assertEquals("solid", nextChild().style.textDecorationStyle)
        assertEquals("double", nextChild().style.textDecorationStyle)
        assertEquals("dotted", nextChild().style.textDecorationStyle)
        assertEquals("dashed", nextChild().style.textDecorationStyle)
        assertEquals("wavy", nextChild().style.textDecorationStyle)
    }

    @Test
    fun textDecorationThickness() = runTest {
        composition {
            Div({
                style {
                    textDecorationThickness("auto")
                }
            })
            Div({
                style {
                    textDecorationThickness("from-font")
                }
            })
            Div({
                style {
                    textDecorationThickness(10.px)
                }
            })
            Div({
                style {
                    textDecorationThickness(2.percent)
                }
            })
        }

        assertEquals("text-decoration-thickness: auto;", nextChild().style.cssText)
        assertEquals("text-decoration-thickness: from-font;", nextChild().style.cssText)
        assertEquals("text-decoration-thickness: 10px;", nextChild().style.cssText)
        assertEquals("text-decoration-thickness: 2%;", nextChild().style.cssText)
    }

    @Test
    fun textDecorationLine() = runTest {
        composition {
            Div({
                style {
                    textDecorationLine("none")
                }
            })
            Div({
                style {
                    textDecorationLine("underline")
                }
            })
            Div({
                style {
                    textDecorationLine("overline")
                }
            })
            Div({
                style {
                    textDecorationLine("line-through")
                }
            })
            Div({
                style {
                    textDecorationLine("blink")
                }
            })
        }

        assertEquals("none", nextChild().style.textDecorationLine)
        assertEquals("underline", nextChild().style.textDecorationLine)
        assertEquals("overline", nextChild().style.textDecorationLine)
        assertEquals("line-through", nextChild().style.textDecorationLine)
        assertEquals("blink", nextChild().style.textDecorationLine)
    }

    @Test
    fun textDecoration() = runTest {
        composition {
            Div({
                style {
                    textDecoration("underline red")
                }
            })
            Div({
                style {
                    textDecoration("wavy overline lime")
                }
            })
            Div({
                style {
                    textDecoration("line-through")
                }
            })
            Div({
                style {
                    textDecoration("dashed underline overline")
                }
            })
        }

        assertEquals("underline red", nextChild().style.textDecoration)
        assertEquals("overline wavy lime", nextChild().style.textDecoration)
        assertEquals("line-through", nextChild().style.textDecoration)
        assertEquals("underline overline dashed", nextChild().style.textDecoration)
    }

    @Test
    fun whiteSpace() = runTest {
        composition {
            Div({
                style {
                    whiteSpace("normal")
                }
            })
            Div({
                style {
                    whiteSpace("nowrap")
                }
            })
            Div({
                style {
                    whiteSpace("pre")
                }
            })
            Div({
                style {
                    whiteSpace("pre-wrap")
                }
            })
            Div({
                style {
                    whiteSpace("pre-line")
                }
            })
            Div({
                style {
                    whiteSpace("break-spaces")
                }
            })
        }

        assertEquals("normal", nextChild().style.whiteSpace)
        assertEquals("nowrap", nextChild().style.whiteSpace)
        assertEquals("pre", nextChild().style.whiteSpace)
        assertEquals("pre-wrap", nextChild().style.whiteSpace)
        assertEquals("pre-line", nextChild().style.whiteSpace)
        assertEquals("break-spaces", nextChild().style.whiteSpace)
    }

}
