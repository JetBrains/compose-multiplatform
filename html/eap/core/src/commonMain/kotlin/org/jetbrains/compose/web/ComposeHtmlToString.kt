/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ControlledComposition
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.ReusableComposition
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.Dispatchers
import org.jetbrains.compose.web.dom.LocalComposeHtmlContext
import org.jetbrains.compose.web.dom.LinearStringComposeHtmlContext
import org.jetbrains.compose.web.dom.StringHtmlApplier
import org.jetbrains.compose.web.dom.StringHtmlElementNode
import org.jetbrains.compose.web.dom.StringHtmlNodeWrapper

/**
 * Composes [content] once into an HTML string without creating browser DOM nodes.
 * With no [key], the backing composition is disposed after the initial HTML is serialized.
 * With a key, the root, composition, and recomposer are retained for later calls with that key.
 * Keyed calls are not thread-safe, even with different keys. Callers must synchronize them.
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
 * @param key when non-null, reuses a renderer across calls. Each render clears remembered
 * state and effects but keeps matching HTML nodes. Keys and trees persist until process exit.
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
    val contentWithValidation: @Composable () -> Unit = {
        CompositionLocalProvider(
            LocalHtmlValidationMode provides if (validateStrictly) {
                HtmlValidationMode.Strict
            } else {
                HtmlValidationMode.Fast
            }
        ) { content() }
    }
    return if (key == null) {
        composeHtmlTree(contentWithValidation) { tree -> tree.toHtmlString(hydratable) }
    } else {
        composeReusableHtmlTree(key, contentWithValidation) { tree -> tree.toHtmlString(hydratable) }
    }
}

private val reusableHtmlCompositions = mutableMapOf<String, ReusableHtmlComposition>()

private class ReusableHtmlComposition {
    val root = StringHtmlElementNode.root()
    val recomposer = Recomposer(Dispatchers.Default).apply {
        // Keep coroutine effects disabled for every render, as in the unkeyed path.
        cancel()
    }
    val composition = ReusableComposition(
        applier = StringHtmlApplier(StringHtmlNodeWrapper(root)),
        parent = recomposer,
    )
    var content: (@Composable () -> Unit)? = null
    var rendering = false

    fun dispose() {
        composition.dispose()
        recomposer.close()
    }
}

/** Renders with a keyed composition that remains available for later calls. */
internal fun <T> composeReusableHtmlTree(
    key: String,
    content: @Composable () -> Unit,
    readTree: (StringHtmlElementNode) -> T,
): T {
    val renderer = reusableHtmlCompositions.getOrPut(key, ::ReusableHtmlComposition)
    check(!renderer.rendering) { "composeHtmlToString key \"$key\" is already rendering" }

    val snapshot = Snapshot.takeMutableSnapshot()
    renderer.rendering = true
    renderer.content = content
    val context = LinearStringComposeHtmlContext(renderer.root)
    try {
        return snapshot.enter {
            try {
                renderer.composition.setContentWithReuse {
                    CompositionLocalProvider(
                        LocalComposeHtmlContext provides context
                    ) {
                        checkNotNull(renderer.content).invoke()
                        context.finish()
                    }
                }
                readTree(renderer.root)
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

/** Renders with a fresh composition that is disposed after this call. */
internal fun <T> composeHtmlTree(
    content: @Composable () -> Unit,
    readTree: (StringHtmlElementNode) -> T,
): T {
    val root = StringHtmlElementNode.root()
    val context = LinearStringComposeHtmlContext(root)
    val snapshot = Snapshot.takeMutableSnapshot()

    return try {
        snapshot.enter {
            val recomposer = Recomposer(Dispatchers.Default).apply {
                // Render the initial composition without starting coroutine effects.
                cancel()
            }
            val composition = ControlledComposition(
                applier = StringHtmlApplier(StringHtmlNodeWrapper(root)),
                parent = recomposer,
            )

            try {
                composition.setContent {
                    CompositionLocalProvider(
                        LocalComposeHtmlContext provides context
                    ) {
                        content()
                        context.finish()
                    }
                }
                readTree(root)
            } finally {
                composition.dispose()
                recomposer.close()
            }
        }
    } finally {
        snapshot.dispose()
    }
}
