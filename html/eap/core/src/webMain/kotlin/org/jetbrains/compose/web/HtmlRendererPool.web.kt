/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web

// JS and Wasm/JS execute synchronously on one thread per runtime, including in workers.
// No synchronization needed.
internal actual class HtmlRendererPools actual constructor(
    private val maxKeys: Int,
    private val maxIdleRenderers: Int,
) {
    private val pools = mutableMapOf<String, HtmlRendererPool>()

    actual operator fun get(key: String): HtmlRendererPool? = pools[key] ?: run {
        if (pools.size >= maxKeys) null
        else HtmlRendererPool(maxIdleRenderers).also { pools[key] = it }
    }
}

internal actual class HtmlRendererPool actual constructor(private val maxIdleRenderers: Int) {
    private val available = ArrayDeque<HtmlRenderer>()

    actual fun borrow(): HtmlRenderer = available.removeFirstOrNull() ?: HtmlRenderer()

    actual fun recycle(renderer: HtmlRenderer) {
        if (renderer.bufferCapacity > MAX_RETAINED_BUFFER_CAPACITY) return
        if (available.size < maxIdleRenderers) available.addLast(renderer)
    }
}
