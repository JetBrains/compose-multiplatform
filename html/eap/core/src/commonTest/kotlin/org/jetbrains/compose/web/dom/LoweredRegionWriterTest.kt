/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi::class)

package org.jetbrains.compose.web.dom

import androidx.compose.runtime.Composable
import kotlinx.browser.dom.Element
import org.jetbrains.compose.web.composeHtmlToStream
import org.jetbrains.compose.web.composeHtmlToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class LoweredRegionWriterTest {
    @Test
    fun transitionsMatchOrdinaryElements() {
        for (strict in listOf(false, true)) for (hydratable in listOf(false, true)) for (stream in listOf(false, true)) {
            val ordinary: @Composable () -> Unit = {
                Div {
                    H1 { Text("Title") }
                    P {
                        Text("a")
                        Text("b")
                    }
                    Span { Text("c") }
                    Text("d")
                }
                Text("after")
            }
            val lowered: @Composable () -> Unit = {
                val writer = currentHtmlStringWriter()
                val parent = writer.openLoweredRegion("div", "<div><h1>Title</h1><p>", "p", strict)
                assertEquals("p", writer.currentTagName)
                Text("a")
                Text("b")
                writer.advanceLoweredRegion("</p><span>", "span")
                assertEquals("span", writer.currentTagName)
                Text("c")
                writer.advanceLoweredRegion("</span>", "div")
                Text("d")
                writer.closeElement("</div>", parent)
                assertNull(writer.currentTagName)
                Text("after")
            }
            assertEquals(render(strict, hydratable, stream, ordinary), render(strict, hydratable, stream, lowered))
        }
    }

    @Test
    fun discardedRegionsStillRunDynamicEffects() {
        var effects = 0
        assertEquals("<input><span>after</span>", composeHtmlToString {
            TagElement<Element>("input", null) {
                val writer = currentHtmlStringWriter()
                val parent = writer.openLoweredRegion("div", "<div><p>", "p", true)
                effects++
                writer.writeText("discarded", true)
                writer.advanceLoweredRegion("</p><span>", "span")
                writer.closeElement("</span></div>", parent)
            }
            Span { Text("after") }
        })
        assertEquals(1, effects)
    }

    @Test
    fun entryFailurePreservesPrecedingOutputAndSkipsDynamicEffect() {
        val output = StringBuilder()
        var effects = 0
        assertFailsWith<IllegalArgumentException> {
            composeHtmlToStream(validateStrictly = true, chunkSize = 1, sink = { output.append(it) }) {
                TagElement<Element>("table", null) {
                    currentHtmlStringWriter().openLoweredRegion("div", "<div><p>", "p", true)
                    effects++
                }
            }
        }
        assertEquals("<table>", output.toString())
        assertEquals(0, effects)
    }

    @Test
    fun restoresForeignNamespaceAndRejectsFinishedWriter() {
        val writer = HtmlStringWriterContext(hydratable = true)
        val outer = writer.enter("svg", "urn:test", false)
        val parent = writer.openLoweredRegion("div", "<div><p>", "p", true)
        writer.closeElement("</p></div>", parent)
        assertEquals("svg", writer.currentTagName)
        assertEquals("urn:test", writer.currentNamespace)
        writer.exit(outer)
        assertEquals("<div><p></p></div>", writer.finish(requireHtmlDocumentRoot = false))
        assertFailsWith<IllegalStateException> { writer.openLoweredRegion("div", "<div>", "div", true) }
        assertFailsWith<IllegalStateException> { writer.advanceLoweredRegion("</p>", "div") }
    }

    private fun render(strict: Boolean, hydratable: Boolean, stream: Boolean, content: @Composable () -> Unit): String {
        if (!stream) return composeHtmlToString(validateStrictly = strict, hydratable = hydratable, content = content)
        val output = StringBuilder()
        composeHtmlToStream(
            validateStrictly = strict, hydratable = hydratable, chunkSize = 1,
            sink = { output.append(it) }, content = content,
        )
        return output.toString()
    }
}
