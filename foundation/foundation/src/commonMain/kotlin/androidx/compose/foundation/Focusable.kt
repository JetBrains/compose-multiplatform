/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.foundation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.onCommit
import androidx.compose.runtime.onDispose
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.focus.isFocused
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.semantics.semantics

/**
 * Configure component to be focusable via focus system or accessibility "focus" event.
 *
 * Add this modifier to the element to make it focusable within its bounds.
 *
 * @sample androidx.compose.foundation.samples.FocusableSample
 *
 * @param enabled Controls the enabled state. When `false`, element won't participate in the focus
 * @param interactionState [InteractionState] that will be updated to contain [Interaction.Focused]
 * when this focusable is focused
 */
fun Modifier.focusable(
    enabled: Boolean = true,
    interactionState: InteractionState? = null,
) = composed(
    inspectorInfo = debugInspectorInfo {
        name = "focusable"
        properties["enabled"] = enabled
        properties["interactionState"] = interactionState
    }
) {
    var isFocused by remember { mutableStateOf(false) }
    onDispose {
        interactionState?.removeInteraction(Interaction.Focused)
    }
    onCommit(enabled) {
        if (!enabled) {
            interactionState?.removeInteraction(Interaction.Focused)
        }
    }

    if (enabled) {
        Modifier
            .semantics {
                this.focused = isFocused
            }
            .onFocusChanged {
                isFocused = it.isFocused
                if (isFocused) {
                    interactionState?.addInteraction(Interaction.Focused)
                } else {
                    interactionState?.removeInteraction(Interaction.Focused)
                }
            }
            .focusModifier()
    } else {
        Modifier
    }
}
