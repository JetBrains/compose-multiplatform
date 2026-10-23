/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi::class)
package org.jetbrains.compose.web.dom
import androidx.compose.runtime.Composable
import kotlinx.browser.dom.HTMLElement
import org.jetbrains.compose.web.composeHtmlToString
import org.jetbrains.compose.web.composeHtmlString
import org.jetbrains.compose.web.composeHtmlToStream
import kotlin.test.*

class PreparedHtmlAttributesTest {
    @Test
    fun preparedTextPreservesLeadingNewlinesAndParentTextBoundaries() {
        for (tag in listOf("pre", "textarea", "listing", "title")) {
            val attributes = preparedHtmlAttributes<HTMLElement>(tag, "<$tag>") {}
            for (value in listOf("", "\n<&é😀", "text")) {
                for (strict in listOf(false, true)) {
                    for (hydratable in listOf(false, true)) {
                        for (stream in listOf(false, true)) {
                            fun render(content: @Composable () -> Unit): String {
                                if (!stream) return composeHtmlToString(
                                    validateStrictly = strict, hydratable = hydratable, content = content,
                                )
                                val output = StringBuilder()
                                composeHtmlToStream(
                                    validateStrictly = strict, hydratable = hydratable, chunkSize = 1,
                                    sink = { output.append(it) }, content = content,
                                )
                                return output.toString()
                            }
                            val expected = render {
                                Pre {
                                    TagElement<HTMLElement>(tag, null) { Text(value) }
                                    Text("\ntail")
                                    Text("adjacent")
                                }
                            }
                            assertEquals(expected, render {
                                Pre {
                                    PreparedTextElement(attributes) { value }
                                    Text("\ntail")
                                    Text("adjacent")
                                }
                            }, "$tag, value=$value, strict=$strict, hydratable=$hydratable, stream=$stream")
                        }
                    }
                }
            }
        }
    }

    @Test fun preparedAttributesAndTextMatchOrdinaryRendering() {
        val attrs = preparedHtmlAttributes<HTMLElement>("div", "<div id=\"x&amp;\" class=\"a&amp; b\">") { classes("a&", "b"); id("x&") }
        val text = "é😀<&>"
        for (strict in listOf(false, true)) {
            assertEquals("<div id=\"x&amp;\" class=\"a&amp; b\">é😀&lt;&amp;&gt;</div>",
                composeHtmlToString(validateStrictly = strict) { PreparedTextElement(attrs) { text } })
        }
    }
    @Test fun changingTextUnderOneKeyDoesNotReuseEarlierValues() {
        val attrs = preparedHtmlAttributes<HTMLElement>("div", "<div class=\"stable\">") { classes("stable") }
        for (value in listOf("first", "<&second", "", "é😀")) {
            assertEquals(composeHtmlToString { Div(attrs = { classes("stable") }) { Text(value) } },
                composeHtmlToString(key = "static-changing-text") { PreparedTextElement(attrs) { value } })
        }
    }
    @Test fun incorrectEncodedDataAndUnsupportedTagsAreRejected() {
        assertFailsWith<IllegalArgumentException> { preparedHtmlAttributes<HTMLElement>("div", "<div injected>") {} }
        for (tag in listOf("bad tag", "script", "style", "iframe", "area", "meta", "noscript")) {
            assertFailsWith<IllegalArgumentException>(tag) {
                preparedHtmlAttributes<HTMLElement>(tag, "<$tag>") {}
            }
        }
    }
    @Test fun aDifferentTagUsesTheOrdinaryAttributeCallback() {
        val attrs = preparedHtmlAttributes<HTMLElement>("div", "<div id=\"x\">") { id("x") }
        assertEquals("<span id=\"x\">value</span>", composeHtmlToString {
            TagElement<HTMLElement>("span", attrs.callback) { Text("value") }
        })
    }

    @Test fun parserChecksAndFailureRecoveryStillApplyToStaticLeaves() {
        val div = preparedHtmlAttributes<HTMLElement>("div", "<div>") {}
        val tr = preparedHtmlAttributes<HTMLElement>("tr", "<tr>") {}
        assertFailsWith<IllegalArgumentException> {
            composeHtmlToString(key = "static-parser-recovery", validateStrictly = true) { Table { PreparedTextElement(div) { "invalid child" } } }
        }
        assertFailsWith<IllegalArgumentException> {
            composeHtmlToString(validateStrictly = true) { Table { Tbody { PreparedTextElement(tr) { "text" } } } }
        }
        assertEquals("<table><div>invalid child</div></table>", composeHtmlToString(validateStrictly = false) {
            Table { PreparedTextElement(div) { "invalid child" } }
        })
        assertEquals("<table><tbody><tr>text</tr></tbody></table>", composeHtmlToString(validateStrictly = false) {
            Table { Tbody { PreparedTextElement(tr) { "text" } } }
        })
        assertEquals("<div>recovered</div>", composeHtmlToString(key = "static-parser-recovery") {
            PreparedTextElement(div) { "recovered" }
        })
        assertFailsWith<IllegalArgumentException> { composeHtmlToString { PreparedTextElement(div) { "bad\u0000" } } }
    }

    @Test fun streamingAndTextBoundariesMatchTheOrdinaryPath() {
        val attrs = preparedHtmlAttributes<HTMLElement>("div", "<div class=\"x\">") { classes("x") }
        val text = "\n<&é😀"
        val expected = composeHtmlToString {
            Text("before"); Div(attrs = { classes("x") }) { Text("\n<&é😀") }; Text("after"); Text("adjacent")
        }
        val chunks = StringBuilder()
        val remainder = composeHtmlString(chunkSize = 1, chunkSink = { chunks.append(it) }) {
            Text("before"); PreparedTextElement(attrs) { text }; Text("after"); Text("adjacent")
        }
        assertEquals(expected, chunks.append(remainder).toString())
    }
}
