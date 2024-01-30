package org.jetbrains.compose.resources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

@Composable
internal actual fun <T> rememberResourceState(
    key: Any,
    getDefault: () -> T,
    block: suspend (ResourceEnvironment) -> T
): State<T> {
    val environment = LocalComposeEnvironment.current.rememberEnvironment()
    val state = remember(key) { mutableStateOf(getDefault()) }
    LaunchedEffect(key) {
        state.value = block(environment)
    }
    return state
}