package org.jetbrains.compose.resources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State

/**
 * This is a platform-specific function that calculates and remembers a state.
 * For all platforms except a JS it is a blocking function.
 * On the JS platform it loads the state asynchronously and uses `getDefault` as an initial state value.
 */
@Composable
internal expect fun <T> rememberResourceState(
    key1: Any,
    getDefault: () -> T,
    block: suspend (ResourceEnvironment) -> T
): State<T>

/**
 * This is a platform-specific function that calculates and remembers a state.
 * For all platforms except a JS it is a blocking function.
 * On the JS platform it loads the state asynchronously and uses `getDefault` as an initial state value.
 */
@Composable
internal expect fun <T> rememberResourceState(
    key1: Any,
    key2: Any,
    getDefault: () -> T,
    block: suspend (ResourceEnvironment) -> T
): State<T>

/**
 * This is a platform-specific function that calculates and remembers a state.
 * For all platforms except a JS it is a blocking function.
 * On the JS platform it loads the state asynchronously and uses `getDefault` as an initial state value.
 */
@Composable
internal expect fun <T> rememberResourceState(
    key1: Any,
    key2: Any,
    key3: Any,
    getDefault: () -> T,
    block: suspend (ResourceEnvironment) -> T
): State<T>


/**
 * This is a platform-specific function that calculates and remembers a state.
 * For all platforms except a JS it is a blocking function.
 * On the JS platform it loads the state asynchronously and uses `getDefault` as an initial state value.
 */
@Composable
internal expect fun <T> rememberResourceState(
    key1: Any,
    key2: Any,
    key3: Any,
    key4: Any,
    getDefault: () -> T,
    block: suspend (ResourceEnvironment) -> T
): State<T>

/**
 * Suspends until all currently pending asynchronous resource state loads complete.
 *
 * This function waits until resource values are written to their Compose states. It does not
 * wait for Compose to process those state changes or perform recomposition. In Compose UI tests,
 * call [ComposeUiTest.waitForIdle] after this function when observing the rendered state.
 *
 * If a resource load fails or is cancelled, it is still considered complete.
 */
@ExperimentalResourceApi
expect suspend fun awaitPendingResourceStateLoads()