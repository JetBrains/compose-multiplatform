package org.jetbrains.compose.web.events

import androidx.compose.web.events.SyntheticEvent
import org.w3c.dom.DataTransfer
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget
import org.jetbrains.compose.web.*

// @param nativeEvent: Event - we don't use [org.w3c.dom.events.InputEvent] here,
// since for cases it can be just [org.w3c.dom.events.Event]
class SyntheticInputEvent<ValueType, Element : EventTarget> internal constructor(
    val value: ValueType,
    nativeEvent: Event
) : SyntheticEvent<Element>(
    nativeEvent = nativeEvent
) {
    val data: String? = nativeEvent.getStringProperty("data")
    val dataTransfer: DataTransfer? = nativeEvent.getAnyProperty<DataTransfer?>("dataTransfer")
    val inputType: String? = nativeEvent.getStringProperty("inputType")
    val isComposing: Boolean = nativeEvent.getBooleanProperty("isComposing")!!
}
