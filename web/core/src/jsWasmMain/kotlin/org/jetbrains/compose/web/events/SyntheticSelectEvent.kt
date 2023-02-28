package org.jetbrains.compose.web.events

import androidx.compose.web.events.SyntheticEvent
import org.jetbrains.compose.web.EventTargetExtension
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget

class SyntheticSelectEvent<Element : EventTarget> internal constructor(
    nativeEvent: Event,
    selectionInfoDetails: SelectionInfoDetails
) : SyntheticEvent<Element>(nativeEvent) {

    val selectionStart: Int = selectionInfoDetails.selectionStart
    val selectionEnd: Int = selectionInfoDetails.selectionEnd


    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    fun selection(): String {
        return (nativeEvent.target!! as EventTargetExtension).value?.substring(
            selectionStart, selectionEnd
        ) ?: ""
    }
}

internal external interface SelectionInfoDetails {
    val selectionStart: Int
    val selectionEnd: Int
}
