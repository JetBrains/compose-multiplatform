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

import androidx.compose.ui.FocusModifier
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusState.Active
import androidx.compose.ui.focus.FocusState.Inactive

/**
 * The focus manager is used by different [Owner][androidx.compose.ui.node.Owner] implementations
 * to control focus.
 */
@ExperimentalFocus
class FocusManager {

    private val focusModifier = FocusModifier(Inactive)
    val modifier: Modifier
        get() = focusModifier

    /**
     * The [Owner][androidx.compose.ui.node.Owner] calls this function when it gains focus. This
     * informs the [focus manager][FocusManager] that the [Owner][androidx.compose.ui.node.Owner]
     * gained focus, and that it should propagate this focus to one of the focus modifiers in the
     * component hierarchy.
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
     * informs the [focus manager][FocusManager] that the [Owner][androidx.compose.ui.node.Owner]
     * lost focus, and that it should clear focus from all the focus modifiers in the component
     * hierarchy.
     */
    fun releaseFocus() {
        focusModifier.focusNode.clearFocus(forcedClear = true)
    }

    /**
     * Call this function to set the focus to the root focus modifier.
     *
     * This could be used to clear focus when a user clicks on empty space outside a focusable
     * component.
     */
    fun clearFocus() {
        if (focusModifier.focusNode.clearFocus(forcedClear = false)) {
            focusModifier.focusState = Active
        }
    }
}