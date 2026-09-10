/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Verifies that common SVG declarations retain browser identity on both web targets.
@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package kotlinx.browser.dom.svg.identity

import kotlinx.browser.document
import kotlinx.browser.css.masking.SVGMaskElement as CommonSVGMaskElement
import kotlinx.browser.dom.svg.SVGCircleElement as CommonSVGCircleElement
import kotlinx.browser.dom.svg.SVGElement as CommonSVGElement
import org.w3c.css.masking.SVGMaskElement as BrowserSVGMaskElement
import org.w3c.dom.svg.SVGCircleElement as BrowserSVGCircleElement
import org.w3c.dom.svg.SVGElement as BrowserSVGElement
import kotlin.js.unsafeCast
import kotlin.test.Test
import kotlin.test.assertEquals

private fun browserElementAsCommon(value: BrowserSVGElement): CommonSVGElement = value

private fun commonElementAsBrowser(value: CommonSVGElement): BrowserSVGElement = value

private fun browserCircleAsCommon(value: BrowserSVGCircleElement): CommonSVGCircleElement = value

private fun commonMaskAsBrowser(value: CommonSVGMaskElement): BrowserSVGMaskElement = value

class GeneratedSvgWebIdentityTest {
    @Test
    fun svgAliasesKeepBrowserIdentity() {
        val circle = document.createElementNS(SVG_NAMESPACE, "circle").unsafeCast<CommonSVGCircleElement>()
        val mask = document.createElementNS(SVG_NAMESPACE, "mask").unsafeCast<CommonSVGMaskElement>()

        browserElementAsCommon(circle)
        commonElementAsBrowser(circle)
        browserCircleAsCommon(circle)
        commonMaskAsBrowser(mask)
        assertEquals("circle", circle.localName)
        assertEquals("mask", mask.localName)
    }
}

private const val SVG_NAMESPACE = "http://www.w3.org/2000/svg"
