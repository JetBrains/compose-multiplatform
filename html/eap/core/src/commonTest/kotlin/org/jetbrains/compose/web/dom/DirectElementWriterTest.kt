/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi::class)

package org.jetbrains.compose.web.dom

import androidx.compose.runtime.Composable
import kotlinx.browser.dom.Element
import kotlinx.browser.dom.HTMLTextAreaElement
import org.jetbrains.compose.web.attributes.setTextAreaValue
import org.jetbrains.compose.web.composeHtmlToStream
import org.jetbrains.compose.web.composeHtmlToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DirectElementWriterTest {
    @Test
    fun chunkedFragmentsValidateAndCountOnceAndRejectWritesAfterFinishing() {
        val writer = HtmlStringWriterContext(hydratable = true)
        assertEquals(true, writer.beginStaticHtmlFragment("html", true))
        writer.appendStaticHtmlChunk("<html><body>")
        writer.appendStaticHtmlChunk("content</body></html>")
        assertEquals("<html><body>content</body></html>", writer.finish(requireHtmlDocumentRoot = true))
        assertFailsWith<IllegalStateException> { writer.beginStaticHtmlFragment("div", true) }

        val invalidParent = HtmlStringWriterContext(hydratable = true)
        invalidParent.openLoweredRegion("table", "<table>", "table", true)
        assertFailsWith<IllegalArgumentException> { invalidParent.beginStaticHtmlFragment("div", true) }
    }

    @Test
    fun bridgeMatchesOrdinaryElementsInEveryRenderingMode() {
        for (strict in listOf(false, true)) {
            for (hydratable in listOf(false, true)) {
                for (stream in listOf(false, true)) {
                    for (value in listOf("", "<&>\ré😀", "\ntext")) {
                        val ordinary: @Composable () -> Unit = {
                            Text("before")
                            Div(attrs = {
                                id("greeting")
                                attr("title", "Hello & goodbye")
                                classes("card")
                            }) {
                                Text(value)
                                Text("")
                                Text("tail")
                                Span { Text("child") }
                            }
                            Text("after")
                            Text("adjacent")
                        }
                        val direct: @Composable () -> Unit = {
                            Text("before")
                            val writer = currentHtmlStringWriter()
                            val parent = writer.openLoweredRegion("div", StartTag, "div", strict)
                            writer.writeText(value, strict)
                            writer.writeText("", strict)
                            writer.writeText("tail", strict)
                            Span { Text("child") }
                            writer.closeElement("</div>", parent)
                            Text("after")
                            Text("adjacent")
                        }
                        assertEquals(render(strict, hydratable, stream, ordinary), render(strict, hydratable, stream, direct))
                    }
                }
            }
        }
    }

    @Test
    fun discardedContentStillEvaluatesText() {
        var calls = 0
        val discarded: @Composable () -> Unit = {
            val writer = currentHtmlStringWriter()
            val parent = writer.openLoweredRegion("div", StartTag, "div", true)
            writer.writeText(run {
                calls++
                "bad\u0000"
            }, true)
            writer.closeElement("</div>", parent)
        }
        assertEquals("<input><textarea>override</textarea><span>after</span>", composeHtmlToString {
            TagElement<Element>("input", null) { discarded() }
            TagElement<HTMLTextAreaElement>("textarea", { prop(setTextAreaValue, "override") }) {
                discarded()
            }
            Span { Text("after") }
        })
        assertEquals(2, calls)
    }

    @Test
    fun invalidParentsRejectElementsBeforeEvaluatingText() {
        for (stream in listOf(false, true)) {
            for (raw in listOf(false, true)) {
                var calls = 0
                assertFailsWith<IllegalArgumentException> {
                    render(true, true, stream) {
                        TagElement<Element>(if (raw) "iframe" else "table", null) {
                            val writer = currentHtmlStringWriter()
                            val parent = writer.openLoweredRegion("div", StartTag, "div", true)
                            writer.writeText(run {
                                calls++
                                "text"
                            }, true)
                            writer.closeElement("</div>", parent)
                        }
                    }
                }
                assertEquals(0, calls)
            }
        }
    }

    @Test
    fun restoresForeignNamespaceAndLeadingNewlineState() {
        assertEquals("<svg><div></div><table><div></div></table></svg><pre><div></div>\ntext</pre>",
            composeHtmlToString(validateStrictly = true) {
                TagElementNS<Element>("svg", "urn:test", null) {
                    EmptyDirectDiv()
                    TagElementNS<Element>("table", "urn:test", null) {
                        EmptyDirectDiv()
                    }
                }
                Pre {
                    EmptyDirectDiv()
                    Text("\ntext")
                }
            })
    }

    @Test
    fun countsDocumentRootsAndRejectsWritesAfterFinishing() {
        val writer = HtmlStringWriterContext(hydratable = true)
        val parent = writer.openLoweredRegion("html", "<html>", "html", true)
        writer.closeElement("</html>", parent)
        assertEquals("<html></html>", writer.finish(requireHtmlDocumentRoot = true))
        assertFailsWith<IllegalStateException> { writer.writeText("late", true) }

        val multipleRoots = HtmlStringWriterContext(hydratable = true)
        repeat(2) {
            val root = multipleRoots.openLoweredRegion("html", "<html>", "html", true)
            multipleRoots.closeElement("</html>", root)
        }
        assertFailsWith<IllegalArgumentException> { multipleRoots.finish(requireHtmlDocumentRoot = true) }
    }

    private fun render(strict: Boolean, hydratable: Boolean, stream: Boolean, content: @Composable () -> Unit): String {
        if (!stream) return composeHtmlToString(validateStrictly = strict, hydratable = hydratable, content = content)
        val output = StringBuilder()
        composeHtmlToStream(validateStrictly = strict, hydratable = hydratable, chunkSize = 1,
            sink = { output.append(it) }, content = content)
        return output.toString()
    }

    @Composable
    private fun EmptyDirectDiv() {
        val writer = currentHtmlStringWriter()
        val parent = writer.openLoweredRegion("div", "<div>", "div", true)
        writer.closeElement("</div>", parent)
    }

    private companion object {
        const val StartTag = "<div id=\"greeting\" title=\"Hello &amp; goodbye\" class=\"card\">"
    }
}
