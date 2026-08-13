package org.jetbrains.compose.web.events

import androidx.compose.web.events.SyntheticEvent
import kotlinx.browser.dom.events.EventTarget
import kotlinx.browser.dom.events.FocusEvent

class SyntheticFocusEvent internal constructor(
    nativeEvent: FocusEvent,
) : SyntheticEvent<EventTarget>(nativeEvent) {

    val relatedTarget: EventTarget? = nativeEvent.relatedTarget
}
