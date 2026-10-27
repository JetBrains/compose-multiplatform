/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi::class)

package org.jetbrains.compose.web.dom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import kotlinx.browser.dom.HTMLElement
import org.jetbrains.compose.web.HtmlValidationMode
import org.jetbrains.compose.web.LocalHtmlValidationMode
import org.jetbrains.compose.web.composeHtmlString
import org.jetbrains.compose.web.composeHtmlToStream
import org.jetbrains.compose.web.composeHtmlToString
import org.jetbrains.compose.web.renderHydratedDocument
import org.jetbrains.compose.web.renderHydratedDocumentToStream
import kotlin.test.*

class LoweredSubtreeTest {
    @Test
    fun specializedContainersKeepParserChecksAndNewlines() {
        val pre = preparedHtmlAttributes<HTMLElement>("pre", "<pre>") {}
        assertEquals("<pre>\n\nfirst<!--c-->second</pre>", composeHtmlToString {
            PreparedContainer(pre) { Text("\nfirst"); Text("second") }
        })
        assertFailsWith<IllegalArgumentException> {
            composeHtmlToString(validateStrictly = true) { Table { PreparedContainer(attrs) {} } }
        }
        assertFailsWith<IllegalArgumentException> {
            composeHtmlToString { TagElement<HTMLElement>("script", null) { PreparedContainer(attrs) {} } }
        }
        var calls = 0
        assertEquals("<input>", composeHtmlToString {
            TagElement<HTMLElement>("input", null) { PreparedContainer(attrs) { calls++; Text("discarded") } }
        })
        assertEquals(1, calls)
    }

    private val attrs = preparedHtmlAttributes<HTMLElement>("span", "<span class=\"x\">") { classes("x") }

    @Test
    fun loweredContainersPreserveContentEffectsAndTextBoundaries() {
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
        val lowered = composeHtmlToString { PreparedContainer(attrs, content) }
        assertEquals(ordinary, lowered)
        assertEquals(ordinaryTrace, trace)
    }

    @Test
    fun dynamicValuesKeepScopeAndCompositionLocals() {
        val local = compositionLocalOf { "default" }
        for (value in listOf("<&é😀", "", "next", "\n")) {
            assertEquals(
                "<span class=\"x\">" + value.replace("&", "&amp;").replace("<", "&lt;") + "</span>",
                composeHtmlToString(key = "lowered-local") {
                    CompositionLocalProvider(local provides value) {
                        PreparedTextElement(attrs) { local.current }
                    }
                })
        }
    }

    @Test
    fun textEvaluationAndFailuresKeepStreamingOrder() {
        fun render(lowered: Boolean): Pair<String, List<String>> {
            val chunks = StringBuilder()
            val events = mutableListOf<String>()
            assertFailsWith<IllegalStateException> {
                composeHtmlString(chunkSize = 1, chunkSink = { chunks.append(it); events += "chunk:$it" }) {
                    if (lowered) PreparedTextElement(attrs) { events += "value"; error("failed value") }
                    else Span(attrs = { classes("x") }) { Text(run { events += "value"; error("failed value") }) }
                }
            }
            return chunks.toString() to events
        }
        assertEquals(render(false), render(true))
        assertEquals("<span class=\"x\">recovered</span>", composeHtmlToString(key = "lowered-local") {
            PreparedTextElement(attrs) { "recovered" }
        })
    }

    @Test
    fun discardedTextStillEvaluatesAndInvalidTextStillFails() {
        var calls = 0
        composeHtmlToString {
            TagElement<HTMLElement>(
                "input",
                null
            ) { PreparedTextElement(attrs) { calls++; "discarded" } }
        }
        assertEquals(1, calls)
        assertFailsWith<IllegalArgumentException> { composeHtmlToString { PreparedTextElement(attrs) { "bad\u0000" } } }
    }

    @Test
    fun staticSubtreeMatchesOrdinaryAndStreamingOutput() {
        val original: @Composable () -> Unit = {
            Div {
                Span {
                    Text("<&")
                    Text("")
                    Text("tail")
                }
                P { Text("é😀") }
            }
        }
        val html = "<div><span>&lt;&amp;<!--c-->tail</span><p>é😀</p></div>"
        assertEquals(html, composeHtmlToString(content = original))
        val write: @Composable () -> Unit = { WriteStaticHtml("div", html) }
        for (hydratable in listOf(false, true)) {
            val boundary = if (hydratable) "<!--c-->" else ""
            val expected = "before${html}after${boundary}next"
            assertEquals(expected, composeHtmlToString(hydratable = hydratable) {
                Text("before"); write(); Text("after"); Text("next")
            })
            val chunks = mutableListOf<String>()
            composeHtmlToStream(hydratable = hydratable, chunkSize = 1, sink = { chunks.add(it) }) {
                Text("before"); write(); Text("after"); Text("next")
            }
            assertEquals(expected, chunks.joinToString(""))
            assertEquals(listOf("before", html, "after", "${boundary}next"), chunks)
        }
    }

    @Test
    fun immutableOutputCanBeReturnedOrExtendedAndReusedUnderOneKey() {
        val html = "<div><span>constant</span></div>"
        val write: @Composable () -> Unit = { WriteStaticHtml("div", html) }
        for (hydratable in listOf(false, true)) {
            fun render(content: @Composable () -> Unit) =
                composeHtmlToString(key = "immutable-output", hydratable = hydratable, content = content)
            assertSame(html, render { write() })
            assertEquals(html + "tail", render { write(); Text("tail") })
            assertEquals("head" + html, render { Text("head"); write() })
            assertEquals(html + html, render { write(); write() })
            assertEquals("<section>head${html}tail</section>", render {
                Section { Text("head"); write(); Text("tail") }
            })
            assertSame(html, render { write() })
        }
    }

    @Test
    fun staticWritesCheckParentsAndDiscardVoidContent() {
        val write: @Composable () -> Unit = { WriteStaticHtml("div", "<div><span>safe</span></div>") }
        assertFailsWith<IllegalArgumentException> { composeHtmlToString(validateStrictly = true) { Table { write() } } }
        for (tag in listOf("script", "iframe")) {
            assertFailsWith<IllegalArgumentException> {
                composeHtmlToString {
                    TagElement<HTMLElement>(
                        tag,
                        null
                    ) { write() }
                }
            }
        }
        assertEquals("<input>", composeHtmlToString { TagElement<HTMLElement>("input", null) { write() } })
    }

    @Test
    fun staticWritesUseTheCurrentValidationMode() {
        val write: @Composable () -> Unit = { WriteStaticHtml("tr", "<tr><td>cell</td></tr>") }
        for (strict in listOf(false, true)) for (stream in listOf(false, true)) {
            // Override the outer setting to verify that static writes read the active provider.
            val content: @Composable () -> Unit = {
                CompositionLocalProvider(LocalHtmlValidationMode provides if (strict) HtmlValidationMode.Strict else HtmlValidationMode.Fast) {
                    Table { write() }
                }
            }

            fun render(): String {
                if (!stream) return composeHtmlToString(validateStrictly = !strict, content = content)
                val output = StringBuilder()
                composeHtmlToStream(
                    sink = { output.append(it) },
                    chunkSize = 1,
                    validateStrictly = !strict,
                    content = content
                )
                return output.toString()
            }
            if (strict) assertFailsWith<IllegalArgumentException> { render() }
            else assertEquals("<table><tr><td>cell</td></tr></table>", render())
        }
    }

    @Test
    fun staticWritesRetainDocumentRootMetadata() {
        val document = "<html><head></head><body></body></html>"
        val write: @Composable () -> Unit = { WriteStaticHtml("html", document) }
        assertSame(document, composeHtmlString(requireHtmlDocumentRoot = true) { write() })
        val chunks = StringBuilder()
        composeHtmlString(requireHtmlDocumentRoot = true, chunkSink = { chunks.append(it) }, chunkSize = 1) { write() }
        assertEquals(document, chunks.toString())
        val invalidRoots = listOf(
            Triple("div", "<div></div>", 1),
            Triple("html", document, 2),
        )
        for ((tag, html, count) in invalidRoots) {
            val content: @Composable () -> Unit = { repeat(count) { WriteStaticHtml(tag, html) } }
            assertFailsWith<IllegalArgumentException> {
                composeHtmlString(requireHtmlDocumentRoot = true, content = content)
            }
            for (strict in listOf(false, true)) for (stream in listOf(false, true)) {
                fun render(): String {
                    if (!stream) return renderHydratedDocument(validateStrictly = strict, content = content)
                    val output = StringBuilder()
                    renderHydratedDocumentToStream(
                        sink = { output.append(it) }, chunkSize = 1,
                        validateStrictly = strict, content = content
                    )
                    return output.toString()
                }
                if (strict) assertFailsWith<IllegalArgumentException> { render() }
                else assertEquals("<!doctype html>" + composeHtmlToString(content = content), render())
            }
        }
    }
}
