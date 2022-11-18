package org.w3c.dom.events

actual interface W3cEventListener {
    actual fun handleEvent(event: Event)
}