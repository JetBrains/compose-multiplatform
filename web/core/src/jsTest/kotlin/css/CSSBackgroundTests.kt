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

class CSSBackgroundTests {
    @Test
    fun backgroundColor() = runTest {
        composition {
            Div({style {
                backgroundColor(rgb(0, 128, 0))
            }})
            Div({style {
                backgroundColor(rgba(0, 129, 0, 0.2))
            }})
        }

        assertEquals("rgb(0, 128, 0)", window.getComputedStyle(nextChild()).backgroundColor)
        assertEquals("rgba(0, 129, 0, 0.2)", window.getComputedStyle(nextChild()).backgroundColor)
    }

    @Test
    fun backgroundAttachment() = runTest {
        composition {
            Div({style {
                backgroundAttachment("scroll")
            }})
            Div({style {
                backgroundAttachment("fixed")
            }})
            Div({style {
                backgroundAttachment("local")
            }})
        }

        assertEquals("scroll", window.getComputedStyle(nextChild()).backgroundAttachment)
        assertEquals("fixed", window.getComputedStyle(nextChild()).backgroundAttachment)
        assertEquals("local", window.getComputedStyle(nextChild()).backgroundAttachment)
    }

    @Test
    fun backgroundImage() = runTest {
        composition {
            Div({style {
                backgroundImage("url(\"https://localhost:3333/media/examples/lizard.png\")")
            }})
            Div({style {
                backgroundImage("url(\"https://localhost:3333/media/examples/lizard.png\"), url(\"https://localhost:3333/media/examples/star.png\")")
            }})
            Div({style {
                backgroundImage("linear-gradient(rgba(0, 0, 255, 0.5), rgba(255, 255, 0, 0.5))")
            }})
        }

        assertEquals("url(\"https://localhost:3333/media/examples/lizard.png\")", window.getComputedStyle(nextChild()).backgroundImage)
        assertEquals("url(\"https://localhost:3333/media/examples/lizard.png\"), url(\"https://localhost:3333/media/examples/star.png\")", window.getComputedStyle(nextChild()).backgroundImage)
        assertEquals("linear-gradient(rgba(0, 0, 255, 0.5), rgba(255, 255, 0, 0.5))", window.getComputedStyle(nextChild()).backgroundImage)
    }

    @Test
    fun backgroundPosition() = runTest {
        composition {
            Div({style {
                backgroundPosition("top")
            }})
            Div({style {
                backgroundPosition("left")
            }})
            Div({style {
                backgroundPosition("center")
            }})
            Div({style {
                backgroundPosition("25% 75%")
            }})
        }

        assertEquals("50% 0%", window.getComputedStyle(nextChild()).backgroundPosition)
        assertEquals("0% 50%", window.getComputedStyle(nextChild()).backgroundPosition)
        assertEquals("50% 50%", window.getComputedStyle(nextChild()).backgroundPosition)
        assertEquals("25% 75%", window.getComputedStyle(nextChild()).backgroundPosition)
    }

    @Test
    fun backgroundRepeat() = runTest {
        composition {
            Div({style {
                backgroundRepeat("space repeat")
            }})
        }

        assertEquals("space repeat", window.getComputedStyle(nextChild()).backgroundRepeat)
    }


    @Test
    fun backgroundClip() = runTest {
        composition {
            Div({style {
                backgroundClip("border-box")
            }})
            Div({style {
                backgroundClip("padding-box")
            }})
            Div({style {
                backgroundClip("content-box")
            }})
        }


        assertEquals("border-box", window.getComputedStyle(nextChild()).backgroundClip)
        assertEquals("padding-box", window.getComputedStyle(nextChild()).backgroundClip)
        assertEquals("content-box", window.getComputedStyle(nextChild()).backgroundClip)
    }

    @Test
    fun backgroundOrigin() = runTest {
        composition {
            Div({style {
                backgroundOrigin("border-box")
            }})
            Div({style {
                backgroundOrigin("padding-box")
            }})
            Div({style {
                backgroundOrigin("content-box")
            }})
        }


        assertEquals("border-box", window.getComputedStyle(nextChild()).backgroundOrigin)
        assertEquals("padding-box", window.getComputedStyle(nextChild()).backgroundOrigin)
        assertEquals("content-box", window.getComputedStyle(nextChild()).backgroundOrigin)
    }


    @Test
    fun backgroundSize() = runTest {
        composition {
            Div({style {
                backgroundSize("contain")
            }})
            Div({style {
                backgroundSize("cover")
            }})
            Div({style {
                backgroundSize("50%")
            }})
            Div({style {
                backgroundSize("auto 50px")
            }})
        }

        assertEquals("contain", window.getComputedStyle(nextChild()).backgroundSize)
        assertEquals("cover", window.getComputedStyle(nextChild()).backgroundSize)
        assertEquals("50%", window.getComputedStyle(nextChild()).backgroundSize)
        assertEquals("auto 50px", window.getComputedStyle(nextChild()).backgroundSize)
    }

    @Test
    fun background() = runTest {
        composition {
            Div({style {
                background("green")
            }})
            Div({style {
                background("content-box radial-gradient(crimson, skyblue)")
            }})
            Div({style {
                background("no-repeat url(\"../../media/examples/lizard.png\")")
            }})
        }

        assertEquals("rgb(0, 128, 0)", window.getComputedStyle(nextChild()).backgroundColor)
        assertEquals("content-box", window.getComputedStyle(nextChild()).backgroundOrigin)
        assertEquals("radial-gradient(rgb(220, 20, 60), rgb(135, 206, 235))", window.getComputedStyle(currentChild()).backgroundImage)
        assertEquals("no-repeat", window.getComputedStyle(nextChild()).backgroundRepeat)
    }

}