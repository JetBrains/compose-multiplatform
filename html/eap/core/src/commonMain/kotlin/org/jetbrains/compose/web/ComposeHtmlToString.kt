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
 * With no [key], rendering storage is discarded after the HTML has been written.
 * With a key, storage is retained for later string and streaming calls.
 * Keyed calls are not thread-safe, even with different keys.
 * Snapshot state changes made while rendering are discarded afterwards.
 * Coroutine effects such as `LaunchedEffect` do not run. `SideEffect` and
 * `DisposableEffect` still execute.
 *
 * Known limitations:
 * - DOM property updates registered with `AttrsScope.prop(...)` are ignored because
 *   string rendering has no underlying DOM element.
 * - Inline styles preserve CSS fallbacks, but do not fully emulate CSSOM validation and
 *   mutation (for example, invalid assignments that also change priority, or shorthand removal).
 *
 * On the JVM or Node, set `COMPOSE_HTML_VALIDATE_STRICTLY=true` to enable additional class and
 * duplicate foreign-attribute checks by default.
 *
 * @param hydratable whether to emit text-boundary markers required to hydrate adjacent `Text`
 * nodes. Set to `false` when the output will not be hydrated.
 * @param key when non-null, reuses rendering storage across calls.
 * @param validateStrictly overrides the default strict-validation setting for this render.
 * @throws IllegalArgumentException if a raw-text element contains unsafe text or element children,
 * serialized `noscript` contents contain a `</noscript>` end tag, or ordinary text, RCDATA,
 * or attribute values contain NUL (U+0000), which HTML parsing cannot preserve.
 * @throws IllegalStateException if rendering reenters with the same non-null [key], or [content]
 * accesses `currentRecomposeScope`.
 * @throws UnsupportedOperationException if [content] uses an unsupported composition operation.
 */
fun composeHtmlToString(
    hydratable: Boolean = true,
    key: String? = null,
    validateStrictly: Boolean = defaultHtmlValidationMode() == HtmlValidationMode.Strict,
    content: @Composable () -> Unit,
): String {
    val validatedContent: @Composable () -> Unit = {
        CompositionLocalProvider(LocalHtmlValidationMode provides htmlValidationMode(validateStrictly)) {
            content()
        }
    }
    return composeHtmlString(hydratable = hydratable, key = key, content = validatedContent)
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
    val renderer = if (key == null) HtmlRenderer(reusable = false) else renderers.getOrPut(key) { HtmlRenderer() }
    return renderer.render(hydratable, requireHtmlDocumentRoot, chunkSink, chunkSize, prefix, content)
}

private const val LARGE_BUFFER_THRESHOLD = 64 * 1024
private const val BUFFER_SHRINK_FACTOR = 4
private val renderers = mutableMapOf<String, HtmlRenderer>()

/** Owns reusable rendering storage for a key, or for a single unkeyed render. */
internal class HtmlRenderer(private val reusable: Boolean = true) {
    private var attrsBuilders = AttrsBuilderPool()
    private var output = StringBuilder()
    private var composer = SinglePassComposer()
    private var rendering = false

    internal val bufferCapacity: Int get() = output.capacity()

    fun render(
        hydratable: Boolean,
        requireHtmlDocumentRoot: Boolean,
        chunkSink: ((StringBuilder) -> Unit)?,
        chunkSize: Int,
        prefix: String,
        content: @Composable () -> Unit,
    ): String {
        check(!rendering) { "Reentrant HTML rendering with the same key is not supported" }
        val snapshot = Snapshot.takeMutableSnapshot()
        rendering = true
        try {
            output.setLength(0)
            output.append(prefix)
            composer.reset()
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
                            output.trimToSize()
                        }
                    }
                } catch (cause: Throwable) {
                    failure = cause
                    throw cause
                } finally {
                    composer.dispose(failure)
                }
            }
        } catch (failure: Throwable) {
            // Only retained renderers need replacement storage after a failed request.
            if (reusable) {
                attrsBuilders = AttrsBuilderPool()
                output = StringBuilder()
                composer = SinglePassComposer()
            }
            throw failure
        } finally {
            rendering = false
            snapshot.dispose()
        }
    }
}

private fun isOversized(capacity: Int, used: Int): Boolean =
    capacity > LARGE_BUFFER_THRESHOLD && used.toLong() * BUFFER_SHRINK_FACTOR < capacity
