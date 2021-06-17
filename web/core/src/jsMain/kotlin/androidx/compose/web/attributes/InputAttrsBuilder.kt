/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package androidx.compose.web.attributes

import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.events.GenericWrappedEvent
import org.jetbrains.compose.web.events.WrappedCheckBoxInputEvent
import org.jetbrains.compose.web.events.WrappedRadioInputEvent
import org.jetbrains.compose.web.events.WrappedTextInputEvent
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event

class SyntheticInputEvent<ValueType, Element : HTMLElement>(
    val value: ValueType,
    val target: Element,
    val nativeEvent: Event
)

class InputAttrsBuilder<T>(val inputType: InputType<T>) : AttrsBuilder<HTMLInputElement>() {

    fun onInput(options: Options = Options.DEFAULT, f: (SyntheticInputEvent<T, HTMLInputElement>) -> Unit) {
        addEventListener(INPUT, options) {
            val value = inputType.inputValue(it.nativeEvent)
            f(SyntheticInputEvent(value, it.nativeEvent.target as HTMLInputElement, it.nativeEvent))
        }
    }

    @Deprecated(
        message = "It's not reliable as it can be applied to any input type.",
        replaceWith = ReplaceWith("onInput(options, listener)"),
        level = DeprecationLevel.WARNING
    )
    fun onTextInput(options: Options = Options.DEFAULT, listener: (WrappedTextInputEvent) -> Unit) {
        listeners.add(TextInputEventListener(options, listener))
    }

    @Deprecated(
        message = "It's not reliable as it can be applied to any input type.",
        replaceWith = ReplaceWith("onInput(options, listener)"),
        level = DeprecationLevel.WARNING
    )
    fun onCheckboxInput(
        options: Options = Options.DEFAULT,
        listener: (WrappedCheckBoxInputEvent) -> Unit
    ) {
        listeners.add(CheckBoxInputEventListener(options, listener))
    }

    @Deprecated(
        message = "It's not reliable as it can be applied to any input type.",
        replaceWith = ReplaceWith("onInput(options, listener)"),
        level = DeprecationLevel.WARNING
    )
    fun onRadioInput(
        options: Options = Options.DEFAULT,
        listener: (WrappedRadioInputEvent) -> Unit
    ) {
        listeners.add(RadioInputEventListener(options, listener))
    }

    @Deprecated(
        message = "It's not reliable as it can be applied to any input type.",
        replaceWith = ReplaceWith("onInput(options, listener)"),
        level = DeprecationLevel.WARNING
    )
    fun onRangeInput(
        options: Options = Options.DEFAULT,
        listener: (GenericWrappedEvent<*>) -> Unit
    ) {
        listeners.add(WrappedEventListener(INPUT, options, listener))
    }
}
