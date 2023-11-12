package org.jetbrains.compose.resources

import androidx.compose.runtime.Composable

internal class RecompositionsCounter {
    var count = 0
        private set

    @Composable
    fun content(block: @Composable () -> Unit) {
        count++
        block()
    }
}