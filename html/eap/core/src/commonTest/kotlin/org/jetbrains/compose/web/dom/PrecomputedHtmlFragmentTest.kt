/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi::class)

package org.jetbrains.compose.web.dom

import org.jetbrains.compose.web.composeHtmlToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PrecomputedHtmlFragmentTest {
    @Test
    fun directConstructionDoesNotRenderOriginalContent() {
        val fragment = PrecomputedHtmlFragment("span", "<span>value</span>") {
            error("Original content must not run during initialization or static string rendering")
        }
        repeat(2) {
            assertEquals("<span>value</span>", composeHtmlToString { LoweredStaticHtmlFragment(fragment) })
        }
    }

    @Test
    fun factoryVerifiesOnceWithoutRepeatingDuringRendering() {
        var calls = 0
        val fragment = precomputedHtmlFragment("span", "<span>value</span>") {
            calls++
            Span { Text("value") }
        }
        assertEquals(1, calls)
        repeat(2) {
            assertEquals("<span>value</span>", composeHtmlToString { LoweredStaticHtmlFragment(fragment) })
        }
        assertEquals(1, calls)
    }

    @Test
    fun ordinaryRootsDoNotNeedASeparateAllowlist() {
        val fragment = precomputedHtmlFragment("section", "<section>value</section>") {
            Section { Text("value") }
        }
        assertEquals("<section>value</section>", composeHtmlToString { LoweredStaticHtmlFragment(fragment) })
    }

    @Test
    fun missingRootContentIsRejected() {
        for (tag in listOf("div", "br", "script", "style")) {
            assertFailsWith<IllegalArgumentException>(tag) {
                precomputedHtmlFragment(tag, "") {}
            }
        }
    }

    @Test
    fun voidAndRawTextRootsUseTheOriginalRendererForValidation() {
        val void = precomputedHtmlFragment("br", "<br>") { Br() }
        val rawText = precomputedHtmlFragment("iframe", "<iframe><b>&</iframe>") {
            Iframe { Text("<b>&") }
        }
        assertEquals("<br>", composeHtmlToString { LoweredStaticHtmlFragment(void) })
        assertEquals("<iframe><b>&</iframe>", composeHtmlToString { LoweredStaticHtmlFragment(rawText) })
        assertFailsWith<IllegalArgumentException> {
            precomputedHtmlFragment("br", "<br></br>") { Br() }
        }
        assertFailsWith<IllegalArgumentException> {
            precomputedHtmlFragment("iframe", "<iframe>&lt;b&gt;&amp;</iframe>") {
                Iframe { Text("<b>&") }
            }
        }
    }
}
