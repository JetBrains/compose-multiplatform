package org.jetbrains.compose.web.internal.runtime

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineDispatcher

internal actual val GlobalSnapshotManagerDispatcher: CoroutineDispatcher = Dispatchers.Default
