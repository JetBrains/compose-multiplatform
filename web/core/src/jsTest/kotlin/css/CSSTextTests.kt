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


}