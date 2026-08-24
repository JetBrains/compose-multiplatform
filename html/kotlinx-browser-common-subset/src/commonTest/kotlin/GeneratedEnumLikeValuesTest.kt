/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Verifies enum-like value behavior shared by all targets.
package kotlinx.browser.dom.enumlike

import kotlinx.browser.JsAny
import kotlinx.browser.dom.AUTO
import kotlinx.browser.dom.BORDER
import kotlinx.browser.dom.CLOSED
import kotlinx.browser.dom.COMPLETE
import kotlinx.browser.dom.CONTENT
import kotlinx.browser.dom.CSSBoxType
import kotlinx.browser.dom.CanPlayTypeResult
import kotlinx.browser.dom.ColorSpaceConversion
import kotlinx.browser.dom.DEFAULT
import kotlinx.browser.dom.DocumentReadyState
import kotlinx.browser.dom.EMPTY
import kotlinx.browser.dom.INSTANT
import kotlinx.browser.dom.LOADING
import kotlinx.browser.dom.LOW
import kotlinx.browser.dom.NONE
import kotlinx.browser.dom.PremultiplyAlpha
import kotlinx.browser.dom.PIXELATED
import kotlinx.browser.dom.ResizeQuality
import kotlinx.browser.dom.SMOOTH
import kotlinx.browser.dom.ScrollBehavior
import kotlinx.browser.dom.ScrollToOptions
import kotlinx.browser.dom.ShadowRootMode
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Compares repeated reads by identity on JS/JVM and by their wrapped string on Wasm/JS.
internal expect fun areIdentical(first: JsAny, second: JsAny): Boolean

// Checks stable, distinct enum-like values and their companion-extension call syntax on every target.
class GeneratedEnumLikeValuesTest {
    @Test
    fun everyValueKeepsOneIdentity() {
        assertStable(ScrollBehavior.AUTO, ScrollBehavior.AUTO)
        assertStable(ScrollBehavior.INSTANT, ScrollBehavior.INSTANT)
        assertStable(ScrollBehavior.SMOOTH, ScrollBehavior.SMOOTH)
        assertStable(DocumentReadyState.LOADING, DocumentReadyState.LOADING)
        assertStable(DocumentReadyState.COMPLETE, DocumentReadyState.COMPLETE)
        assertStable(CSSBoxType.BORDER, CSSBoxType.BORDER)
        assertStable(CSSBoxType.CONTENT, CSSBoxType.CONTENT)
        assertStable(ShadowRootMode.CLOSED, ShadowRootMode.CLOSED)
        assertStable(CanPlayTypeResult.EMPTY, CanPlayTypeResult.EMPTY)
        assertStable(PremultiplyAlpha.DEFAULT, PremultiplyAlpha.DEFAULT)
        assertStable(ColorSpaceConversion.NONE, ColorSpaceConversion.NONE)
        assertStable(ResizeQuality.PIXELATED, ResizeQuality.PIXELATED)
    }

    @Test
    fun valuesOfTheSameEnumStayDistinct() {
        assertFalse(areIdentical(ScrollBehavior.AUTO, ScrollBehavior.SMOOTH))
        assertFalse(areIdentical(ScrollBehavior.INSTANT, ScrollBehavior.SMOOTH))
        assertFalse(areIdentical(CSSBoxType.BORDER, CSSBoxType.CONTENT))
        assertFalse(areIdentical(DocumentReadyState.LOADING, DocumentReadyState.COMPLETE))
        assertFalse(areIdentical(ResizeQuality.PIXELATED, ResizeQuality.LOW))
    }

    // A nullable source-independent dictionary default remains callable on every target.
    @Test
    fun aValueServesAsADictionaryFactoryDefault() {
        ScrollToOptions()
        ScrollToOptions(left = 1.0, top = 2.0)
        ScrollToOptions(behavior = ScrollBehavior.SMOOTH)
    }

    private fun assertStable(first: JsAny, second: JsAny) {
        assertTrue(areIdentical(first, second))
    }
}
