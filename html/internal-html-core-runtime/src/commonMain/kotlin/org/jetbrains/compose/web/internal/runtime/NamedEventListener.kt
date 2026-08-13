package org.jetbrains.compose.web.internal.runtime

import kotlinx.browser.dom.events.EventListener

/**
 * An event listener that retains the DOM event name used to register it.
 *
 * This contract lives in common code because Compose HTML's event attributes
 * also need it when producing a non-browser tree. Browser-specific listener
 * registration remains in the JS DOM implementation.
 */
@ComposeWebInternalApi
interface NamedEventListener : EventListener {
    val name: String
}
