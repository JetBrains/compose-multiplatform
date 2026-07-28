package org.jetbrains.compose.resources

import androidx.compose.runtime.*

@Composable
internal actual fun <T> rememberResourceState(
    key1: Any,
    getDefault: () -> T,
    block: suspend (ResourceEnvironment) -> T
): State<T> {
    val environment = LocalComposeEnvironment.current.rememberEnvironment()
    return remember(key1, environment) {
        mutableStateOf(
            runResourceBlocking { block(environment) }
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
            runResourceBlocking { block(environment) }
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
            runResourceBlocking { block(environment) }
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
            runResourceBlocking { block(environment) }
        )
    }
}

/**
 * Executes the [block] on the current thread and returns its result.
 *
 * It is used to read a resource synchronously during a composition
 * (see [rememberResourceState]).
 *
 * Unlike `kotlinx.coroutines.runBlocking` on the JVM it:
 * - doesn't check the interruption flag of the calling thread, so a resource can be read even
 *   when somebody has interrupted the thread which performs the composition;
 * - executes a [block] which doesn't suspend (a cache hit, for example) without parking
 *   the calling thread at all.
 *
 * The [block] must not wait for the calling thread itself: a `withContext(Dispatchers.Main)`
 * inside a resource reader deadlocks a composition here in the same way as with `runBlocking`.
 */
internal expect fun <T> runResourceBlocking(block: suspend () -> T): T