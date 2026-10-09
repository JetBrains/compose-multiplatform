/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(androidx.compose.runtime.InternalComposeApi::class)

package org.jetbrains.compose.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composer
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.snapshots.Snapshot
import org.jetbrains.compose.web.dom.AttrsBuilderPool
import org.jetbrains.compose.web.dom.HtmlStringWriterContext
import org.jetbrains.compose.web.dom.LocalComposeHtmlContext

/**
 * Composes [content] once into an HTML string without creating browser DOM nodes.
 *
 * Snapshot state changes are discarded after rendering. Coroutine effects such as
 * `LaunchedEffect` do not run. `SideEffect` and `DisposableEffect` still execute.
 *
 * Calls are thread-safe. Synchronize shared mutable data accessed by [content].
 * See [composeHtmlToStream] for validation and failure handling.
 *
 * Inline styles preserve CSS fallbacks but do not fully emulate CSSOM validation and mutation.
 * Custom callbacks passed to `AttrsScope.prop(...)` are ignored during string rendering.
 *
 * On the JVM or Node.js, set `COMPOSE_HTML_VALIDATE_STRICTLY=true` to enable additional class and
 * duplicate foreign-attribute checks.
 *
 * @param hydratable whether to emit text-boundary markers needed to hydrate adjacent `Text`
 * nodes. Set to `false` when the output will not be hydrated.
 * @param key optional key for reusing rendering storage across calls.
 * Use a stable key per template. Reuse is best-effort. `null` disables pooling.
 * @param validateStrictly overrides the default strict-validation setting for this render.
 * @throws IllegalArgumentException if [content] cannot be safely serialized as HTML.
 * @throws IllegalStateException if [content] accesses `currentRecomposeScope`.
 * @throws UnsupportedOperationException if [content] uses an unsupported composition operation.
 */
fun composeHtmlToString(
    hydratable: Boolean = true,
    key: String? = null,
    validateStrictly: Boolean = defaultHtmlValidationMode() == HtmlValidationMode.Strict,
    content: @Composable () -> Unit,
): String {
    return composeHtmlString(hydratable = hydratable, key = key, content = htmlValidatedContent(validateStrictly, content))
}

/** Shared single-pass composition and writer for fragments, documents, and streaming output. */
internal fun composeHtmlString(
    hydratable: Boolean = true,
    requireHtmlDocumentRoot: Boolean = false,
    chunkSink: ((StringBuilder) -> Unit)? = null,
    chunkSize: Int = 2048,
    prefix: String = "",
    key: String? = null,
    content: @Composable () -> Unit,
): String {
    if (chunkSink != null) require(chunkSize > 0) { "chunkSize must be positive" }

    val pool = getHtmlRendererPool(key)
    val renderer = pool?.borrow() ?: HtmlRenderer()

    // Return storage to the pool only after rendering, effect cleanup and snapshot disposal succeed.
    val result = renderer.render(hydratable, requireHtmlDocumentRoot, chunkSink, chunkSize, prefix, content)
    pool?.recycle(renderer)
    return result
}

private val renderers = HtmlRendererPools(maxKeys = 64, maxIdleRenderers = 8)

internal fun getHtmlRendererPool(key: String?): HtmlRendererPool? = if (key == null) null else renderers[key]

/** Storage exclusively owned by one active render, then optionally returned to a pool. */
internal class HtmlRenderer {
    private val attrsBuilders = AttrsBuilderPool()
    private var output = StringBuilder()
    private val composer = SinglePassComposer()

    internal val bufferCapacity: Int get() = output.capacity()
    internal val bufferLength: Int get() = output.length

    fun render(
        hydratable: Boolean,
        requireHtmlDocumentRoot: Boolean,
        chunkSink: ((StringBuilder) -> Unit)?,
        chunkSize: Int,
        prefix: String,
        content: @Composable () -> Unit,
    ): String {
        val snapshot = Snapshot.takeMutableSnapshot()
        try {
            output.append(prefix)
            val context = HtmlStringWriterContext(hydratable, attrsBuilders, output, chunkSink, chunkSize)
            return snapshot.enter {
                var failure: Throwable? = null
                try {
                    val wrapped: @Composable () -> Unit = {
                        CompositionLocalProvider(LocalComposeHtmlContext provides context) { content() }
                    }
                    @Suppress("UNCHECKED_CAST")
                    (wrapped as (Composer, Int) -> Unit)(composer, 1)
                    composer.applyEffects()
                    context.finish(requireHtmlDocumentRoot).also {
                        if (isOversized(output.capacity(), context.peakBufferSize)) {
                            if (chunkSink == null) output.trimToSize()
                            else output = StringBuilder(context.peakBufferSize)
                        }
                    }
                } catch (cause: Throwable) {
                    failure = cause
                    throw cause
                } finally {
                    composer.dispose(failure)
                }
            }
        } finally {
            try {
                snapshot.dispose()
            } finally {
                // Clear the output before reuse. The underlying buffer is not erased.
                output.setLength(0)
            }
        }
    }
}

internal fun htmlValidatedContent(
    validateStrictly: Boolean,
    content: @Composable () -> Unit,
): @Composable () -> Unit = {
    CompositionLocalProvider(LocalHtmlValidationMode provides htmlValidationMode(validateStrictly)) {
        content()
    }
}

private fun isOversized(capacity: Int, used: Int): Boolean =
    capacity > LARGE_BUFFER_THRESHOLD && used.toLong() * BUFFER_SHRINK_FACTOR < capacity
