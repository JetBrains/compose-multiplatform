/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests.css

import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import kotlin.test.Test
import kotlin.test.assertEquals
import org.jetbrains.compose.web.testutils.*

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

        assertEquals("rgb(0, 128, 0)", nextChild().computedStyle.backgroundColor)
        assertEquals("rgba(0, 129, 0, 0.2)", nextChild().computedStyle.backgroundColor)
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

        assertEquals("scroll", nextChild().computedStyle.backgroundAttachment)
        assertEquals("fixed", nextChild().computedStyle.backgroundAttachment)
        assertEquals("local", nextChild().computedStyle.backgroundAttachment)
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

        assertEquals("url(\"https://localhost:3333/media/examples/lizard.png\")", nextChild().computedStyle.backgroundImage)
        assertEquals("url(\"https://localhost:3333/media/examples/lizard.png\"), url(\"https://localhost:3333/media/examples/star.png\")", nextChild().computedStyle.backgroundImage)
        assertEquals("linear-gradient(rgba(0, 0, 255, 0.5), rgba(255, 255, 0, 0.5))", nextChild().computedStyle.backgroundImage)
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

        assertEquals("50% 0%", nextChild().computedStyle.backgroundPosition)
        assertEquals("0% 50%", nextChild().computedStyle.backgroundPosition)
        assertEquals("50% 50%", nextChild().computedStyle.backgroundPosition)
        assertEquals("25% 75%", nextChild().computedStyle.backgroundPosition)
    }

    @Test
    fun backgroundRepeat() = runTest {
        composition {
            Div({style {
                backgroundRepeat("space repeat")
            }})
        }

        assertEquals("space repeat", nextChild().computedStyle.backgroundRepeat)
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


        assertEquals("border-box", nextChild().computedStyle.backgroundClip)
        assertEquals("padding-box", nextChild().computedStyle.backgroundClip)
        assertEquals("content-box", nextChild().computedStyle.backgroundClip)
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


        assertEquals("border-box", nextChild().computedStyle.backgroundOrigin)
        assertEquals("padding-box", nextChild().computedStyle.backgroundOrigin)
        assertEquals("content-box", nextChild().computedStyle.backgroundOrigin)
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

        assertEquals("contain", nextChild().computedStyle.backgroundSize)
        assertEquals("cover", nextChild().computedStyle.backgroundSize)
        assertEquals("50%", nextChild().computedStyle.backgroundSize)
        assertEquals("auto 50px", nextChild().computedStyle.backgroundSize)
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

        assertEquals("rgb(0, 128, 0)", nextChild().computedStyle.backgroundColor)

        with(nextChild().computedStyle) {
            assertEquals("content-box", backgroundOrigin)
            assertEquals("radial-gradient(rgb(220, 20, 60), rgb(135, 206, 235))", backgroundImage)
        }

        assertEquals("no-repeat", nextChild().computedStyle.backgroundRepeat)
    }

}
