/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.text.input

/**
 * Signals the keyboard what type of action should be displayed. It is not guaranteed if
 * the keyboard will show the requested action. Apart from the visuals on the keyboard, when the
 * user performs the action onImeActionPerformed callback will be triggered with the action.
 */
enum class ImeAction {
    /**
     * Use the platform and keyboard defaults and let the keyboard to decide the action. The
     * keyboards will mostly show one of [Done] or [None] actions based on the single/multi
     * line configuration.
     */
    @Deprecated("Use Default instead", ReplaceWith("Default"))
    Unspecified,

    /**
     * Use the platform and keyboard defaults and let the keyboard to decide the action. The
     * keyboards will mostly show one of [Done] or [None] actions based on the single/multi
     * line configuration.
     */
    Default,

    /**
     * Represents that no IME action is available in editor. The keyboards will mostly show new line
     * action.
     */
    @Deprecated("Use None instead", ReplaceWith("None"))
    NoAction,

    /**
     * Represents that no action is expected from the keyboard. Keyboard might choose to show an
     * action which mostly will be newline, however this action is not carried into the app via
     * onImeActionPerformed.
     */
    None,

    /**
     * Represents that the user would like to go to the target of the text in the input i.e.
     * visiting a URL.
     */
    Go,

    /**
     * Represents that the user wants to execute a search, i.e web search query.
     */
    Search,

    /**
     * Represents that the user wants to send the text in the input, i.e an SMS.
     */
    Send,

    /**
     * Represents that the user wants to return to the previous input i.e. going back to the
     * previous field in a form.
     */
    Previous,

    /**
     * Represents that the user is done with the current input, and wants to move to the next
     * one i.e. moving to the next field in a form.
     */
    Next,

    /**
     * Represents that the user is done providing input to a group of inputs. Some
     * kind of finalization behavior should now take place i.e. the field was the last element in
     * a group and the data input is finalized.
     */
    Done
}