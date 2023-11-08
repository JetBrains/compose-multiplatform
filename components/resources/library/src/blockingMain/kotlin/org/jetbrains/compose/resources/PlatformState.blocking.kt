package org.jetbrains.compose.resources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.runBlocking

@Composable
internal actual fun <T> rememberState(key: Any, init: T, block: suspend () -> T): State<T> = remember(key) {
    mutableStateOf(
        runBlocking { block() }
    )
}