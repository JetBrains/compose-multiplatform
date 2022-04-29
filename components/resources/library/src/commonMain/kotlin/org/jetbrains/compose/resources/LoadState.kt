package org.jetbrains.compose.resources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

sealed class LoadState<T> {
    class Loading<T> : LoadState<T>()
    data class Success<T>(val value: T) : LoadState<T>()
    data class Error<T>(val exception: Exception) : LoadState<T>()
}

@Composable
fun <T> load(load: suspend () -> T): LoadState<T> {
    return load(Unit, load)
}

@Composable
fun <T: Any> loadOrNull(load: suspend () -> T): T? {
    return loadOrNull(Unit, load)
}

@Composable
fun <T> load(key1: Any?, load: suspend () -> T): LoadState<T> {
    return load(key1, Unit, load)
}

@Composable
fun <T: Any> loadOrNull(key1: Any?, load: suspend () -> T): T? {
    return loadOrNull(key1, Unit, load)
}

@Composable
fun <T> load(key1: Any?, key2: Any?, load: suspend () -> T): LoadState<T> {
    return load(key1, key2, Unit, load)
}

@Composable
fun <T: Any> loadOrNull(key1: Any?, key2: Any?, load: suspend () -> T): T? {
    return loadOrNull(key1, key2, Unit, load)
}

@Composable
fun <T> load(key1: Any?, key2: Any?, key3: Any?, load: suspend () -> T): LoadState<T> {
    var state: LoadState<T> by remember(key1, key2, key3) { mutableStateOf(LoadState.Loading()) }
    LaunchedEffect(key1, key2, key3) {
        state = try {
            LoadState.Success(load())
        } catch (e: Exception) {
            LoadState.Error(e)
        }
    }
    return state
}

@Composable
fun <T: Any> loadOrNull(key1: Any?, key2: Any?, key3: Any?, load: suspend () -> T): T? {
    val state = load(key1, key2, key3, load)
    return (state as? LoadState.Success<T>)?.value
}