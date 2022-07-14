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

package androidx.compose.ui.text.input

import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.TextRange
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(InternalTextApi::class)
@RunWith(JUnit4::class)
class EditProcessorTest {

    @Test
    fun test_new_state_and_edit_commands() {
        val proc = EditProcessor()
        val tis: TextInputSession = mock()

        val model = TextFieldValue("ABCDE", TextRange.Zero)
        proc.reset(model, tis)

        assertThat(proc.mBufferState).isEqualTo(model)
        val captor = argumentCaptor<TextFieldValue>()
        verify(tis, times(1)).updateState(
            eq(TextFieldValue("", TextRange.Zero)),
            captor.capture()
        )
        assertThat(captor.allValues.size).isEqualTo(1)
        assertThat(captor.firstValue.text).isEqualTo("ABCDE")
        assertThat(captor.firstValue.selection.min).isEqualTo(0)
        assertThat(captor.firstValue.selection.max).isEqualTo(0)

        reset(tis)

        val newState = proc.apply(
            listOf(
                CommitTextCommand("X", 1)
            )
        )

        assertThat(newState.text).isEqualTo("XABCDE")
        assertThat(newState.selection.min).isEqualTo(1)
        assertThat(newState.selection.max).isEqualTo(1)
        // onEditCommands should not fire onStateUpdated since need to pass it to developer first.
        verify(tis, never()).updateState(any(), any())
    }

    @Test
    fun testNewState_bufferNotUpdated_ifSameModelStructurally() {
        val processor = EditProcessor()
        val textInputSession = mock<TextInputSession>()

        val initialBuffer = processor.mBuffer
        processor.reset(
            TextFieldValue("qwerty", TextRange.Zero, TextRange.Zero),
            textInputSession
        )
        assertThat(processor.mBuffer).isNotEqualTo(initialBuffer)

        val updatedBuffer = processor.mBuffer
        processor.reset(
            TextFieldValue("qwerty", TextRange.Zero, TextRange.Zero),
            textInputSession
        )
        assertThat(processor.mBuffer).isEqualTo(updatedBuffer)
    }

    @Test
    fun testNewState_new_buffer_created_if_text_is_different() {
        val processor = EditProcessor()
        val textInputSession = mock<TextInputSession>()

        val textFieldValue = TextFieldValue("qwerty", TextRange.Zero, TextRange.Zero)
        processor.reset(
            textFieldValue,
            textInputSession
        )
        val initialBuffer = processor.mBuffer

        val newTextFieldValue = textFieldValue.copy("abc")
        processor.reset(
            newTextFieldValue,
            textInputSession
        )

        assertThat(processor.mBuffer).isNotEqualTo(initialBuffer)
    }

    @Test
    fun testNewState_buffer_not_recreated_if_selection_is_different() {
        val processor = EditProcessor()
        val textInputSession = mock<TextInputSession>()
        val textFieldValue = TextFieldValue("qwerty", TextRange.Zero, TextRange.Zero)
        processor.reset(
            textFieldValue,
            textInputSession
        )
        val initialBuffer = processor.mBuffer

        val newTextFieldValue = textFieldValue.copy(selection = TextRange(1))
        processor.reset(
            newTextFieldValue,
            textInputSession
        )

        assertThat(processor.mBuffer).isEqualTo(initialBuffer)
        assertThat(newTextFieldValue.selection.start).isEqualTo(processor.mBuffer.selectionStart)
        assertThat(newTextFieldValue.selection.end).isEqualTo(processor.mBuffer.selectionEnd)
    }

    @Test
    fun testNewState_buffer_not_recreated_if_composition_is_different() {
        val processor = EditProcessor()
        val textInputSeson = mock<TextInputSession>()
        val textFieldValue = TextFieldValue("qwerty", TextRange.Zero, TextRange(1))
        processor.reset(
            textFieldValue,
            textInputSeson
        )
        val initialBuffer = processor.mBuffer

        // composition can not be set from app, IME owns it.
        assertThat(EditingBuffer.NOWHERE).isEqualTo(initialBuffer.compositionStart)
        assertThat(EditingBuffer.NOWHERE).isEqualTo(initialBuffer.compositionEnd)

        val newTextFieldValue = textFieldValue.copy(composition = null)
        processor.reset(
            newTextFieldValue,
            textInputSeson
        )

        assertThat(processor.mBuffer).isEqualTo(initialBuffer)
        assertThat(EditingBuffer.NOWHERE).isEqualTo(processor.mBuffer.compositionStart)
        assertThat(EditingBuffer.NOWHERE).isEqualTo(processor.mBuffer.compositionEnd)
    }

    @Test
    fun testNewState_reversedSelection_setsTheSelection() {
        val processor = EditProcessor()
        val textInputSession = mock<TextInputSession>()
        val initialSelection = TextRange(2, 1)
        val textFieldValue = TextFieldValue("qwerty", initialSelection, TextRange(1))

        // set the initial selection to be reversed
        processor.reset(
            textFieldValue,
            textInputSession
        )
        val initialBuffer = processor.mBuffer

        assertThat(initialSelection.min).isEqualTo(initialBuffer.selectionStart)
        assertThat(initialSelection.max).isEqualTo(initialBuffer.selectionEnd)

        val updatedSelection = TextRange(3, 0)
        val newTextFieldValue = textFieldValue.copy(selection = updatedSelection)
        // set the new selection
        processor.reset(
            newTextFieldValue,
            textInputSession
        )

        assertThat(processor.mBuffer).isEqualTo(initialBuffer)
        assertThat(updatedSelection.min).isEqualTo(initialBuffer.selectionStart)
        assertThat(updatedSelection.max).isEqualTo(initialBuffer.selectionEnd)
    }

    @Test
    fun compositionIsCleared_when_textChanged() {
        val processor = EditProcessor()
        val textInputSession = mock<TextInputSession>()

        // set the initial value
        processor.apply(
            listOf(
                CommitTextCommand("ab", 0),
                SetComposingRegionCommand(0, 2)
            )
        )

        // change the text
        val newValue = processor.mBufferState.copy(text = "cd")
        processor.reset(newValue, textInputSession)

        assertThat(processor.mBufferState.text).isEqualTo(newValue.text)
        assertThat(processor.mBufferState.composition).isNull()
    }

    @Test
    fun compositionIsNotCleared_when_textIsSame() {
        val processor = EditProcessor()
        val textInputSession = mock<TextInputSession>()
        val composition = TextRange(0, 2)

        // set the initial value
        processor.apply(
            listOf(
                CommitTextCommand("ab", 0),
                SetComposingRegionCommand(composition.start, composition.end)
            )
        )

        // use the same TextFieldValue
        val newValue = processor.mBufferState.copy()
        processor.reset(newValue, textInputSession)

        assertThat(processor.mBufferState.text).isEqualTo(newValue.text)
        assertThat(processor.mBufferState.composition).isEqualTo(composition)
    }

    @Test
    fun compositionIsCleared_when_compositionReset() {
        val processor = EditProcessor()
        val textInputSession = mock<TextInputSession>()

        // set the initial value
        processor.apply(
            listOf(
                CommitTextCommand("ab", 0),
                SetComposingRegionCommand(-1, -1)
            )
        )

        // change the composition
        val newValue = processor.mBufferState.copy(composition = TextRange(0, 2))
        processor.reset(newValue, textInputSession)

        assertThat(processor.mBufferState.text).isEqualTo(newValue.text)
        assertThat(processor.mBufferState.composition).isNull()
    }

    @Test
    fun compositionIsCleared_when_compositionChanged() {
        val processor = EditProcessor()
        val textInputSession = mock<TextInputSession>()

        // set the initial value
        processor.apply(
            listOf(
                CommitTextCommand("ab", 0),
                SetComposingRegionCommand(0, 2)
            )
        )

        // change the composition
        val newValue = processor.mBufferState.copy(composition = TextRange(0, 1))
        processor.reset(newValue, textInputSession)

        assertThat(processor.mBufferState.text).isEqualTo(newValue.text)
        assertThat(processor.mBufferState.composition).isNull()
    }

    @Test
    fun compositionIsNotCleared_when_onlySelectionChanged() {
        val processor = EditProcessor()
        val textInputSession = mock<TextInputSession>()
        val composition = TextRange(0, 2)
        val selection = TextRange(0, 2)

        // set the initial value
        processor.apply(
            listOf(
                CommitTextCommand("ab", 0),
                SetComposingRegionCommand(composition.start, composition.end),
                SetSelectionCommand(selection.start, selection.end)
            )
        )

        // change selection
        val newSelection = TextRange(1)
        val newValue = processor.mBufferState.copy(selection = newSelection)
        processor.reset(newValue, textInputSession)

        assertThat(processor.mBufferState.text).isEqualTo(newValue.text)
        assertThat(processor.mBufferState.composition).isEqualTo(composition)
        assertThat(processor.mBufferState.selection).isEqualTo(newSelection)
    }

    @Test
    fun throwsDescriptiveMessage_whenCommandFailsInBatch() {
        fun invalidCommand(index: Int) = object : EditCommand {
            override fun applyTo(buffer: EditingBuffer) {
                throw RuntimeException("Better luck next time")
            }

            override fun toStringForLog(): String = "InvalidCommand(index=$index)"
        }

        val processor = EditProcessor().apply {
            mBuffer.replace(0, 0, "hello world")
            mBuffer.setSelection(0, 5)
            mBuffer.setComposition(5, 7)
        }
        val batch = listOf(
            CommitTextCommand("ab", 0),
            invalidCommand(42),
            SetSelectionCommand(0, 2),
        )

        val error = assertFailsWith<RuntimeException> {
            processor.apply(batch)
        }

        assertThat(error).hasMessageThat().isEqualTo(
            "Error while applying EditCommand batch to buffer " +
                "(length=11, composition=null, selection=TextRange(5, 5)):\n" +
                "   CommitTextCommand(text.length=2, newCursorPosition=0)\n" +
                " > InvalidCommand(index=42)\n" +
                "   SetSelectionCommand(start=0, end=2)"
        )
        assertThat(error).hasCauseThat().hasMessageThat().isEqualTo("Better luck next time")
    }
}