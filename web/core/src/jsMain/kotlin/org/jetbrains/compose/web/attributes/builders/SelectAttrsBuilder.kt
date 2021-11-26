/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package androidx.compose.web.attributes

import org.jetbrains.compose.web.attributes.AttrsBuilder
import org.jetbrains.compose.web.attributes.EventsListenerBuilder.Companion.CHANGE
import org.jetbrains.compose.web.attributes.EventsListenerBuilder.Companion.INPUT
import org.jetbrains.compose.web.attributes.SyntheticEventListener
import org.jetbrains.compose.web.events.SyntheticChangeEvent
import org.jetbrains.compose.web.events.SyntheticInputEvent
import org.w3c.dom.HTMLSelectElement
import org.w3c.dom.events.Event

class SelectAttrsBuilder : AttrsBuilder<HTMLSelectElement>() {

    fun onInput(
        listener: (SyntheticInputEvent<String?, HTMLSelectElement>) -> Unit
    ) {
        listeners.add(SelectInputEventListener(INPUT, listener))
    }

    fun onChange(
        listener: (SyntheticChangeEvent<String?, HTMLSelectElement>) -> Unit
    ) {
        listeners.add(SelectChangeEventListener(listener))
    }
}

private class SelectInputEventListener(
    eventName: String = INPUT,
    listener: (SyntheticInputEvent<String?, HTMLSelectElement>) -> Unit
) : SyntheticEventListener<SyntheticInputEvent<String?, HTMLSelectElement>>(
    eventName, listener
) {
    override fun handleEvent(event: Event) {
        val value = event.target?.asDynamic().value?.toString()
        listener(SyntheticInputEvent(value, event))
    }
}

private class SelectChangeEventListener(
    listener: (SyntheticChangeEvent<String?, HTMLSelectElement>) -> Unit
): SyntheticEventListener<SyntheticChangeEvent<String?, HTMLSelectElement>>(
    CHANGE, listener
) {
    override fun handleEvent(event: Event) {
        val value = event.target?.asDynamic().value?.toString()
        listener(SyntheticChangeEvent(value, event))
    }
}
