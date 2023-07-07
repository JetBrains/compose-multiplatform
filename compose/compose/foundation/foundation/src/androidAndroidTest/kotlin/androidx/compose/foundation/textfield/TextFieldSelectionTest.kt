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
import androidx.compose.foundation.text.selection.ReducedVisualTransformation
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
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

class TextFieldSelectionTest {
    @get:Rule
    val rule = createComposeRule()

    private val testTag = "text field"

    @Test
    fun readOnlyTextField_showsSelectionHandles() {
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
        val textFieldValue = mutableStateOf(TextFieldValue("texttexttext"))
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
    fun textField_showsSelectionHandles_whenReducedVisualTransformationIsApplied() {
        rule.setContent {
            BasicTextField(
                value = "text".repeat(10),
                onValueChange = { },
                visualTransformation = ReducedVisualTransformation(),
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
        textField_dragsCursorHandle(
            text = "text text text",
            visualTransformation = VisualTransformation.None,
            expectedCursorPositions = (0..14).toList()
        )
    }

    @Test
    fun textField_dragsCursorHandle_withPasswordVisualTransformation() {
        textField_dragsCursorHandle(
            text = "text text text",
            visualTransformation = PasswordVisualTransformation(),
            expectedCursorPositions = (0..14).toList()
        )
    }

    @Test
    fun textField_dragsCursorHandle_withReducedVisualTransformation() {
        textField_dragsCursorHandle(
            text = "text".repeat(10),
            visualTransformation = ReducedVisualTransformation(),
            expectedCursorPositions = (0..40).filter { it % 2 == 0 }.toList()
        )
    }

    private fun textField_dragsCursorHandle(
        text: String,
        visualTransformation: VisualTransformation,
        expectedCursorPositions: List<Int>
    ) {
        val textFieldValue = mutableStateOf(TextFieldValue(text, TextRange(Int.MAX_VALUE)))
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
                visualTransformation = visualTransformation,
                modifier = Modifier.testTag(testTag)
            )
        }

        // selection and cursor are hidden
        rule.onAllNodes(isPopup()).assertCountEquals(0)

        // focus textfield, cursor should show at the very beginning of textfield
        rule.onNodeWithTag(testTag).performTouchInput { click(Offset.Zero) }
        rule.waitForIdle()

        assertThat(textFieldValue.value.selection.start).isEqualTo(0)

        performHandleDrag(Handle.Cursor, false)

        assertThat(cursorPositions).isEqualTo(expectedCursorPositions)
    }

    @Ignore // b/265023621
    @Test
    fun textField_extendsSelection_toRight() {
        textField_extendsSelection(
            text = "text".repeat(5),
            visualTransformation = VisualTransformation.None,
            expectedSelectionRanges = (1..20).map { TextRange(0, it) }.toList(),
            toLeft = false
        )
    }

    @Ignore // b/265023621
    @Test
    fun textField_extendsSelection_withPasswordVisualTransformation_toRight() {
        textField_extendsSelection(
            text = "text".repeat(5),
            visualTransformation = PasswordVisualTransformation(),
            expectedSelectionRanges = (1..20).map { TextRange(0, it) }.toList(),
            toLeft = false
        )
    }

    @Ignore // b/265023621
    @Test
    fun textField_extendsSelection_withReducedVisualTransformation_toRight() {
        textField_extendsSelection(
            text = "text".repeat(10),
            visualTransformation = ReducedVisualTransformation(),
            expectedSelectionRanges = (1..40)
                .filter { it % 2 == 0 }
                .map { TextRange(0, it) }
                .toList(),
            toLeft = false
        )
    }

    @Ignore // b/265023420
    @Test
    fun textField_extendsSelection_toLeft() {
        textField_extendsSelection(
            text = "text".repeat(5),
            visualTransformation = VisualTransformation.None,
            expectedSelectionRanges = (19 downTo 1).map { TextRange(it, 20) }.toList(),
            toLeft = true
        )
    }

    @Ignore // b/265023621
    @Test
    fun textField_extendsSelection_withPasswordVisualTransformation_toLeft() {
        textField_extendsSelection(
            text = "text".repeat(5),
            visualTransformation = PasswordVisualTransformation(),
            expectedSelectionRanges = (19 downTo 1).map { TextRange(it, 20) }.toList(),
            toLeft = true
        )
    }

    @Test
    fun textField_extendsSelection_withReducedVisualTransformation_toLeft() {
        textField_extendsSelection(
            text = "text".repeat(10),
            visualTransformation = ReducedVisualTransformation(),
            expectedSelectionRanges = (39 downTo 1)
                .filter { it % 2 == 0 }
                .map { TextRange(it, 40) }
                .toList(),
            toLeft = true
        )
    }

    // starts from [0,1] selection
    private fun textField_extendsSelection(
        text: String,
        visualTransformation: VisualTransformation,
        expectedSelectionRanges: List<TextRange>,
        toLeft: Boolean
    ) {
        val textFieldValue =
            mutableStateOf(TextFieldValue(text, TextRange(Int.MAX_VALUE)))
        val selectionRanges = mutableListOf<TextRange>()
        rule.setContent {
            BasicTextField(
                value = textFieldValue.value,
                onValueChange = {
                    textFieldValue.value = it
                    if (!it.selection.collapsed && selectionRanges.lastOrNull() != it.selection) {
                        selectionRanges.add(it.selection)
                    }
                },
                visualTransformation = visualTransformation,
                modifier = Modifier.testTag(testTag)
            )
        }

        // selection is not shown
        rule.onAllNodes(isPopup()).assertCountEquals(0)

        // long click on textfield, selection should start
        rule.onNodeWithTag(testTag).performTouchInput { longClick() }
        rule.waitForIdle()

        // all text should be selected now
        assertThat(textFieldValue.value.selection.start).isEqualTo(0)
        assertThat(textFieldValue.value.selection.end).isEqualTo(text.length)

        // if selection will extend to the left, starting positions should be on the right
        performHandleDrag(Handle.SelectionStart, !toLeft)
        performHandleDrag(Handle.SelectionEnd, !toLeft)

        performHandleDrag(if (toLeft) Handle.SelectionStart else Handle.SelectionEnd, toLeft)

        assertThat(selectionRanges).containsAtLeastElementsIn(expectedSelectionRanges).inOrder()
    }

    private fun extraStarsVisualTransformation(): VisualTransformation {
        return VisualTransformation { text ->
            TransformedText(
                text = AnnotatedString(text.text.map { "$it*" }.joinToString("")),
                offsetMapping = object : OffsetMapping {
                    override fun originalToTransformed(offset: Int) = offset * 2
                    override fun transformedToOriginal(offset: Int) = offset / 2
                })
        }
    }

    private fun performHandleDrag(handle: Handle, toLeft: Boolean) {
        val handleNode = rule.onNode(isSelectionHandle(handle))
        val fieldWidth = rule.onNodeWithTag(testTag)
            .fetchSemanticsNode()
            .boundsInRoot.width.roundToInt()

        handleNode.performTouchInput {
            if (toLeft) {
                swipeLeft(startX = centerX, endX = left - fieldWidth, durationMillis = 1000)
            } else {
                swipeRight(startX = centerX, endX = right + fieldWidth, durationMillis = 1000)
            }
        }
    }
}