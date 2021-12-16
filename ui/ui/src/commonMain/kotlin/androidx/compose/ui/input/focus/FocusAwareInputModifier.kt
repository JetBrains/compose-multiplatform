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

package androidx.compose.ui.input.focus

import androidx.compose.ui.modifier.ModifierLocalConsumer
import androidx.compose.ui.modifier.ModifierLocalProvider
import androidx.compose.ui.modifier.ModifierLocalReadScope
import androidx.compose.ui.modifier.ProvidableModifierLocal

internal interface FocusAwareEvent

/**
 * A modifier that routes [FocusAwareEvent]s to the currently focused item.
 *
 * The event is routed to the focused item. Before reaching the focused item, [onPreEvent]() is
 * called for parents of the focused item. If the parents don't consume the event, [onPreEvent]()
 * is called for the focused item. If the event is still not consumed, [onEvent]() is called on the
 * focused item's parents.
 */
internal open class FocusAwareInputModifier<T : FocusAwareEvent>(
    val onEvent: ((FocusAwareEvent) -> Boolean)?,
    val onPreEvent: ((FocusAwareEvent) -> Boolean)?,
    override val key: ProvidableModifierLocal<FocusAwareInputModifier<T>?>
) : ModifierLocalConsumer,
    ModifierLocalProvider<FocusAwareInputModifier<T>?> {

    // The focus-aware modifier that is a parent of this modifier.
    private var focusAwareEventParent: FocusAwareInputModifier<T>? = null
    override fun onModifierLocalsUpdated(scope: ModifierLocalReadScope) {
        focusAwareEventParent = with(scope) { key.current }
    }
    // Register this modifier as the FocusAwareParent for modifiers further down the hierarchy.
    override val value: FocusAwareInputModifier<T>
        get() = this

    fun propagateFocusAwareEvent(event: T) = propagatePreEvent(event) || propagateEvent(event)

    private fun propagatePreEvent(event: T): Boolean {
        // We first propagate the event to the parent.
        if (focusAwareEventParent?.propagatePreEvent(event) == true) return true

        // If none of the parents consume the event, we attempt to consume it.
        return onPreEvent?.invoke(event) ?: false
    }

    private fun propagateEvent(event: T): Boolean {
        // We attempt to consume the key event first.
        if (onEvent?.invoke(event) == true) return true

        // If the event is not consumed, we propagate it to the parent.
        return focusAwareEventParent?.propagateEvent(event) ?: false
    }
}
