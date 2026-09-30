/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.dom

import androidx.compose.runtime.Composable
import kotlinx.browser.dom.Element
import org.jetbrains.compose.web.composeHtmlToStream
import org.jetbrains.compose.web.composeHtmlToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ComposeHtmlToStreamTest {
    @Test
    fun emitsTextDuringCompositionWithoutSplittingTextCalls() {
        val chunks = mutableListOf<String>()
        var streaming = true
        val content: @Composable () -> Unit = {
            Text("ab&")
            if (streaming) assertEquals(listOf("ab&amp;"), chunks)
            Div {
                Text("first")
                Text("second")
                Span { Text("😀 <&>") }
            }
            Text("tail")
        }

        composeHtmlToStream(sink = chunks::add, chunkSize = 4, content = content)
        streaming = false

        assertEquals(composeHtmlToString(content = content), chunks.joinToString(""))
        assertTrue(chunks.size > 1)
        assertTrue(chunks.all { it.isNotEmpty() })
        assertTrue(chunks.any { it.length > 4 })
    }

    @Test
    fun keepsElementsThatInspectTheirContentsInTheSharedBuffer() {
        val chunks = mutableListOf<String>()
        var streaming = true
        val content: @Composable () -> Unit = {
            Text("prefix")
            TagElement<Element>("pre", null) {
                Text("\nline")
                if (streaming) assertFalse(chunks.any { "<pre" in it })
                Span { Text("child") }
            }
            TagElement<Element>("textarea", null) { Text("\nA & B") }
            TagElement<Element>("listing", null) { Text("\nline") }
            TagElement<Element>("noscript", null) { Span { Text("fallback") } }
            TagElement<Element>("script", null) { Text("const x = '<&';") }
        }

        composeHtmlToStream(sink = chunks::add, chunkSize = 1, content = content)
        streaming = false

        assertEquals(composeHtmlToString(content = content), chunks.joinToString(""))
        assertTrue(chunks.all { it.isNotEmpty() })
    }

    @Test
    fun failedBufferedElementsDoNotEmitInvalidMarkupAndKeyCanBeReused() {
        for (invalid in listOf<@Composable () -> Unit>(
            { TagElement<Element>("script", null) { Text("</scr"); Text("ipt>") } },
            {
                TagElement<Element>("noscript", null) {
                    TagElement<Element>("script", null) { Text("</noscript>") }
                }
            },
        )) {
            val chunks = mutableListOf<String>()
            assertFailsWith<IllegalArgumentException> {
                composeHtmlToStream(sink = chunks::add, chunkSize = 1, key = "stream-invalid") {
                    Text("valid")
                    invalid()
                }
            }
            assertEquals(listOf("valid"), chunks)
            assertEquals("ok", composeHtmlToString(key = "stream-invalid") { Text("ok") })
        }
    }

    @Test
    fun stringAndStreamRendersShareKeyedCompositionAndClearTheBuffer() {
        val chunks = mutableListOf<String>()
        composeHtmlToStream(sink = chunks::add, chunkSize = 3, key = "stream-shared") {
            Div { Text("long & text".repeat(100)) }
        }
        assertEquals(
            composeHtmlToString { Div { Text("long & text".repeat(100)) } },
            chunks.joinToString(""),
        )

        assertEquals(
            "<div>short</div>",
            composeHtmlToString(key = "stream-shared") { Div { Text("short") } },
        )
        chunks.clear()
        composeHtmlToStream(sink = chunks::add, chunkSize = 3, key = "stream-shared") { Text("last") }
        assertEquals(listOf("last"), chunks)

        assertFailsWith<IllegalArgumentException> {
            composeHtmlToStream(sink = chunks::add, chunkSize = 0) { Text("unused") }
        }
    }

    @Test
    fun sinkFailureDisposesTheKeyedRenderer() {
        assertFailsWith<IllegalStateException> {
            composeHtmlToStream(
                sink = { throw IllegalStateException("sink failed") },
                chunkSize = 1,
                key = "stream-sink-failure",
            ) {
                Text("first")
            }
        }
        assertEquals("second", composeHtmlToString(key = "stream-sink-failure") { Text("second") })
    }
}
