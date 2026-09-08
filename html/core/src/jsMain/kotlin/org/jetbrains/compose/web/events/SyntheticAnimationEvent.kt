package org.jetbrains.compose.web.events

import androidx.compose.web.events.SyntheticEvent
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget

class SyntheticAnimationEvent internal constructor(
    nativeEvent: Event,
    animationEventDetails: AnimationEventDetails
) : SyntheticEvent<EventTarget>(nativeEvent) {

    val animationName: String = animationEventDetails.animationName
    val elapsedTime: Number = animationEventDetails.elapsedTime
    val pseudoElement: String = animationEventDetails.pseudoElement
}

internal external interface AnimationEventDetails {
    val animationName: String
    val elapsedTime: Number
    val pseudoElement: String
}
