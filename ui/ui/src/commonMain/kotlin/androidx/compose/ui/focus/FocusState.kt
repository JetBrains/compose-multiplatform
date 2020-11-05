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

/**
 * Different states of the focus system. These are the states used by the Focus Nodes.
 *
 */
@ExperimentalFocus
enum class FocusState {
    /**
     * The focusable component is currently active (i.e. it receives key events).
     */
    Active,

    /**
     * One of the descendants of the focusable component is [Active].
     */
    ActiveParent,

    /**
     * The focusable component is currently active (has focus), and is in a state where
     * it does not want to give up focus. (Eg. a text field with an invalid phone number).
     */
    Captured,

    /**
     * The focusable component is not currently focusable. (eg. A disabled button).
     */
    Disabled,

    /**
     * The focusable component does not receive any key events. (ie it is not active,
     * nor are any of its descendants active).
     */
    Inactive
}

/**
 * Converts a [focus state][FocusState] into a boolean value indicating if the component
 * is focused or not.
 *
 * @return true if the component is focused, false otherwise.
 */
@ExperimentalFocus
val FocusState.isFocused
    get() = when (this) {
        FocusState.Captured,
        FocusState.Active -> true
        FocusState.ActiveParent,
        FocusState.Inactive,
        FocusState.Disabled -> false
    }
