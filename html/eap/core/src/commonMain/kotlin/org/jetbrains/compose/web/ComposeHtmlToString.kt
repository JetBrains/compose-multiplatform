/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ControlledComposition
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.ReusableComposition
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.Dispatchers
import org.jetbrains.compose.web.dom.AttrsBuilderPool
import org.jetbrains.compose.web.dom.HtmlStringWriterContext
import org.jetbrains.compose.web.dom.LocalComposeHtmlContext

/**
 * Composes [content] once into an HTML string without creating browser DOM nodes.
 * With no [key], the backing composition is disposed after the HTML has been written.
 * With a key, the composition and attribute-builder storage are retained for later calls with
 * that key. Keyed calls are not thread-safe, even with different keys.
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
 * @param key when non-null, reuses a composition across calls.
 * @param validateStrictly overrides the default strict-validation setting for this render.
 * @throws IllegalArgumentException if a raw-text element contains unsafe text or element children,
 * serialized `noscript` contents contain a `</noscript>` end tag, or ordinary text, RCDATA,
 * or attribute values contain NUL (U+0000), which HTML parsing cannot preserve.
 * @throws IllegalStateException if another render with the same non-null [key] is in progress.
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
    return if (key == null) {
        composeHtmlString(hydratable = hydratable, content = validatedContent)
    } else {
        composeReusableHtmlString(key, hydratable, content = validatedContent)
    }
}

private val reusableHtmlCompositions = mutableMapOf<String, ReusableHtmlComposition>()

private class ReusableHtmlComposition {
    val recomposer = cancelledRecomposer()
    val composition = ReusableComposition(UnitApplier(), recomposer)
    val attrsBuilders = AttrsBuilderPool()
    val output = StringBuilder()
    var content: (@Composable () -> Unit)? = null
    var rendering = false

    fun dispose() {
        composition.dispose()
        recomposer.close()
    }
}

/** Reuses a keyed composition and its attribute builders across string and streaming renders. */
internal fun composeReusableHtmlString(
    key: String,
    hydratable: Boolean,
    sink: ((String) -> Unit)? = null,
    chunkSize: Int = 4096,
    content: @Composable () -> Unit,
): String {
    val renderer = reusableHtmlCompositions.getOrPut(key, ::ReusableHtmlComposition)
    check(!renderer.rendering) { "HTML render key \"$key\" is already rendering" }

    renderer.output.setLength(0) // Discard output left by the previous render.
    val context = HtmlStringWriterContext(hydratable, renderer.attrsBuilders, renderer.output, sink, chunkSize)
    val snapshot = Snapshot.takeMutableSnapshot()
    renderer.rendering = true
    renderer.content = content
    try {
        return snapshot.enter {
            try {
                renderer.composition.setContentWithReuse {
                    CompositionLocalProvider(LocalComposeHtmlContext provides context) {
                        checkNotNull(renderer.content).invoke()
                    }
                }
                context.finish(requireHtmlDocumentRoot = false)
            } finally {
                renderer.composition.deactivate()
            }
        }
    } catch (failure: Throwable) {
        reusableHtmlCompositions.remove(key)
        renderer.dispose()
        throw failure
    } finally {
        renderer.content = null
        renderer.rendering = false
        snapshot.dispose()
    }
}

/** Runs a fresh composition with a buffer shared by string and streaming output. */
internal fun composeHtmlString(
    hydratable: Boolean = true,
    requireHtmlDocumentRoot: Boolean = false,
    sink: ((String) -> Unit)? = null,
    chunkSize: Int = 4096,
    content: @Composable () -> Unit,
): String {
    val context = HtmlStringWriterContext(hydratable, sink = sink, chunkSize = chunkSize)
    val snapshot = Snapshot.takeMutableSnapshot()

    return try {
        snapshot.enter {
            val recomposer = cancelledRecomposer()
            val composition = ControlledComposition(applier = UnitApplier(), parent = recomposer)

            try {
                composition.setContent {
                    CompositionLocalProvider(LocalComposeHtmlContext provides context) {
                        content()
                    }
                }
                context.finish(requireHtmlDocumentRoot)
            } finally {
                composition.dispose()
                recomposer.close()
            }
        }
    } finally {
        snapshot.dispose()
    }
}

/** Composes the initial content without starting coroutine effects. */
private fun cancelledRecomposer() = Recomposer(Dispatchers.Default).apply { cancel() }

/** [HtmlStringWriterContext] emits no Compose nodes, but Composition still requires an applier. */
private class UnitApplier : AbstractApplier<Unit>(Unit) {
    override fun insertTopDown(index: Int, instance: Unit) = Unit
    override fun insertBottomUp(index: Int, instance: Unit) = Unit
    override fun remove(index: Int, count: Int) = Unit
    override fun move(from: Int, to: Int, count: Int) = Unit
    override fun onClear() = Unit
}
