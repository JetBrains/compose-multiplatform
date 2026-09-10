/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ControlledComposition
import androidx.compose.runtime.Recomposer
import kotlinx.coroutines.Dispatchers
import org.jetbrains.compose.web.dom.LocalComposeHtmlContext
import org.jetbrains.compose.web.dom.StringComposeHtmlContext
import org.jetbrains.compose.web.dom.StringHtmlApplier
import org.jetbrains.compose.web.dom.StringHtmlElementNode
import org.jetbrains.compose.web.dom.StringHtmlNodeWrapper

/**
 * Composes [content] once into an HTML string without creating browser DOM nodes.
 * The backing composition is disposed after the initial HTML has been serialized.
 * Coroutine effects such as `LaunchedEffect` do not run. `SideEffect` and
 * `DisposableEffect` still execute.
 *
 * Known limitations:
 * - DOM property updates registered with `AttrsScope.prop(...)` are ignored because
 *   string rendering has no underlying DOM element.
 * - Inline styles preserve CSS fallbacks, but do not fully emulate CSSOM validation and
 *   mutation (for example, invalid assignments that also change priority, or shorthand removal).
 *
 * @throws IllegalArgumentException if a raw-text element contains unsafe text or element children,
 * or serialized `noscript` contents contain a `</noscript>` end tag.
 */
fun composeHtmlToString(
    content: @Composable () -> Unit
): String {
    val root = StringHtmlElementNode.root()
    val recomposer = Recomposer(Dispatchers.Default).apply {
        // Render the initial composition without starting coroutine effects.
        cancel()
    }
    val composition = ControlledComposition(
        applier = StringHtmlApplier(StringHtmlNodeWrapper(root)),
        parent = recomposer,
    )

    return try {
        composition.setContent {
            CompositionLocalProvider(
                LocalComposeHtmlContext provides StringComposeHtmlContext
            ) {
                content()
            }
        }
        root.toHtmlString()
    } finally {
        composition.dispose()
        recomposer.close()
    }
}
