package org.jetbrains.compose.web.events

import androidx.compose.web.events.SyntheticEvent
import kotlinx.browser.dom.events.Event
import kotlinx.browser.dom.events.EventTarget
import org.jetbrains.compose.web.internal.selectionInfoDetails

class SyntheticSelectEvent<Element : EventTarget> internal constructor(
    nativeEvent: Event,
) : SyntheticEvent<Element>(nativeEvent) {
    private val details = nativeEvent.selectionInfoDetails()

    val selectionStart: Int = details.selectionStart
    val selectionEnd: Int = details.selectionEnd

    fun selection(): String =
        details.value?.substring(selectionStart, selectionEnd) ?: ""
}
