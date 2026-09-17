/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests.svg

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.browser.document
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.delay
import kotlinx.coroutines.promise
import kotlinx.browser.dom.svg.SVGElement
import org.jetbrains.compose.web.ExperimentalComposeWebApi
import org.jetbrains.compose.web.ExperimentalComposeWebSvgApi
import org.jetbrains.compose.web.HydrationMismatchException
import org.jetbrains.compose.web.composeHtmlToString
import org.jetbrains.compose.web.hydrateComposable
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.svg.LinearGradient
import org.jetbrains.compose.web.svg.SVG_NS
import org.jetbrains.compose.web.svg.Svg
import org.jetbrains.compose.web.svg.SvgElement
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.MouseEvent
import kotlin.js.Promise
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

private external interface SvgFixtureResponse {
    val ok: Boolean
    val status: Int
    fun text(): Promise<String>
}

@JsName("fetch")
private external fun fetchSvgFixture(input: String): Promise<SvgFixtureResponse>

@OptIn(ExperimentalComposeWebApi::class, ExperimentalComposeWebSvgApi::class)
class SvgSsrHydrationTest {
    @Test
    fun svgStyleAndScriptTextRoundTripWithHtmlEntities() {
        listOf("style", "script").forEach { tag ->
            val text = "/* < & > </$tag> */"
            val root = document.createElement("div") as HTMLElement
            root.innerHTML = composeHtmlToString {
                Svg { SvgElement<SVGElement>(tag) { Text(text) } }
            }
            val element = root.firstElementChild!!.firstElementChild!!
            assertEquals(SVG_NS, element.namespaceURI)
            assertEquals(text, element.textContent)
            val composition = hydrateComposable(root, onHydrationMismatch = { throw it }) {
                Svg { SvgElement<SVGElement>(tag) { Text(text) } }
            }
            try {
                assertSame(element, root.firstElementChild!!.firstElementChild)
                assertEquals(text, element.textContent)
            } finally {
                composition.dispose()
            }
        }
    }

    @Test
    fun jvmRenderedSvgHydratesAndComposeUpdatesClaimedNodes() = MainScope().promise {
        val response = fetchSvgFixture(SVG_SSR_HYDRATION_FIXTURE_URL).await()
        assertTrue(
            response.ok,
            "Fetching $SVG_SSR_HYDRATION_FIXTURE_URL failed with HTTP ${response.status}",
        )

        val root = document.createElement("div") as HTMLElement
        root.innerHTML = response.text().await()
        document.body!!.appendChild(root)

        val serverSvg = root.requiredElement(SVG_SSR_ROOT_ID)
        val serverGradient = root.requiredElement(SVG_SSR_GRADIENT_ID)
        val serverShape = root.requiredElement(SVG_SSR_SHAPE_ID)
        val serverText = root.requiredElement(SVG_SSR_TEXT_ID)
        val serverImage = root.requiredElement(SVG_SSR_IMAGE_ID)
        val serverCustom = root.requiredElement(SVG_SSR_CUSTOM_ID)

        assertEquals(SVG_NS, serverSvg.namespaceURI)
        assertEquals("svg", serverSvg.localName)
        assertEquals("linearGradient", serverGradient.localName)
        assertEquals("userSpaceOnUse", serverGradient.getAttribute("gradientUnits"))
        assertEquals(null, serverGradient.getAttribute("gradientunits"))
        assertEquals("sparkline", serverCustom.localName)

        var count by mutableStateOf(0)
        val composition = hydrateComposable(
            root = root,
            onHydrationMismatch = { throw it },
        ) {
            SvgSsrHydrationContent(
                count = count,
                increment = { count++ },
            )
        }

        try {
            assertSame(serverSvg, root.requiredElement(SVG_SSR_ROOT_ID))
            assertSame(serverGradient, root.requiredElement(SVG_SSR_GRADIENT_ID))
            assertSame(serverShape, root.requiredElement(SVG_SSR_SHAPE_ID))
            assertSame(serverText, root.requiredElement(SVG_SSR_TEXT_ID))
            assertSame(serverImage, root.requiredElement(SVG_SSR_IMAGE_ID))
            assertSame(serverCustom, root.requiredElement(SVG_SSR_CUSTOM_ID))

            serverShape.dispatchEvent(MouseEvent("click"))
            delay(100.milliseconds)

            assertEquals("1", serverSvg.getAttribute("data-count"))
            assertEquals("1", serverShape.getAttribute("data-count"))
            assertEquals("Count: 1", serverText.textContent)
            assertEquals("0,1 1,2", serverCustom.getAttribute("data-points"))
            assertSame(serverSvg, root.requiredElement(SVG_SSR_ROOT_ID))
            assertSame(serverShape, root.requiredElement(SVG_SSR_SHAPE_ID))
            assertSame(serverText, root.requiredElement(SVG_SSR_TEXT_ID))
            assertSame(serverCustom, root.requiredElement(SVG_SSR_CUSTOM_ID))
        } finally {
            composition.dispose()
            root.parentNode?.removeChild(root)
        }
    }

    @Test
    fun hydrationRejectsSvgFromAnotherNamespace() {
        val root = document.createElement("div") as HTMLElement
        val mathMlNamespace = "http://www.w3.org/1998/Math/MathML"
        val wrongNamespaceSvg = document.createElementNS(mathMlNamespace, "svg")
        root.appendChild(wrongNamespaceSvg)

        val failure = assertFailsWith<HydrationMismatchException> {
            hydrateComposable(root, onHydrationMismatch = { throw it }) {
                Svg()
            }
        }

        assertContains(failure.message.orEmpty(), SVG_NS)
        assertContains(failure.message.orEmpty(), mathMlNamespace)
        assertSame(wrongNamespaceSvg, root.firstChild)
    }

    @Test
    fun hydrationComparesSvgTagNamesCaseSensitively() {
        val root = document.createElement("div") as HTMLElement
        val serverSvg = document.createElementNS(SVG_NS, "svg")
        val incorrectlyCasedGradient = document.createElementNS(SVG_NS, "lineargradient")
        serverSvg.appendChild(incorrectlyCasedGradient)
        root.appendChild(serverSvg)

        val failure = assertFailsWith<HydrationMismatchException> {
            hydrateComposable(root, onHydrationMismatch = { throw it }) {
                Svg { LinearGradient() }
            }
        }

        assertContains(failure.message.orEmpty(), "expected <linearGradient>")
        assertContains(failure.message.orEmpty(), "found <lineargradient>")
        assertSame(incorrectlyCasedGradient, serverSvg.firstChild)
    }

    @Test
    fun hydrationComparesSvgAttributeNamesCaseSensitively() {
        val root = document.createElement("div") as HTMLElement
        val serverSvg = document.createElementNS(SVG_NS, "svg")
        serverSvg.setAttribute("viewbox", "0 0 10 10")
        root.appendChild(serverSvg)

        val failure = assertFailsWith<HydrationMismatchException> {
            hydrateComposable(root, onHydrationMismatch = { throw it }) {
                Svg(viewBox = "0 0 10 10")
            }
        }

        assertContains(failure.message.orEmpty(), "attribute \"viewBox\"")
        assertContains(failure.message.orEmpty(), "found no attribute")
        assertEquals("0 0 10 10", serverSvg.getAttribute("viewbox"))
        assertSame(serverSvg, root.firstChild)
    }

    @Test
    fun customCamelCaseTagDoesNotRoundTripThroughTheHtmlParser() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = "<svg><myCustomTag></myCustomTag></svg>"
        val serverSvg = root.firstElementChild as Element
        val reparsedCustomElement = serverSvg.firstElementChild as Element
        assertEquals("mycustomtag", reparsedCustomElement.localName)

        val failure = assertFailsWith<HydrationMismatchException> {
            hydrateComposable(root, onHydrationMismatch = { throw it }) {
                Svg { SvgElement<SVGElement>("myCustomTag") }
            }
        }

        assertContains(failure.message.orEmpty(), "expected <myCustomTag>")
        assertContains(failure.message.orEmpty(), "found <mycustomtag>")
        assertSame(reparsedCustomElement, serverSvg.firstChild)
    }

    @Test
    fun customCamelCaseAttributeDoesNotRoundTripThroughTheHtmlParser() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = "<svg><sparkline dataPoints=\"0,1\"></sparkline></svg>"
        val serverSvg = root.firstElementChild as Element
        val reparsedCustomElement = serverSvg.firstElementChild as Element
        assertEquals("0,1", reparsedCustomElement.getAttribute("datapoints"))
        assertEquals(null, reparsedCustomElement.getAttribute("dataPoints"))

        val failure = assertFailsWith<HydrationMismatchException> {
            hydrateComposable(root, onHydrationMismatch = { throw it }) {
                Svg {
                    SvgElement<SVGElement>("sparkline", attrs = {
                        attr("dataPoints", "0,1")
                    })
                }
            }
        }

        assertContains(failure.message.orEmpty(), "attribute \"dataPoints\"")
        assertContains(failure.message.orEmpty(), "found no attribute")
        assertSame(reparsedCustomElement, serverSvg.firstChild)
    }

    private fun HTMLElement.requiredElement(id: String): Element =
        assertNotNull(querySelector("#$id"), "Missing fixture element #$id")
}
