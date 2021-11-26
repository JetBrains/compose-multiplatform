/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.attributes.builders

import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.events.SyntheticChangeEvent
import org.jetbrains.compose.web.events.SyntheticSelectEvent
import org.jetbrains.compose.web.events.SyntheticInputEvent
import org.w3c.dom.HTMLTextAreaElement

class TextAreaAttrsBuilder : AttrsBuilder<HTMLTextAreaElement>() {

    fun value(value: String): AttrsBuilder<HTMLTextAreaElement> {
        prop(setInputValue, value)
        return this
    }

    fun defaultValue(value: String): AttrsBuilder<HTMLTextAreaElement> {
        prop(setTextAreaDefaultValue, value)
        return this
    }

    fun onInput(
        listener: (SyntheticInputEvent<String, HTMLTextAreaElement>) -> Unit
    ) {
        listeners.add(InputEventListener(INPUT, InputType.Text, listener))
    }

    fun onChange(
        listener: (SyntheticChangeEvent<String, HTMLTextAreaElement>) -> Unit
    ) {
        listeners.add(ChangeEventListener(InputType.Text, listener))
    }

    fun onBeforeInput(
        listener: (SyntheticInputEvent<String, HTMLTextAreaElement>) -> Unit
    ) {
        listeners.add(InputEventListener(BEFOREINPUT, InputType.Text, listener))
    }

    fun onSelect(
        listener: (SyntheticSelectEvent<HTMLTextAreaElement>) -> Unit
    ) {
        listeners.add(SelectEventListener(listener))
    }
}
