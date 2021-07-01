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
import androidx.compose.ui.focus.FocusDirection.Companion.Next
import androidx.compose.ui.focus.FocusDirection.Companion.Out
import androidx.compose.ui.focus.FocusDirection.Companion.Previous
import androidx.compose.ui.focus.FocusStateImpl.Active
import androidx.compose.ui.focus.FocusStateImpl.ActiveParent
import androidx.compose.ui.focus.FocusStateImpl.Captured
import androidx.compose.ui.focus.FocusStateImpl.Disabled
import androidx.compose.ui.focus.FocusStateImpl.Inactive
import androidx.compose.ui.unit.LayoutDirection

interface FocusManager {
    /**
     * Call this function to clear focus from the currently focused component, and set the focus to
     * the root focus modifier.
     *
     *  @param force: Whether we should forcefully clear focus regardless of whether we have
     *  any components that have Captured focus.
     *
     *  @sample androidx.compose.ui.samples.ClearFocusSample
     */
    fun clearFocus(force: Boolean = false)

    /**
     * Moves focus in the specified [direction][FocusDirection].
     *
     * If you are not satisfied with the default focus order, consider setting a custom order using
     * [Modifier.focusOrder()][focusOrder].
     *
     * @return true if focus was moved successfully. false if the focused item is unchanged.
     *
     * @sample androidx.compose.ui.samples.MoveFocusSample
     */
    fun moveFocus(focusDirection: FocusDirection): Boolean
}

/**
 * The focus manager is used by different [Owner][androidx.compose.ui.node.Owner] implementations
 * to control focus.
 *
 * @param focusModifier The modifier that will be used as the root focus modifier.
 */
internal class FocusManagerImpl(
    private val focusModifier: FocusModifier = FocusModifier(Inactive)
) : FocusManager {

    /**
     * A [Modifier] that can be added to the [Owners][androidx.compose.ui.node.Owner] modifier
     * list that contains the modifiers required by the focus system. (Eg, a root focus modifier).
     */
    val modifier: Modifier
        // TODO(b/168831247): return an empty Modifier when there are no focusable children.
        get() = focusModifier

    lateinit var layoutDirection: LayoutDirection

    /**
     * The [Owner][androidx.compose.ui.node.Owner] calls this function when it gains focus. This
     * informs the [focus manager][FocusManagerImpl] that the
     * [Owner][androidx.compose.ui.node.Owner] gained focus, and that it should propagate this
     * focus to one of the focus modifiers in the component hierarchy.
     */
    fun takeFocus() {
        // If the focus state is not Inactive, it indicates that the focus state is already
        // set (possibly by dispatchWindowFocusChanged). So we don't update the state.
        if (focusModifier.focusState == Inactive) {
            focusModifier.focusState = Active
            // TODO(b/152535715): propagate focus to children based on child focusability.
        }
    }

    /**
     * The [Owner][androidx.compose.ui.node.Owner] calls this function when it loses focus. This
     * informs the [focus manager][FocusManagerImpl] that the
     * [Owner][androidx.compose.ui.node.Owner] lost focus, and that it should clear focus from
     * all the focus modifiers in the component hierarchy.
     */
    fun releaseFocus() {
        focusModifier.focusNode.clearFocus(forcedClear = true)
    }

    /**
     * Call this function to set the focus to the root focus modifier.
     *
     * @param force: Whether we should forcefully clear focus regardless of whether we have
     * any components that have captured focus.
     *
     * This could be used to clear focus when a user clicks on empty space outside a focusable
     * component.
     */
    override fun clearFocus(force: Boolean) {
        // If this hierarchy had focus before clearing it, it indicates that the host view has
        // focus. So after clearing focus within the compose hierarchy, we should reset the root
        // focus modifier to "Active" to maintain consistency with the host view.
        val rootWasFocused = when (focusModifier.focusState) {
            Active, ActiveParent, Captured -> true
            Disabled, Inactive -> false
        }

        if (focusModifier.focusNode.clearFocus(force) && rootWasFocused) {
            focusModifier.focusState = Active
        }
    }

    /**
     * Moves focus in the specified direction.
     *
     * Focus moving is still being implemented. Right now, focus will move only if the user
     * specified a custom focus traversal order for the item that is currently focused. (Using the
     * [Modifier.focusOrder()][focusOrder] API).
     *
     * @return true if focus was moved successfully. false if the focused item is unchanged.
     */
    override fun moveFocus(focusDirection: FocusDirection): Boolean {

        // If there is no active node in this sub-hierarchy, we can't move focus.
        val source = focusModifier.focusNode.findActiveFocusNode() ?: return false

        // Check if a custom focus traversal order is specified.
        val nextFocusRequester = source.customFocusSearch(focusDirection, layoutDirection)
        if (nextFocusRequester != FocusRequester.Default) {
            // TODO(b/175899786): We ideally need to check if the nextFocusRequester points to something
            //  that is visible and focusable in the current mode (Touch/Non-Touch mode).
            nextFocusRequester.requestFocus()
            return true
        }

        val destination = focusModifier.focusNode.focusSearch(focusDirection, layoutDirection)
            ?: return false

        // We don't want moveFocus to set focus to the root, as this would essentially clear focus.
        if (destination.findParentFocusNode() == null) {
            when (focusDirection) {
                // Skip the root and proceed to the next/previous item from the root's perspective.
                // TODO(b/170155659): Add tests after implementing one dimensional focus search.
                Next, Previous -> moveFocus(focusDirection)

                // Instead of moving out to the root, we return false.
                // When we return false the key event will not be consumed, but it will bubble
                // up to the owner. (In the case of Android, the back key will be sent to the
                // activity, where it can be handled appropriately).
                @OptIn(ExperimentalComposeUiApi::class)
                Out -> return false
                else -> error("Move focus landed at the root through an unknown path.")
            }
        }

        // If we found a potential next item, call requestFocus() to move focus to it.
        destination.requestFocus(propagateFocus = false)
        return true
    }
}
