package org.jetbrains.compose.resources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

@Composable
internal actual fun <T> rememberState(init: T, block: suspend () -> T): State<T> {
    val state = remember { mutableStateOf(init) }
    LaunchedEffect(Unit) {
        state.value = block()
    }
    return state
}