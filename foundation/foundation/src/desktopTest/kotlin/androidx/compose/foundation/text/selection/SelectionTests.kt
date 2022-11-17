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

package androidx.compose.foundation.text.selection

import androidx.compose.foundation.DesktopPlatform
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyMapping
import androidx.compose.foundation.text.createPlatformDefaultKeyMapping
import androidx.compose.foundation.text.platformDefaultKeyMapping
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.google.common.truth.Truth
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test

class SelectionTests {

    @get:Rule
    val rule = createComposeRule()

    private val originalPlatformKeyMapping = platformDefaultKeyMapping

    @After
    fun restoreRealDesktopPlatform() {
        setPlatformDefaultKeyMapping(originalPlatformKeyMapping)
    }

    private fun setPlatformDefaultKeyMapping(value: KeyMapping) {
        val field = Class.forName("androidx.compose.foundation.text.KeyMapping_desktopKt")
            .getDeclaredField("platformDefaultKeyMapping")
        field.isAccessible = true
        val modifiersField = java.lang.reflect.Field::class.java.getDeclaredField("modifiers")
        modifiersField.isAccessible = true
        modifiersField.setInt(field, field.modifiers and java.lang.reflect.Modifier.FINAL.inv())
        field.set(null, value)
    }

    @OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
    @Test
    fun `select using Shift_End and Shift_Home combinations with DesktopPlatform-Windows`() = runBlocking {
        setPlatformDefaultKeyMapping(createPlatformDefaultKeyMapping(DesktopPlatform.Windows))
        val state = mutableStateOf(TextFieldValue("line 1\nline 2\nline 3\nline 4\nline 5"))


        rule.setContent {
            BasicTextField(
                value = state.value,
                onValueChange = { state.value = it },
                modifier = Modifier.testTag("textField")
            )
        }
        rule.awaitIdle()
        rule.onNodeWithTag("textField").performMouseInput {
            click(Offset(0f, 0f))
        }
        rule.awaitIdle()
        rule.onNodeWithTag("textField").assertIsFocused()
        Truth.assertThat(state.value.selection).isEqualTo(TextRange(0, 0))

        rule.onNodeWithTag("textField").performKeyInput {
            pressKey(Key.DirectionRight)
        }
        rule.awaitIdle()
        Truth.assertThat(state.value.selection).isEqualTo(TextRange(1, 1))

        rule.onNodeWithTag("textField").performKeyInput {
            keyDown(Key.ShiftLeft)
            pressKey(Key.MoveEnd)
            keyUp(Key.ShiftLeft)
        }
        rule.awaitIdle()
        Truth.assertThat(state.value.selection).isEqualTo(TextRange(1, 6))

        rule.onNodeWithTag("textField").performKeyInput {
            keyDown(Key.ShiftLeft)
            pressKey(Key.MoveHome)
            keyUp(Key.ShiftLeft)
        }
        rule.awaitIdle()
        Truth.assertThat(state.value.selection).isEqualTo(TextRange(1, 0))
    }

    @OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
    @Test
    fun `select using Shift_End and Shift_Home combinations with DesktopPlatform-MacOs`() = runBlocking {
        setPlatformDefaultKeyMapping(createPlatformDefaultKeyMapping(DesktopPlatform.MacOS))
        val state = mutableStateOf(TextFieldValue("line 1\nline 2\nline 3\nline 4\nline 5"))

        rule.setContent {
            BasicTextField(
                value = state.value,
                onValueChange = { state.value = it },
                modifier = Modifier.testTag("textField")
            )
        }
        rule.awaitIdle()
        rule.onNodeWithTag("textField").performMouseInput {
            click(Offset(0f, 0f))
        }
        rule.awaitIdle()
        rule.onNodeWithTag("textField").assertIsFocused()
        Truth.assertThat(state.value.selection).isEqualTo(TextRange(0, 0))

        rule.onNodeWithTag("textField").performKeyInput {
            pressKey(Key.DirectionRight)
        }
        rule.awaitIdle()
        Truth.assertThat(state.value.selection).isEqualTo(TextRange(1, 1))

        rule.onNodeWithTag("textField").performKeyInput {
            keyDown(Key.ShiftLeft)
            pressKey(Key.MoveEnd)
            keyUp(Key.ShiftLeft)
        }
        rule.awaitIdle()
        Truth.assertThat(state.value.selection).isEqualTo(TextRange(1, 34))

        rule.onNodeWithTag("textField").performKeyInput {
            keyDown(Key.ShiftLeft)
            pressKey(Key.MoveHome)
            keyUp(Key.ShiftLeft)
        }
        rule.awaitIdle()
        Truth.assertThat(state.value.selection).isEqualTo(TextRange(1, 0))
    }

    @OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
    @Test
    fun `Ctrl + Backspace on an empty line with DesktopPlatform-Windows`() = runBlocking {
        setPlatformDefaultKeyMapping(createPlatformDefaultKeyMapping(DesktopPlatform.Windows))
        val state = mutableStateOf(TextFieldValue(""))

        rule.setContent {
            BasicTextField(
                value = state.value,
                onValueChange = { state.value = it },
                modifier = Modifier.testTag("textField")
            )
        }
        rule.awaitIdle()
        rule.onNodeWithTag("textField").performMouseInput {
            click(Offset(0f, 0f))
        }
        rule.awaitIdle()
        rule.onNodeWithTag("textField").assertIsFocused()
        Truth.assertThat(state.value.selection).isEqualTo(TextRange(0, 0))

        rule.onNodeWithTag("textField").performKeyInput {
            keyDown(Key.CtrlLeft)
            keyDown(Key.Backspace)
        }
        rule.awaitIdle()
    }

    @OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
    @Test
    fun `Ctrl + Backspace on an empty line with DesktopPlatform-Macos`() = runBlocking {
        setPlatformDefaultKeyMapping(createPlatformDefaultKeyMapping(DesktopPlatform.MacOS))
        val state = mutableStateOf(TextFieldValue(""))

        rule.setContent {
            BasicTextField(
                value = state.value,
                onValueChange = { state.value = it },
                modifier = Modifier.testTag("textField")
            )
        }
        rule.awaitIdle()
        rule.onNodeWithTag("textField").performMouseInput {
            click(Offset(0f, 0f))
        }
        rule.awaitIdle()
        rule.onNodeWithTag("textField").assertIsFocused()
        Truth.assertThat(state.value.selection).isEqualTo(TextRange(0, 0))

        rule.onNodeWithTag("textField").performKeyInput {
            keyDown(Key.AltLeft)
            keyDown(Key.Backspace)
        }
        rule.awaitIdle()
    }
}
