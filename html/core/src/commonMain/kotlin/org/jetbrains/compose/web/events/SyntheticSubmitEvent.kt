package org.jetbrains.compose.web.events

import androidx.compose.web.events.SyntheticEvent
import kotlinx.browser.dom.events.Event
import kotlinx.browser.dom.events.EventTarget

class SyntheticSubmitEvent internal constructor(
    nativeEvent: Event
) : SyntheticEvent<EventTarget>(nativeEvent)
