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
        val tis: TextInputService = mock()
        val inputSessionToken = 10 // We are not using this value in this test.

        val model = TextFieldValue("ABCDE", TextRange.Zero)
        proc.onNewState(model, tis, inputSessionToken)
        assertEquals(model, proc.mPreviousState)
        val captor = argumentCaptor<TextFieldValue>()
        verify(tis, times(1)).onStateUpdated(eq(inputSessionToken), captor.capture())
        assertEquals(1, captor.allValues.size)
        assertEquals("ABCDE", captor.firstValue.text)
        assertEquals(0, captor.firstValue.selection.min)
        assertEquals(0, captor.firstValue.selection.max)

        reset(tis)
        val newState = proc.onEditCommands(
            listOf(
                CommitTextEditOp("X", 1)
            )
        )

        assertEquals("XABCDE", newState.text)
        assertEquals(1, newState.selection.min)
        assertEquals(1, newState.selection.max)
        // onEditCommands should not fire onStateUpdated since need to pass it to developer first.
        verify(tis, never()).onStateUpdated(any(), any())
    }

    @Test
    fun textNewState_bufferNotUpdated_ifSameModelStructurally() {
        val processor = EditProcessor()
        val textInputService = mock<TextInputService>()
        val token = 10 // mock token value

        val initialBuffer = processor.mBuffer
        processor.onNewState(
            TextFieldValue("qwerty", TextRange.Zero, TextRange.Zero),
            textInputService,
            token
        )
        assertNotEquals(initialBuffer, processor.mBuffer)

        val updatedBuffer = processor.mBuffer
        processor.onNewState(
            TextFieldValue("qwerty", TextRange.Zero, TextRange.Zero),
            textInputService,
            token
        )
        assertEquals(
            updatedBuffer,
            processor.mBuffer
        )
    }
}