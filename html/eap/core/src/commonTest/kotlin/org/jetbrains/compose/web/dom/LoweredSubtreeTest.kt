/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi::class)

package org.jetbrains.compose.web.dom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
