/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:JvmName("ComposeHtmlToWriterKt")

package org.jetbrains.compose.web

import androidx.compose.runtime.Composable
import java.io.Writer

/**
 * Composes [content] once and writes HTML synchronously to [writer], without creating a String
 * for each chunk. The writer is not flushed or closed. The writer must consume each write before returning.
 *
 * [chunkSize] is a target minimum in UTF-16 code units. Tags and individual `Text` calls stay
 * intact. Elements requiring content validation are emitted only after validation succeeds.
 * A write can exceed [chunkSize]. No empty writes are made.
 *
 * Calls are thread-safe. Callers must Synchronize shared mutable data accessed by [content].
 * See [composeHtmlToStream] for validation and failure handling.
 *
 * Output already written cannot be retracted on failure, and failed rendering storage is discarded.
 *
 * @param writer receives the HTML chunks. It is not flushed or closed.
 * @param chunkSize target minimum chunk size in UTF-16 code units. Defaults to 2048.
 * @param hydratable whether to emit text-boundary markers needed to hydrate adjacent `Text` calls.
 * Set to `false` when the output will not be hydrated.
 * @param key optional key for reusing rendering storage across calls.
 * Use a stable key per template. Reuse is best-effort. `null` disables pooling.
 * @param validateStrictly overrides the default strict-validation setting for this render.
 * @param content the composable content to render once.
 */
fun composeHtmlToStream(
    writer: Writer,
    chunkSize: Int = 2048,
    hydratable: Boolean = true,
    key: String? = null,
    validateStrictly: Boolean = defaultHtmlValidationMode() == HtmlValidationMode.Strict,
    content: @Composable () -> Unit,
) {
    val chunkSink = htmlWriterSink(writer)
    composeHtmlString(
        hydratable = hydratable, key = key, chunkSink = chunkSink, chunkSize = chunkSize,
        content = htmlValidatedContent(validateStrictly, content),
    )
}

/** Creates a Writer sink that reuses character storage across chunks within one render. */
internal fun htmlWriterSink(writer: Writer): (StringBuilder) -> Unit {
    // Grow on demand so an unusually large target does not allocate before any HTML is written.
    var characters = CharArray(0)
    return { buffer ->
        if (characters.size < buffer.length) characters = CharArray(buffer.length)
        buffer.getChars(0, buffer.length, characters, 0)
        writer.write(characters, 0, buffer.length)
    }
}
