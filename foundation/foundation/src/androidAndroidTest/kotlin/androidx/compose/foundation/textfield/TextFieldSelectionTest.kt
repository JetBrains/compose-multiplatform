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

package androidx.compose.foundation.textfield

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.selection.isSelectionHandle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

class TextFieldSelectionTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun readOnlyTextField_showsSelectionHandles() {
        val testTag = "text field"
        val textFieldValue = mutableStateOf("text text text")
        rule.setContent {
            BasicTextField(
                value = textFieldValue.value,
                onValueChange = { textFieldValue.value = it },
                modifier = Modifier.testTag(testTag),
                readOnly = true
            )
        }

        // selection is not shown
        rule.onAllNodes(isPopup()).assertCountEquals(0)

        // make selection
        rule.onNodeWithTag(testTag).performTouchInput { longClick() }
        rule.waitForIdle()

        rule.onNode(isSelectionHandle(Handle.SelectionStart)).assertIsDisplayed()
        rule.onNode(isSelectionHandle(Handle.SelectionEnd)).assertIsDisplayed()
    }

    @Test
    fun textField_showsSelectionHandles_whenVisualTransformationIsApplied() {
        val testTag = "text field"
        val textFieldValue = mutableStateOf(TextFieldValue("text text text"))
        rule.setContent {
            BasicTextField(
                value = textFieldValue.value,
                onValueChange = { textFieldValue.value = it },
                visualTransformation = extraStarsVisualTransformation(),
                modifier = Modifier.testTag(testTag)
            )
        }

        // selection is not shown
        rule.onAllNodes(isPopup()).assertCountEquals(0)

        // make selection
        rule.onNodeWithTag(testTag).performTouchInput { longClick() }
        rule.waitForIdle()

        rule.onNode(isSelectionHandle(Handle.SelectionStart)).assertIsDisplayed()
        rule.onNode(isSelectionHandle(Handle.SelectionEnd)).assertIsDisplayed()
    }

    @Test
    fun textField_showsCursorHandle() {
        val testTag = "text field"
        val textFieldValue = mutableStateOf("text text text")
        rule.setContent {
            BasicTextField(
                value = textFieldValue.value,
                onValueChange = { textFieldValue.value = it },
                modifier = Modifier.testTag(testTag)
            )
        }

        // selection is not shown
        rule.onAllNodes(isPopup()).assertCountEquals(0)

        // focus textfield, cursor should show
        rule.onNodeWithTag(testTag).performTouchInput { click() }
        rule.waitForIdle()

        rule.onNode(isSelectionHandle(Handle.Cursor)).assertIsDisplayed()
    }

    @Test
    fun textField_dragsCursorHandle() {
        val testTag = "text field"
        val textFieldValue =
            mutableStateOf(TextFieldValue("text text text", TextRange(Int.MAX_VALUE)))
        val cursorPositions = mutableListOf<Int>()
        rule.setContent {
            BasicTextField(
                value = textFieldValue.value,
                onValueChange = {
                    textFieldValue.value = it
                    if (it.selection.collapsed &&
                        cursorPositions.lastOrNull() != it.selection.start
                    ) {
                        cursorPositions.add(it.selection.start)
                    }
                },
                modifier = Modifier.testTag(testTag)
            )
        }

        // selection is not shown
        rule.onAllNodes(isPopup()).assertCountEquals(0)

        var target = 0f
        // focus textfield, cursor should show
        rule.onNodeWithTag(testTag).performTouchInput {
            click(Offset(0f, centerY))
            target = right
        }
        rule.waitForIdle()

        assertThat(textFieldValue.value.selection.start).isEqualTo(0)

        rule.onNode(isSelectionHandle(Handle.Cursor)).performTouchInput {
            swipeRight(startX = centerX, endX = centerX + target, durationMillis = 1000)
        }

        assertThat(cursorPositions).isEqualTo((0..14).toList())
    }

    @Test
    fun textField_dragsCursorHandle_withVisualTransformation() {
        val testTag = "text field"
        val textFieldValue =
            mutableStateOf(TextFieldValue("text text text", TextRange(Int.MAX_VALUE)))
        val cursorPositions = mutableListOf<Int>()
        rule.setContent {
            BasicTextField(
                value = textFieldValue.value,
                onValueChange = {
                    textFieldValue.value = it
                    if (it.selection.collapsed &&
                        cursorPositions.lastOrNull() != it.selection.start
                    ) {
                        cursorPositions.add(it.selection.start)
                    }
                },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.testTag(testTag)
            )
        }

        // selection is not shown
        rule.onAllNodes(isPopup()).assertCountEquals(0)

        var target = 0f
        // focus textfield, cursor should show
        rule.onNodeWithTag(testTag).performTouchInput {
            click(Offset(0f, centerY))
            target = right
        }
        rule.waitForIdle()

        assertThat(textFieldValue.value.selection.start).isEqualTo(0)

        rule.onNode(isSelectionHandle(Handle.Cursor)).performTouchInput {
            swipeRight(startX = centerX, endX = centerX + target, durationMillis = 1000)
        }

        assertThat(cursorPositions).isEqualTo((0..14).toList())
    }

    private fun extraStarsVisualTransformation(): VisualTransformation {
        return VisualTransformation {
            TransformedText(
                text = AnnotatedString(it.text.flatMap { listOf(it, '*') }.joinToString("")),
                offsetMapping = object : OffsetMapping {
                    override fun originalToTransformed(offset: Int) = offset * 2
                    override fun transformedToOriginal(offset: Int) = offset / 2
                })
        }
    }
}