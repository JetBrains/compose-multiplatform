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
 * The focus state of a [FocusModifier]. Use [onFocusChanged] or [onFocusEvent] modifiers to
 * access [FocusState].
 */
interface FocusState {
    /**
     * Whether the component is focused or not.
     *
     * @return true if the component is focused, false otherwise.
     */
    val isFocused: Boolean
    /**
     * Whether the focus modifier associated with this [FocusState] has a child that is focused.
     *
     * @return true if a child is focused, false otherwise.
     */
    val hasFocus: Boolean

    /**
     * Whether focus is captured or not. A focusable component is in a captured state when it
     * wants to hold onto focus. (Eg. when a text field has an invalid phone number]. When we are
     * in a captured state, clicking outside the focused item does not clear focus.
     *
     * You can capture focus by calling [focusRequester.captureFocus()][captureFocus] and free
     * focus by calling [focusRequester.freeFocus()][freeFocus].
     *
     *  @return true if focus is captured, false otherwise.
     */
    val isCaptured: Boolean
}

// Different states of the focus system. These are the states used by the Focus Nodes.
internal enum class FocusStateImpl : FocusState {
    // The focusable component is currently active (i.e. it receives key events).
    Active,

    // One of the descendants of the focusable component is Active.
    ActiveParent,

    // The focusable component is currently active (has focus), and is in a state where
    // it does not want to give up focus. (Eg. a text field with an invalid phone number).
    Captured,

    // The focusable component is not currently focusable. (eg. A disabled button).
    Disabled,

    // The focusable component does not receive any key events. (ie it is not active, nor are any
    // of its descendants active).
    Inactive;

    override val isFocused: Boolean
        get() = when (this) {
            Captured, Active -> true
            ActiveParent, Inactive, Disabled -> false
        }

    override val hasFocus: Boolean
        get() = when (this) {
            ActiveParent -> true
            Active, Captured, Disabled, Inactive -> false
        }

    override val isCaptured: Boolean
        get() = when (this) {
            Captured -> true
            Active, ActiveParent, Inactive, Disabled -> false
        }
}
