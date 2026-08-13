package org.jetbrains.compose.web.events

import androidx.compose.web.events.SyntheticEvent
import kotlinx.browser.dom.DataTransfer
import kotlinx.browser.dom.events.Event
import kotlinx.browser.dom.events.EventTarget
import kotlinx.browser.dom.events.InputEvent
import org.jetbrains.compose.web.internal.inputDataTransferCompat
import org.jetbrains.compose.web.internal.inputTypeCompat
import org.jetbrains.compose.web.internal.unsafeCast

// @param nativeEvent: Event - we don't use [org.w3c.dom.events.InputEvent] here,
// since for cases it can be just [org.w3c.dom.events.Event]
class SyntheticInputEvent<ValueType, Element : EventTarget> internal constructor(
    val value: ValueType,
    nativeEvent: Event
) : SyntheticEvent<Element>(
    nativeEvent = nativeEvent
) {
    val data: String? = nativeEvent.unsafeCast<InputEvent>().data?.unsafeCast<String>()
    val dataTransfer: DataTransfer? = nativeEvent.inputDataTransferCompat()
    val inputType: String? = nativeEvent.inputTypeCompat()
    val isComposing: Boolean = nativeEvent.unsafeCast<InputEvent>().isComposing?.unsafeCast<Boolean>() ?: false
}
