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

package androidx.compose.ui.test.junit4

import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.PlatformTextInputService
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TextInputService
import androidx.compose.ui.text.input.TextInputSession

/**
 * Extra layer that serves as an observer between the text input service and text fields.
 *
 * When a text field gets a focus it calls to the text input service to provide its callback to
 * accept input from the IME. Here we grab that callback so we can fetch it commands the same
 * way IME would do.
 */
internal class TextInputServiceForTests(
    platformTextInputService: PlatformTextInputService
) : TextInputService(platformTextInputService) {

    var onEditCommand: ((List<EditCommand>) -> Unit)? = null
    var onImeActionPerformed: ((ImeAction) -> Unit)? = null

    override fun startInput(
        value: TextFieldValue,
        imeOptions: ImeOptions,
        onEditCommand: (List<EditCommand>) -> Unit,
        onImeActionPerformed: (ImeAction) -> Unit
    ): TextInputSession {
        this.onEditCommand = onEditCommand
        this.onImeActionPerformed = onImeActionPerformed
        return super.startInput(
            value,
            imeOptions,
            onEditCommand,
            onImeActionPerformed
        )
    }

    override fun stopInput(session: TextInputSession) {
        this.onEditCommand = null
        this.onImeActionPerformed = null
        super.stopInput(session)
    }
}