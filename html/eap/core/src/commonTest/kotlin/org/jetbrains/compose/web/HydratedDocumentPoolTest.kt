/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import org.jetbrains.compose.web.dom.Body
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Html
import org.jetbrains.compose.web.dom.Text
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class HydratedDocumentPoolTest {
    @Test
    fun documentsStreamsAndFragmentsShareStorageWithFreshRequestState() {
        val key = "document-request-state"
        val pool = assertNotNull(getHtmlRendererPool(key))
        val renderer = pool.borrow()
        pool.recycle(renderer)
        val snapshotState = mutableStateOf("outside")
        var remembered = 0
        var disposals = 0
        for (stream in listOf(false, true, false, true)) {
            val request = "request-${remembered + 1}"
            val strict = !stream
            val rendered = render(stream, key, strict) {
                val value = remember { ++remembered; request }
                snapshotState.value = request
                DisposableEffect(Unit) { onDispose { disposals++ } }
                Html {
                    Body {
                        HydrationRoot(value, { it }, hydrationId = "application", rootAttrs = {
                            if (strict) classes("first")
                        }) { Text(it) }
                    }
                }
            }
            assertTrue(rendered.startsWith("<!doctype html><html><body>"))
            assertContains(rendered, "$request</div>")
            assertContains(rendered, "$request</script>")
            assertEquals(strict, "class=\"first\"" in rendered)
            assertEquals(strict, HydrationValidationAttribute in rendered)
            assertEquals("outside", snapshotState.value)
            assertEquals(remembered, disposals)
            assertSame(renderer, pool.borrow())
            pool.recycle(renderer)
            assertEquals("fragment", composeHtmlToString(key = key) { Text("fragment") })
            assertSame(renderer, pool.borrow())
            pool.recycle(renderer)
        }
        assertEquals(4, remembered)
        // Document locals must not leak into the next fragment using the same renderer.
        assertFailsWith<IllegalStateException> {
            composeHtmlToString(key = key) { HydrationRoot(Unit, { "null" }) {} }
        }
    }

    @Test
    fun failuresDiscardDocumentStorageAndTheKeyCanRenderAgain() {
        val key = "document-failure-storage"
        val pool = assertNotNull(getHtmlRendererPool(key))
        for (stream in listOf(false, true)) {
            for (failure in listOf("content", "serializer", "validation", "duplicate-id", "disposal", "sink")) {
                if (!stream && failure == "sink") continue
                val renderer = pool.borrow()
                pool.recycle(renderer)
                val cause = IllegalStateException(failure)
                val content: @Composable () -> Unit = {
                    DisposableEffect(Unit) { onDispose { if (failure == "disposal") throw cause } }
                    if (failure == "content") throw cause
                    if (failure == "validation") Div() else Html {
                        Body {
                            HydrationRoot(Unit, { if (failure == "serializer") throw cause; "state" }, hydrationId = "app") {}
                            if (failure == "duplicate-id") HydrationRoot(Unit, { "state" }, hydrationId = "app") {}
                        }
                    }
                }
                if (failure == "validation" || failure == "duplicate-id") {
                    assertFailsWith<IllegalArgumentException> { render(stream, key, true, content = content) }
                } else {
                    assertSame(cause, assertFailsWith<IllegalStateException> {
                        if (failure == "sink") {
                            renderHydratedDocumentToStream({ throw cause }, chunkSize = 1, key = key, content = content)
                        } else render(stream, key, true, content = content)
                    })
                }
                val replacement = pool.borrow()
                assertNotSame(renderer, replacement)
                pool.recycle(replacement)
                assertEquals(
                    "<!doctype html><html><body>recovered</body></html>",
                    render(stream, key, true) { Html { Body { Text("recovered") } } },
                )
            }
        }
    }

    @Test
    fun sameKeyNestedDocumentsKeepHydrationIdsAndOutputIndependent() {
        val key = "nested-document"
        var nested = ""
        val outer = renderHydratedDocument(key = key, validateStrictly = true) {
            Html {
                Body {
                    HydrationRoot("outer", { it }, hydrationId = "app") {
                        nested = render(true, key, true) {
                            Html { Body { HydrationRoot("inner", { it }, hydrationId = "app") { Text(it) } } }
                        }
                        assertFailsWith<IllegalArgumentException> {
                            renderHydratedDocument(key = key) { Div() }
                        }
                        Text(it)
                    }
                    HydrationRoot("after", { it }, hydrationId = "after") { Text(it) }
                }
            }
        }
        assertContains(outer, ">outer</div>")
        assertFalse("inner" in outer)
        assertContains(nested, ">inner</div>")
        assertFalse("outer" in nested)
    }

    @Test
    fun sameKeyNestedRenderFromTheSinkKeepsTheOuterChunksIntact() {
        val key = "document-nested-sink"
        val chunks = mutableListOf<String>()
        renderHydratedDocumentToStream(
            sink = { chunk ->
                assertEquals(
                    "<!doctype html><html><body>inner</body></html>",
                    renderHydratedDocument(key = key) { Html { Body { Text("inner") } } },
                )
                chunks.add(chunk)
            },
            chunkSize = 1,
            key = key,
        ) { Html { Body { Text("outer") } } }
        assertEquals("<!doctype html><html><body>outer</body></html>", chunks.joinToString(""))
    }

    @Test
    fun invalidStreamSizesDoNotBorrowStorageComposeOrEmit() {
        val key = "document-invalid-size"
        val pool = assertNotNull(getHtmlRendererPool(key))
        val renderer = pool.borrow()
        pool.recycle(renderer)
        var composed = false
        var emitted = false
        for (size in listOf(0, -1)) {
            assertFailsWith<IllegalArgumentException> {
                renderHydratedDocumentToStream({ emitted = true }, chunkSize = size, key = key) {
                    composed = true
                    Html()
                }
            }
        }
        assertFalse(composed)
        assertFalse(emitted)
        assertSame(renderer, pool.borrow())
    }

    private fun render(
        stream: Boolean,
        key: String,
        strict: Boolean,
        content: @Composable () -> Unit,
    ): String = if (stream) buildString {
        renderHydratedDocumentToStream({ append(it) }, chunkSize = 1, validateStrictly = strict, key = key, content = content)
    } else renderHydratedDocument(validateStrictly = strict, key = key, content = content)
}
