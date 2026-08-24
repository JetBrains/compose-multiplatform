/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

import kotlin.test.Test
import kotlin.test.assertEquals

class CSSVariablesTest {
    @Test
    fun assignmentsHavePortableCssText() {
        val style = RecordingStyleScope()

        with(style) {
            TestVariables.spacing(16.px)
            TestVariables.accent(Color.rebeccapurple)
            TestVariables.order(3)
            TestVariables.label("card")
            TestVariables.layout(DisplayStyle.Grid)
            TestVariables.blur(TestCSSFilterValue("blur(4px)"))
        }

        assertEquals(
            listOf(
                "spacing" to "16px",
                "accent" to "rebeccapurple",
                "order" to "3",
                "label" to "card",
                "layout" to "grid",
                "blur" to "blur(4px)",
            ),
            style.variables,
        )
    }

    @Test
    fun referencesWithoutFallbackHavePortableCssText() {
        val style = RecordingStyleScope()

        with(style) {
            width(TestVariables.spacing.value())
            backgroundColor(TestVariables.accent.value())
            property("z-index", TestVariables.order.value())
            property("content", TestVariables.label.value())
            display(TestVariables.layout.value())
            filter(TestVariables.blur.value())
        }

        assertEquals(
            listOf(
                "width" to "var(--spacing)",
                "background-color" to "var(--accent)",
                "z-index" to "var(--order)",
                "content" to "var(--label)",
                "display" to "var(--layout)",
                "filter" to "var(--blur)",
            ),
            style.properties,
        )
    }

    @Test
    fun numericReferencesComposeInCalc() {
        val length: CSSNumericValue<CSSUnitLength> = 4.pt + TestVariables.px.value()
        val scaled: CSSNumericValue<CSSUnit.px> = TestVariables.px.value(4.px) * 4

        assertEquals("calc(4pt + var(--px))", length.toString())
        assertEquals("calc(var(--px, 4px) * 4)", scaled.toString())
    }

    @Test
    fun platformAliasSupportsCustomValuesFromGenericCode() {
        val reference: TestCSSFilterValue = genericReference(TestVariables.blur)

        assertEquals("var(--blur)", reference.toString())
    }

    private fun <TValue : StylePropertyValue> genericReference(
        variable: CSSStyleVariable<TValue>,
    ): TValue = variable.value()

    private object TestVariables {
        val spacing by variable<CSSUnitValue>()
        val accent by variable<CSSColorValue>()
        val order by variable<StylePropertyNumber>()
        val label by variable<StylePropertyString>()
        val layout by variable<DisplayStyle>()
        val px by variable<CSSNumericValue<CSSUnit.px>>()
        val blur by variable<TestCSSFilterValue>()
    }

    private fun StyleScope.filter(value: TestCSSFilterValue) {
        property("filter", value)
    }

    private class RecordingStyleScope : StyleScope {
        val properties = mutableListOf<Pair<String, String>>()
        val variables = mutableListOf<Pair<String, String>>()

        override fun property(propertyName: String, value: StylePropertyValue) {
            properties += propertyName to value.toString()
        }

        override fun variable(variableName: String, value: StylePropertyValue) {
            variables += variableName to value.toString()
        }
    }
}
