package org.jetbrains.compose.web.events

import androidx.compose.web.events.SyntheticEvent
import org.w3c.dom.DataTransfer
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget

class SyntheticInputEvent<ValueType, Element : EventTarget>(
    val value: ValueType,
    nativeEvent: Event
) : SyntheticEvent<Element>(
    nativeEvent = nativeEvent
) {
    val data: String? = nativeEvent.asDynamic().data?.unsafeCast<String>()
    val dataTransfer: DataTransfer? = nativeEvent.asDynamic().dataTransfer?.unsafeCast<DataTransfer>()
    val inputType: String? = nativeEvent.asDynamic().inputType?.unsafeCast<String>()
    val isComposing: Boolean = nativeEvent.asDynamic().isComposing?.unsafeCast<Boolean>() ?: false
}
