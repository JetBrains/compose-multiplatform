/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.MultiParagraphIntrinsics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.EditProcessor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TextInputService
import androidx.compose.ui.text.input.TextInputSession
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(InternalFoundationTextApi::class)
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
    private lateinit var textLayoutResultProxy: TextLayoutResultProxy
    private lateinit var textLayoutResult: TextLayoutResult

    /**
     * Test implementation of offset map which doubles the offset in transformed text.
     */
    private val skippingOffsetMap = object : OffsetMapping {
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
        textLayoutResultProxy = mock()
        whenever(textLayoutResultProxy.value).thenReturn(textLayoutResult)
    }

    @Test
    fun test_setCursorOffset() {
        val position = Offset(100f, 200f)
        val offset = 10
        val editorState = TextFieldValue(text = "Hello, World", selection = TextRange(1))
        whenever(processor.toTextFieldValue()).thenReturn(editorState)
        whenever(textLayoutResultProxy.getOffsetForPosition(position)).thenReturn(offset)

        TextFieldDelegate.setCursorOffset(
            position,
            textLayoutResultProxy,
            processor,
            OffsetMapping.Identity,
            onValueChange
        )

        verify(onValueChange, times(1)).invoke(
            eq(TextFieldValue(text = editorState.text, selection = TextRange(offset)))
        )
    }

    @Test
    fun on_focus() {
        val editorState = TextFieldValue(text = "Hello, World", selection = TextRange(1))
        val imeOptions = ImeOptions(
            singleLine = true,
            capitalization = KeyboardCapitalization.Sentences,
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Search
        )

        val textInputSession: TextInputSession = mock()
        whenever(
            textInputService.startInput(
                eq(editorState),
                eq(imeOptions),
                any(),
                eq(onEditorActionPerformed)
            )
        ).thenReturn(textInputSession)

        val actual = TextFieldDelegate.onFocus(
            textInputService = textInputService,
            value = editorState,
            editProcessor = processor,
            imeOptions = imeOptions,
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
            eq(imeOptions),
            any(),
            eq(onEditorActionPerformed)
        )

        assertThat(actual).isEqualTo(textInputSession)
    }

    @Test
    fun on_blur_with_hiding() {
        val editorState = TextFieldValue(
            text = "Hello, World",
            selection = TextRange(1),
            composition = TextRange(3, 5)
        )
        whenever(processor.toTextFieldValue()).thenReturn(editorState)

        val textInputSession = mock<TextInputSession>()

        TextFieldDelegate.onBlur(textInputSession, processor, onValueChange)

        inOrder(textInputSession) {
            verify(textInputSession).dispose()
        }
        verify(onValueChange, times(1)).invoke(
            eq(editorState.copy(composition = null))
        )
    }

    @Test
    fun check_setCursorOffset_uses_offset_map() {
        val position = Offset(100f, 200f)
        val offset = 10
        val editorState = TextFieldValue(text = "Hello, World", selection = TextRange(1))
        whenever(processor.toTextFieldValue()).thenReturn(editorState)
        whenever(textLayoutResultProxy.getOffsetForPosition(position)).thenReturn(offset)

        TextFieldDelegate.setCursorOffset(
            position,
            textLayoutResultProxy,
            processor,
            skippingOffsetMap,
            onValueChange
        )

        verify(onValueChange, times(1)).invoke(
            eq(TextFieldValue(text = editorState.text, selection = TextRange(offset / 2)))
        )
    }

    @Test
    fun use_identity_mapping_if_none_visual_transformation() {
        val transformedText = VisualTransformation.None.filter(
            AnnotatedString(text = "Hello, World")
        )
        val visualText = transformedText.text
        val offsetMapping = transformedText.offsetMapping

        assertThat(visualText.text).isEqualTo("Hello, World")
        for (i in 0..visualText.text.length) {
            // Identity mapping returns if no visual filter is provided.
            assertThat(offsetMapping.originalToTransformed(i)).isEqualTo(i)
            assertThat(offsetMapping.transformedToOriginal(i)).isEqualTo(i)
        }
    }

    @Test
    fun apply_composition_decoration() {
        val identityOffsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = offset
            override fun transformedToOriginal(offset: Int): Int = offset
        }

        val input = TransformedText(
            text = AnnotatedString.Builder().apply {
                pushStyle(SpanStyle(color = Color.Red))
                append("Hello, World")
            }.toAnnotatedString(),
            offsetMapping = identityOffsetMapping
        )

        val result = TextFieldDelegate.applyCompositionDecoration(
            compositionRange = TextRange(3, 6),
            transformed = input
        )

        assertThat(result.text.text).isEqualTo(input.text.text)
        assertThat(result.text.spanStyles.size).isEqualTo(2)
        assertThat(result.text.spanStyles).contains(
            AnnotatedString.Range(SpanStyle(textDecoration = TextDecoration.Underline), 3, 6)
        )
    }

    @Test
    fun apply_composition_decoration_with_offsetmap() {
        val offsetAmount = 5
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = offsetAmount + offset
            override fun transformedToOriginal(offset: Int): Int = offset - offsetAmount
        }

        val input = TransformedText(
            text = AnnotatedString.Builder().apply {
                append(" ".repeat(offsetAmount))
                append("Hello World")
            }.toAnnotatedString(),
            offsetMapping = offsetMapping
        )

        val range = TextRange(0, 2)
        val result = TextFieldDelegate.applyCompositionDecoration(
            compositionRange = range,
            transformed = input
        )

        assertThat(result.text.spanStyles.size).isEqualTo(1)
        assertThat(result.text.spanStyles).contains(
            AnnotatedString.Range(
                SpanStyle(textDecoration = TextDecoration.Underline),
                range.start + offsetAmount,
                range.end + offsetAmount
            )
        )
    }
}