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

import androidx.compose.foundation.AmbientIndication
import androidx.compose.foundation.Indication
import androidx.compose.foundation.Interaction
import androidx.compose.foundation.InteractionState
import androidx.compose.foundation.Strings
import androidx.compose.foundation.indication
import androidx.compose.runtime.Composable
import androidx.compose.runtime.onCommit
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.gesture.pressIndicatorGestureFilter
import androidx.compose.ui.gesture.tapGestureFilter
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.state.ToggleableState.Indeterminate
import androidx.compose.ui.state.ToggleableState.Off
import androidx.compose.ui.state.ToggleableState.On

/**
 * Configure component to make it toggleable via input and accessibility events
 *
 * @sample androidx.compose.foundation.samples.ToggleableSample
 *
 * @see [Modifier.triStateToggleable] if you require support for an indeterminate state.
 *
 * @param value whether Toggleable is on or off
 * @param onValueChange callback to be invoked when toggleable is clicked,
 * therefore the change of the state in requested.
 * @param enabled whether or not this [toggleable] will handle input events and appear
 * enabled for semantics purposes
 * @param interactionState [InteractionState] that will be updated when this toggleable is
 * pressed, using [Interaction.Pressed]
 * @param indication indication to be shown when modified element is pressed. Be default,
 * indication from [AmbientIndication] will be used. Pass `null` to show no indication
 */
// TODO: b/172938345
@Suppress("ComposableModifierFactory")
@Composable
fun Modifier.toggleable(
    value: Boolean,
    enabled: Boolean = true,
    interactionState: InteractionState = remember { InteractionState() },
    indication: Indication? = AmbientIndication.current(),
    onValueChange: (Boolean) -> Unit
) = composed(
    inspectorInfo = debugInspectorInfo {
        name = "toggleable"
        properties["value"] = value
        properties["enabled"] = enabled
        properties["interactionState"] = interactionState
        properties["indication"] = indication
        properties["onValueChange"] = onValueChange
    },
    factory = {
        toggleableImpl(
            state = ToggleableState(value),
            onClick = { onValueChange(!value) },
            enabled = enabled,
            interactionState = interactionState,
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
 * @sample androidx.compose.foundation.samples.TriStateToggleableSample
 *
 * @see [Modifier.toggleable] if you want to support only two states: on and off
 *
 * @param state current value for the component
 * @param onClick will be called when user clicks the toggleable.
 * @param enabled whether or not this [triStateToggleable] will handle input events and
 * appear enabled for semantics purposes
 * @param interactionState [InteractionState] that will be updated when this toggleable is
 * pressed, using [Interaction.Pressed]
 * @param indication indication to be shown when modified element is pressed. Be default,
 * indication from [AmbientIndication] will be used. Pass `null` to show no indication
 */
// TODO: b/172938345
@Suppress("ComposableModifierFactory")
@Composable
fun Modifier.triStateToggleable(
    state: ToggleableState,
    enabled: Boolean = true,
    interactionState: InteractionState = remember { InteractionState() },
    indication: Indication? = AmbientIndication.current(),
    onClick: () -> Unit
) = composed(
    inspectorInfo = debugInspectorInfo {
        name = "triStateToggleable"
        properties["state"] = state
        properties["enabled"] = enabled
        properties["interactionState"] = interactionState
        properties["indication"] = indication
        properties["onClick"] = onClick
    },
    factory = { toggleableImpl(state, enabled, interactionState, indication, onClick) }
)

@Suppress("ModifierInspectorInfo")
private fun Modifier.toggleableImpl(
    state: ToggleableState,
    enabled: Boolean,
    interactionState: InteractionState,
    indication: Indication?,
    onClick: () -> Unit
): Modifier = composed {
    // TODO(pavlis): Handle multiple states for Semantics
    val semantics = Modifier.semantics(mergeDescendants = true) {
        this.stateDescription = when (state) {
            // TODO(ryanmentley): These should be set by Checkbox, Switch, etc.
            On -> Strings.Checked
            Off -> Strings.Unchecked
            Indeterminate -> Strings.Indeterminate
        }
        this.toggleableState = state

        if (enabled) {
            onClick(action = { onClick(); return@onClick true }, label = "Toggle")
        } else {
            disabled()
        }
    }
    val interactionUpdate =
        if (enabled) {
            Modifier.pressIndicatorGestureFilter(
                onStart = { interactionState.addInteraction(Interaction.Pressed, it) },
                onStop = { interactionState.removeInteraction(Interaction.Pressed) },
                onCancel = { interactionState.removeInteraction(Interaction.Pressed) }
            )
        } else {
            Modifier
        }
    val click = if (enabled) Modifier.tapGestureFilter { onClick() } else Modifier

    onCommit(interactionState) {
        onDispose {
            interactionState.removeInteraction(Interaction.Pressed)
        }
    }
    this
        .then(semantics)
        .indication(interactionState, indication)
        .then(interactionUpdate)
        .then(click)
}
