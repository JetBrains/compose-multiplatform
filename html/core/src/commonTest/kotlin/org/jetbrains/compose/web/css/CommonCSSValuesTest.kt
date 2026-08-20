/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

import org.jetbrains.compose.web.css.keywords.auto
import kotlin.test.Test
import kotlin.test.assertEquals

class CommonCSSValuesTest {
    @Test
    fun stringBackedValuesHavePortableCssText() {
        assertEquals("flex", DisplayStyle.Flex.value)
        assertEquals("inline-grid", DisplayStyle("inline-grid").name)
        assertEquals("rebeccapurple", Color.rebeccapurple.toString())
        assertEquals("auto", auto.toString())
    }

    @Test
    fun functionalColorsHavePortableCssText() {
        assertEquals("rgb(10, 20, 30)", rgb(10, 20, 30).toString())
        assertEquals("rgba(10, 20, 30, 0.5)", rgba(10, 20, 30, 0.5).toString())
        assertEquals("hsl(90deg, 50%, 25%)", hsl(90, 50, 25).toString())
        assertEquals("hsla(90deg, 50%, 25%, 0.5)", hsla(90, 50, 25, 0.5).toString())
    }

    @Test
    fun numericOperationsHavePortableCssText() {
        val first: CSSNumericValue<CSSUnit.px> = 10.px
        val second: CSSNumericValue<CSSUnit.px> = 5.px

        assertEquals("15px", (10.px + 5.px).toString())
        assertEquals("calc((10px + 5px) * 2)", ((first + second) * 2).toString())
    }

    @Test
    fun enumFamiliesHavePortableCssText() {
        val values = listOf<StylePropertyValue>(
            LineStyle.Dashed,
            DisplayStyle.Flex,
            FlexDirection.RowReverse,
            FlexWrap.Wrap,
            JustifyContent.SpaceBetween,
            AlignSelf.Baseline,
            AlignItems.Stretch,
            AlignContent.SpaceEvenly,
            Position.Sticky,
            StepPosition.JumpEnd,
            AnimationTimingFunction.EaseInOut,
            AnimationDirection.Alternate,
            AnimationFillMode.Forwards,
            AnimationPlayState.Running,
            VisibilityStyle.Collapse,
        )

        assertEquals(
            listOf(
                "dashed",
                "flex",
                "row-reverse",
                "wrap",
                "space-between",
                "baseline",
                "stretch",
                "space-evenly",
                "sticky",
                "jump-end",
                "ease-in-out",
                "alternate",
                "forwards",
                "running",
                "collapse",
            ),
            values.map { it.toString() },
        )
    }

    @Test
    fun gridAutoFlowValuesHavePortableCssText() {
        val values = listOf(
            GridAutoFlow.Row,
            GridAutoFlow.Column,
            GridAutoFlow.Dense,
            GridAutoFlow.RowDense,
            GridAutoFlow.ColumnDense,
        )

        assertEquals(
            listOf("row", "column", "dense", "row dense", "column dense"),
            values.map { it.toString() },
        )
    }
}
