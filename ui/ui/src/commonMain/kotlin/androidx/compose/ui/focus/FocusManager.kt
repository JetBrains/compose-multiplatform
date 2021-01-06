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
import androidx.compose.ui.focus.FocusState.Active
import androidx.compose.ui.focus.FocusState.Inactive
import androidx.compose.ui.gesture.PointerInputModifierImpl
import androidx.compose.ui.gesture.TapGestureFilter

interface FocusManager {
    /**
     * Call this function to clear focus from the currently focused component, and set the focus to
     * the root focus modifier.
     *
     *  @param forcedClear: Whether we should forcefully clear focus regardless of whether we have
     *  any components that have [Captured][FocusState.Captured] focus.
     */
    fun clearFocus(forcedClear: Boolean = false)
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
     * This gesture is fired when the user clicks on a non-clickable / non-focusable part of the
     * screen. Since no other gesture handled this click, we handle it here.
     */
    private val passThroughClickModifier = PointerInputModifierImpl(
        TapGestureFilter().apply {
            onTap = {
                if (focusModifier.focusState == Inactive) {

                    // This view does not have focus, and the user clicked on some non-clickable
                    // part of the screen. This is an indication that the user wants to bring
                    // this view (this app) in focus.
                    focusModifier.focusNode.requestFocus(propagateFocus = false)
                } else {
                    // The user clicked on a non-clickable part of the screen when something was
                    // focused. This is an indication that the user wants to clear focus.
                    clearFocus()
                }
            }
            consumeChanges = false
        }
    )

    /**
     * A [Modifier] that can be added to the [Owners][androidx.compose.ui.node.Owner] modifier
     * list that contains the modifiers required by the focus system. (Eg, a root focus modifier).
     */
    val modifier: Modifier
        // TODO(b/168831247): return an empty Modifier when there are no focusable children.
        get() = passThroughClickModifier
            .then(focusModifier)

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
     * @param forcedClear: Whether we should forcefully clear focus regardless of whether we have
     * any components that have [Captured][FocusState.Captured] focus.
     *
     * This could be used to clear focus when a user clicks on empty space outside a focusable
     * component.
     */
    override fun clearFocus(forcedClear: Boolean) {
        if (focusModifier.focusNode.clearFocus(forcedClear)) {
            focusModifier.focusState = Active
        }
    }
}