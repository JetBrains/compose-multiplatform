package org.jetbrains.compose.web.internal.runtime

import org.w3c.dom.events.Event

expect interface EventListener {
    fun handleEvent(event: Event)
}

expect fun EventListener.getW3cListener(): org.w3c.dom.events.EventListener