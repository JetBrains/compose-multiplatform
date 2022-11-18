package org.jetbrains.compose.web.internal.runtime

import org.w3c.dom.events.Event

actual interface EventListener {
    actual fun handleEvent(event: Event)
}

@JsFun("(listener, handler) => ({ handleEvent: (event) => handler(listener, event) })")
private external fun wrapListenerEvent(eventListener: EventListener, wasmHandler: (EventListener, Event) -> Unit): org.w3c.dom.events.EventListener

actual fun EventListener.getW3cListener(): org.w3c.dom.events.EventListener =
    wrapListenerEvent(this) { listener, event ->
        listener.handleEvent(event)
    }