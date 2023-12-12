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
    key: Any,
    getDefault: () -> T,
    block: suspend (ResourceEnvironment) -> T
): State<T>