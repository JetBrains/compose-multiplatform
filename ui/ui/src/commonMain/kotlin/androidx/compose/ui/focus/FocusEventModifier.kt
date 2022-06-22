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

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collection.MutableVector
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusStateImpl.Active
import androidx.compose.ui.focus.FocusStateImpl.ActiveParent
import androidx.compose.ui.focus.FocusStateImpl.Captured
import androidx.compose.ui.focus.FocusStateImpl.Deactivated
import androidx.compose.ui.focus.FocusStateImpl.DeactivatedParent
import androidx.compose.ui.focus.FocusStateImpl.Inactive
import androidx.compose.ui.modifier.ModifierLocalConsumer
import androidx.compose.ui.modifier.ModifierLocalProvider
import androidx.compose.ui.modifier.ModifierLocalReadScope
import androidx.compose.ui.modifier.ProvidableModifierLocal
import androidx.compose.ui.modifier.modifierLocalOf
import androidx.compose.ui.platform.debugInspectorInfo
import kotlin.jvm.JvmDefaultWithCompatibility

/**
 * A [modifier][Modifier.Element] that can be used to observe focus state events.
 */
@JvmDefaultWithCompatibility
interface FocusEventModifier : Modifier.Element {
    /**
     * A callback that is called whenever the focus system raises events.
     */
    fun onFocusEvent(focusState: FocusState)
}

internal val ModifierLocalFocusEvent = modifierLocalOf<FocusEventModifierLocal?> { null }

internal class FocusEventModifierLocal(
    val onFocusEvent: (FocusState) -> Unit,
) : ModifierLocalProvider<FocusEventModifierLocal?>, ModifierLocalConsumer {
    // parent/children form three of FocusEventModifierLocals
    private var parent: FocusEventModifierLocal? = null
    private val children = mutableVectorOf<FocusEventModifierLocal>()

    /**
     * This is the list of modifiers that contribute to the focus event's state.
     * When there are multiple, all FocusModifier states must be considered when notifying an event.
     */
    private val focusModifiers = mutableVectorOf<FocusModifier>()

    override val key: ProvidableModifierLocal<FocusEventModifierLocal?>
        get() = ModifierLocalFocusEvent
    override val value: FocusEventModifierLocal
        get() = this

    override fun onModifierLocalsUpdated(scope: ModifierLocalReadScope) = with(scope) {
        val newParent = ModifierLocalFocusEvent.current
        if (newParent != parent) {
            parent?.let { parent ->
                parent.children -= this@FocusEventModifierLocal
                parent.removeFocusModifiers(focusModifiers)
            }
            parent = newParent
            if (newParent != null) {
                newParent.children += this@FocusEventModifierLocal
                newParent.addFocusModifiers(focusModifiers)
            }
        }
        parent = ModifierLocalFocusEvent.current
    }

    fun addFocusModifier(focusModifier: FocusModifier) {
        focusModifiers += focusModifier
        parent?.addFocusModifier(focusModifier)
    }

    private fun addFocusModifiers(modifiers: MutableVector<FocusModifier>) {
        focusModifiers.addAll(modifiers)
        parent?.addFocusModifiers(modifiers)
    }

    fun removeFocusModifier(focusModifier: FocusModifier) {
        focusModifiers -= focusModifier
        parent?.removeFocusModifier(focusModifier)
    }

    private fun removeFocusModifiers(modifiers: MutableVector<FocusModifier>) {
        focusModifiers.removeAll(modifiers)
        parent?.removeFocusModifiers(modifiers)
    }

    fun propagateFocusEvent() {
        val notifiedState = when (focusModifiers.size) {
            0 -> Inactive
            1 -> focusModifiers[0].focusState
            else -> {
                // We have multiple children, so we have to recalculate the focus state
                var focusedChild: FocusModifier? = null
                var allChildrenDisabled: Boolean? = null
                focusModifiers.forEach {
                    when (it.focusState) {
                        Active,
                        ActiveParent,
                        Captured,
                        DeactivatedParent -> {
                            focusedChild = it
                            allChildrenDisabled = false
                        }
                        Deactivated -> if (allChildrenDisabled == null) {
                            allChildrenDisabled = true
                        }
                        Inactive -> allChildrenDisabled = false
                    }
                }

                focusedChild?.focusState ?: if (allChildrenDisabled == true) {
                    Deactivated
                } else {
                    Inactive
                }
            }
        }
        onFocusEvent(notifiedState)
        parent?.propagateFocusEvent()
    }

    fun notifyIfNoFocusModifiers() {
        if (focusModifiers.isEmpty()) {
            onFocusEvent(FocusStateImpl.Inactive)
        }
    }
}

/**
 * Add this modifier to a component to observe focus state events.
 */
fun Modifier.onFocusEvent(onFocusEvent: (FocusState) -> Unit): Modifier = composed(
    debugInspectorInfo {
        name = "onFocusEvent"
        properties["onFocusEvent"] = onFocusEvent
    }
) {
    val modifier = remember(onFocusEvent) {
        FocusEventModifierLocal(onFocusEvent = onFocusEvent)
    }

    SideEffect {
        modifier.notifyIfNoFocusModifiers()
    }

    modifier
}
