/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests.css

import org.jetbrains.compose.web.ExperimentalComposeWebApi
import org.jetbrains.compose.web.testutils.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Img
import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalComposeWebApi
class FilterTests {
    @Test
    fun blur() = runTest {
        composition {
            Img(src = "icon.png", attrs = { style { filter { blur(10.px) } } })
        }

        assertEquals("blur(10px)", nextChild().style.filter)
    }

    @Test
    fun brightness() = runTest {
        composition {
            Div({ style { filter { brightness(1.75) } } })
            Div({ style { filter { brightness(200.percent) } } })
        }

        assertEquals("brightness(1.75)", nextChild().style.filter)
        assertEquals("brightness(200%)", nextChild().style.filter)
    }

    @Test
    fun contrast() = runTest {
        composition {
            Div({ style { filter { contrast(2.75) } } })
            Div({ style { filter { contrast(177.percent) } } })
        }

        assertEquals("contrast(2.75)", nextChild().style.filter)
        assertEquals("contrast(177%)", nextChild().style.filter)
    }

    @Test
    fun grayscale() = runTest {
        composition {
            Div({ style { filter { grayscale(0.15) } } })
            Div({ style { filter { grayscale(90.percent) } } })
        }

        assertEquals("grayscale(0.15)", nextChild().style.filter)
        assertEquals("grayscale(90%)", nextChild().style.filter)
    }

    @Test
    fun hueRotate() = runTest {
        composition {
            Div({ style { filter { hueRotate(90.deg) } } })
            Div({ style { filter { hueRotate(0.5.turn) } } })
        }

        assertEquals("hue-rotate(90deg)", nextChild().style.filter)
        assertEquals("hue-rotate(0.5turn)", nextChild().style.filter)
    }

    @Test
    fun invert() = runTest {
        composition {
            Div({ style { filter { invert(0.75) } } })
            Div({ style { filter { invert(30.percent) } } })
        }

        assertEquals("invert(0.75)", nextChild().style.filter)
        assertEquals("invert(30%)", nextChild().style.filter)
    }

    @Test
    fun opacity() = runTest {
        composition {
            Div({ style { filter { opacity(.25) } } })
            Div({ style { filter { opacity(30.percent) } } })
        }

        assertEquals("opacity(0.25)", nextChild().style.filter)
        assertEquals("opacity(30%)", nextChild().style.filter)
    }

    @Test
    fun saturate() = runTest {
        composition {
            Div({ style { filter { saturate(.25) } } })
            Div({ style { filter { saturate(20.percent) } } })
        }

        assertEquals("saturate(0.25)", nextChild().style.filter)
        assertEquals("saturate(20%)", nextChild().style.filter)
    }

    @Test
    fun sepia() = runTest {
        composition {
            Div({ style { filter { sepia(.95) } } })
            Div({ style { filter { sepia(80.percent) } } })
        }

        assertEquals("sepia(0.95)", nextChild().style.filter)
        assertEquals("sepia(80%)", nextChild().style.filter)
    }

    @Test
    fun dropShadow() = runTest {
        composition {
            Div({ style { filter { dropShadow(10.em, 5.px) } } })
            Div({ style { filter { dropShadow(7.px, 2.px, 20.px) } } })
            Div({ style { filter { dropShadow(7.px, 2.px, Color.yellow) } } })
            Div({ style { filter { dropShadow(16.px, 16.px, 10.px, Color.black) } } })
        }

        assertEquals("drop-shadow(10em 5px)", nextChild().style.filter)
        assertEquals("drop-shadow(7px 2px 20px)", nextChild().style.filter)
        assertEquals("drop-shadow(yellow 7px 2px)", nextChild().style.filter)
        assertEquals("drop-shadow(black 16px 16px 10px)", nextChild().style.filter)
    }

}
