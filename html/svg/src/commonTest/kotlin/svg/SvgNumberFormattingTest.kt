/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests.svg

import org.jetbrains.compose.web.ExperimentalComposeWebApi
import org.jetbrains.compose.web.ExperimentalComposeWebSvgApi
import org.jetbrains.compose.web.composeHtmlToString
import org.jetbrains.compose.web.svg.Svg
import org.jetbrains.compose.web.svg.cx
import org.jetbrains.compose.web.svg.cy
import org.jetbrains.compose.web.svg.fillOpacity
import org.jetbrains.compose.web.svg.height
import org.jetbrains.compose.web.svg.points
import org.jetbrains.compose.web.svg.r
import org.jetbrains.compose.web.svg.rx
import org.jetbrains.compose.web.svg.ry
import org.jetbrains.compose.web.svg.width
import org.jetbrains.compose.web.svg.viewBox
import org.jetbrains.compose.web.svg.x
import org.jetbrains.compose.web.svg.x1
import org.jetbrains.compose.web.svg.x2
import org.jetbrains.compose.web.svg.y
import org.jetbrains.compose.web.svg.y1
import org.jetbrains.compose.web.svg.y2
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalComposeWebApi::class, ExperimentalComposeWebSvgApi::class)
class SvgNumberFormattingTest {
    @Test
    fun rawNumberAttributesUsePortableFormatting() {
        val html = composeHtmlToString {
            Svg(attrs = {
                width(1)
                height(2.0)
                fillOpacity(0.5f)
                cx(3)
                cy(4.0)
                r(5f)
                rx(6)
                ry(7.0)
                x(8f)
                y(9)
                x1(10.0)
                y1(11f)
                x2(12)
                y2(1.0 / 3.0)
            })
        }

        assertEquals(
            "<svg width=\"1\" height=\"2\" fill-opacity=\"0.5\" cx=\"3\" cy=\"4\" " +
                "r=\"5\" rx=\"6\" ry=\"7\" x=\"8\" y=\"9\" x1=\"10\" y1=\"11\" " +
                "x2=\"12\" y2=\"0.33333334\"></svg>",
            html,
        )
    }

    @Test
    fun pointListsFormatEveryCoordinatePortably() {
        val html = composeHtmlToString {
            Svg(attrs = {
                points(0, 1.0, 2.5f, 1.0 / 3.0)
            })
        }

        assertEquals("<svg points=\"0,1 2.5,0.33333334\"></svg>", html)
    }

    @Test
    fun viewBoxFormatsEveryComponentPortably() {
        val html = composeHtmlToString {
            Svg(attrs = {
                viewBox(0, 1.0, 2.5f, 1.0 / 3.0)
            })
        }

        assertEquals("<svg viewBox=\"0 1 2.5 0.33333334\"></svg>", html)
    }
}
