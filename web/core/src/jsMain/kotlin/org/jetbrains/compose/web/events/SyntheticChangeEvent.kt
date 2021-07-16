package org.jetbrains.compose.web.events

import androidx.compose.web.events.SyntheticEvent
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget

class SyntheticChangeEvent<Value, Element : EventTarget> internal constructor(
    val value: Value,
    nativeEvent: Event,
) : SyntheticEvent<Element>(nativeEvent)
