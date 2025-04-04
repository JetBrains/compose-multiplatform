package org.jetbrains.compose.resources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonSkippableComposable

internal class RecompositionsCounter {
    var count = 0
        private set

    @Composable
    @NonSkippableComposable
    fun content(block: @Composable () -> Unit) {
        count++
        block()
    }
}