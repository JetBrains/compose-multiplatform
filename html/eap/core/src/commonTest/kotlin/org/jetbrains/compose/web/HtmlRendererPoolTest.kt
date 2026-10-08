/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web

import androidx.compose.runtime.DisposableEffect
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class HtmlRendererPoolTest {
    @Test
    fun invalidChunkSizesDoNotBorrowStorageComposeOrEmit() {
        val key = "invalid-chunk-size"
        val pool = assertNotNull(getHtmlRendererPool(key))
        val renderer = pool.borrow()
        pool.recycle(renderer)
        val chunks = mutableListOf<String>()
        var composed = false

        for (size in listOf(0, -1)) {
            val failure = assertFailsWith<IllegalArgumentException> {
                composeHtmlToStream(chunks::add, chunkSize = size, key = key) {
                    composed = true
                    Text("unused")
                }
            }
            assertEquals("chunkSize must be positive", failure.message)
        }
        assertFalse(composed)
        assertTrue(chunks.isEmpty())
        assertSame(renderer, pool.borrow())
    }

    @Test
    fun attributesDoNotLeakIntoChildrenSiblingsOrLaterRequests() {
        for (key in listOf(null, "attribute-reset")) {
            assertEquals(
                "<div data-user=\"first\" class=\"parent\"><div>child</div></div><div>sibling</div>",
                composeHtmlToString(key = key, validateStrictly = true) {
                    Div(attrs = { attr("data-user", "first"); classes("parent") }) {
                        Div { Text("child") }
                    }
                    Div { Text("sibling") }
                },
            )
            assertEquals("<div>next</div>", composeHtmlToString(key = key) { Div { Text("next") } })
        }
    }

    @Test
    fun outputIsClearedAfterSuccessAndContentSinkOrDisposalFailure() {
        for (failure in listOf(null, "content", "sink", "disposal")) {
            val renderer = HtmlRenderer()
            val render = {
                renderer.render(false, false, if (failure == "sink") {
                    { throw IllegalStateException("sink") }
                } else null, 1, "prefix") {
                    DisposableEffect(Unit) {
                        onDispose { if (failure == "disposal") error("disposal") }
                    }
                    Text("request-data")
                    if (failure == "content") error("content")
                }
            }
            if (failure == null) assertEquals("prefixrequest-data", render())
            else assertFailsWith<IllegalStateException> { render() }
            assertEquals(0, renderer.bufferLength)
        }
    }

    @Test
    fun activeRendersHaveExclusiveStorageAndSuccessfulStorageCanBeReused() {
        val pool = HtmlRendererPool(maxIdleRenderers = 2)
        val first = pool.borrow()
        val second = pool.borrow()
        assertNotSame(first, second)
        pool.recycle(first)
        assertSame(first, pool.borrow())
        assertNotSame(second, pool.borrow())
    }

    @Test
    fun idleRetentionIsBoundedAndOverflowDoesNotPreventBorrowing() {
        for (capacity in listOf(0, 1, 4)) {
            val pool = HtmlRendererPool(maxIdleRenderers = capacity)
            val renderers = List(8) { pool.borrow() }
            renderers.forEach(pool::recycle)
            val borrowed = List(8) { pool.borrow() }
            assertEquals(capacity, borrowed.count { it in renderers })
            assertEquals(8, borrowed.toSet().size)
        }
    }

    @Test
    fun dynamicKeysDoNotGrowTheRegistryPastItsLimit() {
        val pools = HtmlRendererPools(maxKeys = 2, maxIdleRenderers = 1)
        val first = assertNotNull(pools["template-first"])
        val second = assertNotNull(pools["template-second"])
        repeat(1000) { assertNull(pools["request-$it"]) }
        assertSame(first, pools["template-first"])
        assertSame(second, pools["template-second"])
        assertNull(HtmlRendererPools(maxKeys = 0, maxIdleRenderers = 1)["uncached"])
    }

    @Test
    fun emptyRegisteredPoolsAreNotEvictedForLaterTemplateKeys() {
        val pools = HtmlRendererPools(maxKeys = 1, maxIdleRenderers = 1)
        val earlyRequestPool = assertNotNull(pools["early-request-id"])
        val activeRenderer = earlyRequestPool.borrow()
        assertNull(pools["real-template"])
        earlyRequestPool.recycle(activeRenderer)
        assertNull(pools["real-template"])
        assertSame(earlyRequestPool, pools["early-request-id"])
        assertSame(activeRenderer, earlyRequestPool.borrow())
    }

    @Test
    fun failedStorageIsDiscardedAfterContentSinkOrDisposalFailure() {
        val key = "discard-failed-renderer"
        val pool = assertNotNull(getHtmlRendererPool(key))
        for (failure in listOf("content", "sink", "disposal")) {
            val renderer = pool.borrow()
            pool.recycle(renderer)
            val cause = IllegalStateException(failure)
            assertSame(cause, assertFailsWith<IllegalStateException> {
                composeHtmlToStream(
                    sink = { if (failure == "sink") throw cause },
                    key = key,
                    chunkSize = 1,
                ) {
                    DisposableEffect(Unit) {
                        onDispose { if (failure == "disposal") throw cause }
                    }
                    if (failure == "content") throw cause
                    Text("failed")
                }
            })
            val replacement = pool.borrow()
            assertNotSame(renderer, replacement)
            pool.recycle(replacement)
            assertEquals("recovered", composeHtmlToString(key = key) { Text("recovered") })
        }
    }

    @Test
    fun caughtSameKeyNestedFailurePreservesTheOuterRender() {
        assertEquals("<div>before<!--c-->after</div>", composeHtmlToString(key = "nested-failure") {
            Div {
                Text("before")
                assertFailsWith<IllegalArgumentException> {
                    composeHtmlToString(key = "nested-failure") { Text("invalid\u0000text") }
                }
                Text("after")
            }
        })
        assertEquals("next", composeHtmlToString(key = "nested-failure") { Text("next") })
    }
}
