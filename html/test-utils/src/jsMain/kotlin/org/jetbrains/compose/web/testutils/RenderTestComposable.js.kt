package org.jetbrains.compose.web.testutils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MonotonicFrameClock
import org.jetbrains.compose.web.renderComposable
import org.w3c.dom.HTMLElement
import org.w3c.dom.MutationObserverInit

internal actual fun renderTestComposable(
    root: HTMLElement,
    monotonicFrameClock: MonotonicFrameClock,
    content: @Composable () -> Unit,
) {
    renderComposable(root, monotonicFrameClock) {
        content()
    }
}

private object MutationObserverOptions : MutationObserverInit {
    override var childList: Boolean? = true
    override var attributes: Boolean? = true
    override var characterData: Boolean? = true
    override var subtree: Boolean? = true
    override var attributeOldValue: Boolean? = true
}

internal actual fun createMutationObserverOptions(): MutationObserverInit =
    MutationObserverOptions
