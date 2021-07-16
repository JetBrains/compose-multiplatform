package org.jetbrains.compose.web.events

import androidx.compose.web.events.SyntheticEvent
import org.w3c.dom.TouchEvent
import org.w3c.dom.TouchList
import org.w3c.dom.events.EventTarget

class SyntheticTouchEvent(
    nativeEvent: TouchEvent,
) : SyntheticEvent<EventTarget>(nativeEvent) {

    val altKey: Boolean = nativeEvent.altKey
    val changedTouches: TouchList = nativeEvent.changedTouches
    val ctrlKey: Boolean = nativeEvent.ctrlKey
    val metaKey: Boolean = nativeEvent.metaKey
    val shiftKey: Boolean = nativeEvent.shiftKey
    val touches: TouchList = nativeEvent.touches
}
