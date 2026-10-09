/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web

import androidx.compose.runtime.Composable

/**
 * Composes [content] once and sends HTML chunks synchronously to [sink]. Concatenating the chunks
 * produces the same HTML as [composeHtmlToString] with the same arguments.
 *
 * [chunkSize] defaults to 2048 and is a target minimum measured in UTF-16 code units. Tags and individual `Text`
 * calls stay intact, as do elements whose contents require validation. A chunk can exceed [chunkSize],
 * and no empty chunks are sent.
 *
 * Calls are thread-safe. Synchronize shared mutable data accessed by [content].
 *
 * [sink] runs synchronously while rendering and may block. Output already sent cannot be retracted
 * if rendering or the sink throws. Failed rendering storage is discarded.
 *
 * @param sink receives each non-empty HTML chunk.
 * @param chunkSize target minimum chunk size in UTF-16 code units. Defaults to 2048.
 * @param hydratable whether to emit text-boundary markers needed to hydrate adjacent `Text` calls.
 * Set to `false` when the output will not be hydrated.
 * @param key optional key for reusing rendering storage across calls.
 * Use a stable key per template. Reuse is best-effort. `null` disables pooling.
 * @param validateStrictly overrides the default strict-validation setting for this render.
 * @param content the composable content to render once.
 * @throws IllegalArgumentException if [chunkSize] is not positive or [content] cannot be safely serialized as HTML.
 * @throws IllegalStateException if [content] accesses `currentRecomposeScope`.
 * @throws UnsupportedOperationException if [content] uses an unsupported composition operation.
 */
fun composeHtmlToStream(
    sink: (String) -> Unit,
    chunkSize: Int = 2048,
    hydratable: Boolean = true,
    key: String? = null,
    validateStrictly: Boolean = defaultHtmlValidationMode() == HtmlValidationMode.Strict,
    content: @Composable () -> Unit,
) {
    val chunkSink: (StringBuilder) -> Unit = { sink(it.toString()) }
    composeHtmlString(
        hydratable = hydratable, key = key, chunkSink = chunkSink, chunkSize = chunkSize,
        content = htmlValidatedContent(validateStrictly, content),
    )
}
