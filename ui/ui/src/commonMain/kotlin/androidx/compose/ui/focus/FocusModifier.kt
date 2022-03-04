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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusStateImpl.Inactive
import androidx.compose.ui.input.focus.FocusAwareInputModifier
import androidx.compose.ui.input.rotary.ModifierLocalRotaryScrollParent
import androidx.compose.ui.input.rotary.RotaryScrollEvent
import androidx.compose.ui.modifier.ModifierLocalConsumer
import androidx.compose.ui.modifier.ModifierLocalProvider
import androidx.compose.ui.modifier.ModifierLocalReadScope
import androidx.compose.ui.modifier.ProvidableModifierLocal
import androidx.compose.ui.modifier.modifierLocalOf
import androidx.compose.ui.node.ModifiedFocusNode
import androidx.compose.ui.node.OwnerScope
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
    OwnerScope,
    InspectorValueInfo(inspectorInfo) {
    // TODO(b/188684110): Move focusState and focusedChild to ModifiedFocusNode and make this
    //  modifier stateless.
    var focusState: FocusStateImpl = initialFocus
    var focusedChild: ModifiedFocusNode? = null
    var hasFocusListeners: Boolean = false
    @OptIn(ExperimentalComposeUiApi::class)
    private var rotaryScrollParent: FocusAwareInputModifier<RotaryScrollEvent>? = null
    lateinit var focusNode: ModifiedFocusNode
    lateinit var modifierLocalReadScope: ModifierLocalReadScope
    var focusPropertiesModifier: FocusPropertiesModifier? = null
    val focusProperties: FocusProperties = FocusPropertiesImpl()

    // Reading the FocusProperties ModifierLocal.
    override fun onModifierLocalsUpdated(scope: ModifierLocalReadScope) {
        modifierLocalReadScope = scope

        with(scope) {
            hasFocusListeners = ModifierLocalHasFocusEventListener.current
            @OptIn(ExperimentalComposeUiApi::class)
            rotaryScrollParent = ModifierLocalRotaryScrollParent.current

            // Update the focus node with the current focus properties.
            focusPropertiesModifier = ModifierLocalFocusProperties.current
            refreshFocusProperties()
        }
    }

    @ExperimentalComposeUiApi
    fun propagateRotaryEvent(event: RotaryScrollEvent): Boolean {
        return rotaryScrollParent?.propagateFocusAwareEvent(event) ?: false
    }

    override val isValid: Boolean
        get() = focusNode.isAttached

    companion object {
        val RefreshFocusProperties: (FocusModifier) -> Unit = { focusModifier ->
            focusModifier.refreshFocusProperties()
        }
    }
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
    val focusModifier = remember { FocusModifier(Inactive) }
    focusTarget(focusModifier)
}

/**
 * Add this modifier to a component to make it focusable.
 */
@Deprecated(
    "Replaced by focusTarget",
    ReplaceWith("focusTarget()", "androidx.compose.ui.focus.focusTarget")
)
fun Modifier.focusModifier(): Modifier = composed(debugInspectorInfo { name = "focusModifier" }) {
    val focusModifier = remember { FocusModifier(Inactive) }
    focusTarget(focusModifier)
}

/**
 * A helper function that allows you to pass in an instance of FocusModifier.
 * This is only used internally, to set the root focus modifier or in tests where we need to set an
 * initial focus state or inspect the focus modifier state after running some operation.
 */
internal fun Modifier.focusTarget(focusModifier: FocusModifier): Modifier {
    return this.then(focusModifier).then(ResetFocusModifierLocals)
}

/**
 * This modifier local is used as a temporary work-around to improve performance.
 * Instead of sending focus state change events up the hierarchy, we only send it if we have
 * listeners that need this state.
 */
internal val ModifierLocalHasFocusEventListener = modifierLocalOf { false }

/**
 * The Focus Modifier reads the state of some Modifier Locals that are set by the parents. Consider
 * the following example:
 *
 *     Box(
 *         Modifier
 *             .focusRequester(item1)
 *             .onFocusChanged { ... }
 *             .focusProperties {
 *                 canFocus = false
 *                 next = item2
 *             }
 *             .focusTarget()          // focusModifier1
 *     ) {
 *         Box(
 *             Modifier.focusTarget()  // focusModifier2
 *         )
 *     }
 *
 * Here, the focusRequester, onFocusChanged, and focusProperties modifiers provide
 * modifier local values that are intended for focusModifier1.
 *
 * We don't want these modifier locals to be read by focusModifier2.
 *
 * Add this modifier after every FocusModifier to reset all the focus related modifier locals, so
 * that focus modifiers further down the tree do not read these values.
 */
internal val ResetFocusModifierLocals: Modifier = Modifier
    // Reset the FocusProperties modifier local.
    .then(
        @Suppress("ModifierFactoryExtensionFunction", "ModifierFactoryReturnType")
        object : ModifierLocalProvider<FocusPropertiesModifier?> {
            override val key: ProvidableModifierLocal<FocusPropertiesModifier?>
                get() = ModifierLocalFocusProperties
            override val value: FocusPropertiesModifier?
                get() = null
        }

    )
    // Update the HasFocusEventListener modifier local value to false.
    .then(
        object : ModifierLocalProvider<Boolean> {
            override val key: ProvidableModifierLocal<Boolean>
                get() = ModifierLocalHasFocusEventListener
            override val value: Boolean
                get() = false
        }
    )