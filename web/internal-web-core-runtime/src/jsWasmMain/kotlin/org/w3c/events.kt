package org.w3c.dom.events

expect interface W3cEventListener {
    fun handleEvent(event: Event)
}