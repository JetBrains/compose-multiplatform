package org.jetbrains.compose.web.events

import androidx.compose.web.events.SyntheticEvent
import kotlinx.browser.dom.events.Event
import kotlinx.browser.dom.events.EventTarget

class SyntheticChangeEvent<Value, Element : EventTarget> internal constructor(
    val value: Value,
    nativeEvent: Event,
) : SyntheticEvent<Element>(nativeEvent)
