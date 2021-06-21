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

import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusStateImpl.Active
import androidx.compose.ui.focus.FocusStateImpl.ActiveParent
import androidx.compose.ui.focus.FocusStateImpl.Captured
import androidx.compose.ui.focus.FocusStateImpl.Disabled
import androidx.compose.ui.focus.FocusStateImpl.Inactive

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
        return focusModifier.focusNode.moveFocus(focusDirection)
    }
}
