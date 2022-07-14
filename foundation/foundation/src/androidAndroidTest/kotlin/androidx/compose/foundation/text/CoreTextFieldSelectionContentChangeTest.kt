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

import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.NativeKeyEvent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyPress
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextRange
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.FlakyTest
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@LargeTest
@RunWith(AndroidJUnit4::class)
class CoreTextFieldSelectionContentChangeTest {

    @get:Rule
    val rule = createComposeRule()

    private val Tag = "textField"

    private val backspaceKeyDown = KeyEvent(
        NativeKeyEvent(
            NativeKeyEvent.ACTION_DOWN,
            NativeKeyEvent.KEYCODE_DEL
        )
    )
    private val backspaceKeyUp = KeyEvent(
        NativeKeyEvent(
            NativeKeyEvent.ACTION_UP,
            NativeKeyEvent.KEYCODE_DEL
        )
    )

    @Test
    fun whenSelectedTextIsRemoved_SelectionCoerces() {
        val textFieldValue = mutableStateOf("Hello")
        rule.setContent {
            BasicTextField(
                value = textFieldValue.value,
                onValueChange = {
                    textFieldValue.value = it
                },
                modifier = Modifier.testTag(Tag).wrapContentSize()
            )
        }
        val textNode = rule.onNodeWithTag(Tag)
        textNode.performTouchInput { longClick() }
        textNode.performTextInputSelection(TextRange(0, 4))
        textFieldValue.value = ""

        rule.waitForIdle()
        val expected = TextRange(0, 0)
        val actual = textNode.fetchSemanticsNode().config
            .getOrNull(SemanticsProperties.TextSelectionRange)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun whenPartiallySelectedTextIsRemoved_SelectionCoercesToEdges() {
        val textFieldValue = mutableStateOf("Hello World!")
        rule.setContent {
            BasicTextField(
                value = textFieldValue.value,
                onValueChange = {
                    textFieldValue.value = it
                },
                modifier = Modifier.testTag(Tag).wrapContentSize()
            )
        }
        val textNode = rule.onNodeWithTag(Tag)
        textNode.performTouchInput { longClick() }
        textNode.performTextInputSelection(TextRange(2, 8))
        textFieldValue.value = "Hello"

        rule.waitForIdle()

        val expected = TextRange(2, 5)
        val actual = textNode.fetchSemanticsNode().config
            .getOrNull(SemanticsProperties.TextSelectionRange)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    @FlakyTest(bugId = 238875392)
    fun whenSelectedTextIsRemoved_addedLater_SelectionRemains() {
        val textFieldValue = mutableStateOf("Hello")
        rule.setContent {
            BasicTextField(
                value = textFieldValue.value,
                onValueChange = {
                    textFieldValue.value = it
                },
                modifier = Modifier.testTag(Tag).wrapContentSize()
            )
        }
        val textNode = rule.onNodeWithTag(Tag)
        textNode.performTouchInput { longClick() }
        textNode.performTextInputSelection(TextRange(0, 4))

        textFieldValue.value = ""
        rule.waitForIdle()

        textNode.assertTextEquals("")

        textFieldValue.value = "Hello"
        rule.waitForIdle()

        val expected = TextRange(0, 4)
        val actual = textNode.fetchSemanticsNode().config
            .getOrNull(SemanticsProperties.TextSelectionRange)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun whenSelectedTextIsRemovedByIME_SelectionDoesNotRevert() {
        // hard to find a descriptive name. Take a look at
        // `whenSelectedTextIsRemoved_addedLater_SelectionRemains` to understand this case better.

        val textFieldValue = mutableStateOf("Hello")
        rule.setContent {
            BasicTextField(
                value = textFieldValue.value,
                onValueChange = {
                    textFieldValue.value = it
                },
                modifier = Modifier.testTag(Tag).wrapContentSize()
            )
        }
        val textNode = rule.onNodeWithTag(Tag)
        textNode.performTouchInput { longClick() }
        textNode.performTextInputSelection(TextRange(0, 4))
        textNode.performKeyPress(backspaceKeyDown)
        textNode.performKeyPress(backspaceKeyUp)

        textFieldValue.value = "Hello"

        rule.waitForIdle()

        val expected = TextRange(0, 0)
        val actual = textNode.fetchSemanticsNode().config
            .getOrNull(SemanticsProperties.TextSelectionRange)
        assertThat(actual).isEqualTo(expected)
    }
}