/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.dom

import androidx.compose.runtime.Composable
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.io.Writer
import kotlinx.browser.dom.Element
import org.jetbrains.compose.web.HtmlRenderer
import org.jetbrains.compose.web.composeHtmlToStream
import org.jetbrains.compose.web.composeHtmlToString
import org.jetbrains.compose.web.htmlWriterSink
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ComposeHtmlToWriterTest {
    private class RecordingWriter : Writer() {
        val chunks = mutableListOf<String>()
        var flushes = 0
        var closes = 0
        override fun write(chars: CharArray, offset: Int, length: Int) {
            assertTrue(length > 0)
            chunks.add(String(chars, offset, length))
        }
        override fun flush() { flushes++ }
        override fun close() { closes++ }
    }

    @Test
    fun defaultChunkSizeWritesAt2048CodeUnitsDuringComposition() {
        val text = "x".repeat(2048)
        for (key in listOf(null, "writer-default-chunk-size")) {
            val writer = RecordingWriter()
            composeHtmlToStream(writer, hydratable = false, key = key) {
                Text(text)
                assertEquals(listOf(text), writer.chunks)
                Text("tail")
            }
            assertEquals(listOf(text, "tail"), writer.chunks)
        }
    }

    @Test
    fun writerAndStringSinkHaveEquivalentOutputAndChunkBoundaries() {
        val content: @Composable () -> Unit = {
            Div(attrs = { attr("title", "<&\"\n\t\r"); classes("one", "two") }) {
                Text("😀<&>α")
                Text("adjacent")
                TagElement<Element>("textarea", null) { Text("\nA & B") }
                TagElement<Element>("pre", null) { Span { Text("child") }; Text("\nline") }
                TagElement<Element>("script", null) { Text("const a = '<&';") }
                TagElement(StringElementBuilder<Element>("svg", "http://www.w3.org/2000/svg"), null) {
                    TagElement(StringElementBuilder<Element>("path", "http://www.w3.org/2000/svg"), { attr("d", "M 0 0") }, null)
                }
            }
        }
        for (size in listOf(1, 4, 32, 2048, 4096, Int.MAX_VALUE)) {
            for (hydratable in listOf(false, true)) {
                for (strict in listOf(false, true)) {
                    val strings = mutableListOf<String>()
                    val writer = RecordingWriter()
                    composeHtmlToStream(strings::add, size, hydratable, validateStrictly = strict, content = content)
                    composeHtmlToStream(writer, size, hydratable, validateStrictly = strict, content = content)
                    assertEquals(strings, writer.chunks)
                    assertEquals(composeHtmlToString(hydratable, validateStrictly = strict, content = content), writer.chunks.joinToString(""))
                    assertEquals(0, writer.flushes)
                    assertEquals(0, writer.closes)
                }
            }
        }
    }

    @Test
    fun emptyContentDoesNotWriteFlushOrCloseAndRejectsInvalidSizes() {
        val writer = RecordingWriter()
        composeHtmlToStream(writer) {}
        assertTrue(writer.chunks.isEmpty())
        assertEquals(0, writer.flushes)
        assertEquals(0, writer.closes)
        var composed = false
        for (size in listOf(0, -1)) {
            val failure = assertFailsWith<IllegalArgumentException> {
                composeHtmlToStream(writer, chunkSize = size) { composed = true; Text("unused") }
            }
            assertEquals("chunkSize must be positive", failure.message)
        }
        assertFalse(composed)
        assertTrue(writer.chunks.isEmpty())
        assertEquals(0, writer.flushes)
        assertEquals(0, writer.closes)
    }

    @Test
    fun utf8WriterPreservesSupplementaryCharactersAcrossWrites() {
        val bytes = ByteArrayOutputStream()
        val writer = OutputStreamWriter(bytes, Charsets.UTF_8)
        composeHtmlToStream(writer, chunkSize = 1, hydratable = false) {
            Text("\uD83D")
            Text("\uDE00")
            Text("α&")
        }
        writer.flush()
        assertEquals("😀α&amp;", bytes.toString(Charsets.UTF_8))
    }

    @Test
    fun writesSynchronouslyBeforeCompositionContinuesAndSupportsMixedKeyedCalls() {
        val writer = RecordingWriter()
        composeHtmlToStream(writer, chunkSize = 1, key = "writer-shared") {
            Text("first")
            assertEquals(listOf("first"), writer.chunks)
            Text("second")
        }
        assertEquals("short", composeHtmlToString(key = "writer-shared") { Text("short") })
        val strings = mutableListOf<String>()
        composeHtmlToStream(sink = strings::add, chunkSize = 1, key = "writer-shared") { Text("last") }
        assertEquals(listOf("last"), strings)
    }

    @Test
    fun writerFailureAndContentValidationFailureDisposeTheKey() {
        val failing = object : Writer() {
            override fun write(chars: CharArray, offset: Int, length: Int) { throw IOException("write failed") }
            override fun flush() = Unit
            override fun close() = Unit
        }
        assertFailsWith<IOException> {
            composeHtmlToStream(failing, chunkSize = 1, key = "writer-failure") { Text("first") }
        }
        assertEquals("second", composeHtmlToString(key = "writer-failure") { Text("second") })
        for (invalid in listOf<@Composable () -> Unit>(
            { TagElement<Element>("script", null) { Text("</scr"); Text("ipt>") } },
            { TagElement<Element>("script", { attr("SRC", "script.js") }) { Text("") } },
            { TagElement<Element>("noscript", null) { TagElement<Element>("script", null) { Text("</noscript>") } } },
            { Div({ attr("title", "\u0000") }) },
            { TagElement(StringElementBuilder<Element>("svg", "http://www.w3.org/2000/svg"), { attr("viewBox", "a"); attr("VIEWBOX", "b") }, null) },
        )) {
            val writer = RecordingWriter()
            assertFailsWith<IllegalArgumentException> {
                composeHtmlToStream(writer, chunkSize = 1, key = "writer-invalid", validateStrictly = true) {
                    Text("valid")
                    invalid()
                }
            }
            assertEquals(listOf("valid"), writer.chunks)
            assertEquals("ok", composeHtmlToString(key = "writer-invalid") { Text("ok") })
        }
    }

    @Test
    fun shrinksOldLargeBuffersAfterSmallRendersButReusesRepeatedLargeBuffers() {
        val renderer = HtmlRenderer()
        fun render(text: String, writer: Writer? = null): String = renderer.render(
            hydratable = false,
            requireHtmlDocumentRoot = false,
            chunkSink = writer?.let(::htmlWriterSink),
            chunkSize = 2048,
            prefix = "",
        ) { Text(text) }
        val large = "α".repeat(1024 * 1024)
        render(large)
        val largeCapacity = renderer.bufferCapacity
        assertTrue(largeCapacity >= large.length)
        render(large)
        assertEquals(largeCapacity, renderer.bufferCapacity)
        assertEquals("small", render("small"))
        assertTrue(renderer.bufferCapacity <= 65536)

        render(large)
        assertTrue(renderer.bufferCapacity >= large.length)
        render("small", RecordingWriter())
        assertTrue(renderer.bufferCapacity <= 65536)

        render(large)
        val chunk = "x".repeat(20 * 1024)
        val writer = RecordingWriter()
        render(chunk, writer)
        assertEquals(listOf(chunk), writer.chunks)
        assertEquals(chunk.length, renderer.bufferCapacity)
        assertEquals(0, renderer.bufferLength)
        render(chunk, RecordingWriter())
        assertEquals(chunk.length, renderer.bufferCapacity)
    }
}
