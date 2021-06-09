/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests

import org.jetbrains.compose.web.css.CSSUnitValue
import org.jetbrains.compose.web.css.CSSVariables
import org.jetbrains.compose.web.css.Style
import org.jetbrains.compose.web.css.StyleSheet
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.value
import org.jetbrains.compose.web.css.variable
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.HTMLElement
import org.w3c.dom.get
import kotlin.test.Test
import kotlin.test.assertEquals

object AppCSSVariables : CSSVariables {
    val width by variable<CSSUnitValue>()
    val height by variable<CSSUnitValue>()

    val stringWidth by variable<String>()
    val stringHeight by variable<String>()
}

object AppStylesheet : StyleSheet() {
    val classWithProperties by style {
        property("width", 300.px)
        property("height", 200.px)
    }

    val classWithRawProperties by style {
        property("width", "200px")
        property("height", "300px")
    }

    val classWithRawVariables by style {
        AppCSSVariables.stringWidth.value("150px")
        AppCSSVariables.stringHeight.value("170px")
        property("width", AppCSSVariables.stringWidth.value())
        property("height", AppCSSVariables.stringHeight.value())
    }

}


class CSSVariableTests {
    @Test
    fun styleProperties() = runTest {
        composition {
            Style(AppStylesheet)
            Div({
                classes(AppStylesheet.classWithProperties)
            })
        }

        val boundingRect = (root.children[1] as HTMLElement).getBoundingClientRect()
        assertEquals(300.toDouble(), boundingRect.width)
        assertEquals(200.toDouble(), boundingRect.height)
    }

    @Test
    fun styleRawProperties() = runTest {
        composition {
            Style(AppStylesheet)
            Div({
                classes(AppStylesheet.classWithRawProperties)
            })
        }

        val boundingRect = (root.children[1] as HTMLElement).getBoundingClientRect()
        assertEquals(200.toDouble(), boundingRect.width)
        assertEquals(300.toDouble(), boundingRect.height)
    }

//    @Test
//    fun styleRawVariables() = runTest {
//        composition {
//            Style(AppStylesheet)
//            Div({
//                classes(AppStylesheet.classWithRawVariables)
//            })
//        }
//
//        val boundingRect = (root.children[1] as HTMLElement).getBoundingClientRect()
//        assertEquals(150.toDouble(), boundingRect.width)
//        assertEquals(170.toDouble(), boundingRect.height)
//    }

}