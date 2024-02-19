package org.jetbrains.compose.resources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

@Composable
internal actual fun <T> rememberResourceState(
    key1: Any,
    getDefault: () -> T,
    block: suspend (ResourceEnvironment) -> T
): State<T> {
    val environment = LocalComposeEnvironment.current.rememberEnvironment()
    val state = remember(key1) { mutableStateOf(getDefault()) }
    LaunchedEffect(key1) {
        state.value = block(environment)
    }
    return state
}

@Composable
internal actual fun <T> rememberResourceState(
    key1: Any,
    key2: Any,
    getDefault: () -> T,
    block: suspend (ResourceEnvironment) -> T
): State<T> {
    val environment = LocalComposeEnvironment.current.rememberEnvironment()
    val state = remember(key1, key2) { mutableStateOf(getDefault()) }
    LaunchedEffect(key1, key2) {
        state.value = block(environment)
    }
    return state
}

@Composable
internal actual fun <T> rememberResourceState(
    key1: Any,
    key2: Any,
    key3: Any,
    getDefault: () -> T,
    block: suspend (ResourceEnvironment) -> T
): State<T> {
    val environment = LocalComposeEnvironment.current.rememberEnvironment()
    val state = remember(key1, key2, key3) { mutableStateOf(getDefault()) }
    LaunchedEffect(key1, key2, key3) {
        state.value = block(environment)
    }
    return state
}