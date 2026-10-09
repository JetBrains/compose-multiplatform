/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web

// Buffer limits use UTF-16 code units. JVM capacity measures allocated buffer space;
// JS and Wasm/JS report text length, so the limit cannot bound retained buffer memory there.
internal const val LARGE_BUFFER_THRESHOLD = 64 * 1024
internal const val BUFFER_SHRINK_FACTOR = 4
internal const val MAX_RETAINED_BUFFER_CAPACITY = 2 * 1024 * 1024

/**
 * Stores a limited number of keys and does not remove them.
 * Once full, new keys render without pooling.
 */
internal expect class HtmlRendererPools(maxKeys: Int, maxIdleRenderers: Int) {
    operator fun get(key: String): HtmlRendererPool?
}

/**
 * Each active render owns its storage. Only successful renders return it to the pool.
 * Keeps at most [maxIdleRenderers] idle renderers; oversized JVM buffers are discarded.
 */
internal expect class HtmlRendererPool(maxIdleRenderers: Int) {
    fun borrow(): HtmlRenderer
    fun recycle(renderer: HtmlRenderer)
}
