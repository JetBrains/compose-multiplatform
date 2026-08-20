/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

import org.jetbrains.compose.web.css.keywords.auto
import kotlin.test.Test
import kotlin.test.assertEquals

class CommonCSSPropertiesTest {
    @Test
    fun opacityAndBoxPropertiesSerialize() {
        val style = RecordingStyleScope()

        with(style) {
            opacity(0.5)
            opacity(25.percent)
            width(320.px)
            height(auto)
            boxSizing("border-box")
            outline(Color.red, "solid", 2.px)
            minWidth("min-content")
            maxHeight(480.px)
        }

        assertEquals(
            listOf(
                "opacity" to "0.5",
                "opacity" to "0.25",
                "width" to "320px",
                "height" to "auto",
                "box-sizing" to "border-box",
                "outline" to "red solid 2px",
                "min-width" to "min-content",
                "max-height" to "480px",
            ),
            style.declarations,
        )
    }

    @Test
    fun backgroundPropertiesSerialize() {
        val style = RecordingStyleScope()

        with(style) {
            backgroundColor(Color.rebeccapurple)
            backgroundImage("linear-gradient(red, blue)")
            backgroundRepeat("no-repeat")
            background("black center / cover")
        }

        assertEquals(
            listOf(
                "background-color" to "rebeccapurple",
                "background-image" to "linear-gradient(red, blue)",
                "background-repeat" to "no-repeat",
                "background" to "black center / cover",
            ),
            style.declarations,
        )
    }

    @Test
    fun textPropertiesSerialize() {
        val style = RecordingStyleScope()

        with(style) {
            fontFamily("Inter", "Noto Sans", "sans-serif")
            fontSize(16.px)
            fontWeight(600)
            lineHeight(24.px)
            textDecorationColor(Color.red)
            whiteSpace("pre-wrap")
        }

        assertEquals(
            listOf(
                "font-family" to "Inter, \"Noto Sans\", sans-serif",
                "font-size" to "16px",
                "font-weight" to "600",
                "line-height" to "24px",
                "text-decoration-color" to "red",
                "white-space" to "pre-wrap",
            ),
            style.declarations,
        )
    }

    @Test
    fun positionPropertiesSerialize() {
        val style = RecordingStyleScope()

        with(style) {
            position(Position.Sticky)
            top(12.px)
            right(auto)
            bottom(25.percent)
            left(auto)
            inset(1.px, 2.px, 3.px, 4.px)
        }

        assertEquals(
            listOf(
                "position" to "sticky",
                "top" to "12px",
                "right" to "auto",
                "bottom" to "25%",
                "left" to "auto",
                "inset" to "1px 2px 3px 4px",
            ),
            style.declarations,
        )
    }

    @Test
    fun gridPropertiesSerialize() {
        val style = RecordingStyleScope()

        with(style) {
            gridColumn(2, "span 3")
            gridAutoFlow(GridAutoFlow.RowDense)
            gridTemplateAreas("header header", "nav main")
            gap(8.px, 16.px)
        }

        assertEquals(
            listOf(
                "grid-column" to "2 / span 3",
                "grid-auto-flow" to "row dense",
                "grid-template-areas" to "\"header header\" \"nav main\"",
                "gap" to "8px 16px",
            ),
            style.declarations,
        )
    }

    private class RecordingStyleScope : StyleScope {
        val declarations = mutableListOf<Pair<String, String>>()

        override fun property(propertyName: String, value: StylePropertyValue) {
            declarations += propertyName to value.toString()
        }

        override fun variable(variableName: String, value: StylePropertyValue) = Unit
    }
}
