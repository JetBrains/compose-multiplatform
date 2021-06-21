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

package androidx.compose.foundation.textfield

import android.view.KeyEvent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performKeyPress
import androidx.compose.ui.text.input.TextInputService
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalFoundationApi::class)
class TextFieldUndoTest {
    @get:Rule
    val rule = createComposeRule()

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun undo_redo() {
        val textInputService = TextInputService(mock())
        val state = mutableStateOf("hi")
        val focusFequester = FocusRequester()
        rule.setContent {
            CompositionLocalProvider(
                LocalTextInputService provides textInputService
            ) {
                BasicTextField(
                    value = state.value,
                    modifier = Modifier.focusRequester(focusFequester),
                    onValueChange = {
                        state.value = it
                    }
                )
            }
        }

        rule.runOnIdle { focusFequester.requestFocus() }

        state.value = "hello"

        rule.waitForIdle()

        // undo command
        rule.onNode(hasSetTextAction()).performKeyPress(downEvent(Key.Z, KeyEvent.META_CTRL_ON))

        rule.runOnIdle {
            assertThat(state.value).isEqualTo("hi")
        }

        // redo command
        rule.onNode(hasSetTextAction()).performKeyPress(
            downEvent(
                Key.Z,
                KeyEvent.META_CTRL_ON or KeyEvent.META_SHIFT_ON
            )
        )

        rule.runOnIdle {
            assertThat(state.value).isEqualTo("hello")
        }
    }
}

private fun downEvent(key: Key, metaState: Int = 0): androidx.compose.ui.input.key.KeyEvent {
    return androidx.compose.ui.input.key.KeyEvent(
        KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, key.nativeKeyCode, 0, metaState)
    )
}