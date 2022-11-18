package org.jetbrains.compose.web.events

import androidx.compose.web.events.SyntheticEvent
import org.w3c.dom.events.EventTarget
import org.w3c.dom.events.FocusEvent

class SyntheticFocusEvent internal constructor(
    nativeEvent: FocusEvent,
) : SyntheticEvent<EventTarget>(nativeEvent) {

    val relatedTarget: EventTarget? = nativeEvent.relatedTarget
}
