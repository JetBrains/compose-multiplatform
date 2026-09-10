package org.jetbrains.compose.resources

import kotlinx.coroutines.yield

internal actual suspend fun webYield() = yield()