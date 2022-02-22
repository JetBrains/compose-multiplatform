/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests

import androidx.compose.runtime.mutableStateOf
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.stringPresentation
import org.w3c.dom.HTMLElement
import org.w3c.dom.get
import org.jetbrains.compose.web.testutils.*
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLStyleElement
import org.w3c.dom.css.CSSStyleSheet
import org.w3c.dom.css.get
import kotlin.test.*

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

    val withNestedWithImplicitSelf2Layers by style {
        color(Color.green)
        "h1" {
            color(Color.lime)
            "span" {
                color(Color.red)
            }
        }
    }

    val withNestedWithExplicitSelf by style {
        color(Color.green)
        desc(self, "h1") style {
            color(Color.lime)
        }
    }

    val parent by style {
        (sibling(self, self)) {
            color(Color.red)
        }
    }

    val child by style {
        (className(parent) + ":hover " + self) {  // Selector + String,  Combine + Selector -> ".parent:hover .child"
            color(Color.lime)
        }
        (className(parent) + self + ":hover") {  // Selector + Selector, Combine + String -> ".parent.child:hover"
            color(Color.yellow)
        }
        ("h1 " + (className(parent) + ":hover")) { // String + Combine -> ".child h1 .parent:hover"
            color(Color.lavender)
        }
        (self + (" div" + id("el"))) {  // Selector + Combine, String + Selector -> ".child div#el"
            color(Color.green)
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
        assertEquals("4", el.computedStyle.order)
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


        (root.children[1] as HTMLElement?)?.let { el ->
            assertEquals("rgb(0, 255, 0)", el.computedStyle.color)
            assertEquals("rgb(0, 128, 0)", el.computedStyle.backgroundColor)
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

    @Test
    fun nestedStyleWithImplicitSelf2Layers() = runTest {
        val generatedRules = AppStylesheet.cssRules.map { it.stringPresentation() }

        assertContains(
            generatedRules,
            """
                .AppStylesheet-withNestedWithImplicitSelf2Layers h1 {
                    color: lime;
                }
            """.trimIndent(),
            "Nested selector with implicit self isn't generated correctly"
        )
        assertContains(
            generatedRules,
            """
                .AppStylesheet-withNestedWithImplicitSelf2Layers h1 span {
                    color: red;
                }
            """.trimIndent(),
            "Nested selector with implicit self isn't generated correctly"
        )
    }

    @Test
    fun testCombine() = runTest {
        val generatedRules = AppStylesheet.cssRules.map { it.stringPresentation() }

        // Selector + String,  Combine + Selector
        assertContains(
            generatedRules,
            """
                .AppStylesheet-parent:hover .AppStylesheet-child {
                    color: lime;
                }
            """.trimIndent(),
            "Selector isn't generated correctly"
        )

        // Selector + Selector, Combine + String
        assertContains(
            generatedRules,
            """
                .AppStylesheet-parent.AppStylesheet-child:hover {
                    color: yellow;
                }
            """.trimIndent(),
            "Selector isn't generated correctly"
        )

        // String + Combine
        assertContains(
            generatedRules,
            """
                .AppStylesheet-child h1 .AppStylesheet-parent:hover {
                    color: lavender;
                }
            """.trimIndent(),
            "Selector isn't generated correctly"
        )

        // Selector + Combine, String + Selector
        assertContains(
            generatedRules,
            """
                .AppStylesheet-child div#el {
                    color: green;
                }
            """.trimIndent(),
            "Selector isn't generated correctly"
        )
    }

    @Test
    fun testSibling() = runTest {
        val generatedRules = AppStylesheet.cssRules.map { it.stringPresentation() }

        assertContains(
            generatedRules,
            """
                .AppStylesheet-parent ~ .AppStylesheet-parent {
                    color: red;
                }
            """.trimIndent(),
            "Sibling selector isn't generated correctly"
        )
    }

    @Test
    fun testStringPlusCSSSelectorConcatenation() {
        assertFailsWith<IllegalStateException>("Concatenation of String + CSSSelector should be restricted.") {
            object : StyleSheet() {
                val myClass by style {
                    ("h1" + self) {
                        color(Color.green)
                    }
                }
            }
        }
    }

    @Test
    fun testChildStylesheetsGetApplied() = runTest {
        val showContent = mutableStateOf(false)
        composition {
            Style(TestRootStylesheet2)
            if (showContent.value) {
                Div(attrs = {
                    classes(ChildStylesheet2.content)
                }) {}
            }
        }

        val styleElem = root.children[0] as HTMLStyleElement
        val cssStyleSheet = styleElem.sheet as CSSStyleSheet
        assertEquals(1, cssStyleSheet.cssRules.length)
        assertEquals(".cls1 { padding: 10px; }", cssStyleSheet.cssRules[0]!!.cssText)

        showContent.value = true
        waitForRecompositionComplete()

        with(root.children[1] as HTMLDivElement) {
            assertEquals("content", getAttribute("class"))
        }

        // 2nd recomposition occurs since TestRootStylesheet2 was changed during recomposition
        // when ChildStylesheet2 was initialised after showContent was set into "true"
        waitForRecompositionComplete()

        val styleElem2 = root.children[0] as HTMLStyleElement
        val cssStyleSheet2 = styleElem2.sheet as CSSStyleSheet
        assertEquals(".cls1 { padding: 10px; }", cssStyleSheet2.cssRules[0]!!.cssText)
        assertEquals(2, cssStyleSheet2.cssRules.length)
        assertEquals(".content { color: red; }", cssStyleSheet2.cssRules[1]!!.cssText)
    }

    @Test
    fun testChildStylesheetsGetApplied2() = runTest {
        composition {
            Style(TestRootStylesheet)
            Div(attrs = { classes(ChildStylesheet.content) })
        }

        val styleElem = root.children[0] as HTMLStyleElement
        val cssStyleSheet = styleElem.sheet as CSSStyleSheet
        assertEquals(2, cssStyleSheet.cssRules.length)
        assertEquals(".cls1 { padding: 10px; }", cssStyleSheet.cssRules[0]!!.cssText)
        assertEquals(".content { color: red; }", cssStyleSheet.cssRules[1]!!.cssText)
    }
}

private object TestRootStylesheet : StyleSheet(usePrefix = false) {
    val cls1 by style {
        padding(10.px)
    }
}
private object ChildStylesheet : StyleSheet(TestRootStylesheet, usePrefix = false) {
    val content by style {
        color(Color.red)
    }
}

// Use duplicates for another test to ensure uninitialized objects will be used
private object TestRootStylesheet2 : StyleSheet(usePrefix = false) {
    val cls1 by style {
        padding(10.px)
    }
}
private object ChildStylesheet2 : StyleSheet(TestRootStylesheet2, usePrefix = false) {
    val content by style {
        color(Color.red)
    }
}
