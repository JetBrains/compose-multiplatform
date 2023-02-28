package org.jetbrains.compose.web.events

import androidx.compose.web.events.SyntheticEvent
import org.jetbrains.compose.web.NativeEventExtension
import org.w3c.dom.DataTransfer
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget

// @param nativeEvent: Event - we don't use [org.w3c.dom.events.InputEvent] here,
// since for cases it can be just [org.w3c.dom.events.Event]
class SyntheticInputEvent<ValueType, Element : EventTarget> internal constructor(
    val value: ValueType,
    nativeEvent: Event
) : SyntheticEvent<Element>(
    nativeEvent = nativeEvent
) {
    private val nativeEventExtension: NativeEventExtension? = nativeEvent as? NativeEventExtension

    val data: String?
        get() = nativeEventExtension?.data
    val dataTransfer: DataTransfer?
        get() = nativeEventExtension?.dataTransfer
    val inputType: String?
        get() = nativeEventExtension?.inputType
    val isComposing: Boolean
        get() = nativeEventExtension?.isComposing ?: false
}
