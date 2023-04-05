package org.jetbrains.compose.web.internal.runtime

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlin.coroutines.CoroutineContext
import kotlin.js.Promise

@ComposeWebInternalApi
class JsMicrotasksDispatcher : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        Promise.resolve(Unit).then { block.run() }
    }
}
