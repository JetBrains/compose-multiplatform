package org.jetbrains.compose.web.events

import androidx.compose.web.events.SyntheticEvent
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event

class SyntheticChangeEvent<Value, Element: HTMLElement>(
    val value: Value,
    nativeEvent: Event,
) : SyntheticEvent<Element>(nativeEvent)
