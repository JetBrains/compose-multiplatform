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

internal class SkiaTextInputService(
    showSoftwareKeyboard: () -> Unit,
    hideSoftwareKeyboard: () -> Unit,
) : PlatformTextInputService {

    private val _showSoftwareKeyboard: () -> Unit = showSoftwareKeyboard
    private val _hideSoftwareKeyboard: () -> Unit = hideSoftwareKeyboard
    private var listener: ((String) -> Unit)? = null

    override fun startInput(
        value: TextFieldValue,
        imeOptions: ImeOptions,
        onEditCommand: (List<EditCommand>) -> Unit,
        onImeActionPerformed: (ImeAction) -> Unit
    ) {
        listener = { text: String ->
            onEditCommand(
                listOf(CommitTextCommand(text, 1))
            )
        }
        showSoftwareKeyboard()
    }

    override fun stopInput() {
        listener = null
    }

    override fun showSoftwareKeyboard() {
        _showSoftwareKeyboard()
    }

    override fun hideSoftwareKeyboard() {
        _hideSoftwareKeyboard()
    }

    override fun updateState(oldValue: TextFieldValue?, newValue: TextFieldValue) {

    }

    internal fun sendInputEvent(event: org.jetbrains.skiko.SkikoInputEvent) {
        val inputText: String = event.input
        listener?.invoke(inputText)
    }
}
