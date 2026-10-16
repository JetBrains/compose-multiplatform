/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi::class)
package org.jetbrains.compose.web.dom
import kotlinx.browser.dom.HTMLElement
import org.jetbrains.compose.web.composeHtmlToString
import org.jetbrains.compose.web.composeHtmlString
import kotlin.test.*

class StaticHtmlAttributesTest {
    @Test fun staticPrefixAndLiteralTextMatchOrdinaryRendering() {
        val attrs = precomputedHtmlAttributes<HTMLElement>("div", "<div id=\"x&amp;\" class=\"a&amp; b\">") { classes("a&", "b"); id("x&") }
        val text = precomputedHtmlText("é😀<&>", "é😀&lt;&amp;&gt;")
        for (strict in listOf(false, true)) {
            assertEquals("<div id=\"x&amp;\" class=\"a&amp; b\">é😀&lt;&amp;&gt;</div>",
                composeHtmlToString(validateStrictly = strict) { LoweredTextTag(attrs, text) })
        }
    }
    @Test fun changingTextUnderOneKeyDoesNotReuseEarlierValues() {
        val attrs = precomputedHtmlAttributes<HTMLElement>("div", "<div class=\"stable\">") { classes("stable") }
        for (value in listOf("first", "<&second", "", "é😀")) {
            assertEquals(composeHtmlToString { Div(attrs = { classes("stable") }) { Text(value) } },
                composeHtmlToString(key = "static-changing-text") { LoweredTextTag(attrs, value) })
        }
    }
    @Test fun incorrectEncodedDataAndRawTagsAreRejected() {
        assertFailsWith<IllegalArgumentException> { precomputedHtmlText("<", "<") }
        assertFailsWith<IllegalArgumentException> { precomputedHtmlText("\u0000", "\u0000") }
        assertFailsWith<IllegalArgumentException> { precomputedHtmlAttributes<HTMLElement>("div", "<div injected>") {} }
        assertFailsWith<IllegalArgumentException> { precomputedHtmlAttributes<HTMLElement>("script", "<script>") {} }
    }
    @Test fun aDifferentTagUsesTheOrdinaryAttributeCallback() {
        val attrs = precomputedHtmlAttributes<HTMLElement>("div", "<div id=\"x\">") { id("x") }
        assertEquals("<span id=\"x\">value</span>", composeHtmlToString {
            TagElement<HTMLElement>("span", attrs.callback) { Text("value") }
        })
    }

    @Test fun parserChecksAndFailureRecoveryStillApplyToStaticLeaves() {
        val div = precomputedHtmlAttributes<HTMLElement>("div", "<div>") {}
        val tr = precomputedHtmlAttributes<HTMLElement>("tr", "<tr>") {}
        assertFailsWith<IllegalArgumentException> {
            composeHtmlToString(key = "static-parser-recovery") { Table { LoweredTextTag(div, "invalid child") } }
        }
        assertFailsWith<IllegalArgumentException> {
            composeHtmlToString { Table { Tbody { LoweredTextTag(tr, precomputedHtmlText("text", "text")) } } }
        }
        assertEquals("<div>recovered</div>", composeHtmlToString(key = "static-parser-recovery") {
            LoweredTextTag(div, "recovered")
        })
        assertFailsWith<IllegalArgumentException> { composeHtmlToString { LoweredTextTag(div, "bad\u0000") } }
    }

    @Test fun streamingAndTextBoundariesMatchTheOrdinaryPath() {
        val attrs = precomputedHtmlAttributes<HTMLElement>("div", "<div class=\"x\">") { classes("x") }
        val text = precomputedHtmlText("\n<&é😀", "\n&lt;&amp;é😀")
        val expected = composeHtmlToString {
            Text("before"); Div(attrs = { classes("x") }) { Text("\n<&é😀") }; Text("after"); Text("adjacent")
        }
        val chunks = StringBuilder()
        val remainder = composeHtmlString(chunkSize = 1, chunkSink = { chunks.append(it) }) {
            Text("before"); LoweredTextTag(attrs, text); Text("after"); Text("adjacent")
        }
        assertEquals(expected, chunks.append(remainder).toString())
    }
}
