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
import androidx.compose.ui.focus.FocusDirection.Companion.Down
import androidx.compose.ui.focus.FocusDirection.Companion.In
import androidx.compose.ui.focus.FocusDirection.Companion.Left
import androidx.compose.ui.focus.FocusDirection.Companion.Next
import androidx.compose.ui.focus.FocusDirection.Companion.Out
import androidx.compose.ui.focus.FocusDirection.Companion.Previous
import androidx.compose.ui.focus.FocusDirection.Companion.Right
import androidx.compose.ui.focus.FocusDirection.Companion.Up
import androidx.compose.ui.focus.FocusRequester.Companion.Default
import androidx.compose.ui.focus.FocusStateImpl.Active
import androidx.compose.ui.focus.FocusStateImpl.ActiveParent
import androidx.compose.ui.focus.FocusStateImpl.Captured
import androidx.compose.ui.focus.FocusStateImpl.Disabled
import androidx.compose.ui.focus.FocusStateImpl.Inactive
import androidx.compose.ui.node.ModifiedFocusNode
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.LayoutDirection.Ltr
import androidx.compose.ui.unit.LayoutDirection.Rtl

private const val invalidFocusDirection = "Invalid FocusDirection"

/**
 * The [FocusDirection] is used to specify direction when a focus change request is made via
 * [moveFocus].
 *
 * @sample androidx.compose.ui.samples.MoveFocusSample
 */
@Suppress("INLINE_CLASS_DEPRECATED", "EXPERIMENTAL_FEATURE_WARNING")
inline class FocusDirection(val value: Int) {

    override fun toString(): String {
        return when (this) {
            Next -> "Next"
            Previous -> "Previous"
            Left -> "Left"
            Right -> "Right"
            Up -> "Up"
            Down -> "Down"
            @OptIn(ExperimentalComposeUiApi::class)
            In -> "In"
            @OptIn(ExperimentalComposeUiApi::class)
            Out -> "Out"
            else -> invalidFocusDirection
        }
    }

    companion object {
        /**
         *  Direction used in [moveFocus] to indicate that you are searching for the next
         *  focusable item.
         */
        val Next: FocusDirection = FocusDirection(1)

        /**
         *  Direction used in [moveFocus] to indicate that you are searching for the previous
         *  focusable item.
         */
        val Previous: FocusDirection = FocusDirection(2)

        /**
         *  Direction used in [moveFocus] to indicate that you are searching for the next
         *  focusable item to the left of the currently focused item.
         */
        val Left: FocusDirection = FocusDirection(3)

        /**
         *  Direction used in [moveFocus] to indicate that you are searching for the next
         *  focusable item to the right of the currently focused item.
         */
        val Right: FocusDirection = FocusDirection(4)

        /**
         *  Direction used in [moveFocus] to indicate that you are searching for the next
         *  focusable item that is above the currently focused item.
         */
        val Up: FocusDirection = FocusDirection(5)

        /**
         *  Direction used in [moveFocus] to indicate that you are searching for the next
         *  focusable item that is below the currently focused item.
         */
        val Down: FocusDirection = FocusDirection(6)

        /**
         *  Direction used in [moveFocus] to indicate that you are searching for the next
         *  focusable item that is a child of the currently focused item.
         */
        @ExperimentalComposeUiApi
        val In: FocusDirection = FocusDirection(7)

        /**
         *  Direction used in [moveFocus] to indicate that you want to move focus to the parent
         *  of the currently focused item.
         */
        @ExperimentalComposeUiApi
        val Out: FocusDirection = FocusDirection(8)
    }
}

/**
 * Moves focus based on the requested focus direction.
 *
 * @param focusDirection The requested direction to move focus.
 * @return whether a focus node was found. If a focus node was found and the focus request was
 * not granted, this function still returns true.
 */
internal fun ModifiedFocusNode.moveFocus(focusDirection: FocusDirection): Boolean {
    // TODO(b/176847718): Pass the layout direction as a parameter instead of this hardcoded value.
    val layoutDirection: LayoutDirection = Ltr

    // If there is no active node in this sub-hierarchy, we can't move focus.
    val activeNode = findActiveFocusNode() ?: return false

    // TODO(b/175899779) If the direction is "Next", cache the current node so we can come back
    //  to the same place if the user requests "Previous"

    // Check if a custom focus traversal order is specified.
    val nextFocusRequester = activeNode.customFocusSearch(focusDirection, layoutDirection)
    if (nextFocusRequester != Default) {
        // TODO(b/175899786): We ideally need to check if the nextFocusRequester points to something
        //  that is visible and focusable in the current mode (Touch/Non-Touch mode).
        nextFocusRequester.requestFocus()
        return true
    }

    // If no custom focus traversal order is specified, perform a search for the appropriate item
    // to move focus to.
    val nextNode = when (focusDirection) {
        Next, Previous -> null // TODO(b/170155659): Perform one dimensional focus search.
        Left, Right, Up, Down -> twoDimensionalFocusSearch(focusDirection)
        @OptIn(ExperimentalComposeUiApi::class)
        In -> {
            // we search among the children of the active item.
            val direction = when (layoutDirection) { Rtl -> Left; Ltr -> Right }
            activeNode.twoDimensionalFocusSearch(direction)
        }
        @OptIn(ExperimentalComposeUiApi::class)
        Out -> activeNode.findParentFocusNode()
        else -> error(invalidFocusDirection)
    } ?: return false

    // If we found a potential next item, call requestFocus() to move focus to it.
    nextNode.requestFocus(propagateFocus = false)
    return true
}

internal fun ModifiedFocusNode.findActiveFocusNode(): ModifiedFocusNode? {
    return when (focusState) {
        Active, Captured -> this
        ActiveParent -> focusedChild?.findActiveFocusNode()
        Inactive, Disabled -> null
    }
}

/**
 * Search up the component tree for any parent/parents that have specified a custom focus order.
 * Allowing parents higher up the hierarchy to overwrite the focus order specified by their
 * children.
 */
private fun ModifiedFocusNode.customFocusSearch(
    focusDirection: FocusDirection,
    layoutDirection: LayoutDirection
): FocusRequester {
    val focusOrder = FocusOrder()
    wrappedBy?.populateFocusOrder(focusOrder)

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
        // TODO(b/183746982): add focus order API for "In" and "Out".
        //  Developers can to specify a custom "In" to specify which child should be visited when
        //  the user presses dPad center. (They can also redirect the "In" to some other item).
        //  Developers can specify a custom "Out" to specify which composable should take focus
        //  when the user presses the back button.
        @OptIn(ExperimentalComposeUiApi::class)
        In -> Default
        @OptIn(ExperimentalComposeUiApi::class)
        Out -> Default
        else -> error(invalidFocusDirection)
    }
}
