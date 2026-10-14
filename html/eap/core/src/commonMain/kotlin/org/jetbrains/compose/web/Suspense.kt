/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import org.jetbrains.compose.web.dom.LocalComposeHtmlContext

/**
 * Shows [fallback] while a descendant calls [SuspensePending], then shows all [content] together.
 * Hidden content keeps its state and effects. DOM reference callbacks wait until it is visible.
 *
 * Call [SuspensePending] while loading and show error content on failure.
 * For SSR hydration, transfer state so content is ready immediately.
 */
@Composable
fun Suspense(fallback: @Composable () -> Unit, content: @Composable () -> Unit) {
    LocalComposeHtmlContext.current.Suspense(fallback, content)
}

/** Uses an existing [pending] state to show the fallback. Content stays composed. */
@Composable
fun Suspense(pending: Boolean, fallback: @Composable () -> Unit, content: @Composable () -> Unit) {
    Suspense(fallback) {
        if (pending) SuspensePending()
        content()
    }
}

/**
 * Keeps the nearest boundary pending while this call is composed. Emits no visible content.
 * Removing the call clears its pending contribution. Without a boundary, async server rendering
 * waits before sending the shell. Browser and synchronous rendering emit nothing.
 */
@Composable
fun SuspensePending() {
    val registry = LocalSuspenseRegistry.current ?: return
    remember(registry) { PendingRegistration(registry) }
}

internal class SuspenseRegistry {
    var count by mutableIntStateOf(0)
        private set
    fun add() { count++ }
    fun remove() { count-- }
}

private class PendingRegistration(private val registry: SuspenseRegistry) : RememberObserver {
    override fun onRemembered() = registry.add()
    override fun onForgotten() = registry.remove()
    override fun onAbandoned() = Unit
}

internal val LocalSuspenseRegistry = staticCompositionLocalOf<SuspenseRegistry?> { null }
