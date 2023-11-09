package org.jetbrains.compose.resources

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
internal actual val cacheDispatcher: CoroutineDispatcher = newSingleThreadContext("resources_cache_ctx")