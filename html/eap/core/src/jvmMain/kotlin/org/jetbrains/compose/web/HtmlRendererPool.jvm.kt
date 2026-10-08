/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicReferenceArray

internal actual class HtmlRendererPools actual constructor(
    private val maxKeys: Int,
    private val maxIdleRenderers: Int,
) {
    private val pools = ConcurrentHashMap<String, HtmlRendererPool>()
    private val keyCount = AtomicInteger()

    actual operator fun get(key: String): HtmlRendererPool? = pools[key] ?: register(key)

    private fun register(key: String): HtmlRendererPool? {
        // Avoid counter writes when capacity is already reserved; a racing insert may have published this key.
        if (keyCount.get() >= maxKeys) return pools[key]
        // Reserve capacity before insertion: ConcurrentHashMap.size cannot enforce a concurrent cap.
        if (keyCount.incrementAndGet() > maxKeys) {
            keyCount.decrementAndGet()
            return pools[key]
        }
        val pool = HtmlRendererPool(maxIdleRenderers)
        val existing = pools.putIfAbsent(key, pool)
        if (existing != null) keyCount.decrementAndGet()
        return existing ?: pool
    }
}

internal actual class HtmlRendererPool actual constructor(private val maxIdleRenderers: Int) {
    // The first slot keeps the usual single-renderer path short. Overflow slots bound total
    // retention without queue nodes or an idle counter. All publication transfers ownership.
    private val first = AtomicReference<HtmlRenderer>()
    private val overflow = AtomicReferenceArray<HtmlRenderer>((maxIdleRenderers - 1).coerceAtLeast(0))

    actual fun borrow(): HtmlRenderer = first.getAndSet(null) ?: borrowOverflow()

    private fun borrowOverflow(): HtmlRenderer {
        for (index in 0 until overflow.length()) {
            val renderer = overflow.get(index)
            if (renderer != null && overflow.compareAndSet(index, renderer, null)) return renderer
        }
        return HtmlRenderer()
    }

    actual fun recycle(renderer: HtmlRenderer) {
        if (maxIdleRenderers <= 0 || renderer.bufferCapacity > MAX_RETAINED_BUFFER_CAPACITY) return
        if (!first.compareAndSet(null, renderer)) recycleOverflow(renderer)
    }

    private fun recycleOverflow(renderer: HtmlRenderer) {
        for (index in 0 until overflow.length()) {
            if (overflow.compareAndSet(index, null, renderer)) return
        }
    }
}
