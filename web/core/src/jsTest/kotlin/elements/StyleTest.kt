/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests.elements

import androidx.compose.runtime.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.testutils.*
import org.w3c.dom.*
import org.w3c.dom.css.*
import kotlin.test.*

class StyleTest {
    @Test
    fun testingComposableStyle() = runTest {
        var color by mutableStateOf(Color.green)
        composition {
            Style {
                val element = remember { "body" }
                element {
                    backgroundColor(color)
                }
            }
        }
        val element = root.firstChild
        assertTrue(element is HTMLStyleElement)
        val sheet = element.sheet
        assertTrue(sheet is CSSStyleSheet)
        assertEquals("""body { background-color: green; }""", sheet.cssRules.asList().single().cssText)

        color = Color.red
        waitForRecompositionComplete()
        assertEquals("""body { background-color: red; }""", sheet.cssRules.asList().single().cssText)
    }
}
