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

/**
 * An extension of [AttrsBuilder].
 * This class provides a set of methods specific for [Input] element:
 *
 * [value] - sets the current input's value.
 * [defaultValue] - sets the default input's value.
 *
 * [checked] - sets the current checked/unchecked state of a checkbox or a radio.
 * [defaultChecked] - sets the default checked state of a checkbox or a radio.
 *
 * [onInvalid] - adds invalid` event listener
 * [onInput] - adds `input` event listener
 * [onChange] - adds `change` event listener
 * [onBeforeInput] - add `beforeinput` event listener
 * [onSelect] - add `select` event listener
 */
class InputAttrsBuilder<ValueType>(
    val inputType: InputType<ValueType>
) : AttrsBuilder<HTMLInputElement>() {

    fun value(value: String): InputAttrsBuilder<ValueType> {
        when (inputType) {
            InputType.Checkbox,
            InputType.Radio,
            InputType.Hidden,
            InputType.Submit -> attr("value", value)
            else -> prop(setInputValue, value)
        }
        return this
    }

    fun value(value: Number): InputAttrsBuilder<ValueType> {
        value(value.toString())
        return this
    }

    fun checked(checked: Boolean): InputAttrsBuilder<ValueType> {
        prop(setCheckedValue, checked)
        return this
    }

    fun defaultChecked(): InputAttrsBuilder<ValueType> {
        attr("checked", "")
        return this
    }

    fun defaultValue(value: String): InputAttrsBuilder<ValueType> {
        attr("value", value)
        return this
    }

    fun defaultValue(value: Number): InputAttrsBuilder<ValueType> {
        attr("value", value.toString())
        return this
    }

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

internal external interface JsWeakMap {
    fun delete(key: Any)
    fun get(key: Any): Any?
    fun has(key: Any): Boolean
    fun set(key: Any, value: Any): JsWeakMap
}


