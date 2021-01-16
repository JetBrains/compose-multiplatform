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

import androidx.compose.ui.unit.LayoutDirection.Ltr
import androidx.compose.ui.unit.LayoutDirection.Rtl
import androidx.compose.ui.focus.FocusDirection.Down
import androidx.compose.ui.focus.FocusDirection.Left
import androidx.compose.ui.focus.FocusDirection.Next
import androidx.compose.ui.focus.FocusDirection.Previous
import androidx.compose.ui.focus.FocusDirection.Right
import androidx.compose.ui.focus.FocusDirection.Up
import androidx.compose.ui.focus.FocusRequester.Companion.Default
import androidx.compose.ui.node.ModifiedFocusNode

/**
 * This enum specifies the direction of the requested focus change.
 */
enum class FocusDirection { Next, Previous, Left, Right, Up, Down }

/**
 * Moves focus based on the requested focus direction.
 *
 * @param focusDirection The requested direction to move focus.
 * @return whether focus was moved or not.
 */
internal fun ModifiedFocusNode.moveFocus(focusDirection: FocusDirection): Boolean {
    val activeNode = findActiveFocusNode()

    // If there is no active node in this sub-hierarchy, we can't move focus.
    if (activeNode == null) {
        return false
    }

    // TODO(b/175899779) If the direction is "Next", cache the current node so we can come back
    //  to the same place if the user requests "Previous"

    // Check if a custom focus traversal order is specified.
    val nextFocusRequester = activeNode.customFocusSearch(focusDirection)
    if (nextFocusRequester != Default) {
        // TODO(b/175899786): We ideally need to check if the nextFocusRequester points to something
        //  that is visible and focusable in the current mode (Touch/Non-Touch mode).
        nextFocusRequester.requestFocus()
        return true
    }

    // If no custom focus traversal order is specified, perform a search for the appropriate item
    // to move focus to.
    return when (focusDirection) {
        Next, Previous -> {
            // TODO(b/170155659): Perform one dimensional focus search.
            false
        }
        Left, Right, Up, Down -> {
            // TODO(b/170155926): Perform two dimensional focus search.
            false
        }
    }
}

/**
 * Search up the component tree for any parent/parents that have specified a custom focus order.
 * Allowing parents higher up the hierarchy to overwrite the focus order specified by their
 * children.
 */
private fun ModifiedFocusNode.customFocusSearch(focusDirection: FocusDirection): FocusRequester {
    val focusOrder = FocusOrder()
    wrappedBy?.populateFocusOrder(focusOrder)

    // TODO(b/176847718): Pass the layout direction as a parameter to customFocusSearch, and use
    //  that instead of this hardcoded value.
    val layoutDirection = Ltr

    return when (focusDirection) {
        Next -> focusOrder.next
        Previous -> focusOrder.previous
        Up -> focusOrder.up
        Down -> focusOrder.down
        Left -> when (layoutDirection) {
            Ltr -> focusOrder.start
            Rtl -> focusOrder.end
        }.takeUnless { it == Default } ?: focusOrder.left
        Right -> when (layoutDirection) {
            Ltr -> focusOrder.end
            Rtl -> focusOrder.start
        }.takeUnless { it == Default } ?: focusOrder.right
    }
}
