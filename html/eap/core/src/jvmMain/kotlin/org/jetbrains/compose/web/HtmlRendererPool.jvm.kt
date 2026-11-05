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

    // Includes registrations in progress, not just pools already in the map.
    private val keyCount = AtomicInteger()

    actual operator fun get(key: String): HtmlRendererPool? = pools[key] ?: register(key)

    private fun register(key: String): HtmlRendererPool? {
        // Avoid updating the counter when all slots are already reserved.
        if (keyCount.get() >= maxKeys) return pools[key]

        // Reserve a slot before insertion to enforce the limit under concurrency.
        if (keyCount.incrementAndGet() > maxKeys) {
            keyCount.decrementAndGet()
            return pools[key]
        }

        val pool = HtmlRendererPool(maxIdleRenderers)
        val existing = pools.putIfAbsent(key, pool)

        // Another thread registered the same key, so release our reservation.
        if (existing != null) keyCount.decrementAndGet()

        return existing ?: pool
    }
}

internal actual class HtmlRendererPool actual constructor(private val maxIdleRenderers: Int) {
    // Optimize for a single idle renderer. Additional slots bound retention
    // without queue allocations or a separate idle counter.
    private val first = AtomicReference<HtmlRenderer>()
    private val overflow = AtomicReferenceArray<HtmlRenderer>((maxIdleRenderers - 1).coerceAtLeast(0))

    actual fun borrow(): HtmlRenderer = first.getAndSet(null) ?: borrowOverflow()

    private fun borrowOverflow(): HtmlRenderer {
        for (index in 0 until overflow.length()) {
            val renderer = overflow.get(index)

            // Claim the renderer only if another thread hasn't taken it.
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
        // All slots are occupied; let the renderer be garbage-collected.
    }
}
