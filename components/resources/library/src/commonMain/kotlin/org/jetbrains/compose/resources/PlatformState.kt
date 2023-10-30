package org.jetbrains.compose.resources

import androidx.compose.runtime.State

internal expect fun <T> rememberState(init: T, block: suspend () -> T): State<T>