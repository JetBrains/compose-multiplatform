package org.jetbrains.compose.web.events

import androidx.compose.web.events.SyntheticEvent
import kotlinx.browser.dom.events.Event
import kotlinx.browser.dom.events.EventTarget
import org.jetbrains.compose.web.internal.animationEventDetails

class SyntheticAnimationEvent internal constructor(
    nativeEvent: Event,
) : SyntheticEvent<EventTarget>(nativeEvent) {
    private val animationEventDetails = nativeEvent.animationEventDetails()

    val animationName: String = animationEventDetails.animationName
    val elapsedTime: Number = animationEventDetails.elapsedTime
    val pseudoElement: String = animationEventDetails.pseudoElement
}
