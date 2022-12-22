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

package androidx.compose.foundation.copyPasteAndroidTests.textfield

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.assertThat
import androidx.compose.foundation.isEqualTo
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.PlatformTextInputService
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TextInputService
import kotlin.test.*

@OptIn(ExperimentalFoundationApi::class, ExperimentalTestApi::class)
class TextFieldUndoTest {

    @OptIn(ExperimentalComposeUiApi::class)
    @Test @Ignore // TODO: figure out how to make target-specific tweaks (Key.Ctrl vs Key.Command)
    fun undo_redo() = runSkikoComposeUiTest {
        val textInputService = TextInputService(mock)
        val state = mutableStateOf("hi")
        val focusRequester = FocusRequester()
        setContent {
            CompositionLocalProvider(
                LocalTextInputService provides textInputService
            ) {
                BasicTextField(
                    value = state.value,
                    modifier = Modifier.focusRequester(focusRequester),
                    onValueChange = {
                        state.value = it
                    }
                )
            }
        }

        runOnIdle { focusRequester.requestFocus() }

        state.value = "hello"

        waitForIdle()

        // undo command
        onNode(hasSetTextAction()).performKeyInput {
            keyDown(Key.CtrlLeft)
            pressKey(Key.Z)
            keyUp(Key.CtrlLeft)
        }

        runOnIdle {
            assertThat(state.value).isEqualTo("hi")
        }

        // redo command
        onNode(hasSetTextAction()).performKeyInput {
            keyDown(Key.CtrlLeft)
            keyDown(Key.ShiftLeft)
            pressKey(Key.Z)
            keyUp(Key.CtrlLeft)
            keyUp(Key.ShiftLeft)
        }

        runOnIdle {
            assertThat(state.value).isEqualTo("hello")
        }
    }

    private val mock = object : PlatformTextInputService {
        override fun startInput(
            value: TextFieldValue,
            imeOptions: ImeOptions,
            onEditCommand: (List<EditCommand>) -> Unit,
            onImeActionPerformed: (ImeAction) -> Unit
        ) {}
        override fun stopInput() {}
        override fun showSoftwareKeyboard() {}
        override fun hideSoftwareKeyboard() {}
        override fun updateState(oldValue: TextFieldValue?, newValue: TextFieldValue) {}
    }
}
