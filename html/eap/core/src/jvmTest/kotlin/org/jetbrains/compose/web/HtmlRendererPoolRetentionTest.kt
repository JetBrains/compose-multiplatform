/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web

import java.io.StringWriter
import kotlinx.browser.dom.Element
import org.jetbrains.compose.web.dom.TagElement
import org.jetbrains.compose.web.dom.Text
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class HtmlRendererPoolRetentionTest {
    @Test
    fun largeStringBelowTwoMiCeilingReusesItsRenderer() {
        val key = "retention-large-reusable-string"
        val renderer = seedRenderer(key)
        val text = "α".repeat(1_747_011)
        assertEquals(text, composeHtmlToString(hydratable = false, key = key) { Text(text) })
        assertSame(renderer, assertNotNull(getHtmlRendererPool(key)).borrow())
    }

    @Test
    fun capacityAtTheCeilingIsRetainedAndOneUnitOverItIsDiscarded() {
        val highWater = "α".repeat(MAX_RETAINED_BUFFER_CAPACITY * 8)
        for (extra in 0..1) {
            val pool = HtmlRendererPool(maxIdleRenderers = 1)
            val renderer = pool.borrow()
            render(renderer, highWater)
            // A smaller render trims to its exact length, allowing an exact capacity boundary test.
            val text = "α".repeat(MAX_RETAINED_BUFFER_CAPACITY + extra)
            assertEquals(text, render(renderer, text))
            assertEquals(text.length, renderer.bufferCapacity)
            pool.recycle(renderer)
            val next = pool.borrow()
            if (extra == 0) assertSame(renderer, next) else assertNotSame(renderer, next)

            // Rejection must not consume the only idle slot.
            assertEquals("small", render(next, "small"))
            pool.recycle(next)
            assertSame(next, pool.borrow())
        }
    }

    @Test
    fun oversizedStringOutputIsCompleteAndItsRendererIsDiscarded() {
        val key = "retention-large-string"
        val renderer = seedRenderer(key)
        val text = "α".repeat(MAX_RETAINED_BUFFER_CAPACITY + 1)
        assertEquals(text, composeHtmlToString(hydratable = false, key = key) { Text(text) })
        assertNotSame(renderer, assertNotNull(getHtmlRendererPool(key)).borrow())
        assertEquals("next", composeHtmlToString(key = key) { Text("next") })
    }

    @Test
    fun largeStreamingOutputWithSmallChunksStillReusesItsRenderer() {
        val key = "retention-small-chunks"
        val renderer = seedRenderer(key)
        val chunk = "α".repeat(1024)
        val chunks = mutableListOf<String>()
        val count = MAX_RETAINED_BUFFER_CAPACITY / chunk.length + 1
        composeHtmlToStream(chunks::add, chunkSize = chunk.length, hydratable = false, key = key) {
            repeat(count) { Text(chunk) }
        }
        assertEquals(List(count) { chunk }, chunks)
        assertSame(renderer, assertNotNull(getHtmlRendererPool(key)).borrow())
    }

    @Test
    fun oversizedValidatedWriterChunkIsCompleteAndItsRendererIsDiscarded() {
        val key = "retention-large-writer-chunk"
        val renderer = seedRenderer(key)
        val text = "α".repeat(MAX_RETAINED_BUFFER_CAPACITY + 1)
        val writer = StringWriter()
        composeHtmlToStream(writer, chunkSize = 1024, key = key) {
            TagElement<Element>("script", null) { Text(text) }
        }
        assertEquals("<script>$text</script>", writer.toString())
        assertNotSame(renderer, assertNotNull(getHtmlRendererPool(key)).borrow())
    }

    private fun seedRenderer(key: String): HtmlRenderer {
        val pool = assertNotNull(getHtmlRendererPool(key))
        return pool.borrow().also(pool::recycle)
    }

    private fun render(renderer: HtmlRenderer, text: String): String =
        renderer.render(false, false, null, 2048, "") { Text(text) }
}
