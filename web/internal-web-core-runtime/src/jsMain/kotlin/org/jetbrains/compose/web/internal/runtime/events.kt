package org.jetbrains.compose.web.internal.runtime

actual typealias EventListener = org.w3c.dom.events.EventListener

actual fun EventListener.getW3cListener(): org.w3c.dom.events.EventListener = this