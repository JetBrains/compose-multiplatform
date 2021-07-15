/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests.css

import kotlinx.browser.window
import org.jetbrains.compose.web.core.tests.runTest
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.HTMLElement
import org.w3c.dom.get
import kotlin.test.Test
import kotlin.test.assertEquals

class ColorTests {
    @Test
    fun colorConstants() = runTest {
        composition {
            Div({ style { color(Color.black) } })
            Div({ style { color(Color.gray) } })
            Div({ style { color(Color.maroon) } })
            Div({ style { color(Color.purple) } })
            Div({ style { color(Color.red) } })
            Div({ style { color(Color.silver) } })
            Div({ style { color(Color.white) } })
        }

        var counter = 0
        assertEquals("black", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("gray", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("maroon", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("purple", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("red", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("silver", (root.children[counter++] as HTMLElement).style.color)
        assertEquals("white", (root.children[counter++] as HTMLElement).style.color)
    }
}
