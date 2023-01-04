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

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusStateImpl.Active
import androidx.compose.ui.focus.FocusStateImpl.ActiveParent
import androidx.compose.ui.focus.FocusStateImpl.Captured
import androidx.compose.ui.focus.FocusStateImpl.Inactive
import androidx.compose.ui.layout.BeyondBoundsLayout
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.modifier.ModifierLocalNode
import androidx.compose.ui.node.Nodes
import androidx.compose.ui.node.ObserverNode
import androidx.compose.ui.node.modifierElementOf
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.node.requireOwner
import androidx.compose.ui.node.visitAncestors

/**
 * This modifier node can be used to create a modifier that makes a component focusable.
 * Use a different instance of [FocusTargetModifierNode] for each focusable component.
 */
@ExperimentalComposeUiApi
class FocusTargetModifierNode : ObserverNode, ModifierLocalNode, Modifier.Node() {
    /**
     * The [FocusState] associated with this [FocusTargetModifierNode].
     */
    val focusState: FocusState
        get() = focusStateImpl

    internal var focusStateImpl = Inactive
    internal val beyondBoundsLayoutParent: BeyondBoundsLayout?
        get() = ModifierLocalBeyondBoundsLayout.current

    override fun onObservedReadsChanged() {
        val previousFocusState = focusState
        invalidateFocus()
        if (previousFocusState != focusState) refreshFocusEventNodes()
    }

    internal fun onRemoved() {
        when (focusState) {
            // Clear focus from the current FocusTarget.
            // This currently clears focus from the entire hierarchy, but we can change the
            // implementation so that focus is sent to the immediate focus parent.
            Active, Captured -> requireOwner().focusOwner.clearFocus(force = true)

            ActiveParent, Inactive -> scheduleInvalidationForFocusEvents()
        }
    }

    internal fun invalidateFocus() {
        when (focusState) {
            // Clear focus from the current FocusTarget.
            // This currently clears focus from the entire hierarchy, but we can change the
            // implementation so that focus is sent to the immediate focus parent.
            Active, Captured -> {
                lateinit var focusProperties: FocusProperties
                observeReads {
                    focusProperties = fetchFocusProperties()
                }
                if (!focusProperties.canFocus) {
                    requireOwner().focusOwner.clearFocus(force = true)
                }
            }

            ActiveParent, Inactive -> {}
        }
    }

    /**
     * Visits parent [FocusPropertiesModifierNode]s and runs
     * [FocusPropertiesModifierNode.modifyFocusProperties] on each parent.
     * This effectively collects an aggregated focus state.
     */
    @ExperimentalComposeUiApi
    internal fun fetchFocusProperties(): FocusProperties {
        val properties = FocusPropertiesImpl()
        visitAncestors(Nodes.FocusProperties or Nodes.FocusTarget) {
            // If we reach the previous default focus properties node, we have gone too far, as
            //  this is applies to the parent focus modifier.
            if (it.isKind(Nodes.FocusTarget)) return properties

            // Parent can override any values set by this
            check(it is FocusPropertiesModifierNode)
            it.modifyFocusProperties(properties)
        }
        return properties
    }

    internal fun scheduleInvalidationForFocusEvents() {
        visitAncestors(Nodes.FocusEvent or Nodes.FocusTarget) {
            if (it.isKind(Nodes.FocusTarget)) return@visitAncestors

            check(it is FocusEventModifierNode)
            requireOwner().focusOwner.scheduleInvalidation(it)
        }
    }

    internal companion object {
        internal val FocusTargetModifierElement = modifierElementOf(
            create = { FocusTargetModifierNode() },
            definitions = { name = "focusTarget" }
        )
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
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.focusTarget(): Modifier = this then FocusTargetModifierNode.FocusTargetModifierElement

/**
 * Add this modifier to a component to make it focusable.
 */
@Deprecated(
    "Replaced by focusTarget",
    ReplaceWith("focusTarget()", "androidx.compose.ui.focus.focusTarget")
)
fun Modifier.focusModifier(): Modifier = focusTarget()
