/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests

import kotlinx.browser.window
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.css.selectors.desc
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.stringPresentation
import org.w3c.dom.HTMLElement
import org.w3c.dom.get
import kotlin.test.Test
import kotlin.test.assertEquals
import org.jetbrains.compose.web.testutils.*
import kotlin.test.assertContains

object AppCSSVariables {
    val width by variable<CSSUnitValue>()
    val height by variable<CSSUnitValue>()

    val stringWidth by variable<StylePropertyString>()
    val stringHeight by variable<StylePropertyString>()

    val order by variable<StylePropertyNumber>()
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
        AppCSSVariables.stringWidth("150px")
        AppCSSVariables.stringHeight("170px")
        AppCSSVariables.order(4)
        property("width", AppCSSVariables.stringWidth.value())
        property("height", AppCSSVariables.stringHeight.value())
        property("order", AppCSSVariables.order.value())
    }

    val classWithTypedVariables by style {
        AppCSSVariables.width(100.px)
        AppCSSVariables.height(200.px)
        property("width", AppCSSVariables.width.value())
        property("height", AppCSSVariables.height.value())
    }

    val withMedia by style {
        color(Color.lime)
        backgroundColor(Color.lime)

        media(mediaMinWidth(20000.px)) {
            self style {
                color(Color.red)
            }
        }

        media(mediaMinWidth(20.px)) {
            self style {
                backgroundColor(Color.green)
            }
        }

    }

    val withNestedWithImplicitSelf by style {
        color(Color.green)
        "h1" {
            color(Color.lime)
        }
    }
    val withNestedWithExplicitSelf by style {
        color(Color.green)
        desc(self, "h1") style {
            color(Color.lime)
        }
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

    @Test
    fun styleRawVariables() = runTest {
        composition {
            Style(AppStylesheet)
            Div({
                classes(AppStylesheet.classWithRawVariables)
            })
        }

        val el = root.children[1] as HTMLElement
        val boundingRect = el.getBoundingClientRect()
        assertEquals("4", window.getComputedStyle(el).order)
        assertEquals(150.toDouble(), boundingRect.width)
        assertEquals(170.toDouble(), boundingRect.height)
    }

    @Test
    fun styleTypedVariables() = runTest {
        composition {
            Style(AppStylesheet)
            Div({
                classes(AppStylesheet.classWithTypedVariables)
            })
        }

        val boundingRect = (root.children[1] as HTMLElement).getBoundingClientRect()
        assertEquals(100.toDouble(), boundingRect.width)
        assertEquals(200.toDouble(), boundingRect.height)
    }

    @Test
    fun withMediaQuery() = runTest {
        composition {
            Style(AppStylesheet)
            Div({
                classes(AppStylesheet.withMedia)
            })
        }

        root.children[1]?.let { el ->
            assertEquals("rgb(0, 255, 0)", window.getComputedStyle(el).color)
            assertEquals("rgb(0, 128, 0)", window.getComputedStyle(el).backgroundColor)
        }
    }

    @Test
    fun nestedStyleWithImplicitSelf() = runTest {
        val generatedRules = AppStylesheet.cssRules.map { it.stringPresentation() }

        assertContains(
            generatedRules,
            """
                .AppStylesheet-withNestedWithImplicitSelf h1 {
                    color: lime;
                }
            """.trimIndent(),
            "Nested selector with implicit self isn't generated correctly"
        )
    }

    @Test
    fun nestedStyleWithExplicitSelf() = runTest {
        val generatedRules = AppStylesheet.cssRules.map { it.stringPresentation() }

        assertContains(
            generatedRules,
            """
                .AppStylesheet-withNestedWithExplicitSelf h1 {
                    color: lime;
                }
            """.trimIndent(),
            "Nested selector with implicit self isn't generated correctly"
        )
    }
}
