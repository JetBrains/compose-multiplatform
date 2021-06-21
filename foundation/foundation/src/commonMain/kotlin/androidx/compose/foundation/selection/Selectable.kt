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

import androidx.compose.foundation.Indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics

/**
 * Configure component to be selectable, usually as a part of a mutually exclusive group, where
 * only one item can be selected at any point in time.
 * A typical example of mutually exclusive set is a RadioGroup or a row of Tabs. To ensure
 * correct accessibility behavior, make sure to pass [Modifier.selectableGroup] modifier into the
 * RadioGroup or the row.
 *
 * If you want to make an item support on/off capabilities without being part of a set, consider
 * using [Modifier.toggleable]
 *
 * This version has no [MutableInteractionSource] or [Indication] parameters, default
 * indication from [LocalIndication] will be used. To specify [MutableInteractionSource] or
 * [Indication], use another overload.
 *
 * @sample androidx.compose.foundation.samples.SelectableSample
 *
 * @param selected whether or not this item is selected in a mutually exclusion set
 * @param enabled whether or not this [selectable] will handle input events
 * and appear enabled from a semantics perspective
 * @param role the type of user interface element. Accessibility services might use this
 * to describe the element or do customizations
 * @param onClick callback to invoke when this item is clicked
 */
fun Modifier.selectable(
    selected: Boolean,
    enabled: Boolean = true,
    role: Role? = null,
    onClick: () -> Unit
) = composed(
    inspectorInfo = debugInspectorInfo {
        name = "selectable"
        properties["selected"] = selected
        properties["enabled"] = enabled
        properties["role"] = role
        properties["onClick"] = onClick
    }
) {
    Modifier.selectable(
        selected = selected,
        enabled = enabled,
        role = role,
        interactionSource = remember { MutableInteractionSource() },
        indication = LocalIndication.current,
        onClick = onClick
    )
}

/**
 * Configure component to be selectable, usually as a part of a mutually exclusive group, where
 * only one item can be selected at any point in time.
 * A typical example of mutually exclusive set is a RadioGroup or a row of Tabs. To ensure
 * correct accessibility behavior, make sure to pass [Modifier.selectableGroup] modifier into the
 * RadioGroup or the row.
 *
 * If you want to make an item support on/off capabilities without being part of a set, consider
 * using [Modifier.toggleable]
 *
 * This version requires both [MutableInteractionSource] and [Indication] to work properly. Use another
 * overload if you don't need these parameters.
 *
 * @sample androidx.compose.foundation.samples.SelectableSample
 *
 * @param selected whether or not this item is selected in a mutually exclusion set
 * @param interactionSource [MutableInteractionSource] that will be used to emit
 * press events when this selectable is being pressed.
 * @param indication indication to be shown when the modified element is pressed. By default,
 * the indication from [LocalIndication] will be used. Set to `null` to show no indication, or
 * current value from [LocalIndication] to show theme default
 * @param enabled whether or not this [selectable] will handle input events
 * and appear enabled from a semantics perspective
 * @param role the type of user interface element. Accessibility services might use this
 * to describe the element or do customizations
 * @param onClick callback to invoke when this item is clicked
 */
// TODO: b/191017532 remove Modifier.composed
@Suppress("UnnecessaryComposedModifier")
fun Modifier.selectable(
    selected: Boolean,
    interactionSource: MutableInteractionSource,
    indication: Indication?,
    enabled: Boolean = true,
    role: Role? = null,
    onClick: () -> Unit
) = composed(
    factory = {
        Modifier.clickable(
            enabled = enabled,
            role = role,
            interactionSource = interactionSource,
            indication = indication,
            onClick = onClick
        ).semantics {
            this.selected = selected
        }
    },
    inspectorInfo = debugInspectorInfo {
        name = "selectable"
        properties["selected"] = selected
        properties["enabled"] = enabled
        properties["role"] = role
        properties["interactionSource"] = interactionSource
        properties["indication"] = indication
        properties["onClick"] = onClick
    }
)