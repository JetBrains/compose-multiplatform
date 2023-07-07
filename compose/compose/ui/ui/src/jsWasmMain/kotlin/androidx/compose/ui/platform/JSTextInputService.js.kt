/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.platform

import androidx.compose.ui.text.input.CommitTextCommand
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.PlatformTextInputService
import androidx.compose.ui.text.input.TextFieldValue
import org.jetbrains.skiko.SkikoInput
import org.jetbrains.skiko.SkikoInputEvent

internal class JSTextInputService : PlatformTextInputService {

    data class CurrentInput(
        var value: TextFieldValue,
        val onEditCommand: ((List<EditCommand>) -> Unit),
    )

    private var currentInput: CurrentInput? = null

    override fun startInput(
        value: TextFieldValue,
        imeOptions: ImeOptions,
        onEditCommand: (List<EditCommand>) -> Unit,
        onImeActionPerformed: (ImeAction) -> Unit
    ) {
        currentInput = CurrentInput(
            value,
            onEditCommand
        )
        showSoftwareKeyboard()
    }

    override fun stopInput() {
        currentInput = null
    }

    override fun showSoftwareKeyboard() {
        println("TODO showSoftwareKeyboard in JS")
    }

    override fun hideSoftwareKeyboard() {
        println("TODO showSoftwareKeyboard in JS")
    }

    override fun updateState(oldValue: TextFieldValue?, newValue: TextFieldValue) {
        currentInput?.let { input ->
            input.value = newValue
        }
    }

    fun sendInputText(text: String) {
        currentInput?.let { input ->
            input.onEditCommand(listOf(CommitTextCommand(text, 1)))
        }
    }

    val input = object : SkikoInput {
        override fun onInputEvent(event: SkikoInputEvent) {
            sendInputText(event.input)
        }
    }

}
