/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Verifies that portable SVG declarations retain browser identity on both web targets.
@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package kotlinx.browser.dom.svg.identity

import kotlinx.browser.document
import kotlinx.browser.css.masking.SVGMaskElement as PortableSVGMaskElement
import kotlinx.browser.dom.svg.SVGCircleElement as PortableSVGCircleElement
import kotlinx.browser.dom.svg.SVGElement as PortableSVGElement
import org.w3c.css.masking.SVGMaskElement as BrowserSVGMaskElement
import org.w3c.dom.svg.SVGCircleElement as BrowserSVGCircleElement
import org.w3c.dom.svg.SVGElement as BrowserSVGElement
import kotlin.js.unsafeCast
import kotlin.test.Test
import kotlin.test.assertEquals

private fun browserElementAsPortable(value: BrowserSVGElement): PortableSVGElement = value

private fun portableElementAsBrowser(value: PortableSVGElement): BrowserSVGElement = value

private fun browserCircleAsPortable(value: BrowserSVGCircleElement): PortableSVGCircleElement = value

private fun portableMaskAsBrowser(value: PortableSVGMaskElement): BrowserSVGMaskElement = value

class GeneratedSvgWebIdentityTest {
    @Test
    fun svgAliasesKeepBrowserIdentity() {
        val circle = document.createElementNS(SVG_NAMESPACE, "circle").unsafeCast<PortableSVGCircleElement>()
        val mask = document.createElementNS(SVG_NAMESPACE, "mask").unsafeCast<PortableSVGMaskElement>()

        browserElementAsPortable(circle)
        portableElementAsBrowser(circle)
        browserCircleAsPortable(circle)
        portableMaskAsBrowser(mask)
        assertEquals("circle", circle.localName)
        assertEquals("mask", mask.localName)
    }
}

private const val SVG_NAMESPACE = "http://www.w3.org/2000/svg"
