/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests.css

import kotlinx.browser.window
import org.jetbrains.compose.web.core.tests.runTest
import org.jetbrains.compose.web.css.backgroundColor
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
                backgroundColor("rgb(0, 128, 0)")
            }})
            Div({style {
                backgroundColor("rgba(0, 129, 0, 0.2)")
            }})
        }

        assertEquals("rgb(0, 128, 0)", window.getComputedStyle(root.children[0] as HTMLElement).backgroundColor)
        assertEquals("rgba(0, 129, 0, 0.2)", window.getComputedStyle(root.children[1] as HTMLElement).backgroundColor)
    }
}