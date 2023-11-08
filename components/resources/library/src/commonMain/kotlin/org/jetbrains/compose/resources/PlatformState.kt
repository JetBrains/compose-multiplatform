package org.jetbrains.compose.resources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State

@Composable
internal expect fun <T> rememberState(key: Any, init: T, block: suspend () -> T): State<T>