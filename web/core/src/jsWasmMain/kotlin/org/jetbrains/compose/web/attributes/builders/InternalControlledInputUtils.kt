/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.attributes.builders

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.NonRestartableComposable
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.dom.ElementScope
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLTextAreaElement


private val controlledInputsValuesWeakMap: JsWeakMap = js("new WeakMap();").unsafeCast<JsWeakMap>()

internal fun restoreControlledInputState(inputElement: HTMLInputElement) {
    val type = InputType.fromString(inputElement.type)

    if (controlledInputsValuesWeakMap.has(inputElement)) {
        if (type == InputType.Radio) {
            controlledRadioGroups[inputElement.name]?.forEach { radio ->
                radio.checked = controlledInputsValuesWeakMap.get(radio).toString().toBoolean()
            }
            inputElement.checked = controlledInputsValuesWeakMap.get(inputElement).toString().toBoolean()
            return
        }

        if (type == InputType.Checkbox) {
            inputElement.checked = controlledInputsValuesWeakMap.get(inputElement).toString().toBoolean()
        } else {
            inputElement.value = controlledInputsValuesWeakMap.get(inputElement).toString()
        }
    }
}

internal fun restoreControlledTextAreaState(element: HTMLTextAreaElement) {
    if (controlledInputsValuesWeakMap.has(element)) {
        element.value = controlledInputsValuesWeakMap.get(element).toString()
    }
}

internal fun <V : Any> saveControlledInputState(element: HTMLElement, value: V) {
    controlledInputsValuesWeakMap.set(element, value)

    if (element is HTMLInputElement) {
        updateRadioGroupIfNeeded(element)
    }
}

// internal only for testing purposes. It actually should be private.
internal val controlledRadioGroups = mutableMapOf<String, MutableSet<HTMLInputElement>>()

private fun updateRadioGroupIfNeeded(element: HTMLInputElement) {
    if (element.type == "radio" && element.name.isNotEmpty()) {
        if (!controlledRadioGroups.containsKey(element.name)) {
            controlledRadioGroups[element.name] = mutableSetOf()
        }
        controlledRadioGroups[element.name]!!.add(element)
    }
}

@Composable
@NonRestartableComposable
internal fun ElementScope<HTMLInputElement>.DisposeRadioGroupEffect() {
    DisposableEffect(null) {
        val ref = scopeElement
        onDispose {
            controlledRadioGroups[ref.name]?.remove(ref)
            if (controlledRadioGroups[ref.name]?.isEmpty() == true) {
                controlledRadioGroups.remove(ref.name)
            }
        }
    }
}
