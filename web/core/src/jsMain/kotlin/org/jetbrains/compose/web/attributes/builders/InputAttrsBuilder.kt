/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.attributes.builders

import androidx.compose.web.events.SyntheticEvent
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.events.SyntheticChangeEvent
import org.jetbrains.compose.web.events.SyntheticInputEvent
import org.jetbrains.compose.web.events.SyntheticSelectEvent
import org.w3c.dom.HTMLInputElement

class InputAttrsBuilder<ValueType>(
    val inputType: InputType<ValueType>
) : AttrsBuilder<HTMLInputElement>() {

    fun onInvalid(
        options: Options = Options.DEFAULT,
        listener: (SyntheticEvent<HTMLInputElement>) -> Unit
    ) {
        addEventListener(INVALID, options, listener)
    }

    fun onInput(
        options: Options = Options.DEFAULT,
        listener: (SyntheticInputEvent<ValueType, HTMLInputElement>) -> Unit
    ) {
        listeners.add(InputEventListener(eventName = INPUT, options, inputType, listener))
    }

    fun onChange(
        options: Options = Options.DEFAULT,
        listener: (SyntheticChangeEvent<ValueType, HTMLInputElement>) -> Unit
    ) {
        listeners.add(ChangeEventListener(options, inputType, listener))
    }

    fun onBeforeInput(
        options: Options = Options.DEFAULT,
        listener: (SyntheticInputEvent<ValueType, HTMLInputElement>) -> Unit
    ) {
        listeners.add(InputEventListener(eventName = BEFOREINPUT, options, inputType, listener))
    }

    fun onSelect(
        options: Options = Options.DEFAULT,
        listener: (SyntheticSelectEvent<HTMLInputElement>) -> Unit
    ) {
        listeners.add(SelectEventListener(options, listener))
    }
}
