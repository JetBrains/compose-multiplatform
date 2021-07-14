/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package androidx.compose.web.attributes

import androidx.compose.web.events.SyntheticEvent
import org.jetbrains.compose.web.attributes.AttrsBuilder
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.Options
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget

class SyntheticInputEvent<ValueType, Element : EventTarget>(
    val value: ValueType,
    nativeEvent: Event
) : SyntheticEvent<Element>(
    nativeEvent = nativeEvent
)

class InputAttrsBuilder<T>(
    val inputType: InputType<T>
) : AttrsBuilder<HTMLInputElement>() {

    fun onInput(options: Options = Options.DEFAULT, listener: (SyntheticInputEvent<T, HTMLInputElement>) -> Unit) {
        addEventListener(INPUT, options) {
            val value = inputType.inputValue(it.nativeEvent)
            listener(SyntheticInputEvent(value, it.nativeEvent))
        }
    }
}
