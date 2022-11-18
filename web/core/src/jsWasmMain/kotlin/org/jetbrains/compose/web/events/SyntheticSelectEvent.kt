package org.jetbrains.compose.web.events

import androidx.compose.web.events.SyntheticEvent
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget
import org.jetbrains.compose.web.*

class SyntheticSelectEvent<Element : EventTarget> internal constructor(
    nativeEvent: Event,
    selectionInfoDetails: SelectionInfoDetails
) : SyntheticEvent<Element>(nativeEvent) {

    val selectionStart: Int = selectionInfoDetails.selectionStart
    val selectionEnd: Int = selectionInfoDetails.selectionEnd


    fun selection(): String {
        return nativeEvent.target?.getStringProperty("value")?.substring(
            selectionStart, selectionEnd
        ) ?: ""
    }
}

internal external interface SelectionInfoDetails {
    val selectionStart: Int
    val selectionEnd: Int
}
