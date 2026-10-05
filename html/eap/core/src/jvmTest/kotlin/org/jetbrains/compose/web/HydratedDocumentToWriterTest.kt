/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web

import androidx.compose.runtime.Composable
import java.io.IOException
import java.io.Writer
import org.jetbrains.compose.web.dom.Body
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Html
import org.jetbrains.compose.web.dom.Text
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class HydratedDocumentToWriterTest {
    private class RecordingWriter : Writer() {
        val chunks = mutableListOf<String>()
        var flushes = 0
        var closes = 0
        override fun write(chars: CharArray, offset: Int, length: Int) {
            assertTrue(length > 0)
            chunks.add(String(chars, offset, length))
        }
        override fun write(text: String, offset: Int, length: Int) = error("must write a character array")
        override fun flush() { flushes++ }
        override fun close() { closes++ }
    }

    @Test
    fun defaultChunkSizeWritesAt2048CodeUnitsDuringComposition() {
        val text = "x".repeat(2048)
        val writer = RecordingWriter()
        renderHydratedDocumentToStream(writer) {
            Html {
                Body {
                    Text(text)
                    assertEquals(listOf("<!doctype html><html><body>$text"), writer.chunks)
                }
            }
        }
        assertEquals(listOf("<!doctype html><html><body>$text", "</body></html>"), writer.chunks)
    }

    @Test
    fun defaultValidationModeIsTransportedToTheWriter() {
        val writer = RecordingWriter()
        renderHydratedDocumentToStream(writer) {
            Html { Body { HydrationRoot(Unit, { "null" }) {} } }
        }
        assertEquals(
            defaultHtmlValidationMode() == HtmlValidationMode.Strict,
            HydrationValidationAttribute in writer.chunks.single(),
        )
    }

    @Test
    fun writerMatchesStringSinkChunksAndDoesNotFlushOrClose() {
        val content: @Composable () -> Unit = {
            Html {
                Body {
                    HydrationRoot("😀</script>\r\u0000", { it }, hydrationId = "application") {
                        Text("<&😀")
                        Text("adjacent")
                    }
                    HydrationRoot(42, { it.toString() }, hydrationId = "counter") { Text(it.toString()) }
                }
            }
        }
        for (strict in listOf(false, true)) {
            for (size in listOf(1, 4, 32, 2048, 4096, Int.MAX_VALUE)) {
                val writer = RecordingWriter()
                val strings = mutableListOf<String>()
                renderHydratedDocumentToStream(strings::add, size, strict, content)
                renderHydratedDocumentToStream(writer, size, strict, content)
                assertEquals(strings, writer.chunks)
                assertEquals(renderHydratedDocument(strict, content), writer.chunks.joinToString(""))
                assertEquals(0, writer.flushes)
                assertEquals(0, writer.closes)
            }
        }
    }

    @Test
    fun rejectsInvalidSizesBeforeWritingOrComposingAndValidatesDocuments() {
        val writer = RecordingWriter()
        var composed = false
        for (size in listOf(0, -1)) {
            assertFailsWith<IllegalArgumentException> {
                renderHydratedDocumentToStream(writer, size) { composed = true; Html() }
            }
        }
        assertFalse(composed)
        assertTrue(writer.chunks.isEmpty())
        assertFailsWith<IllegalArgumentException> {
            renderHydratedDocumentToStream(writer, chunkSize = 1) { Div() }
        }
        assertEquals("<!doctype html><div></div>", writer.chunks.joinToString(""))
        assertEquals(0, writer.flushes)
        assertEquals(0, writer.closes)
    }

    @Test
    fun propagatesWriterFailuresDuringCompositionAndCanRenderAgain() {
        val failure = IOException("write failed")
        val writer = object : Writer() {
            override fun write(chars: CharArray, offset: Int, length: Int) { throw failure }
            override fun flush() = error("must not flush")
            override fun close() = error("must not close")
        }
        var childrenComposed = false
        assertSame(failure, assertFailsWith<IOException> {
            renderHydratedDocumentToStream(writer, chunkSize = 1) {
                Html { childrenComposed = true }
            }
        })
        assertFalse(childrenComposed)
        val nextWriter = RecordingWriter()
        renderHydratedDocumentToStream(nextWriter) { Html { Body { Text("next") } } }
        assertEquals("<!doctype html><html><body>next</body></html>", nextWriter.chunks.single())
    }
}
