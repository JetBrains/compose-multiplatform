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
 * the keyboard will show the requested action.
 */
@kotlin.jvm.JvmInline
value class ImeAction internal constructor(@Suppress("unused") private val value: Int) {

    override fun toString(): String {
        return when (this) {
            None -> "None"
            Default -> "Default"
            Go -> "Go"
            Search -> "Search"
            Send -> "Send"
            Previous -> "Previous"
            Next -> "Next"
            Done -> "Done"
            else -> "Invalid"
        }
    }

    companion object {
        /**
         * Use the platform and keyboard defaults and let the keyboard to decide the action. The
         * keyboards will mostly show one of [Done] or [None] actions based on the single/multi
         * line configuration.
         */
        val Default: ImeAction = ImeAction(1)

        /**
         * Represents that no action is expected from the keyboard. Keyboard might choose to show an
         * action which mostly will be newline, however this action is not carried into the app via
         * any [Keyboard Action][androidx.compose.foundation.text.KeyboardAction].
         */
        val None: ImeAction = ImeAction(0)

        /**
         * Represents that the user would like to go to the target of the text in the input i.e.
         * visiting a URL.
         */
        val Go: ImeAction = ImeAction(2)

        /**
         * Represents that the user wants to execute a search, i.e. web search query.
         */
        val Search: ImeAction = ImeAction(3)

        /**
         * Represents that the user wants to send the text in the input, i.e. an SMS.
         */
        val Send: ImeAction = ImeAction(4)

        /**
         * Represents that the user wants to return to the previous input i.e. going back to the
         * previous field in a form.
         */
        val Previous: ImeAction = ImeAction(5)

        /**
         * Represents that the user is done with the current input, and wants to move to the next
         * one i.e. moving to the next field in a form.
         */
        val Next: ImeAction = ImeAction(6)

        /**
         * Represents that the user is done providing input to a group of inputs. Some
         * kind of finalization behavior should now take place i.e. the field was the last element in
         * a group and the data input is finalized.
         */
        val Done: ImeAction = ImeAction(7)
    }
}