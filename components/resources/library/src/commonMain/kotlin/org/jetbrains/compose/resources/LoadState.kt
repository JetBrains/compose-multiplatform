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
    var state: LoadState<T> by remember(load) { mutableStateOf(LoadState.Loading()) }
    LaunchedEffect(load) {
        state = try {
            LoadState.Success(load())
        } catch (e: Exception) {
            LoadState.Error(e)
        }
    }
    return state
}

@Composable
fun <T: Any> loadOrNull(load: suspend () -> T): T? {
    val state = load(load)
    return (state as? LoadState.Success<T>)?.value
}