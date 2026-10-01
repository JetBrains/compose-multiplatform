/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:JvmName("HydratedDocumentToWriterKt")

package org.jetbrains.compose.web

import androidx.compose.runtime.Composable
import java.io.Writer

/**
 * Composes one complete HTML document and writes it synchronously to [writer], without creating
 * a String for each chunk. Output and document validation follow [renderHydratedDocumentToStream]
 * with a String sink, including the doctype, hydration state, and validation mode.
 *
 * [chunkSize] is a target minimum in UTF-16 code units. A write can exceed it. The writer must
 * consume each write before returning because the character array is reused. The writer is not flushed
 * or closed. Output already written cannot be retracted if rendering, validation or writing throws.
 */
fun renderHydratedDocumentToStream(
    writer: Writer,
    chunkSize: Int = 2048,
    validateStrictly: Boolean = defaultHtmlValidationMode() == HtmlValidationMode.Strict,
    content: @Composable () -> Unit,
) {
    require(chunkSize > 0) { "chunkSize must be positive" }
    composeHydratedDocument(
        validateStrictly,
        chunkSize = chunkSize,
        chunkSink = htmlWriterSink(writer),
        content = content,
    )
}
