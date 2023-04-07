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

class CSSBorderTests {

    @Test
    fun border() = runTest {
        composition {
            Div({ style { property("border", "1px solid red") } })
            Div({ style { border(3.px, color = Color("green")) } })
        }

        assertEquals("1px solid red", nextChild().style.border)
        nextChild().let { el ->
            assertEquals("green", el.style.getPropertyValue("border-color"))
            assertEquals("3px", el.style.getPropertyValue("border-width"))
        }
    }

    @Test
    fun borderThreeValues() = runTest {
        composition {
            Div({ style { border(3.px, LineStyle.Dotted, Color("green")) } })
        }

        assertEquals("3px dotted green", nextChild().style.border)
    }

    @Test
    fun borderRadius() = runTest {
        composition {
            Div({ style { borderRadius(3.px) } })
            Div({ style { borderRadius(3.px, 5.px) } })
            Div({ style { borderRadius(3.px, 5.px, 4.px) } })
            Div({ style { borderRadius(3.px, 5.px, 4.px, 1.px) } })
        }

        assertEquals("3px", nextChild().style.borderRadius)
        assertEquals("3px 5px", nextChild().style.borderRadius)
        assertEquals("3px 5px 4px", nextChild().style.borderRadius)
        assertEquals("3px 5px 4px 1px", nextChild().style.borderRadius)
    }


    @Test
    fun borderWidth() = runTest {
        composition {
            Div({ style { borderWidth(2.px) } })
            Div({ style { borderWidth(3.px, 7.px) } })
            Div({ style { borderWidth(3.px, 5.px, 4.px) } })
            Div({ style { borderWidth(3.px, 5.px, 4.px, 2.px) } })

            Div({ style { borderWidth(topLeft = 3.px, 7.px) } })
            Div({ style { borderWidth(topLeft = 3.px, 5.px, 4.px) } })
            Div({ style { borderWidth(topLeft = 3.px, 5.px, 4.px, 2.px) } })
        }

        assertEquals("2px", nextChild().style.borderWidth)
        assertEquals("3px 7px", nextChild().style.borderWidth)
        assertEquals("3px 5px 4px", nextChild().style.borderWidth)
        assertEquals("3px 5px 4px 2px", nextChild().style.borderWidth)

        assertEquals("3px 7px", nextChild().style.borderWidth)
        assertEquals("3px 5px 4px", nextChild().style.borderWidth)
        assertEquals("3px 5px 4px 2px", nextChild().style.borderWidth)
    }
}
