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

package androidx.compose.ui.input

import android.view.KeyEvent
import android.view.inputmethod.CorrectionInfo
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.BackspaceKeyEditOp
import androidx.compose.ui.text.input.CommitTextEditOp
import androidx.compose.ui.text.input.DeleteSurroundingTextEditOp
import androidx.compose.ui.text.input.DeleteSurroundingTextInCodePointsEditOp
import androidx.compose.ui.text.input.EditOperation
import androidx.compose.ui.text.input.FinishComposingTextEditOp
import androidx.compose.ui.text.input.InputEventListener
import androidx.compose.ui.text.input.MoveCursorEditOp
import androidx.compose.ui.text.input.RecordingInputConnection
import androidx.compose.ui.text.input.SetComposingRegionEditOp
import androidx.compose.ui.text.input.SetComposingTextEditOp
import androidx.compose.ui.text.input.SetSelectionEditOp
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.filters.SmallTest
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class RecordingInputConnectionTest {

    private lateinit var ic: RecordingInputConnection
    private lateinit var listener: InputEventListener

    @Before
    fun setup() {
        listener = mock()
        ic = RecordingInputConnection(
            initState = TextFieldValue("", TextRange.Zero),
            eventListener = listener,
            autoCorrect = true
        )
    }

    @Test
    fun getTextBeforeAndAfterCursorTest() {
        assertEquals("", ic.getTextBeforeCursor(100, 0))
        assertEquals("", ic.getTextAfterCursor(100, 0))

        // Set "Hello, World", and place the cursor at the beginning of the text.
        ic.mTextFieldValue = TextFieldValue(
            text = "Hello, World",
            selection = TextRange.Zero
        )

        assertEquals("", ic.getTextBeforeCursor(100, 0))
        assertEquals("Hello, World", ic.getTextAfterCursor(100, 0))

        // Set "Hello, World", and place the cursor between "H" and "e".
        ic.mTextFieldValue = TextFieldValue(
            text = "Hello, World",
            selection = TextRange(1)
        )

        assertEquals("H", ic.getTextBeforeCursor(100, 0))
        assertEquals("ello, World", ic.getTextAfterCursor(100, 0))

        // Set "Hello, World", and place the cursor at the end of the text.
        ic.mTextFieldValue = TextFieldValue(
            text = "Hello, World",
            selection = TextRange(12)
        )

        assertEquals("Hello, World", ic.getTextBeforeCursor(100, 0))
        assertEquals("", ic.getTextAfterCursor(100, 0))
    }

    @Test
    fun getTextBeforeAndAfterCursorTest_maxCharTest() {
        // Set "Hello, World", and place the cursor at the beginning of the text.
        ic.mTextFieldValue = TextFieldValue(
            text = "Hello, World",
            selection = TextRange.Zero
        )

        assertEquals("", ic.getTextBeforeCursor(5, 0))
        assertEquals("Hello", ic.getTextAfterCursor(5, 0))

        // Set "Hello, World", and place the cursor between "H" and "e".
        ic.mTextFieldValue = TextFieldValue(
            text = "Hello, World",
            selection = TextRange(1)
        )

        assertEquals("H", ic.getTextBeforeCursor(5, 0))
        assertEquals("ello,", ic.getTextAfterCursor(5, 0))

        // Set "Hello, World", and place the cursor at the end of the text.
        ic.mTextFieldValue = TextFieldValue(
            text = "Hello, World",
            selection = TextRange(12)
        )

        assertEquals("World", ic.getTextBeforeCursor(5, 0))
        assertEquals("", ic.getTextAfterCursor(5, 0))
    }

    @Test
    fun getSelectedTextTest() {
        // Set "Hello, World", and place the cursor at the beginning of the text.
        ic.mTextFieldValue = TextFieldValue(
            text = "Hello, World",
            selection = TextRange.Zero
        )

        assertEquals("", ic.getSelectedText(0))

        // Set "Hello, World", and place the cursor between "H" and "e".
        ic.mTextFieldValue = TextFieldValue(
            text = "Hello, World",
            selection = TextRange(0, 1)
        )

        assertEquals("H", ic.getSelectedText(0))

        // Set "Hello, World", and place the cursor at the end of the text.
        ic.mTextFieldValue = TextFieldValue(
            text = "Hello, World",
            selection = TextRange(0, 12)
        )

        assertEquals("Hello, World", ic.getSelectedText(0))
    }

    @Test
    fun commitTextTest() {
        val captor = argumentCaptor<List<EditOperation>>()

        ic.mTextFieldValue = TextFieldValue(text = "", selection = TextRange.Zero)

        // Inserting "Hello, " into the empty text field.
        assertTrue(ic.commitText("Hello, ", 1))

        verify(listener, times(1)).onEditOperations(captor.capture())
        val editOps = captor.lastValue
        assertEquals(1, editOps.size)
        assertEquals(CommitTextEditOp("Hello, ", 1), editOps[0])
    }

    @Test
    fun commitTextTest_batchSession() {
        val captor = argumentCaptor<List<EditOperation>>()

        ic.mTextFieldValue = TextFieldValue(text = "", selection = TextRange.Zero)

        // IME set text "Hello, World." with two commitText API within the single batch session.
        // Do not callback to listener during batch session.
        ic.beginBatchEdit()

        assertTrue(ic.commitText("Hello, ", 1))
        verify(listener, never()).onEditOperations(any())

        assertTrue(ic.commitText("World.", 1))
        verify(listener, never()).onEditOperations(any())

        ic.endBatchEdit()

        verify(listener, times(1)).onEditOperations(captor.capture())
        val editOps = captor.lastValue
        assertEquals(2, editOps.size)
        assertEquals(CommitTextEditOp("Hello, ", 1), editOps[0])
        assertEquals(CommitTextEditOp("World.", 1), editOps[1])
    }

    @Test
    fun setComposingRegion() {
        val captor = argumentCaptor<List<EditOperation>>()

        ic.mTextFieldValue = TextFieldValue(text = "Hello, World.", selection = TextRange.Zero)

        // Mark first "H" as composition.
        assertTrue(ic.setComposingRegion(0, 1))

        verify(listener, times(1)).onEditOperations(captor.capture())
        val editOps = captor.lastValue
        assertEquals(1, editOps.size)
        assertEquals(SetComposingRegionEditOp(0, 1), editOps[0])
    }

    @Test
    fun setComposingRegion_batchSession() {
        val captor = argumentCaptor<List<EditOperation>>()

        ic.mTextFieldValue = TextFieldValue(text = "Hello, World", selection = TextRange.Zero)

        // Do not callback to listener during batch session.
        ic.beginBatchEdit()

        assertTrue(ic.setComposingRegion(0, 1))
        verify(listener, never()).onEditOperations(any())

        assertTrue(ic.setComposingRegion(1, 2))
        verify(listener, never()).onEditOperations(any())

        ic.endBatchEdit()

        verify(listener, times(1)).onEditOperations(captor.capture())
        val editOps = captor.lastValue
        assertEquals(2, editOps.size)
        assertEquals(SetComposingRegionEditOp(0, 1), editOps[0])
        assertEquals(SetComposingRegionEditOp(1, 2), editOps[1])
    }

    @Test
    fun setComposingTextTest() {
        val captor = argumentCaptor<List<EditOperation>>()

        ic.mTextFieldValue = TextFieldValue(text = "", selection = TextRange.Zero)

        // Inserting "Hello, " into the empty text field.
        assertTrue(ic.setComposingText("Hello, ", 1))

        verify(listener, times(1)).onEditOperations(captor.capture())
        val editOps = captor.lastValue
        assertEquals(1, editOps.size)
        assertEquals(SetComposingTextEditOp("Hello, ", 1), editOps[0])
    }

    @Test
    fun setComposingTextTest_batchSession() {
        val captor = argumentCaptor<List<EditOperation>>()

        ic.mTextFieldValue = TextFieldValue(text = "", selection = TextRange.Zero)

        // IME set text "Hello, World." with two setComposingText API within the single batch
        // session. Do not callback to listener during batch session.
        ic.beginBatchEdit()

        assertTrue(ic.setComposingText("Hello, ", 1))
        verify(listener, never()).onEditOperations(any())

        assertTrue(ic.setComposingText("World.", 1))
        verify(listener, never()).onEditOperations(any())

        ic.endBatchEdit()

        verify(listener, times(1)).onEditOperations(captor.capture())
        val editOps = captor.lastValue
        assertEquals(2, editOps.size)
        assertEquals(SetComposingTextEditOp("Hello, ", 1), editOps[0])
        assertEquals(SetComposingTextEditOp("World.", 1), editOps[1])
    }

    @Test
    fun deleteSurroundingText() {
        val captor = argumentCaptor<List<EditOperation>>()

        ic.mTextFieldValue = TextFieldValue(text = "Hello, World.", selection = TextRange.Zero)

        // Delete first "Hello, " characters
        assertTrue(ic.deleteSurroundingText(0, 6))

        verify(listener, times(1)).onEditOperations(captor.capture())
        val editOps = captor.lastValue
        assertEquals(1, editOps.size)
        assertEquals(DeleteSurroundingTextEditOp(0, 6), editOps[0])
    }

    @Test
    fun deleteSurroundingText_batchSession() {
        val captor = argumentCaptor<List<EditOperation>>()

        ic.mTextFieldValue = TextFieldValue(text = "Hello, World", selection = TextRange.Zero)

        // Do not callback to listener during batch session.
        ic.beginBatchEdit()

        assertTrue(ic.deleteSurroundingText(0, 6))
        verify(listener, never()).onEditOperations(any())

        assertTrue(ic.deleteSurroundingText(0, 5))
        verify(listener, never()).onEditOperations(any())

        ic.endBatchEdit()

        verify(listener, times(1)).onEditOperations(captor.capture())
        val editOps = captor.lastValue
        assertEquals(2, editOps.size)
        assertEquals(DeleteSurroundingTextEditOp(0, 6), editOps[0])
        assertEquals(DeleteSurroundingTextEditOp(0, 5), editOps[1])
    }

    @Test
    fun deleteSurroundingTextInCodePoints() {
        val captor = argumentCaptor<List<EditOperation>>()

        ic.mTextFieldValue = TextFieldValue(text = "Hello, World.", selection = TextRange.Zero)

        // Delete first "Hello, " characters
        assertTrue(ic.deleteSurroundingTextInCodePoints(0, 6))

        verify(listener, times(1)).onEditOperations(captor.capture())
        val editOps = captor.lastValue
        assertEquals(1, editOps.size)
        assertEquals(DeleteSurroundingTextInCodePointsEditOp(0, 6), editOps[0])
    }

    @Test
    fun deleteSurroundingTextInCodePoints_batchSession() {
        val captor = argumentCaptor<List<EditOperation>>()

        ic.mTextFieldValue = TextFieldValue(text = "Hello, World", selection = TextRange.Zero)

        // Do not callback to listener during batch session.
        ic.beginBatchEdit()

        assertTrue(ic.deleteSurroundingTextInCodePoints(0, 6))
        verify(listener, never()).onEditOperations(any())

        assertTrue(ic.deleteSurroundingTextInCodePoints(0, 5))
        verify(listener, never()).onEditOperations(any())

        ic.endBatchEdit()

        verify(listener, times(1)).onEditOperations(captor.capture())
        val editOps = captor.lastValue
        assertEquals(2, editOps.size)
        assertEquals(DeleteSurroundingTextInCodePointsEditOp(0, 6), editOps[0])
        assertEquals(DeleteSurroundingTextInCodePointsEditOp(0, 5), editOps[1])
    }

    @Test
    fun setSelection() {
        val captor = argumentCaptor<List<EditOperation>>()

        ic.mTextFieldValue = TextFieldValue(text = "Hello, World.", selection = TextRange.Zero)

        // Select "Hello, "
        assertTrue(ic.setSelection(0, 6))

        verify(listener, times(1)).onEditOperations(captor.capture())
        val editOps = captor.lastValue
        assertEquals(1, editOps.size)
        assertEquals(SetSelectionEditOp(0, 6), editOps[0])
    }

    @Test
    fun setSelection_batchSession() {
        val captor = argumentCaptor<List<EditOperation>>()

        ic.mTextFieldValue = TextFieldValue(text = "Hello, World", selection = TextRange.Zero)

        // Do not callback to listener during batch session.
        ic.beginBatchEdit()

        assertTrue(ic.setSelection(0, 6))
        verify(listener, never()).onEditOperations(any())

        assertTrue(ic.setSelection(6, 11))
        verify(listener, never()).onEditOperations(any())

        ic.endBatchEdit()

        verify(listener, times(1)).onEditOperations(captor.capture())
        val editOps = captor.lastValue
        assertEquals(2, editOps.size)
        assertEquals(SetSelectionEditOp(0, 6), editOps[0])
        assertEquals(SetSelectionEditOp(6, 11), editOps[1])
    }

    @Test
    fun finishComposingText() {
        val captor = argumentCaptor<List<EditOperation>>()

        ic.mTextFieldValue = TextFieldValue(text = "Hello, World.", selection = TextRange.Zero)

        // Cancel any ongoing composition. In this example, there is no composition range, but
        // should record the API call
        assertTrue(ic.finishComposingText())

        verify(listener, times(1)).onEditOperations(captor.capture())
        val editOps = captor.lastValue
        assertEquals(1, editOps.size)
        assertEquals(FinishComposingTextEditOp(), editOps[0])
    }

    @Test
    fun finishComposingText_batchSession() {
        val captor = argumentCaptor<List<EditOperation>>()

        ic.mTextFieldValue = TextFieldValue(text = "Hello, World", selection = TextRange.Zero)

        // Do not callback to listener during batch session.
        ic.beginBatchEdit()

        assertTrue(ic.finishComposingText())
        verify(listener, never()).onEditOperations(any())

        assertTrue(ic.finishComposingText())
        verify(listener, never()).onEditOperations(any())

        ic.endBatchEdit()

        verify(listener, times(1)).onEditOperations(captor.capture())
        val editOps = captor.lastValue
        assertEquals(2, editOps.size)
        assertEquals(FinishComposingTextEditOp(), editOps[0])
        assertEquals(FinishComposingTextEditOp(), editOps[1])
    }

    @Test
    fun mixedAPICalls_batchSession() {
        val captor = argumentCaptor<List<EditOperation>>()

        ic.mTextFieldValue = TextFieldValue(text = "", selection = TextRange.Zero)

        // Do not callback to listener during batch session.
        ic.beginBatchEdit()

        assertTrue(ic.setComposingText("Hello, ", 1))
        verify(listener, never()).onEditOperations(any())

        assertTrue(ic.finishComposingText())
        verify(listener, never()).onEditOperations(any())

        assertTrue(ic.commitText("World.", 1))
        verify(listener, never()).onEditOperations(any())

        assertTrue(ic.setSelection(0, 12))
        verify(listener, never()).onEditOperations(any())

        assertTrue(ic.commitText("", 1))
        verify(listener, never()).onEditOperations(any())

        ic.endBatchEdit()

        verify(listener, times(1)).onEditOperations(captor.capture())
        val editOps = captor.lastValue
        assertEquals(5, editOps.size)
        assertEquals(SetComposingTextEditOp("Hello, ", 1), editOps[0])
        assertEquals(FinishComposingTextEditOp(), editOps[1])
        assertEquals(CommitTextEditOp("World.", 1), editOps[2])
        assertEquals(SetSelectionEditOp(0, 12), editOps[3])
        assertEquals(CommitTextEditOp("", 1), editOps[4])
    }

    @Test
    fun closeConnection() {
        // Everything is internal and there is nothing to expect.
        // Just make sure it is not crashed by calling method.
        ic.closeConnection()
    }

    @Test
    fun key_event_del_down() {
        val captor = argumentCaptor<List<EditOperation>>()
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
        verify(listener, times(1)).onEditOperations(captor.capture())

        val editOps = captor.lastValue
        assertEquals(1, editOps.size)
        assertEquals(BackspaceKeyEditOp(), editOps[0])
    }

    @Test
    fun key_event_del_up() {
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
        verify(listener, never()).onEditOperations(any())
    }

    @Test
    fun key_event_left_down() {
        val captor = argumentCaptor<List<EditOperation>>()
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT))
        verify(listener, times(1)).onEditOperations(captor.capture())

        val editOps = captor.lastValue
        assertEquals(1, editOps.size)
        assertEquals(MoveCursorEditOp(-1), editOps[0])
    }

    @Test
    fun key_event_left_up() {
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT))
        verify(listener, never()).onEditOperations(any())
    }

    @Test
    fun key_event_right_down() {
        val captor = argumentCaptor<List<EditOperation>>()
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT))
        verify(listener, times(1)).onEditOperations(captor.capture())

        val editOps = captor.lastValue
        assertEquals(1, editOps.size)
        assertEquals(MoveCursorEditOp(1), editOps[0])
    }

    @Test
    fun key_event_right_up() {
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT))
        verify(listener, never()).onEditOperations(any())
    }

    @Test
    fun key_event_printablekey_down() {
        val captor = argumentCaptor<List<EditOperation>>()
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_1))
        verify(listener, times(1)).onEditOperations(captor.capture())

        val editOps = captor.lastValue
        assertEquals(1, editOps.size)
        assertEquals(CommitTextEditOp("1", 1), editOps[0])
    }

    @Test
    fun do_not_callback_empty_edit_ops() {
        ic.beginBatchEdit()
        ic.endBatchEdit()
        verify(listener, never()).onEditOperations(any())
    }

    @Test
    fun commitCorrection_returns_true_when_autoCorrect_is_on() {
        val inputConnection = RecordingInputConnection(
            initState = TextFieldValue(),
            eventListener = listener,
            autoCorrect = true
        )
        val anyCorrectionInfo = CorrectionInfo(0, "", "")

        assertTrue(inputConnection.commitCorrection(anyCorrectionInfo))
    }

    @Test
    fun commitCorrection_returns_false_when_autoCorrect_is_off() {
        val inputConnection = RecordingInputConnection(
            initState = TextFieldValue(),
            eventListener = listener,
            autoCorrect = false
        )
        val anyCorrectionInfo = CorrectionInfo(0, "", "")

        assertFalse(inputConnection.commitCorrection(anyCorrectionInfo))
    }

    @Test
    fun do_not_callback_if_only_readonly_ops() {
        ic.beginBatchEdit()
        ic.getSelectedText(1)
        ic.endBatchEdit()
        verify(listener, never()).onEditOperations(any())
    }
}