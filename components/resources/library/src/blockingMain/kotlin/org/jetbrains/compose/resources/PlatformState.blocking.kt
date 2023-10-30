package org.jetbrains.compose.resources

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.runBlocking

internal actual fun <T> rememberState(init: T, block: suspend () -> T): State<T> = remember {
    mutableStateOf(
        runBlocking { block() }
    )
}