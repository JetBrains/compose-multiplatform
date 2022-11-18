package org.jetbrains.compose.web.events

import androidx.compose.web.events.SyntheticEvent
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget

class SyntheticSubmitEvent internal constructor(
    nativeEvent: Event
) : SyntheticEvent<EventTarget>(nativeEvent)
