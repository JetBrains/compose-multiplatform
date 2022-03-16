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
import androidx.compose.ui.focus.FocusDirection.Companion.Next
import androidx.compose.ui.focus.FocusDirection.Companion.Previous
import androidx.compose.ui.focus.FocusStateImpl.Active
import androidx.compose.ui.focus.FocusStateImpl.ActiveParent
import androidx.compose.ui.focus.FocusStateImpl.Captured
import androidx.compose.ui.focus.FocusStateImpl.Deactivated
import androidx.compose.ui.focus.FocusStateImpl.DeactivatedParent
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
     * [Modifier.focusProperties()][focusProperties].
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
    // TODO(b/168831247): return an empty Modifier when there are no focusable children.
    val modifier: Modifier = Modifier.focusTarget(focusModifier)

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
        focusModifier.clearFocus(forcedClear = true)
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
        // focus. So after clearing focus within the compose hierarchy, we should restore focus to
        // the root focus modifier to maintain consistency with the host view.
        val rootInitialState = focusModifier.focusState
        if (focusModifier.clearFocus(force)) {
            focusModifier.focusState = when (rootInitialState) {
                Active, ActiveParent, Captured -> Active
                Deactivated, DeactivatedParent -> Deactivated
                Inactive -> Inactive
            }
        }
    }

    /**
     * Moves focus in the specified direction.
     *
     * @return true if focus was moved successfully. false if the focused item is unchanged.
     */
    override fun moveFocus(focusDirection: FocusDirection): Boolean {

        // If there is no active node in this sub-hierarchy, we can't move focus.
        val source = focusModifier.findActiveFocusNode() ?: return false

        // Check if a custom focus traversal order is specified.
        val nextFocusRequester = source.customFocusSearch(focusDirection, layoutDirection)
        if (nextFocusRequester != FocusRequester.Default) {
            // TODO(b/175899786): We ideally need to check if the nextFocusRequester points to something
            //  that is visible and focusable in the current mode (Touch/Non-Touch mode).
            nextFocusRequester.requestFocus()
            return true
        }

        return focusModifier.focusSearch(focusDirection, layoutDirection) { destination ->
            if (destination == source) return@focusSearch false
            checkNotNull(destination.parent) { "Move focus landed at the root." }
            // If we found a potential next item, move focus to it.
            destination.requestFocus()
            true
        }.let { foundNextItem -> foundNextItem || wrapAroundFocus(focusDirection) }
    }

    /**
     * Runs the focus properties block for all [focusProperties] modifiers to fetch updated
     * [FocusProperties].
     *
     * The [focusProperties] block is run automatically whenever the properties change, and you
     * rarely need to invoke this function manually. However, if you have a situation where you want
     * to change a property, and need to see the change in the current snapshot, use this API.
     */
    fun fetchUpdatedFocusProperties() {
        focusModifier.updateProperties()
    }

    /**
     * Searches for the currently focused item.
     *
     * @return the currently focused item.
     */
    @Suppress("ModifierFactoryExtensionFunction", "ModifierFactoryReturnType")
    internal fun getActiveFocusModifier(): FocusModifier? {
        return focusModifier.findActiveItem()
    }

    // TODO(b/144116848): This is a hack to make Next/Previous wrap around. This must be
    //  replaced by code that sends the move request back to the view system. The view system
    //  will then pass focus to other views, and ultimately return back to this compose view.
    private fun wrapAroundFocus(focusDirection: FocusDirection): Boolean {
        // Wrap is not supported when this sub-hierarchy doesn't have focus.
        if (!focusModifier.focusState.hasFocus || focusModifier.focusState.isFocused) return false

        // Next and Previous wraps around.
        when (focusDirection) {
            Next, Previous -> {
                // Clear Focus to send focus the root node.
                clearFocus(force = false)
                if (!focusModifier.focusState.isFocused) return false

                // Wrap around by calling moveFocus after the root gains focus.
                return moveFocus(focusDirection)
            }
            // We only wrap-around for 1D Focus search.
            else -> return false
        }
    }
}

private fun FocusModifier.updateProperties() {
    // Update the focus node with the current focus properties.
    refreshFocusProperties()

    // Update the focus properties for all children.
    children.forEach { it.updateProperties() }
}

/**
 * Find the focus modifier in this sub-hierarchy that is currently focused.
 */
@Suppress("ModifierFactoryExtensionFunction", "ModifierFactoryReturnType")
private fun FocusModifier.findActiveItem(): FocusModifier? {
    return when (focusState) {
        Active, Captured -> this
        ActiveParent, DeactivatedParent -> {
            focusedChild?.findActiveItem() ?: error("no child")
        }
        Deactivated, Inactive -> null
    }
}
