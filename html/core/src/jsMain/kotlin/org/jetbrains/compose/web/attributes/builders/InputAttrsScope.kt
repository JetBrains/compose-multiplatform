/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.attributes.builders

import androidx.compose.web.events.SyntheticEvent
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.attributes.EventsListenerScope.Companion.BEFOREINPUT
import org.jetbrains.compose.web.attributes.EventsListenerScope.Companion.INPUT
import org.jetbrains.compose.web.attributes.EventsListenerScope.Companion.INVALID
import org.jetbrains.compose.web.events.SyntheticChangeEvent
import org.jetbrains.compose.web.events.SyntheticInputEvent
import org.jetbrains.compose.web.events.SyntheticSelectEvent
import org.w3c.dom.HTMLInputElement

@Deprecated(
    message = "Renamed to InputAttrsScope<T>",
    replaceWith = ReplaceWith("InputAttrsScope", "org.jetbrains.compose.web.attributes.builders.InputAttrsScope")
)
typealias InputAttrsBuilder<T> = InputAttrsScope<T>

/**
 * An extension of [AttrsScope].
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
class InputAttrsScope<ValueType>(
    val inputType: InputType<ValueType>,
    attrsScope: AttrsScope<HTMLInputElement>
) : AttrsScope<HTMLInputElement> by attrsScope {

    fun value(value: String): InputAttrsScope<ValueType> {
        when (inputType) {
            InputType.Checkbox,
            InputType.Radio,
            InputType.Hidden,
            InputType.Submit -> attr("value", value)
            else -> prop(setInputValue, value)
        }
        return this
    }

    fun value(value: Number): InputAttrsScope<ValueType> {
        value(value.toString())
        return this
    }

    fun checked(checked: Boolean): InputAttrsScope<ValueType> {
        prop(setCheckedValue, checked)
        return this
    }

    fun defaultChecked(): InputAttrsScope<ValueType> {
        attr("checked", "")
        return this
    }

    fun defaultValue(value: String): InputAttrsScope<ValueType> {
        attr("value", value)
        return this
    }

    fun defaultValue(value: Number): InputAttrsScope<ValueType> {
        attr("value", value.toString())
        return this
    }

    fun onInvalid(
        listener: (SyntheticEvent<HTMLInputElement>) -> Unit
    ) {
        addEventListener(INVALID, listener)
    }

    fun onInput(
        listener: (SyntheticInputEvent<ValueType, HTMLInputElement>) -> Unit
    ) {
        registerEventListener(InputEventListener(eventName = INPUT, inputType, listener))
    }

    fun onChange(
        listener: (SyntheticChangeEvent<ValueType, HTMLInputElement>) -> Unit
    ) {
        registerEventListener(ChangeEventListener(inputType, listener))
    }

    fun onBeforeInput(
        listener: (SyntheticInputEvent<ValueType, HTMLInputElement>) -> Unit
    ) {
        registerEventListener(InputEventListener(eventName = BEFOREINPUT, inputType, listener))
    }

    fun onSelect(
        listener: (SyntheticSelectEvent<HTMLInputElement>) -> Unit
    ) {
        registerEventListener(SelectEventListener(listener))
    }
}

internal external interface JsWeakMap {
    fun delete(key: Any)
    fun get(key: Any): Any?
    fun has(key: Any): Boolean
    fun set(key: Any, value: Any): JsWeakMap
}
