/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.MultiParagraphIntrinsics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextDelegate
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.CommitTextEditOp
import androidx.compose.ui.text.input.EditOperation
import androidx.compose.ui.text.input.EditProcessor
import androidx.compose.ui.text.input.FinishComposingTextEditOp
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMap
import androidx.compose.ui.text.input.SetSelectionEditOp
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TextInputService
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(
    InternalTextApi::class,
    ExperimentalTextApi::class
)
@RunWith(JUnit4::class)
class TextFieldDelegateTest {

    private lateinit var canvas: Canvas
    private lateinit var mDelegate: TextDelegate
    private lateinit var processor: EditProcessor
    private lateinit var onValueChange: (TextFieldValue) -> Unit
    private lateinit var onEditorActionPerformed: (Any) -> Unit
    private lateinit var textInputService: TextInputService
    private lateinit var layoutCoordinates: LayoutCoordinates
    private lateinit var multiParagraphIntrinsics: MultiParagraphIntrinsics
    private lateinit var textLayoutResult: TextLayoutResult

    private val layoutDirection = LayoutDirection.Ltr

    /**
     * Test implementation of offset map which doubles the offset in transformed text.
     */
    private val skippingOffsetMap = object : OffsetMap {
        override fun originalToTransformed(offset: Int): Int = offset * 2
        override fun transformedToOriginal(offset: Int): Int = offset / 2
    }

    @Before
    fun setup() {
        mDelegate = mock()
        canvas = mock()
        processor = mock()
        onValueChange = mock()
        onEditorActionPerformed = mock()
        textInputService = mock()
        layoutCoordinates = mock()
        multiParagraphIntrinsics = mock()
        textLayoutResult = mock()
    }

    @Test
    fun test_on_edit_command() {
        val ops = listOf(CommitTextEditOp("Hello, World", 1))
        val editorState = TextFieldValue(text = "Hello, World", selection = TextRange(1))

        whenever(processor.onEditCommands(ops)).thenReturn(editorState)

        TextFieldDelegate.onEditCommand(ops, processor, onValueChange)

        verify(onValueChange, times(1)).invoke(
            eq(
                TextFieldValue(
                    text = editorState.text,
                    selection = editorState.selection
                )
            )
        )
    }

    @Test
    fun test_setCursorOffset() {
        val position = Offset(100f, 200f)
        val offset = 10
        val editorState = TextFieldValue(text = "Hello, World", selection = TextRange(1))

        whenever(textLayoutResult.getOffsetForPosition(position)).thenReturn(offset)

        val captor = argumentCaptor<List<EditOperation>>()

        whenever(processor.onEditCommands(captor.capture())).thenReturn(editorState)

        TextFieldDelegate.setCursorOffset(
            position,
            textLayoutResult,
            processor,
            OffsetMap.identityOffsetMap,
            onValueChange
        )

        assertThat(captor.allValues.size).isEqualTo(1)
        assertThat(captor.firstValue.size).isEqualTo(1)
        assertThat(captor.firstValue[0] is SetSelectionEditOp).isTrue()
        verify(onValueChange, times(1)).invoke(
            eq(
                TextFieldValue(
                    text = editorState.text,
                    selection = editorState.selection
                )
            )
        )
    }

    @Test
    fun on_focus() {
        val editorState = TextFieldValue(text = "Hello, World", selection = TextRange(1))
        val keyboardOptions = KeyboardOptions(
            singleLine = true,
            capitalization = KeyboardCapitalization.Sentences
        )
        val keyboardType = KeyboardType.Phone
        val imeAction = ImeAction.Search

        TextFieldDelegate.onFocus(
            textInputService = textInputService,
            value = editorState,
            editProcessor = processor,
            keyboardType = keyboardType,
            imeAction = imeAction,
            keyboardOptions = keyboardOptions,
            onValueChange = onValueChange,
            onImeActionPerformed = onEditorActionPerformed
        )
        verify(textInputService).startInput(
            eq(
                TextFieldValue(
                    text = editorState.text,
                    selection = editorState.selection
                )
            ),
            eq(keyboardType),
            eq(imeAction),
            eq(keyboardOptions),
            any(),
            eq(onEditorActionPerformed)
        )

        verify(textInputService).showSoftwareKeyboard(any())
    }

    @Test
    fun on_blur() {
        val captor = argumentCaptor<List<EditOperation>>()
        val inputSessionToken = 10 // We are not using this value in this test.

        whenever(processor.onEditCommands(captor.capture())).thenReturn(TextFieldValue())

        TextFieldDelegate.onBlur(
            textInputService,
            inputSessionToken,
            processor,
            true,
            onValueChange
        )

        assertThat(captor.allValues.size).isEqualTo(1)
        assertThat(captor.firstValue.size).isEqualTo(1)
        assertThat(captor.firstValue[0] is FinishComposingTextEditOp).isTrue()
        verify(textInputService).stopInput(eq(inputSessionToken))
        verify(textInputService, never()).hideSoftwareKeyboard(any())
    }

    @Test
    fun on_blur_with_hiding() {
        val captor = argumentCaptor<List<EditOperation>>()
        val inputSessionToken = 10 // We are not using this value in this test.

        whenever(processor.onEditCommands(captor.capture())).thenReturn(TextFieldValue())

        TextFieldDelegate.onBlur(
            textInputService,
            inputSessionToken,
            processor,
            false, // There is no next focused client. Hide the keyboard.
            onValueChange
        )

        assertThat(captor.allValues.size).isEqualTo(1)
        assertThat(captor.firstValue.size).isEqualTo(1)
        assertThat(captor.firstValue[0] is FinishComposingTextEditOp).isTrue()
        verify(textInputService).stopInput(eq(inputSessionToken))
        verify(textInputService).hideSoftwareKeyboard(eq(inputSessionToken))
    }

    @Test
    fun notify_focused_rect() {
        val rect = Rect(0f, 1f, 2f, 3f)
        whenever(textLayoutResult.getBoundingBox(any())).thenReturn(rect)
        val point = Offset(5f, 6f)
        layoutCoordinates = MockCoordinates(
            rootOffset = point
        )
        val editorState = TextFieldValue(text = "Hello, World", selection = TextRange(1))
        val inputSessionToken = 10 // We are not using this value in this test.
        TextFieldDelegate.notifyFocusedRect(
            editorState,
            mDelegate,
            textLayoutResult,
            layoutCoordinates,
            textInputService,
            inputSessionToken,
            true /* hasFocus */,
            OffsetMap.identityOffsetMap
        )
        verify(textInputService).notifyFocusedRect(eq(inputSessionToken), any())
    }

    @Test
    fun notify_focused_rect_without_focus() {
        val editorState = TextFieldValue(text = "Hello, World", selection = TextRange(1))
        val inputSessionToken = 10 // We are not using this value in this test.
        TextFieldDelegate.notifyFocusedRect(
            editorState,
            mDelegate,
            textLayoutResult,
            layoutCoordinates,
            textInputService,
            inputSessionToken,
            false /* hasFocus */,
            OffsetMap.identityOffsetMap
        )
        verify(textInputService, never()).notifyFocusedRect(any(), any())
    }

    @Test
    fun notify_rect_tail() {
        val rect = Rect(0f, 1f, 2f, 3f)
        whenever(textLayoutResult.getBoundingBox(any())).thenReturn(rect)
        val point = Offset(5f, 6f)
        layoutCoordinates = MockCoordinates(
            rootOffset = point
        )
        val editorState = TextFieldValue(text = "Hello, World", selection = TextRange(12))
        val inputSessionToken = 10 // We are not using this value in this test.
        TextFieldDelegate.notifyFocusedRect(
            editorState,
            mDelegate,
            textLayoutResult,
            layoutCoordinates,
            textInputService,
            inputSessionToken,
            true /* hasFocus */,
            OffsetMap.identityOffsetMap
        )
        verify(textInputService).notifyFocusedRect(eq(inputSessionToken), any())
    }

    @Test
    fun check_notify_rect_uses_offset_map() {
        val rect = Rect(0f, 1f, 2f, 3f)
        val point = Offset(5f, 6f)
        val editorState = TextFieldValue(text = "Hello, World", selection = TextRange(1, 3))
        val inputSessionToken = 10 // We are not using this value in this test.
        whenever(textLayoutResult.getBoundingBox(any())).thenReturn(rect)
        layoutCoordinates = MockCoordinates(
            rootOffset = point
        )

        TextFieldDelegate.notifyFocusedRect(
            editorState,
            mDelegate,
            textLayoutResult,
            layoutCoordinates,
            textInputService,
            inputSessionToken,
            true /* hasFocus */,
            skippingOffsetMap
        )
        verify(textLayoutResult).getBoundingBox(6)
        verify(textInputService).notifyFocusedRect(eq(inputSessionToken), any())
    }

    @Test
    fun check_setCursorOffset_uses_offset_map() {
        val position = Offset(100f, 200f)
        val offset = 10
        val editorState = TextFieldValue(text = "Hello, World", selection = TextRange(1))

        whenever(textLayoutResult.getOffsetForPosition(position)).thenReturn(offset)

        val captor = argumentCaptor<List<EditOperation>>()

        whenever(processor.onEditCommands(captor.capture())).thenReturn(editorState)

        TextFieldDelegate.setCursorOffset(
            position,
            textLayoutResult,
            processor,
            skippingOffsetMap,
            onValueChange
        )

        val cursorOffsetInTransformedText = offset / 2
        assertThat(captor.allValues.size).isEqualTo(1)
        assertThat(captor.firstValue.size).isEqualTo(1)
        assertThat(captor.firstValue[0] is SetSelectionEditOp).isTrue()
        val setSelectionEditOp = captor.firstValue[0] as SetSelectionEditOp
        assertThat(setSelectionEditOp.start).isEqualTo(cursorOffsetInTransformedText)
        assertThat(setSelectionEditOp.end).isEqualTo(cursorOffsetInTransformedText)
        verify(onValueChange, times(1)).invoke(
            eq(
                TextFieldValue(
                    text = editorState.text,
                    selection = editorState.selection
                )
            )
        )
    }

    @Test
    fun use_identity_mapping_if_none_visual_transformation() {
        val (visualText, offsetMap) =
            VisualTransformation.None.filter(AnnotatedString(text = "Hello, World"))

        assertEquals("Hello, World", visualText.text)
        for (i in 0..visualText.text.length) {
            // Identity mapping returns if no visual filter is provided.
            assertThat(offsetMap.originalToTransformed(i)).isEqualTo(i)
            assertThat(offsetMap.transformedToOriginal(i)).isEqualTo(i)
        }
    }

    @Test
    fun apply_composition_decoration() {
        val identityOffsetMap = object : OffsetMap {
            override fun originalToTransformed(offset: Int): Int = offset
            override fun transformedToOriginal(offset: Int): Int = offset
        }

        val input = TransformedText(
            transformedText = AnnotatedString.Builder().apply {
                pushStyle(SpanStyle(color = Color.Red))
                append("Hello, World")
            }.toAnnotatedString(),
            offsetMap = identityOffsetMap
        )

        val result = TextFieldDelegate.applyCompositionDecoration(
            compositionRange = TextRange(3, 6),
            transformed = input
        )

        assertThat(result.transformedText.text).isEqualTo(input.transformedText.text)
        assertThat(result.transformedText.spanStyles.size).isEqualTo(2)
        assertThat(result.transformedText.spanStyles).contains(
            AnnotatedString.Range(SpanStyle(textDecoration = TextDecoration.Underline), 3, 6)
        )
    }

    @Test
    fun apply_composition_decoration_with_offsetmap() {
        val offsetAmount = 5
        val offsetMap = object : OffsetMap {
            override fun originalToTransformed(offset: Int): Int = offsetAmount + offset
            override fun transformedToOriginal(offset: Int): Int = offset - offsetAmount
        }

        val input = TransformedText(
            transformedText = AnnotatedString.Builder().apply {
                append(" ".repeat(offsetAmount))
                append("Hello World")
            }.toAnnotatedString(),
            offsetMap = offsetMap
        )

        val range = TextRange(0, 2)
        val result = TextFieldDelegate.applyCompositionDecoration(
            compositionRange = range,
            transformed = input
        )

        assertThat(result.transformedText.spanStyles.size).isEqualTo(1)
        assertThat(result.transformedText.spanStyles).contains(
            AnnotatedString.Range(
                SpanStyle(textDecoration = TextDecoration.Underline),
                range.start + offsetAmount,
                range.end + offsetAmount
            )
        )
    }

    private class MockCoordinates(
        override val size: IntSize = IntSize.Zero,
        val localOffset: Offset = Offset.Zero,
        val globalOffset: Offset = Offset.Zero,
        val rootOffset: Offset = Offset.Zero
    ) : LayoutCoordinates {
        override val providedAlignmentLines: Set<AlignmentLine>
            get() = emptySet()
        override val parentCoordinates: LayoutCoordinates?
            get() = null
        override val isAttached: Boolean
            get() = true
        override fun globalToLocal(global: Offset): Offset = localOffset

        override fun localToGlobal(local: Offset): Offset = globalOffset

        override fun localToRoot(local: Offset): Offset = rootOffset

        override fun childToLocal(child: LayoutCoordinates, childLocal: Offset): Offset =
            Offset.Zero

        override fun childBoundingBox(child: LayoutCoordinates): Rect = Rect.Zero

        override fun get(line: AlignmentLine): Int = 0
    }
}