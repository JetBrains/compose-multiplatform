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

import androidx.compose.foundation.border
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.selection.isSelectionHandle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.FlakyTest
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt
import kotlin.test.assertFailsWith
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This test ensures that [BasicTextField] throws the right exceptions when the
 * [VisualTransformation]'s [OffsetMapping] is invalid. See b/229378536.
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
internal class TextFieldVisualTransformationSelectionBoundsTest {

    @get:Rule
    val rule = createComposeRule()
    private val testTag = "text field"
    private val invalidIndex = Int.MAX_VALUE
    private val text = "supercalifragilisticexpialidocious"

    // If we make the mapping return an invalid value immediately, we can't catch the exception
    // in the test, so we have to return a valid value until the runtime is initialized.
    private var isOriginalToTransformedValid = true
    private var isTransformedToOriginalValid = true

    @Before
    fun setUp() {
        var value by mutableStateOf(TextFieldValue(text))
        rule.setContent {
            BasicTextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier
                    .testTag(testTag)
                    .border(1.dp, Color.Black),
                visualTransformation = { original ->
                    TransformedText(
                        text = original,
                        offsetMapping = object : OffsetMapping {
                            override fun originalToTransformed(offset: Int): Int =
                                if (isOriginalToTransformedValid) offset else invalidIndex

                            override fun transformedToOriginal(offset: Int): Int =
                                if (isTransformedToOriginalValid) offset else invalidIndex
                        }
                    )
                }
            )
        }
    }

    @Test
    fun cursor_throws_onShow_whenInvalidOriginalToTransformed() {
        rule.runOnIdle {
            isOriginalToTransformedValid = false
        }

        // Show cursor handle.
        val error = assertFailsWith<IllegalStateException> {
            rule.onNodeWithTag(testTag).performClick()
            rule.waitForIdle()
        }
        assertValidMessage(error, sourceIndex = 0, toTransformed = true)
    }

    @Test
    fun cursor_throws_onShow_whenInvalidTransformedToOriginal() {
        rule.runOnIdle {
            isTransformedToOriginalValid = false
        }

        // Show cursor handle.
        val error = assertFailsWith<IllegalStateException> {
            rule.onNodeWithTag(testTag).performClick()
            rule.waitForIdle()
        }
        // Actual source index depends on text layout, and we don't really care.
        assertValidMessage(error, sourceIndex = null, toTransformed = false)
    }

    @Test
    fun cursor_throws_onDrag_whenInvalidOriginalToTransformed() {
        rule.onNodeWithTag(testTag).performClick()
        rule.runOnIdle {
            isOriginalToTransformedValid = false
        }

        // Show cursor handle.
        val error = assertFailsWith<IllegalStateException> {
            performHandleDrag(Handle.Cursor, distance = 1f)
        }

        // Actual source index depends on text layout, and we don't really care.
        assertValidMessage(error, sourceIndex = null, toTransformed = true)
    }

    @Test
    fun cursor_throws_onDrag_whenInvalidTransformedToOriginal() {
        rule.onNodeWithTag(testTag).performClick()
        rule.runOnIdle {
            isTransformedToOriginalValid = false
        }

        // Show cursor handle.
        val error = assertFailsWith<IllegalStateException> {
            performHandleDrag(Handle.Cursor, distance = 1f)
        }

        // Actual source index depends on text layout, and we don't really care.
        assertValidMessage(error, sourceIndex = null, toTransformed = false)
    }

    @Test
    fun selectionStart_throws_onStart_whenInvalidOriginalToTransformed() {
        rule.runOnIdle {
            isOriginalToTransformedValid = false
        }

        val error = assertFailsWith<IllegalStateException> {
            rule.onNodeWithTag(testTag).performTouchInput { longClick() }
            rule.waitForIdle()
        }

        assertValidMessage(error, sourceIndex = 0, toTransformed = true)
    }

    @Test
    fun selectionStart_throws_onStart_whenInvalidTransformedToOriginal() {
        rule.runOnIdle {
            isTransformedToOriginalValid = false
        }

        val error = assertFailsWith<IllegalStateException> {
            rule.onNodeWithTag(testTag).performTouchInput { longClick() }
            rule.waitForIdle()
        }

        assertValidMessage(error, sourceIndex = 0, toTransformed = false)
    }

    @FlakyTest(bugId = 241572024)
    @Test
    fun selectionEnd_throws_onStart_whenInvalidOriginalToTransformed() {
        rule.runOnIdle {
            isOriginalToTransformedValid = false
        }

        val error = assertFailsWith<IllegalStateException> {
            rule.onNodeWithTag(testTag).performTouchInput { longClick() }
            rule.waitForIdle()
        }

        assertValidMessage(error, sourceIndex = 0, toTransformed = true)
    }

    @Test
    fun selectionEnd_throws_onStart_whenInvalidTransformedToOriginal() {
        rule.runOnIdle {
            isTransformedToOriginalValid = false
        }

        val error = assertFailsWith<IllegalStateException> {
            rule.onNodeWithTag(testTag).performTouchInput { longClick() }
            rule.waitForIdle()
        }

        assertValidMessage(error, sourceIndex = 0, toTransformed = false)
    }

    @Test
    fun selectionStart_throws_onDrag_whenInvalidOriginalToTransformed() {
        rule.onNodeWithTag(testTag).performTouchInput { longClick() }
        rule.runOnIdle {
            isOriginalToTransformedValid = false
        }

        val error = assertFailsWith<IllegalStateException> {
            // Drag right since this is the start handle.
            performHandleDrag(Handle.SelectionStart, distance = 1f)
        }

        assertValidMessage(error, sourceIndex = 0, toTransformed = true)
    }

    @Test
    fun selectionStart_throws_onDrag_whenInvalidTransformedToOriginal() {
        rule.onNodeWithTag(testTag).performTouchInput { longClick() }
        rule.runOnIdle {
            isTransformedToOriginalValid = false
        }

        val error = assertFailsWith<IllegalStateException> {
            // Drag right since this is the start handle.
            performHandleDrag(Handle.SelectionStart, distance = 1f)
        }

        assertValidMessage(error, sourceIndex = 0, toTransformed = false)
    }

    @Test
    fun selectionEnd_throws_onDrag_whenInvalidOriginalToTransformed() {
        rule.onNodeWithTag(testTag).performTouchInput { longClick() }
        rule.runOnIdle {
            isOriginalToTransformedValid = false
        }

        val error = assertFailsWith<IllegalStateException> {
            // Drag left since this is the end handle.
            performHandleDrag(Handle.SelectionEnd, -1f)
        }

        assertValidMessage(error, sourceIndex = text.length, toTransformed = true)
    }

    @Test
    fun selectionEnd_throws_onDrag_whenInvalidTransformedToOriginal() {
        rule.onNodeWithTag(testTag).performTouchInput { longClick() }
        rule.runOnIdle {
            isTransformedToOriginalValid = false
        }

        val error = assertFailsWith<IllegalStateException> {
            // Drag left since this is the end handle.
            performHandleDrag(Handle.SelectionEnd, distance = -1f)
        }

        assertValidMessage(error, sourceIndex = 0, toTransformed = false)
    }

    private fun performHandleDrag(handle: Handle, distance: Float) {
        val handleNode = rule.onNode(isSelectionHandle(handle))
        val fieldWidth = rule.onNodeWithTag(testTag)
            .fetchSemanticsNode()
            .boundsInRoot.width.roundToInt()

        handleNode.performTouchInput {
            down(center)
        }

        repeat(fieldWidth) {
            handleNode.performTouchInput {
                moveBy(Offset(x = distance, y = 0f))
            }
        }
    }

    private fun assertValidMessage(error: Throwable, sourceIndex: Int?, toTransformed: Boolean) {
        val methodName = if (toTransformed) "originalToTransformed" else "transformedToOriginal"
        val label = if (toTransformed) "transformed" else "original"
        val sourceIndexPattern = sourceIndex?.toString() ?: "\\d+"
        assertThat(error).hasMessageThat().matches(
            "OffsetMapping\\.$methodName returned invalid mapping: " +
                "$sourceIndexPattern -> $invalidIndex is not in range of $label text " +
                "\\[0, ${text.length}]"
        )
    }
}