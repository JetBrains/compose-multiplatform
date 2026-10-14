/* Copyright 2026 JetBrains s.r.o. Licensed under the Apache License, Version 2.0. */
package org.jetbrains.compose.web.dom

import org.jetbrains.compose.web.composeHtmlToString
import org.jetbrains.compose.web.css.*
import kotlin.test.Test
import kotlin.test.assertEquals

class FractionalStyleSsrTest {
    @Test
    fun changingFractionalValuesRenderThroughInlineCss() {
        for ((value, number, length) in listOf(
            Triple(1f / 3f, "0.33333334", "0.33333334px"),
            Triple(1.375f, "1.375", "1.375px"),
            Triple(-2070951.25f, "-2070951.3", "-2070951.3px"),
        )) {
            assertEquals("<div style=\"width: $length; opacity: $number; --fraction: $number\"></div>",
                composeHtmlToString(key = "fractional-regression") {
                    Div({ style { width(value.px); property("opacity", value); variable("fraction", value) } }) {}
                })
            assertEquals("<div style=\"opacity: $number !important; width: $length; --fraction: $number\"></div>",
                composeHtmlToString(key = "fractional-priority-regression") {
                    Div({ style {
                        property("opacity", value, true); width(value.px)
                        property("removed", "unused"); property("removed", "")
                        variable("fraction", value)
                    } }) {}
                })
        }
    }
}
