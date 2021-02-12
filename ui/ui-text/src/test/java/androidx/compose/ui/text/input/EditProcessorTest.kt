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
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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

        assertEquals(model, proc.mBufferState)
        val captor = argumentCaptor<TextFieldValue>()
        verify(tis, times(1)).updateState(
            eq(TextFieldValue("", TextRange.Zero)),
            captor.capture()
        )
        assertEquals(1, captor.allValues.size)
        assertEquals("ABCDE", captor.firstValue.text)
        assertEquals(0, captor.firstValue.selection.min)
        assertEquals(0, captor.firstValue.selection.max)

        reset(tis)

        val newState = proc.apply(
            listOf(
                CommitTextCommand("X", 1)
            )
        )

        assertEquals("XABCDE", newState.text)
        assertEquals(1, newState.selection.min)
        assertEquals(1, newState.selection.max)
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
        assertNotEquals(initialBuffer, processor.mBuffer)

        val updatedBuffer = processor.mBuffer
        processor.reset(
            TextFieldValue("qwerty", TextRange.Zero, TextRange.Zero),
            textInputSession
        )
        assertEquals(updatedBuffer, processor.mBuffer)
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

        assertNotEquals(initialBuffer, processor.mBuffer)
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

        assertEquals(initialBuffer, processor.mBuffer)
        assertEquals(processor.mBuffer.selectionStart, newTextFieldValue.selection.start)
        assertEquals(processor.mBuffer.selectionEnd, newTextFieldValue.selection.end)
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
        assertEquals(initialBuffer.compositionStart, EditingBuffer.NOWHERE)
        assertEquals(initialBuffer.compositionEnd, EditingBuffer.NOWHERE)

        val newTextFieldValue = textFieldValue.copy(composition = null)
        processor.reset(
            newTextFieldValue,
            textInputSeson
        )

        assertEquals(initialBuffer, processor.mBuffer)
        assertEquals(processor.mBuffer.compositionStart, EditingBuffer.NOWHERE)
        assertEquals(processor.mBuffer.compositionEnd, EditingBuffer.NOWHERE)
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

        assertEquals(initialBuffer.selectionStart, initialSelection.min)
        assertEquals(initialBuffer.selectionEnd, initialSelection.max)

        val updatedSelection = TextRange(3, 0)
        val newTextFieldValue = textFieldValue.copy(selection = updatedSelection)
        // set the new selection
        processor.reset(
            newTextFieldValue,
            textInputSession
        )

        assertEquals(initialBuffer, processor.mBuffer)
        assertEquals(initialBuffer.selectionStart, updatedSelection.min)
        assertEquals(initialBuffer.selectionEnd, updatedSelection.max)
    }
}