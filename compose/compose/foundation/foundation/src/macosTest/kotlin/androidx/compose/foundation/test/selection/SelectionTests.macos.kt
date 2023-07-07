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

package androidx.compose.foundation.test.selection

import androidx.compose.foundation.assertThat
import androidx.compose.foundation.isEqualTo
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.Test

class SelectionMacosTests {

    @OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
    @Test
    fun `select using Shift_End and Shift_Home combinations`() = runSkikoComposeUiTest {
        val state = mutableStateOf(TextFieldValue("line 1\nline 2\nline 3\nline 4\nline 5"))

        setContent {
            BasicTextField(
                value = state.value,
                onValueChange = { state.value = it },
                modifier = Modifier.testTag("textField")
            )
        }
        waitForIdle()
        onNodeWithTag("textField").performMouseInput {
            click(Offset(0f, 0f))
        }
        waitForIdle()
        onNodeWithTag("textField").assertIsFocused()
        assertThat(state.value.selection).isEqualTo(TextRange(0, 0))

        onNodeWithTag("textField").performKeyInput {
            pressKey(Key.DirectionRight)
        }
        waitForIdle()
        assertThat(state.value.selection).isEqualTo(TextRange(1, 1))

        onNodeWithTag("textField").performKeyInput {
            keyDown(Key.ShiftLeft)
            pressKey(Key.MoveEnd)
            keyUp(Key.ShiftLeft)
        }
        waitForIdle()
        assertThat(state.value.selection).isEqualTo(TextRange(1, 34))

        onNodeWithTag("textField").performKeyInput {
            keyDown(Key.ShiftLeft)
            pressKey(Key.MoveHome)
            keyUp(Key.ShiftLeft)
        }
        waitForIdle()
        assertThat(state.value.selection).isEqualTo(TextRange(1, 0))
    }

    @OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
    @Test
    fun `Ctrl + Backspace on an empty line`() = runSkikoComposeUiTest {
        val state = mutableStateOf(TextFieldValue(""))

        setContent {
            BasicTextField(
                value = state.value,
                onValueChange = { state.value = it },
                modifier = Modifier.testTag("textField")
            )
        }
        waitForIdle()
        onNodeWithTag("textField").performMouseInput {
            click(Offset(0f, 0f))
        }
        waitForIdle()
        onNodeWithTag("textField").assertIsFocused()
        assertThat(state.value.selection).isEqualTo(TextRange(0, 0))

        onNodeWithTag("textField").performKeyInput {
            keyDown(Key.AltLeft)
            keyDown(Key.Backspace)
        }
        waitForIdle()
    }
}
