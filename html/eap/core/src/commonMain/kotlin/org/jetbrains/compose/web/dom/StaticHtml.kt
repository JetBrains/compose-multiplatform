/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi::class)

package org.jetbrains.compose.web.dom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import org.jetbrains.compose.web.HtmlValidationMode
import org.jetbrains.compose.web.LocalHtmlValidationMode
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi

// Runtime support for generated code and tests.

/**
 * Only called by compiler plugin.
 * Writes compiler-validated HTML directly to the current output buffer.
 * Inlining puts the StringBuilder append in the caller.
 */
@ComposeWebInternalApi
@Composable
@NonRestartableComposable
@androidx.compose.runtime.ExplicitGroupsComposable
@Suppress("NOTHING_TO_INLINE") // Put the append in generated callers, without a callback allocation.
inline fun WriteStaticHtml(rootTag: String, encoded: String) {
    val writer = currentHtmlStringWriter()
    val output =
        writer.beginStaticHtml(rootTag, encoded, LocalHtmlValidationMode.current == HtmlValidationMode.Strict) ?: return
    output.append(encoded)
    writer.flushChunk()
}

/** Used by compiler-generated calls and inline [WriteStaticHtml]. */
@PublishedApi
@Composable
@NonRestartableComposable
@androidx.compose.runtime.ExplicitGroupsComposable
internal fun currentHtmlStringWriter(): HtmlStringWriterContext =
    LocalComposeHtmlContext.current as? HtmlStringWriterContext
        ?: error("Direct HTML writes require HtmlStringWriterContext; this rendering context does not support them")

/** Only called by compiler plugin. */
@PublishedApi
@Composable
@NonRestartableComposable
@androidx.compose.runtime.ExplicitGroupsComposable
@Suppress("NOTHING_TO_INLINE")
internal inline fun currentHtmlValidationIsStrict(): Boolean =
    LocalHtmlValidationMode.current == HtmlValidationMode.Strict
