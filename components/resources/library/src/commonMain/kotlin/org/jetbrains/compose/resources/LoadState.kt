package org.jetbrains.compose.resources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * Represents the load state of [T].
 */
sealed class LoadState<T> {
    class Loading<T> : LoadState<T>()
    data class Success<T>(val value: T) : LoadState<T>()
    data class Error<T>(val exception: Exception) : LoadState<T>()
}


/**
 * Load an item with type [T] asynchronously, and notify the caller about the load state.
 * Whenever the load state changes (for example it succeeds or fails), the caller will be recomposed with the new state.
 * The load will be cancelled when the [load] leaves the composition.
 */
@Composable
fun <T> load(load: suspend () -> T): LoadState<T> {
    return load(Unit, load)
}

/**
 * Load an item with type [T] asynchronously. Returns null while loading or if the load has failed.
 * Whenever the result changes, the caller will be recomposed with the new value.
 * The load will be cancelled when the [loadOrNull] leaves the composition.
 */
@Composable
fun <T: Any> loadOrNull(load: suspend () -> T): T? {
    return loadOrNull(Unit, load)
}


/**
 * Load an item with type [T] asynchronously, and notify the caller about the load state.
 * Whenever the load state changes (for example it succeeds or fails), the caller will be recomposed with the new state.
 * The load will be cancelled and re-launched when [load] is recomposed with a different [key1].
 * The load will be cancelled when the [load] leaves the composition.
 */
@Composable
fun <T> load(key1: Any?, load: suspend () -> T): LoadState<T> {
    return load(key1, Unit, load)
}

/**
 * Load an item with type [T] asynchronously. Returns null while loading or if the load has failed.
 * Whenever the result changes, the caller will be recomposed with the new value.
 * The load will be cancelled and re-launched when [loadOrNull] is recomposed with a different [key1].
 * The load will be cancelled when the [loadOrNull] leaves the composition.
 */
@Composable
fun <T: Any> loadOrNull(key1: Any?, load: suspend () -> T): T? {
    return loadOrNull(key1, Unit, load)
}

/**
 * Load an item with type [T] asynchronously, and notify the caller about the load state.
 * Whenever the load state changes (for example it succeeds or fails), the caller will be recomposed with the new state.
 * The load will be cancelled and re-launched when [load] is recomposed with a different [key1] or [key2].
 * The load will be cancelled when the [load] leaves the composition.
 */
@Composable
fun <T> load(key1: Any?, key2: Any?, load: suspend () -> T): LoadState<T> {
    return load(key1, key2, Unit, load)
}

/**
 * Load an item with type [T] asynchronously. Returns null while loading or if the load has failed.
 * Whenever the result changes, the caller will be recomposed with the new value.
 * The load will be cancelled and re-launched when [loadOrNull] is recomposed with a different [key1] or [key2].
 * The load will be cancelled when the [loadOrNull] leaves the composition.
 */
@Composable
fun <T: Any> loadOrNull(key1: Any?, key2: Any?, load: suspend () -> T): T? {
    return loadOrNull(key1, key2, Unit, load)
}

/**
 * Load an item with type [T] asynchronously, and notify the caller about the load state.
 * Whenever the load state changes (for example it succeeds or fails), the caller will be recomposed with the new state.
 * The load will be cancelled and re-launched when [load] is recomposed with a different [key1], [key2] or [key3].
 * The load will be cancelled when the [load] leaves the composition.
 */
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

/**
 * Load an item with type [T] asynchronously. Returns null while loading or if the load has failed..
 * Whenever the result changes, the caller will be recomposed with the new value.
 * The load will be cancelled and re-launched when [loadOrNull] is recomposed with a different [key1], [key2] or [key3].
 * The load will be cancelled when the [loadOrNull] leaves the composition.
 */
@Composable
fun <T: Any> loadOrNull(key1: Any?, key2: Any?, key3: Any?, load: suspend () -> T): T? {
    val state = load(key1, key2, key3, load)
    return (state as? LoadState.Success<T>)?.value
}