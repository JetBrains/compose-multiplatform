/*
 * Copyright 2023 The Android Open Source Project
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

/**
 * The [FocusDirection] is used to specify the direction for a [FocusManager.moveFocus]
 * request.
 *
 * @sample androidx.compose.ui.samples.MoveFocusSample
 */
@kotlin.jvm.JvmInline
value class FocusDirection internal constructor(@Suppress("unused") private val value: Int) {

    override fun toString(): String {
        return when (this) {
            Next -> "Next"
            Previous -> "Previous"
            Left -> "Left"
            Right -> "Right"
            Up -> "Up"
            Down -> "Down"
            @OptIn(ExperimentalComposeUiApi::class)
            Enter -> "Enter"
            @OptIn(ExperimentalComposeUiApi::class)
            Exit -> "Exit"
            else -> "Invalid FocusDirection"
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
        @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
        @get:ExperimentalComposeUiApi
        @ExperimentalComposeUiApi
        val Enter: FocusDirection = FocusDirection(7)

        /**
         *  Direction used in [FocusManager.moveFocus] to indicate that you want to move focus to
         *  the parent of the currently focused item.
         */
        @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
        @get:ExperimentalComposeUiApi
        @ExperimentalComposeUiApi
        val Exit: FocusDirection = FocusDirection(8)

        /**
         *  Direction used in [FocusManager.moveFocus] to indicate that you are searching for the
         *  next focusable item that is a child of the currently focused item.
         */
        @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET", "Unused")
        @get:ExperimentalComposeUiApi
        @ExperimentalComposeUiApi
        @Deprecated(
            "Use FocusDirection.Enter instead.",
            ReplaceWith("Enter", "androidx.compose.ui.focus.FocusDirection.Companion.Enter"),
            DeprecationLevel.WARNING
        )
        val In: FocusDirection = Enter

        /**
         *  Direction used in [FocusManager.moveFocus] to indicate that you want to move focus to
         *  the parent of the currently focused item.
         */
        @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET", "Unused")
        @get:ExperimentalComposeUiApi
        @ExperimentalComposeUiApi
        @Deprecated(
            "Use FocusDirection.Exit instead.",
            ReplaceWith("Exit", "androidx.compose.ui.focus.FocusDirection.Companion.Exit"),
            DeprecationLevel.WARNING
        )
        val Out: FocusDirection = Exit
    }
}
