package org.jetbrains.codeviewer.util

import androidx.compose.runtime.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope

@Composable
fun <T : Any> loadable(load: () -> T): MutableState<T?> {
    return loadableScoped { load() }
}

@Composable
fun <T : Any> loadableScoped(load: CoroutineScope.() -> T): MutableState<T?> {
    val state: MutableState<T?> = remember { mutableStateOf(null) }
    LaunchedTask {
        try {
            state.value = load()
        } catch (e: CancellationException) {
            // ignore
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return state
}