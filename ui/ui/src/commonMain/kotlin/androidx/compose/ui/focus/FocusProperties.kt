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

package androidx.compose.ui.focus

import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.modifier.ModifierLocalConsumer
import androidx.compose.ui.modifier.ModifierLocalProvider
import androidx.compose.ui.modifier.ModifierLocalReadScope
import androidx.compose.ui.modifier.modifierLocalOf
import androidx.compose.ui.node.ModifiedFocusNode
import androidx.compose.ui.platform.debugInspectorInfo

/**
 * A Modifier local that stores [FocusProperties] for a sub-hierarchy.
 *
 * @see [focusProperties]
 */
internal val ModifierLocalFocusProperties = modifierLocalOf { defaultFocusProperties }

internal val defaultFocusProperties: FocusProperties = FocusPropertiesImpl(canFocus = true)

/**
 * Properties that are applied to [focusTarget]s that can read the [ModifierLocalFocusProperties]
 * Modifier Local.
 *
 * @see [focusProperties]
 */
interface FocusProperties {
    /**
     * When set to false, indicates that the [focusTarget] that this is applied to can no longer
     * take focus. If the [focusTarget] is currently focused, setting this property to false will
     * end up clearing focus.
     */
    var canFocus: Boolean
}

/**
 * This modifier allows you to specify properties that are accessible to [focusTarget]s further
 * down the modifier chain or on child layout nodes.
 *
 * @sample androidx.compose.ui.samples.FocusPropertiesSample
 */
fun Modifier.focusProperties(scope: FocusProperties.() -> Unit): Modifier = composed(
    debugInspectorInfo {
        name = "focusProperties"
        properties["scope"] = scope
    }
) {
    val rememberedScope by rememberUpdatedState(scope)
    remember { FocusPropertiesModifier(focusPropertiesScope = rememberedScope) }
}

internal class FocusPropertiesModifier(
    val focusPropertiesScope: FocusProperties.() -> Unit
) : ModifierLocalConsumer, ModifierLocalProvider<FocusProperties> {

    private var parentFocusProperties: FocusProperties? = null

    override fun onModifierLocalsUpdated(scope: ModifierLocalReadScope) {
        parentFocusProperties = scope.run { ModifierLocalFocusProperties.current }
    }

    override val key = ModifierLocalFocusProperties

    override val value: FocusProperties
        get() = defaultFocusProperties.copy {
            // Populate with the specified focus properties.
            apply(focusPropertiesScope)

            // current value for deactivated can be overridden by a parent's value.
            parentFocusProperties?.let {
                if (it != defaultFocusProperties) {
                    canFocus = it.canFocus
                }
            }
        }
}

internal fun ModifiedFocusNode.setUpdatedProperties(properties: FocusProperties) {
    if (properties.canFocus) activateNode() else deactivateNode()
}

private class FocusPropertiesImpl(override var canFocus: Boolean) : FocusProperties

private fun FocusProperties.copy(scope: FocusProperties.() -> Unit): FocusProperties {
    return FocusPropertiesImpl(canFocus = canFocus).apply(scope)
}