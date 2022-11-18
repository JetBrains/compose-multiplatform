package org.jetbrains.compose.web.internal.runtime

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlin.coroutines.CoroutineContext
import kotlin.js.Promise

@ComposeWebInternalApi
expect class JsMicrotasksDispatcher : CoroutineDispatcher
