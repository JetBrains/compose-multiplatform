package org.jetbrains.compose.web.events

import androidx.compose.web.events.SyntheticEvent
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget

class SyntheticAnimationEvent(
    nativeEvent: Event,
) : SyntheticEvent<EventTarget>(nativeEvent) {

    val animationName: String = nativeEvent.asDynamic().animationName.unsafeCast<String>()
    val elapsedTime: Number = nativeEvent.asDynamic().elapsedTime.unsafeCast<Number>()
    val pseudoElement: String = nativeEvent.asDynamic().pseudoElement.unsafeCast<String>()
}
