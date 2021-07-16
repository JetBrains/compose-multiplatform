package org.jetbrains.compose.web.events

import androidx.compose.web.events.SyntheticEvent
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget

class SyntheticSelectEvent<Element : EventTarget> internal constructor(
    nativeEvent: Event,
    selectionInfoDetails: SelectionInfoDetails
) : SyntheticEvent<Element>(nativeEvent) {

    val selectionStart: Int = selectionInfoDetails.selectionStart
    val selectionEnd: Int = selectionInfoDetails.selectionEnd


    fun selection(): String {
        return nativeEvent.target.asDynamic().value.unsafeCast<String?>()?.substring(
            selectionStart, selectionEnd
        ) ?: ""
    }
}

internal external interface SelectionInfoDetails {
    val selectionStart: Int
    val selectionEnd: Int
}
