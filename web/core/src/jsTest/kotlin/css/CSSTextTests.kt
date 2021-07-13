/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests.css

import kotlinx.browser.window
import org.jetbrains.compose.web.core.tests.runTest
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.HTMLElement
import org.w3c.dom.get
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

        assertEquals("20em", (root.children[0] as HTMLElement).style.fontSize)
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

        assertEquals("italic", (root.children[0] as HTMLElement).style.fontStyle)
        assertEquals("oblique", (root.children[1] as HTMLElement).style.fontStyle)
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

        assertEquals("bold", (root.children[0] as HTMLElement).style.fontWeight)
        assertEquals("bolder", (root.children[1] as HTMLElement).style.fontWeight)
        assertEquals("lighter", (root.children[2] as HTMLElement).style.fontWeight)
        assertEquals("100", (root.children[3] as HTMLElement).style.fontWeight)
        assertEquals("800", (root.children[4] as HTMLElement).style.fontWeight)
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

        assertEquals("normal", (root.children[0] as HTMLElement).style.lineHeight)
        assertEquals("2em", (root.children[1] as HTMLElement).style.lineHeight)
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

        assertEquals("normal", (root.children[0] as HTMLElement).style.letterSpacing)
        assertEquals("2em", (root.children[1] as HTMLElement).style.letterSpacing)
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

        assertEquals("\"Gill Sans Extrabold\", sans-serif", (root.children[0] as HTMLElement).style.fontFamily)
        assertEquals("sans-serif", (root.children[1] as HTMLElement).style.fontFamily)
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

        assertEquals("italic bold 0.8em / 1.2 Arial, sans-serif", (root.children[0] as HTMLElement).style.font)
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

        assertEquals("left", (root.children[0] as HTMLElement).style.textAlign)
        assertEquals("right", (root.children[1] as HTMLElement).style.textAlign)
        assertEquals("center", (root.children[2] as HTMLElement).style.textAlign)
        assertEquals("justify", (root.children[3] as HTMLElement).style.textAlign)
        assertEquals("start", (root.children[4] as HTMLElement).style.textAlign)
        assertEquals("end", (root.children[5] as HTMLElement).style.textAlign)
    }


    @Test
    fun textDecorationColor() = runTest {
        composition {
            Div({
                style {
                    textDecorationColor("red")
                }
            })
            Div({
                style {
                    textDecorationColor(Color.RGBA(0, 200, 20, 0.85))
                }
            })
        }

        assertEquals("red", (root.children[0] as HTMLElement).style.textDecorationColor)
        assertEquals("rgba(0, 200, 20, 0.85)", (root.children[1] as HTMLElement).style.textDecorationColor)
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

        assertEquals("solid", (root.children[0] as HTMLElement).style.textDecorationStyle)
        assertEquals("double", (root.children[1] as HTMLElement).style.textDecorationStyle)
        assertEquals("dotted", (root.children[2] as HTMLElement).style.textDecorationStyle)
        assertEquals("dashed", (root.children[3] as HTMLElement).style.textDecorationStyle)
        assertEquals("wavy", (root.children[4] as HTMLElement).style.textDecorationStyle)
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

        assertEquals("text-decoration-thickness: auto;", (root.children[0] as HTMLElement).style.cssText)
        assertEquals("text-decoration-thickness: from-font;", (root.children[1] as HTMLElement).style.cssText)
        assertEquals("text-decoration-thickness: 10px;", (root.children[2] as HTMLElement).style.cssText)
        assertEquals("text-decoration-thickness: 2%;", (root.children[3] as HTMLElement).style.cssText)
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

        assertEquals("none", (root.children[0] as HTMLElement).style.textDecorationLine)
        assertEquals("underline", (root.children[1] as HTMLElement).style.textDecorationLine)
        assertEquals("overline", (root.children[2] as HTMLElement).style.textDecorationLine)
        assertEquals("line-through", (root.children[3] as HTMLElement).style.textDecorationLine)
        assertEquals("blink", (root.children[4] as HTMLElement).style.textDecorationLine)
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

        assertEquals("underline red", (root.children[0] as HTMLElement).style.textDecoration)
        assertEquals("overline wavy lime", (root.children[1] as HTMLElement).style.textDecoration)
        assertEquals("line-through", (root.children[2] as HTMLElement).style.textDecoration)
        assertEquals("underline overline dashed", (root.children[3] as HTMLElement).style.textDecoration)
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

        assertEquals("normal", (root.children[0] as HTMLElement).style.whiteSpace)
        assertEquals("nowrap", (root.children[1] as HTMLElement).style.whiteSpace)
        assertEquals("pre", (root.children[2] as HTMLElement).style.whiteSpace)
        assertEquals("pre-wrap", (root.children[3] as HTMLElement).style.whiteSpace)
        assertEquals("pre-line", (root.children[4] as HTMLElement).style.whiteSpace)
        assertEquals("break-spaces", (root.children[5] as HTMLElement).style.whiteSpace)
    }

}