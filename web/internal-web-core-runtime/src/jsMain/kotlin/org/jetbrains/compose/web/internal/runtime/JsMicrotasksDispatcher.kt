package org.jetbrains.compose.web.internal.runtime

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventListener
import kotlin.coroutines.CoroutineContext
import kotlin.js.Promise

@OptIn(ComposeWebInternalApi::class)
internal actual val GlobalSnapshotManagerDispatcher: CoroutineDispatcher = Dispatchers.Default

@ComposeWebInternalApi
class JsMicrotasksDispatcher : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        Promise.resolve(Unit).then { block.run() }
    }
}

internal actual fun createEventListener(handleEvent: (Event) -> Unit): EventListener =
    object : EventListener {
        override fun handleEvent(event: Event) {
            handleEvent(event)
        }
    }
