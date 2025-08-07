package org.jetbrains.compose.resources

import androidx.compose.runtime.*
import kotlinx.coroutines.runBlocking

@Composable
internal actual fun <T> rememberResourceState(
    key1: Any,
    getDefault: () -> T,
    block: suspend (ResourceEnvironment) -> T
): State<T> {
    val environment = LocalComposeEnvironment.current.rememberEnvironment()
    return remember(key1, environment) {
        mutableStateOf(
            runBlocking { block(environment) }
        )
    }
}

@Composable
internal actual fun <T> rememberResourceState(
    key1: Any,
    key2: Any,
    getDefault: () -> T,
    block: suspend (ResourceEnvironment) -> T
): State<T> {
    val environment = LocalComposeEnvironment.current.rememberEnvironment()
    return remember(key1, key2, environment) {
        mutableStateOf(
            runBlocking { block(environment) }
        )
    }
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
    return remember(key1, key2, key3, environment) {
        mutableStateOf(
            runBlocking { block(environment) }
        )
    }
}

@Composable
internal actual fun <T> rememberResourceState(
    key1: Any,
    key2: Any,
    key3: Any,
    key4: Any,
    getDefault: () -> T,
    block: suspend (ResourceEnvironment) -> T
): State<T> {
    val environment = LocalComposeEnvironment.current.rememberEnvironment()
    return remember(key1, key2, key3, key4, environment) {
        mutableStateOf(
            runBlocking { block(environment) }
        )
    }
}