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
 * The [FocusDirection] is used to specify the direction for a [FocusManager.moveFocus]
 * request.
 *
 * @sample androidx.compose.ui.samples.MoveFocusSample
 */
@Suppress("INLINE_CLASS_DEPRECATED", "EXPERIMENTAL_FEATURE_WARNING")
inline class FocusDirection internal constructor(@Suppress("unused") private val value: Int) {

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
         *  Direction used in [FocusManager.moveFocus] to indicate that you are searching for the
         *  next focusable item.
         *
         *  @sample androidx.compose.ui.samples.MoveFocusSample
         */
        val Next: FocusDirection = FocusDirection(1)

        /**
         *  Direction used in [FocusManager.moveFocus] to indicate that you are searching for the
         *  previous focusable item.
         *
         *  @sample androidx.compose.ui.samples.MoveFocusSample
         */
        val Previous: FocusDirection = FocusDirection(2)

        /**
         *  Direction used in [FocusManager.moveFocus] to indicate that you are searching for the
         *  next focusable item to the left of the currently focused item.
         *
         *  @sample androidx.compose.ui.samples.MoveFocusSample
         */
        val Left: FocusDirection = FocusDirection(3)

        /**
         *  Direction used in [FocusManager.moveFocus] to indicate that you are searching for the
         *  next focusable item to the right of the currently focused item.
         *
         *  @sample androidx.compose.ui.samples.MoveFocusSample
         */
        val Right: FocusDirection = FocusDirection(4)

        /**
         *  Direction used in [FocusManager.moveFocus] to indicate that you are searching for the
         *  next focusable item that is above the currently focused item.
         *
         *  @sample androidx.compose.ui.samples.MoveFocusSample
         */
        val Up: FocusDirection = FocusDirection(5)

        /**
         *  Direction used in [FocusManager.moveFocus] to indicate that you are searching for the
         *  next focusable item that is below the currently focused item.
         *
         *  @sample androidx.compose.ui.samples.MoveFocusSample
         */
        val Down: FocusDirection = FocusDirection(6)

        /**
         *  Direction used in [FocusManager.moveFocus] to indicate that you are searching for the
         *  next focusable item that is a child of the currently focused item.
         */
        @ExperimentalComposeUiApi
        val In: FocusDirection = FocusDirection(7)

        /**
         *  Direction used in [FocusManager.moveFocus] to indicate that you want to move focus to
         *  the parent of the currently focused item.
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
internal fun ModifiedFocusNode.focusSearch(
    focusDirection: FocusDirection,
    layoutDirection: LayoutDirection
): ModifiedFocusNode? {
    return when (focusDirection) {
        Next, Previous -> null // TODO(b/170155659): Perform one dimensional focus search.
        Left, Right, Up, Down -> twoDimensionalFocusSearch(focusDirection)
        @OptIn(ExperimentalComposeUiApi::class)
        In -> {
            // we search among the children of the active item.
            val direction = when (layoutDirection) { Rtl -> Left; Ltr -> Right }
            findActiveFocusNode()?.twoDimensionalFocusSearch(direction)
        }
        @OptIn(ExperimentalComposeUiApi::class)
        Out -> findActiveFocusNode()?.findParentFocusNode()
        else -> error(invalidFocusDirection)
    }
}

internal fun ModifiedFocusNode.findActiveFocusNode(): ModifiedFocusNode? {
    return when (focusState) {
        Active, Captured -> this
        ActiveParent -> focusedChild?.findActiveFocusNode()
        Inactive, Disabled -> null
    }
}
