package org.jetbrains.compose.web.internal.runtime

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineDispatcher
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventListener

internal actual val GlobalSnapshotManagerDispatcher: CoroutineDispatcher = Dispatchers.Default


internal actual fun createEventListener(handleEvent: (Event) -> Unit): EventListener =
    jsCreateEventListener(handleEvent)

@JsFun("handleEvent => ({ handleEvent })")
private external fun jsCreateEventListener(handleEvent: (Event) -> Unit): EventListener
