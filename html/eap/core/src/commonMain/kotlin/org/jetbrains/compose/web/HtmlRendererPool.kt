/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web

// Buffer policy uses the platform's reported capacity in UTF-16 code units.
// JVM reports backing capacity. JS and Wasm/JS report logical length, so after clearing
// the output their capacity is zero and the retention ceiling does not constrain backing storage.
internal const val LARGE_BUFFER_THRESHOLD = 64 * 1024
internal const val BUFFER_SHRINK_FACTOR = 4
internal const val MAX_RETAINED_BUFFER_CAPACITY = 2 * 1024 * 1024

/** Bounded registry without eviction. Empty pools retain their key slots; unregistered keys render uncached. */
internal expect class HtmlRendererPools(maxKeys: Int, maxIdleRenderers: Int) {
    operator fun get(key: String): HtmlRendererPool?
}

/**
 * Borrowers own their storage exclusively and recycle it only after successful rendering.
 * Output buffers with reported capacity above [MAX_RETAINED_BUFFER_CAPACITY] are not retained.
 * On JS and Wasm/JS, capacity is logical length, which is zero when recycling a cleared renderer.
 */
internal expect class HtmlRendererPool(maxIdleRenderers: Int) {
    fun borrow(): HtmlRenderer
    fun recycle(renderer: HtmlRenderer)
}
