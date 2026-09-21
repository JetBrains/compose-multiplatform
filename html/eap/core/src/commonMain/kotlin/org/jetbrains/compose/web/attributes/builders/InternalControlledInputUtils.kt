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
import org.jetbrains.compose.web.internal.WeakElementMap
import org.jetbrains.compose.web.internal.createWeakElementMap
import kotlinx.browser.dom.HTMLElement
import kotlinx.browser.dom.HTMLInputElement
import kotlinx.browser.dom.HTMLTextAreaElement


private val controlledInputsValues: WeakElementMap<HTMLElement, Any> = createWeakElementMap()

internal fun restoreControlledInputState(inputElement: HTMLInputElement) {
    val type = InputType.fromString(inputElement.type)

    if (controlledInputsValues.has(inputElement)) {
        if (type == InputType.Radio) {
            controlledRadioGroups[inputElement.name]?.forEach { radio ->
                radio.checked = controlledInputsValues.get(radio).toString().toBoolean()
            }
            inputElement.checked = controlledInputsValues.get(inputElement).toString().toBoolean()
            return
        }

        if (type == InputType.Checkbox) {
            inputElement.checked = controlledInputsValues.get(inputElement).toString().toBoolean()
        } else {
            inputElement.value = controlledInputsValues.get(inputElement).toString()
        }
    }
}

internal fun restoreControlledTextAreaState(element: HTMLTextAreaElement) {
    if (controlledInputsValues.has(element)) {
        element.value = controlledInputsValues.get(element).toString()
    }
}

internal fun <V : Any> saveControlledInputState(element: HTMLElement, value: V) {
    controlledInputsValues.set(element, value)

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
