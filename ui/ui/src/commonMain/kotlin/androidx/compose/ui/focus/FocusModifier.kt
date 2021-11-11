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

package androidx.compose.ui.focus

import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusStateImpl.Inactive
import androidx.compose.ui.modifier.ModifierLocalConsumer
import androidx.compose.ui.modifier.ModifierLocalProvider
import androidx.compose.ui.modifier.ModifierLocalReadScope
import androidx.compose.ui.modifier.ProvidableModifierLocal
import androidx.compose.ui.modifier.modifierLocalOf
import androidx.compose.ui.node.ModifiedFocusNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.InspectorValueInfo
import androidx.compose.ui.platform.NoInspectorInfo
import androidx.compose.ui.platform.debugInspectorInfo

/**
 * A [Modifier.Element] that wraps makes the modifiers on the right into a Focusable. Use a
 * different instance of [FocusModifier] for each focusable component.
 */
internal class FocusModifier(
    initialFocus: FocusStateImpl,
    // TODO(b/172265016): Make this a required parameter and remove the default value.
    //  Set this value in AndroidComposeView, and other places where we create a focus modifier
    //  using this internal constructor.
    inspectorInfo: InspectorInfo.() -> Unit = NoInspectorInfo
) : ModifierLocalConsumer,
    ModifierLocalProvider<FocusProperties>,
    InspectorValueInfo(inspectorInfo) {

    // TODO(b/188684110): Move focusState and focusedChild to ModifiedFocusNode and make this
    //  modifier stateless.
    var focusState: FocusStateImpl = initialFocus
    var focusedChild: ModifiedFocusNode? = null
    var hasFocusListeners: Boolean = false
    lateinit var focusNode: ModifiedFocusNode
    lateinit var modifierLocalReadScope: ModifierLocalReadScope

    // Reading the FocusProperties ModifierLocal.
    override fun onModifierLocalsUpdated(scope: ModifierLocalReadScope) {
        modifierLocalReadScope = scope

        // Update the focus node with the current focus properties.
        with(scope) {
            hasFocusListeners = ModifierLocalHasFocusEventListener.current
            focusNode.setUpdatedProperties(ModifierLocalFocusProperties.current)
        }
    }

    override val key = ModifierLocalFocusProperties

    // Writing the FocusProperties ModifierLocal so that any child focus modifiers don't read
    // properties that were meant for this focus modifier.
    override val value = DefaultFocusProperties
}

/**
 * Add this modifier to a component to make it focusable.
 *
 * Focus state is stored within this modifier. The bounds of this modifier reflect the bounds of
 * the focus box.
 *
 * Note: This is a low level modifier. Before using this consider using
 * [Modifier.focusable()][androidx.compose.foundation.focusable]. It uses a [focusTarget] in
 * its implementation. [Modifier.focusable()][androidx.compose.foundation.focusable] adds semantics
 * that are needed for accessibility.
 *
 * @sample androidx.compose.ui.samples.FocusableSampleUsingLowerLevelFocusTarget
 */
fun Modifier.focusTarget(): Modifier = composed(debugInspectorInfo { name = "focusTarget" }) {
    remember { FocusModifier(Inactive).then(NoFocusListener) }
}

/**
 * Add this modifier to a component to make it focusable.
 */
@Deprecated(
    "Replaced by focusTarget",
    ReplaceWith("focusTarget()", "androidx.compose.ui.focus.focusTarget")
)
fun Modifier.focusModifier(): Modifier = composed(debugInspectorInfo { name = "focusModifier" }) {
    remember { FocusModifier(Inactive).then(NoFocusListener) }
}

/**
 * This modifier local is used as a temporary work-around to improve performance.
 * Instead of sending focus state change events up the hierarchy, we only send it if we have
 * listeners that need this state.
 */
internal val ModifierLocalHasFocusEventListener = modifierLocalOf { false }

/**
 * A modifier that updates the HasFocusEventListener modifier local value to false.
 * This is used after every focusModifier because the listeners that appeared before
 * are not applicable further down the hierarchy.
 */
internal object NoFocusListener : ModifierLocalProvider<Boolean> {
    override val key: ProvidableModifierLocal<Boolean> get() = ModifierLocalHasFocusEventListener
    override val value: Boolean get() = false
}
