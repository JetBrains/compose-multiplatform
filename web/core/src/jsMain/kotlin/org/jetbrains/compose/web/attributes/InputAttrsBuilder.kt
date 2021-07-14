/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package androidx.compose.web.attributes

import androidx.compose.web.events.SyntheticEvent
import org.jetbrains.compose.web.attributes.AttrsBuilder
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.Options
import org.jetbrains.compose.web.events.SyntheticChangeEvent
import org.jetbrains.compose.web.events.SyntheticSelectEvent
import org.w3c.dom.DataTransfer
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
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

class InputAttrsBuilder<T>(
    val inputType: InputType<T>
) : AttrsBuilder<HTMLInputElement>() {

    fun onInvalid(options: Options = Options.DEFAULT, listener: (SyntheticEvent<HTMLInputElement>) -> Unit) {
        addEventListener(INVALID, options) {
            listener(SyntheticEvent(it.nativeEvent))
        }
    }

    fun onInput(
        options: Options = Options.DEFAULT,
        listener: (SyntheticInputEvent<T, HTMLInputElement>) -> Unit
    ) {
        addEventListener(INPUT, options) {
            val value = inputType.inputValue(it.nativeEvent)
            listener(SyntheticInputEvent(value, it.nativeEvent))
        }
    }

    fun onChange(
        options: Options = Options.DEFAULT,
        listener: (SyntheticChangeEvent<T, HTMLInputElement>) -> Unit
    ) {
        addEventListener(CHANGE, options) {
            val value = inputType.inputValue(it.nativeEvent)
            listener(SyntheticChangeEvent(value, it.nativeEvent))
        }
    }

    fun onBeforeInput(
        options: Options = Options.DEFAULT,
        listener: (SyntheticInputEvent<T, HTMLInputElement>) -> Unit
    ) {
        addEventListener(BEFOREINPUT, options) {
            val value = inputType.inputValue(it.nativeEvent)
            listener(SyntheticInputEvent(value, it.nativeEvent))
        }
    }

    fun onSelect(
        options: Options = Options.DEFAULT,
        listener: (SyntheticSelectEvent<HTMLInputElement>) -> Unit
    ) {
        addEventListener(SELECT, options) {
            listener(SyntheticSelectEvent(it.nativeEvent))
        }
    }
}
