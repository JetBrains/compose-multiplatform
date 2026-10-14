/* Copyright 2026 JetBrains s.r.o. Licensed under the Apache License, Version 2.0. */
@file:OptIn(org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi::class)
package org.jetbrains.compose.web.dom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import kotlinx.browser.dom.HTMLElement
import org.jetbrains.compose.web.composeHtmlString
import org.jetbrains.compose.web.composeHtmlToString
import kotlin.test.*

class LoweredSubtreeTest {
    @Test fun specializedContainersKeepParserChecksAndNewlines() {
        val pre = precomputedHtmlAttributes<HTMLElement>("pre", "<pre>") {}
        assertEquals("<pre>\n\nfirst<!--c-->second</pre>", composeHtmlToString {
            LoweredHtmlContainer(pre) { Text("\nfirst"); Text("second") }
        })
        assertFailsWith<IllegalArgumentException> {
            composeHtmlToString { Table { LoweredHtmlContainer(attrs) {} } }
        }
        assertFailsWith<IllegalArgumentException> {
            composeHtmlToString { TagElement<HTMLElement>("script", null) { LoweredHtmlContainer(attrs) {} } }
        }
        var calls = 0
        assertEquals("<input>", composeHtmlToString {
            TagElement<HTMLElement>("input", null) { LoweredHtmlContainer(attrs) { calls++; Text("discarded") } }
        })
        assertEquals(1, calls)
    }

    private val attrs = precomputedHtmlAttributes<HTMLElement>("span", "<span class=\"x\">") { classes("x") }

    @Test fun loweredContainersPreserveContentEffectsAndTextBoundaries() {
        val trace = mutableListOf<String>()
        val content: @Composable ElementScope<HTMLElement>.() -> Unit = {
            androidx.compose.runtime.remember { trace += "remember"; Unit }
            androidx.compose.runtime.SideEffect { trace += "side" }
            androidx.compose.runtime.DisposableEffect(Unit) { trace += "enter"; onDispose { trace += "dispose" } }
            Text("a"); Text("b"); Span { Text("nested") }
        }
        val ordinary = composeHtmlToString { TagElement(attrs.elementBuilder, attrs.callback, content) }
        val ordinaryTrace = trace.toList()
        trace.clear()
        val lowered = composeHtmlToString { LoweredHtmlContainer(attrs, content) }
        assertEquals(ordinary, lowered)
        assertEquals(ordinaryTrace, trace)
    }

    @Test fun dynamicValuesKeepScopeAndCompositionLocals() {
        val local = compositionLocalOf { "default" }
        for (value in listOf("<&é😀", "", "next", "\n")) {
            assertEquals("<span class=\"x\">" + value.replace("&", "&amp;").replace("<", "&lt;") + "</span>",
                composeHtmlToString(key = "lowered-local") {
                    CompositionLocalProvider(local provides value) {
                        LoweredDynamicTextTag(attrs) { local.current }
                    }
                })
        }
    }

    @Test fun textEvaluationAndFailuresKeepStreamingOrder() {
        fun render(lowered: Boolean): Pair<String, List<String>> {
            val chunks = StringBuilder()
            val events = mutableListOf<String>()
            assertFailsWith<IllegalStateException> {
                composeHtmlString(chunkSize = 1, chunkSink = { chunks.append(it); events += "chunk:$it" }) {
                    if (lowered) LoweredDynamicTextTag(attrs) { events += "value"; error("failed value") }
                    else Span(attrs = { classes("x") }) { Text(run { events += "value"; error("failed value") }) }
                }
            }
            return chunks.toString() to events
        }
        assertEquals(render(false), render(true))
        assertEquals("<span class=\"x\">recovered</span>", composeHtmlToString(key = "lowered-local") {
            LoweredDynamicTextTag(attrs) { "recovered" }
        })
    }

    @Test fun discardedTextStillEvaluatesAndInvalidTextStillFails() {
        var calls = 0
        composeHtmlToString { TagElement<HTMLElement>("input", null) { LoweredDynamicTextTag(attrs) { calls++; "discarded" } } }
        assertEquals(1, calls)
        assertFailsWith<IllegalArgumentException> { composeHtmlToString { LoweredDynamicTextTag(attrs) { "bad\u0000" } } }
    }

    @Test fun staticSubtreeMatchesOrdinaryAndStreamingOutput() {
        val original: @Composable () -> Unit = { Div { Span { Text("<&") }; P { Text("é😀") } } }
        val html = "<div><span>&lt;&amp;</span><p>é😀</p></div>"
        val fragment = precomputedHtmlFragment("div", html, original)
        for (hydratable in listOf(false, true)) {
            val expected = composeHtmlToString(hydratable = hydratable) { Text("before"); original(); Text("after"); Text("next") }
            assertEquals(expected, composeHtmlToString(hydratable = hydratable) {
                Text("before"); LoweredStaticHtmlFragment(fragment); Text("after"); Text("next")
            })
            val chunks = StringBuilder()
            composeHtmlString(hydratable = hydratable, chunkSize = 1, chunkSink = { chunks.append(it) }) {
                Text("before"); LoweredStaticHtmlFragment(fragment); Text("after"); Text("next")
            }
            assertEquals(expected, chunks.toString())
        }
    }

    @Test fun immutableOutputCanBeReturnedOrExtendedAndReusedUnderOneKey() {
        val html = "<div><span>constant</span></div>"
        val fragment = precomputedHtmlFragment("div", html) { Div { Span { Text("constant") } } }
        assertSame(html, composeHtmlToString(key = "immutable-output") { LoweredStaticHtmlFragment(fragment) })
        assertEquals(html + "tail", composeHtmlToString(key = "immutable-output") {
            LoweredStaticHtmlFragment(fragment); Text("tail")
        })
        assertEquals("head" + html, composeHtmlToString(key = "immutable-output") {
            Text("head"); LoweredStaticHtmlFragment(fragment)
        })
        assertEquals(html + html, composeHtmlToString(key = "immutable-output") {
            LoweredStaticHtmlFragment(fragment); LoweredStaticHtmlFragment(fragment)
        })
        assertSame(html, composeHtmlToString(key = "immutable-output") { LoweredStaticHtmlFragment(fragment) })
    }

    @Test fun subtreeMetadataAndParserChecksCannotBeBypassed() {
        assertFailsWith<IllegalArgumentException> { precomputedHtmlFragment("div", "text") { Text("text") } }
        assertFailsWith<IllegalArgumentException> { precomputedHtmlFragment("div", "<span></span>") { Span {} } }
        assertFailsWith<IllegalArgumentException> { precomputedHtmlFragment("div", "<div></div><div></div>") { Div {}; Div {} } }
        assertFailsWith<IllegalArgumentException> { precomputedHtmlFragment("div", "<div>unsafe</div>") { Div { Text("safe") } } }
        val fragment = precomputedHtmlFragment("div", "<div><span>safe</span></div>") { Div { Span { Text("safe") } } }
        assertFailsWith<IllegalArgumentException> { composeHtmlToString { Table { LoweredStaticHtmlFragment(fragment) } } }
        assertFailsWith<IllegalArgumentException> { composeHtmlToString { TagElement<HTMLElement>("script", null) { LoweredStaticHtmlFragment(fragment) } } }
    }
}
