/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests.svg

import androidx.compose.runtime.Composable
import kotlinx.browser.dom.svg.SVGElement
import org.jetbrains.compose.web.ExperimentalComposeWebApi
import org.jetbrains.compose.web.ExperimentalComposeWebSvgApi
import org.jetbrains.compose.web.composeHtmlToString
import org.jetbrains.compose.web.svg.AnimateTransform
import org.jetbrains.compose.web.svg.Defs
import org.jetbrains.compose.web.svg.Image
import org.jetbrains.compose.web.svg.LinearGradient
import org.jetbrains.compose.web.svg.Rect
import org.jetbrains.compose.web.svg.Stop
import org.jetbrains.compose.web.svg.Svg
import org.jetbrains.compose.web.svg.SvgElement
import org.jetbrains.compose.web.svg.SvgText
import org.jetbrains.compose.web.svg.attributeName
import org.jetbrains.compose.web.svg.fill
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal const val SVG_SSR_HYDRATION_FIXTURE_URL = "/base/kotlin/svg-ssr-hydration.html"
internal const val SVG_SSR_ROOT_ID = "svg-ssr-root"
internal const val SVG_SSR_GRADIENT_ID = "svg-ssr-gradient"
internal const val SVG_SSR_SHAPE_ID = "svg-ssr-shape"
internal const val SVG_SSR_TEXT_ID = "svg-ssr-text"
internal const val SVG_SSR_IMAGE_ID = "svg-ssr-image"
internal const val SVG_SSR_CUSTOM_ID = "svg-ssr-custom"
internal const val SVG_SSR_IMAGE_HREF =
    "data:image/gif;base64,R0lGODlhAQABAAD/ACwAAAAAAQABAAACADs="

@Composable
@OptIn(ExperimentalComposeWebSvgApi::class)
internal fun SvgSsrHydrationContent(
    count: Int,
    increment: () -> Unit,
) {
    Svg(
        viewBox = "0 0 120 60",
        attrs = {
            id(SVG_SSR_ROOT_ID)
            attr("data-count", count.toString())
        },
    ) {
        Defs {
            LinearGradient(
                id = SVG_SSR_GRADIENT_ID,
                attrs = { attr("gradientUnits", "userSpaceOnUse") },
            ) {
                Stop(attrs = {
                    attr("offset", "0%")
                    attr("stop-color", "navy")
                })
                Stop(attrs = {
                    attr("offset", "100%")
                    attr("stop-color", "aqua")
                })
            }
        }
        Rect(
            x = 4,
            y = 5,
            width = 40,
            height = 20,
            attrs = {
                id(SVG_SSR_SHAPE_ID)
                fill("url(#$SVG_SSR_GRADIENT_ID)")
                attr("data-count", count.toString())
                onClick { increment() }
            },
        )
        SvgText(
            text = "Count: $count",
            x = 50,
            y = 20,
            attrs = { id(SVG_SSR_TEXT_ID) },
        )
        Image(
            href = SVG_SSR_IMAGE_HREF,
            attrs = {
                id(SVG_SSR_IMAGE_ID)
                attr("preserveAspectRatio", "xMidYMid meet")
            },
        )
        SvgElement<SVGElement>(
            name = "sparkline",
            attrs = {
                id(SVG_SSR_CUSTOM_ID)
                attr("data-points", "0,$count 1,${count + 1}")
            },
        )
    }
}

@OptIn(ExperimentalComposeWebApi::class, ExperimentalComposeWebSvgApi::class)
class SvgSsrRenderingTest {
    @Test
    fun rendersRepresentativeSvgExactly() {
        val html = composeHtmlToString {
            SvgSsrHydrationContent(count = 0, increment = {})
        }

        assertEquals(
            "<svg viewBox=\"0 0 120 60\" id=\"svg-ssr-root\" data-count=\"0\">" +
                "<defs><linearGradient id=\"svg-ssr-gradient\" gradientUnits=\"userSpaceOnUse\">" +
                "<stop offset=\"0%\" stop-color=\"navy\"></stop>" +
                "<stop offset=\"100%\" stop-color=\"aqua\"></stop>" +
                "</linearGradient></defs>" +
                "<rect x=\"4\" y=\"5\" width=\"40\" height=\"20\" id=\"svg-ssr-shape\" " +
                "fill=\"url(#svg-ssr-gradient)\" data-count=\"0\"></rect>" +
                "<text x=\"50\" y=\"20\" id=\"svg-ssr-text\">Count: 0</text>" +
                "<image href=\"$SVG_SSR_IMAGE_HREF\" id=\"svg-ssr-image\" " +
                "preserveAspectRatio=\"xMidYMid meet\"></image>" +
                "<sparkline id=\"svg-ssr-custom\" data-points=\"0,0 1,1\"></sparkline>" +
                "</svg>",
            html,
        )
    }

    @Test
    fun preservesCaseSensitiveSvgTagsAndAttributes() {
        val html = composeHtmlToString {
            Svg(viewBox = "0 0 10 10") {
                LinearGradient(attrs = { attr("gradientUnits", "objectBoundingBox") })
                AnimateTransform(attrs = { attributeName("viewBox") })
            }
        }

        assertEquals(
            "<svg viewBox=\"0 0 10 10\">" +
                "<linearGradient gradientUnits=\"objectBoundingBox\"></linearGradient>" +
                "<animateTransform attributeName=\"viewBox\"></animateTransform>" +
                "</svg>",
            html,
        )
    }

    @Test
    fun rendersCustomSvgElementWithItsGenericElementType() {
        val html = composeHtmlToString {
            Svg {
                SvgElement<SVGElement>(
                    name = "sparkline",
                    attrs = { attr("data-series", "1,2,3") },
                )
            }
        }

        assertEquals(
            "<svg><sparkline data-series=\"1,2,3\"></sparkline></svg>",
            html,
        )
    }

    @Test
    fun rejectsStringRenderedCustomSvgElementWithoutSvgRoot() {
        val failure = assertFailsWith<IllegalArgumentException> {
            composeHtmlToString {
                SvgElement<SVGElement>(name = "sparkline")
            }
        }

        assertContains(failure.message.orEmpty(), "<sparkline>")
        assertContains(failure.message.orEmpty(), "SVG <svg> root")
    }
}
