/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.foundation.selection

import androidx.compose.foundation.Indication
import androidx.compose.foundation.PressedInteractionSourceDisposableEffect
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.focusableInNonTouchMode
import androidx.compose.foundation.gestures.detectTapAndPress
import androidx.compose.foundation.handlePressInteraction
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.platform.inspectable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState

/**
 * Configure component to make it toggleable via input and accessibility events
 *
 * This version has no [MutableInteractionSource] or [Indication] parameters, default indication from
 * [LocalIndication] will be used. To specify [MutableInteractionSource] or [Indication], use another
 * overload.
 *
 * @sample androidx.compose.foundation.samples.ToggleableSample
 *
 * @see [Modifier.triStateToggleable] if you require support for an indeterminate state.
 *
 * @param value whether Toggleable is on or off
 * @param enabled whether or not this [toggleable] will handle input events and appear
 * enabled for semantics purposes
 * @param role the type of user interface element. Accessibility services might use this
 * to describe the element or do customizations
 * @param onValueChange callback to be invoked when toggleable is clicked,
 * therefore the change of the state in requested.
 */
fun Modifier.toggleable(
    value: Boolean,
    enabled: Boolean = true,
    role: Role? = null,
    onValueChange: (Boolean) -> Unit
) = composed(
    inspectorInfo = debugInspectorInfo {
        name = "toggleable"
        properties["value"] = value
        properties["enabled"] = enabled
        properties["role"] = role
        properties["onValueChange"] = onValueChange
    }
) {
    toggleableImpl(
        state = ToggleableState(value),
        onClick = { onValueChange(!value) },
        enabled = enabled,
        role = role,
        interactionSource = remember { MutableInteractionSource() },
        indication = LocalIndication.current
    )
}

/**
 * Configure component to make it toggleable via input and accessibility events.
 *
 * This version requires both [MutableInteractionSource] and [Indication] to work properly. Use another
 * overload if you don't need these parameters.
 *
 * @sample androidx.compose.foundation.samples.ToggleableSample
 *
 * @see [Modifier.triStateToggleable] if you require support for an indeterminate state.
 *
 * @param value whether Toggleable is on or off
 * @param interactionSource [MutableInteractionSource] that will be used to emit
 * [PressInteraction.Press] when this toggleable is being pressed.
 * @param indication indication to be shown when modified element is pressed. Be default,
 * indication from [LocalIndication] will be used. Pass `null` to show no indication, or
 * current value from [LocalIndication] to show theme default
 * @param enabled whether or not this [toggleable] will handle input events and appear
 * enabled for semantics purposes
 * @param role the type of user interface element. Accessibility services might use this
 * to describe the element or do customizations
 * @param onValueChange callback to be invoked when toggleable is clicked,
 * therefore the change of the state in requested.
 */
fun Modifier.toggleable(
    value: Boolean,
    interactionSource: MutableInteractionSource,
    indication: Indication?,
    enabled: Boolean = true,
    role: Role? = null,
    onValueChange: (Boolean) -> Unit
) = inspectable(
    inspectorInfo = debugInspectorInfo {
        name = "toggleable"
        properties["value"] = value
        properties["interactionSource"] = interactionSource
        properties["indication"] = indication
        properties["enabled"] = enabled
        properties["role"] = role
        properties["onValueChange"] = onValueChange
    },
    factory = {
        toggleableImpl(
            state = ToggleableState(value),
            onClick = { onValueChange(!value) },
            enabled = enabled,
            role = role,
            interactionSource = interactionSource,
            indication = indication
        )
    }
)

/**
 * Configure component to make it toggleable via input and accessibility events with three
 * states: On, Off and Indeterminate.
 *
 * TriStateToggleable should be used when there are dependent Toggleables associated to this
 * component and those can have different values.
 *
 * This version has no [MutableInteractionSource] or [Indication] parameters, default indication
 * from [LocalIndication] will be used. To specify [MutableInteractionSource] or [Indication],
 * use another overload.
 *
 * @sample androidx.compose.foundation.samples.TriStateToggleableSample
 *
 * @see [Modifier.toggleable] if you want to support only two states: on and off
 *
 * @param state current value for the component
 * @param enabled whether or not this [triStateToggleable] will handle input events and
 * appear enabled for semantics purposes
 * @param role the type of user interface element. Accessibility services might use this
 * to describe the element or do customizations
 * @param onClick will be called when user clicks the toggleable.
 */
fun Modifier.triStateToggleable(
    state: ToggleableState,
    enabled: Boolean = true,
    role: Role? = null,
    onClick: () -> Unit
) = composed(
    inspectorInfo = debugInspectorInfo {
        name = "triStateToggleable"
        properties["state"] = state
        properties["enabled"] = enabled
        properties["role"] = role
        properties["onClick"] = onClick
    }
) {
    toggleableImpl(
        state,
        enabled,
        role,
        remember { MutableInteractionSource() },
        LocalIndication.current,
        onClick
    )
}

/**
 * Configure component to make it toggleable via input and accessibility events with three
 * states: On, Off and Indeterminate.
 *
 * TriStateToggleable should be used when there are dependent Toggleables associated to this
 * component and those can have different values.
 *
 * This version requires both [MutableInteractionSource] and [Indication] to work properly. Use another
 * overload if you don't need these parameters.
 *
 * @sample androidx.compose.foundation.samples.TriStateToggleableSample
 *
 * @see [Modifier.toggleable] if you want to support only two states: on and off
 *
 * @param state current value for the component
 * @param interactionSource [MutableInteractionSource] that will be used to emit
 * [PressInteraction.Press] when this triStateToggleable is being pressed.
 * @param indication indication to be shown when modified element is pressed. Be default,
 * indication from [LocalIndication] will be used. Pass `null` to show no indication, or
 * current value from [LocalIndication] to show theme default
 * @param enabled whether or not this [triStateToggleable] will handle input events and
 * appear enabled for semantics purposes
 * @param role the type of user interface element. Accessibility services might use this
 * to describe the element or do customizations
 * @param onClick will be called when user clicks the toggleable.
 */
fun Modifier.triStateToggleable(
    state: ToggleableState,
    interactionSource: MutableInteractionSource,
    indication: Indication?,
    enabled: Boolean = true,
    role: Role? = null,
    onClick: () -> Unit
) = inspectable(
    inspectorInfo = debugInspectorInfo {
        name = "triStateToggleable"
        properties["state"] = state
        properties["enabled"] = enabled
        properties["role"] = role
        properties["interactionSource"] = interactionSource
        properties["indication"] = indication
        properties["onClick"] = onClick
    },
    factory = {
        toggleableImpl(state, enabled, role, interactionSource, indication, onClick)
    }
)

@Suppress("ModifierInspectorInfo")
private fun Modifier.toggleableImpl(
    state: ToggleableState,
    enabled: Boolean,
    role: Role? = null,
    interactionSource: MutableInteractionSource,
    indication: Indication?,
    onClick: () -> Unit
): Modifier = composed {
    val pressedInteraction = remember { mutableStateOf<PressInteraction.Press?>(null) }
    // TODO(pavlis): Handle multiple states for Semantics
    val semantics = Modifier.semantics(mergeDescendants = true) {
        if (role != null) {
            this.role = role
        }
        this.toggleableState = state

        onClick(action = { onClick(); true })
        if (!enabled) {
            disabled()
        }
    }
    val onClickState = rememberUpdatedState(onClick)
    if (enabled) {
        PressedInteractionSourceDisposableEffect(interactionSource, pressedInteraction)
    }
    val gestures = Modifier.pointerInput(interactionSource, enabled) {
        detectTapAndPress(
            onPress = { offset ->
                if (enabled) {
                    handlePressInteraction(offset, interactionSource, pressedInteraction)
                }
            },
            onTap = { if (enabled) onClickState.value.invoke() }
        )
    }
    this
        .then(semantics)
        .indication(interactionSource, indication)
        .hoverable(enabled = enabled, interactionSource = interactionSource)
        .focusableInNonTouchMode(enabled = enabled, interactionSource = interactionSource)
        .then(gestures)
}