/*
 * Copyright 2019 The Android Open Source Project
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
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics

/**
 * Configure component to be selectable, usually as a part of a mutually exclusive group, where
 * only one item can be selected at any point in time. A typical example of mutually exclusive set
 * is a RadioGroup or a row of Tabs.
 *
 * If you want to make an item support on/off capabilities without being part of a set, consider
 * using [Modifier.toggleable]
 *
 * @sample androidx.compose.foundation.samples.SelectableSample
 *
 * @param selected whether or not this item is selected in a mutually exclusion set
 * @param onClick callback to invoke when this item is clicked
 * @param enabled whether or not this [selectable] will handle input events
 * and appear enabled from a semantics perspective
 * @param role the type of user interface element. Accessibility services might use this
 * to describe the element or do customizations
 * @param interactionState [InteractionState] that will be updated when this element is
 * pressed, using [Interaction.Pressed]
 * @param indication indication to be shown when the modified element is pressed. By default,
 * the indication from [AmbientIndication] will be used. Set to `null` to show no indication
 */
// TODO: b/172938345
@Suppress("ComposableModifierFactory")
@Composable
fun Modifier.selectable(
    selected: Boolean,
    enabled: Boolean = true,
    role: Role? = null,
    interactionState: InteractionState = remember { InteractionState() },
    indication: Indication? = AmbientIndication.current(),
    onClick: () -> Unit
) = composed(
    factory = {
        Modifier.clickable(
            enabled = enabled,
            role = role,
            interactionState = interactionState,
            indication = indication,
            onClick = onClick
        ).semantics {
            this.selected = selected
            this.stateDescription = if (selected) Strings.Selected else Strings.NotSelected
        }
    },
    inspectorInfo = debugInspectorInfo {
        name = "selectable"
        properties["selected"] = selected
        properties["enabled"] = enabled
        properties["role"] = role
        properties["interactionState"] = interactionState
        properties["indication"] = indication
        properties["onClick"] = onClick
    }
)
