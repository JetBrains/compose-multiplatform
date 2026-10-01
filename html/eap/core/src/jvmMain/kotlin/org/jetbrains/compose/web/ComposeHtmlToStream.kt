/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:JvmName("ComposeHtmlToWriterKt")

package org.jetbrains.compose.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import java.io.Writer

/**
 * Composes [content] once and writes HTML synchronously to [writer], without creating a String
 * for each chunk. The writer is not flushed or closed. The writer must consume each write before returning.
 *
 * [chunkSize] is a target minimum in UTF-16 code units. Tags and individual `Text` calls stay
 * intact. Elements requiring content validation are emitted only after validation succeeds.
 * A write can exceed [chunkSize]. No empty writes are made.
 *
 * Composition reuse, effects, validation, and failure handling follow [composeHtmlToStream]
 * with a String sink. Keyed renders are not thread-safe. Output already written cannot be
 * retracted on failure, and a failed keyed composition is disposed.
 */
fun composeHtmlToStream(
    writer: Writer,
    chunkSize: Int = 2048,
    hydratable: Boolean = true,
    key: String? = null,
    validateStrictly: Boolean = defaultHtmlValidationMode() == HtmlValidationMode.Strict,
    content: @Composable () -> Unit,
) {
    require(chunkSize > 0) { "chunkSize must be positive" }
    val chunkSink = htmlWriterSink(writer)
    val validatedContent: @Composable () -> Unit = {
        CompositionLocalProvider(LocalHtmlValidationMode provides htmlValidationMode(validateStrictly)) {
            content()
        }
    }
    if (key == null) {
        composeHtmlString(hydratable = hydratable, chunkSize = chunkSize, chunkSink = chunkSink, content = validatedContent)
    } else {
        composeReusableHtmlString(key, hydratable, chunkSize = chunkSize, chunkSink = chunkSink, content = validatedContent)
    }
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
