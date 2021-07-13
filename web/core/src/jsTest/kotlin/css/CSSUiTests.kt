/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests.css

import org.jetbrains.compose.web.core.tests.runTest
import org.jetbrains.compose.web.css.cursor
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.HTMLElement
import org.w3c.dom.get
import kotlin.test.Test
import kotlin.test.assertEquals

class CSSUiTests {
    @Test
    fun cursor() = runTest {
        composition {
            Div({
                style {
                    cursor("pointer")
                }
            })
            Div({
                style {
                    cursor("auto")
                }
            })
            Div({
                style {
                    cursor("url(hand.cur), pointer")
                }
            })
            Div({
                style {
                    cursor("url(cursor2.png) 2 2, pointer")
                }
            })
        }

        assertEquals("pointer", (root.children[0] as HTMLElement).style.cursor)
        assertEquals("auto", (root.children[1] as HTMLElement).style.cursor)
        assertEquals("url(\"hand.cur\"), pointer", (root.children[2] as HTMLElement).style.cursor)
        assertEquals("url(\"cursor2.png\") 2 2, pointer", (root.children[3] as HTMLElement).style.cursor)
    }
}