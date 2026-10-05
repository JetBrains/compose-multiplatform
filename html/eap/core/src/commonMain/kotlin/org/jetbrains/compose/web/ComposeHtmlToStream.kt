/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Composes [content] once and sends HTML chunks synchronously to [sink]. Concatenating the chunks
 * produces the same HTML as [composeHtmlToString] with the same arguments.
 *
 * [chunkSize] defaults to 2048 and is a target minimum measured in UTF-16 code units. Tags and individual `Text`
 * calls stay intact, as do elements whose contents require validation. Leading-newline repair
 * for `pre`, `textarea`, and `listing` happens before their first content is emitted.
 * A chunk can exceed [chunkSize], and no empty chunks are sent.
 *
 * With no [key], the composition is disposed after rendering. A key retains the composition and
 * attribute-builder storage for later calls to either rendering function. Keyed calls are not
 * thread-safe, even with different keys. Snapshot state changes are discarded; coroutine effects
 * such as `LaunchedEffect` do not run, while `SideEffect` and `DisposableEffect` still execute.
 *
 * [sink] runs synchronously while rendering and may block. Output already sent cannot be retracted
 * if rendering or the sink throws. A failed keyed render is disposed before the exception is rethrown.
 *
 * @param hydratable whether to emit text-boundary markers needed to hydrate adjacent `Text` calls.
 * @param validateStrictly overrides the default strict-validation setting for this render.
 * @throws IllegalArgumentException if [chunkSize] is not positive or content cannot be serialized.
 * @throws IllegalStateException if another render with the same non-null [key] is in progress.
 */
fun composeHtmlToStream(
    sink: (String) -> Unit,
    chunkSize: Int = 2048,
    hydratable: Boolean = true,
    key: String? = null,
    validateStrictly: Boolean = defaultHtmlValidationMode() == HtmlValidationMode.Strict,
    content: @Composable () -> Unit,
) {
    require(chunkSize > 0) { "chunkSize must be positive" }
    val chunkSink: (StringBuilder) -> Unit = { sink(it.toString()) }
    val validatedContent: @Composable () -> Unit = {
        CompositionLocalProvider(LocalHtmlValidationMode provides htmlValidationMode(validateStrictly)) {
            content()
        }
    }
    if (key == null) {
        composeHtmlString(hydratable = hydratable, content = validatedContent, chunkSink = chunkSink, chunkSize = chunkSize)
    } else {
        composeReusableHtmlString(key, hydratable, content = validatedContent, chunkSink = chunkSink, chunkSize = chunkSize)
    }
}
