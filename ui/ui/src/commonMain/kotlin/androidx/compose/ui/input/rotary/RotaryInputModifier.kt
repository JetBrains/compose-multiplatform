/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.input.rotary

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.focus.FocusAwareEvent
import androidx.compose.ui.input.focus.FocusAwareInputModifier
import androidx.compose.ui.modifier.modifierLocalOf
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.platform.inspectable

/**
 * Adding this [modifier][Modifier] to the [modifier][Modifier] parameter of a component will
 * allow it to intercept [RotaryScrollEvent]s if it (or one of its children) is focused.
 *
 * @param onRotaryScrollEvent This callback is invoked when the user interacts with the
 * rotary side button or the bezel on a wear device. While implementing this callback, return true
 * to stop propagation of this event. If you return false, the event will be sent to this
 * [onRotaryScrollEvent]'s parent.
 *
 * @return true if the event is consumed, false otherwise.
 *
 * Here is an example of a scrollable container that scrolls in response to
 * [RotaryScrollEvent]s.
 * @sample androidx.compose.ui.samples.RotaryEventSample
 *
 * This sample demonstrates how a parent can add an [onRotaryScrollEvent] modifier to gain
 * access to a [RotaryScrollEvent] when a child does not consume it:
 * @sample androidx.compose.ui.samples.PreRotaryEventSample
 */
@ExperimentalComposeUiApi
fun Modifier.onRotaryScrollEvent(
    onRotaryScrollEvent: (RotaryScrollEvent) -> Boolean
): Modifier = inspectable(
    inspectorInfo = debugInspectorInfo {
        name = "onRotaryScrollEvent"
        properties["onRotaryScrollEvent"] = onRotaryScrollEvent
    }
) {
    FocusAwareInputModifier(
        onEvent = onRotaryScrollEvent.focusAwareCallback(),
        onPreEvent = null,
        key = ModifierLocalRotaryScrollParent
    )
}

/**
 * Adding this [modifier][Modifier] to the [modifier][Modifier] parameter of a component will
 * allow it to intercept [RotaryScrollEvent]s if it (or one of its children) is focused.
 *
 * @param onPreRotaryScrollEvent This callback is invoked when the user interacts with the
 * rotary button on a wear device. It gives ancestors of a focused component the chance to
 * intercept a [RotaryScrollEvent].
 *
 * When the user rotates the side button on a wear device, a [RotaryScrollEvent] is sent to
 * the focused item. Before reaching the focused item, this event starts at the root composable,
 * and propagates down the hierarchy towards the focused item. It invokes any
 * [onPreRotaryScrollEvent]s it encounters on ancestors of the focused item. After reaching
 * the focused item, the event propagates up the hierarchy back towards the parent. It invokes any
 * [onRotaryScrollEvent]s it encounters on its way back.
 *
 * Return true to indicate that you consumed the event and want to stop propagation of this event.
 *
 * @return true if the event is consumed, false otherwise.
 *
 * @sample androidx.compose.ui.samples.PreRotaryEventSample
 */
@ExperimentalComposeUiApi
fun Modifier.onPreRotaryScrollEvent(
    onPreRotaryScrollEvent: (RotaryScrollEvent) -> Boolean
): Modifier = inspectable(
    inspectorInfo = debugInspectorInfo {
        name = "onPreRotaryScrollEvent"
        properties["onPreRotaryScrollEvent"] = onPreRotaryScrollEvent
    }
) {
    FocusAwareInputModifier(
        onEvent = null,
        onPreEvent = onPreRotaryScrollEvent.focusAwareCallback(),
        key = ModifierLocalRotaryScrollParent
    )
}

@ExperimentalComposeUiApi
internal val ModifierLocalRotaryScrollParent =
    modifierLocalOf<FocusAwareInputModifier<RotaryScrollEvent>?> { null }

@ExperimentalComposeUiApi
private fun ((RotaryScrollEvent) -> Boolean).focusAwareCallback() = { e: FocusAwareEvent ->
    check(e is RotaryScrollEvent) {
        "FocusAwareEvent is dispatched to the wrong FocusAwareParent."
    }
    invoke(e)
}
