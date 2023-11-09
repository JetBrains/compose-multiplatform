package org.jetbrains.compose.resources

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal actual val cacheDispatcher: CoroutineDispatcher = Dispatchers.Default