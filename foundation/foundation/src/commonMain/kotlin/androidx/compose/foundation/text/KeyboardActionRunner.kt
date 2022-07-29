/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation.text

import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeAction.Companion.Default
import androidx.compose.ui.text.input.ImeAction.Companion.None
import androidx.compose.ui.text.input.ImeAction.Companion.Go
import androidx.compose.ui.text.input.ImeAction.Companion.Search
import androidx.compose.ui.text.input.ImeAction.Companion.Send
import androidx.compose.ui.text.input.ImeAction.Companion.Previous
import androidx.compose.ui.text.input.ImeAction.Companion.Next
import androidx.compose.ui.text.input.ImeAction.Companion.Done
import androidx.compose.ui.text.input.TextInputSession

/**
 * This class can be used to run keyboard actions when the user triggers an IME action.
 */
internal class KeyboardActionRunner : KeyboardActionScope {

    /**
     * The developer specified [KeyboardActions].
     */
    lateinit var keyboardActions: KeyboardActions

    /**
     * A reference to the [FocusManager] composition local.
     */
    lateinit var focusManager: FocusManager

    /**
     * A reference to the current [TextInputSession].
     */
    // TODO(b/241399013) replace with SoftwareKeyboardController when it becomes stable.
    var inputSession: TextInputSession? = null

    /**
     * Run the keyboard action corresponding to the specified imeAction. If a keyboard action is
     * not specified, use the default implementation provided by [defaultKeyboardAction].
     */
    fun runAction(imeAction: ImeAction) {
        val keyboardAction = when (imeAction) {
            Done -> keyboardActions.onDone
            Go -> keyboardActions.onGo
            Next -> keyboardActions.onNext
            Previous -> keyboardActions.onPrevious
            Search -> keyboardActions.onSearch
            Send -> keyboardActions.onSend
            Default, None -> null
            else -> error("invalid ImeAction")
        }
        keyboardAction?.invoke(this) ?: defaultKeyboardAction(imeAction)
    }

    /**
     * Default implementations for [KeyboardActions].
     */
    override fun defaultKeyboardAction(imeAction: ImeAction) {
        when (imeAction) {
            Next -> focusManager.moveFocus(FocusDirection.Next)
            Previous -> focusManager.moveFocus(FocusDirection.Previous)
            Done -> inputSession?.hideSoftwareKeyboard()
            // Note: Don't replace this with an else. These are specified explicitly so that we
            // don't forget to update this when statement when new imeActions are added.
            Go, Search, Send, Default, None -> Unit // Do Nothing.
        }
    }
}
