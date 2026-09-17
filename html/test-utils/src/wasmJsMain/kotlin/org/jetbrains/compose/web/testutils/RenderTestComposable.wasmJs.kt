@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.jetbrains.compose.web.testutils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MonotonicFrameClock
import kotlinx.browser.dom.HTMLElement
import kotlinx.browser.dom.MutationObserverInit
import org.jetbrains.compose.web.renderComposable
import kotlin.js.js

internal actual fun renderTestComposable(
    root: HTMLElement,
    monotonicFrameClock: MonotonicFrameClock,
    content: @Composable () -> Unit,
) {
    renderComposable(root, monotonicFrameClock) {
        content()
    }
}

internal actual fun createMutationObserverOptions(): MutationObserverInit =
    js("({ childList: true, attributes: true, characterData: true, subtree: true, attributeOldValue: true })")
