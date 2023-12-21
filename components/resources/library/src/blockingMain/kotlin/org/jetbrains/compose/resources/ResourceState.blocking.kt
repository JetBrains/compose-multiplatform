package org.jetbrains.compose.resources

import androidx.compose.runtime.*
import kotlinx.coroutines.runBlocking

@Composable
internal actual fun <T> rememberResourceState(
    key: Any,
    getDefault: () -> T,
    block: suspend (ResourceEnvironment) -> T
): State<T> {
    val environment = LocalComposeEnvironment.current.rememberEnvironment()
    return remember(key, environment) {
        mutableStateOf(
            runBlocking { block(environment) }
        )
    }
}