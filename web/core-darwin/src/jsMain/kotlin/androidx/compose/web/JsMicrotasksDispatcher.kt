package org.jetbrains.compose.web

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlin.coroutines.CoroutineContext
import kotlin.js.Promise

internal class JsMicrotasksDispatcher : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        Promise.resolve(Unit).then { block.run() }
    }
}
