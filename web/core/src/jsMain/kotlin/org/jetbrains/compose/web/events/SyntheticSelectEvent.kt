package org.jetbrains.compose.web.events

import androidx.compose.web.events.SyntheticEvent
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget

class SyntheticSelectEvent<Element : EventTarget>(
    nativeEvent: Event,
) : SyntheticEvent<Element>(nativeEvent) {

    val selectionStart: Int = nativeEvent.target.asDynamic().selectionStart.unsafeCast<Int>()
    val selectionEnd: Int = nativeEvent.target.asDynamic().selectionEnd.unsafeCast<Int>()


    fun selection(): String {
        return nativeEvent.target.asDynamic().value.unsafeCast<String?>()?.substring(
            selectionStart, selectionEnd
        ) ?: ""
    }
}
