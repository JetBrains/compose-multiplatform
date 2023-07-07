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
import androidx.compose.ui.text.input.CommitTextCommand
import androidx.compose.ui.text.input.DeleteSurroundingTextCommand
import androidx.compose.ui.text.input.DeleteSurroundingTextInCodePointsCommand
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.FinishComposingTextCommand
import androidx.compose.ui.text.input.InputEventCallback2
import androidx.compose.ui.text.input.RecordingInputConnection
import androidx.compose.ui.text.input.SetComposingRegionCommand
import androidx.compose.ui.text.input.SetComposingTextCommand
import androidx.compose.ui.text.input.SetSelectionCommand
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class RecordingInputConnectionTest {

    private lateinit var ic: RecordingInputConnection
    private lateinit var mCallback: InputEventCallback2

    @Before
    fun setup() {
        mCallback = mock()
        ic = RecordingInputConnection(
            initState = TextFieldValue("", TextRange.Zero),
            eventCallback = mCallback,
            autoCorrect = true
        )
    }

    @Test
    fun getTextBeforeAndAfterCursorTest() {
        assertThat(ic.getTextBeforeCursor(100, 0)).isEqualTo("")
        assertThat(ic.getTextAfterCursor(100, 0)).isEqualTo("")

        // Set "Hello, World", and place the cursor at the beginning of the text.
        ic.mTextFieldValue = TextFieldValue(
            text = "Hello, World",
            selection = TextRange.Zero
        )

        assertThat(ic.getTextBeforeCursor(100, 0)).isEqualTo("")
        assertThat(ic.getTextAfterCursor(100, 0)).isEqualTo("Hello, World")

        // Set "Hello, World", and place the cursor between "H" and "e".
        ic.mTextFieldValue = TextFieldValue(
            text = "Hello, World",
            selection = TextRange(1)
        )

        assertThat(ic.getTextBeforeCursor(100, 0)).isEqualTo("H")
        assertThat(ic.getTextAfterCursor(100, 0)).isEqualTo("ello, World")

        // Set "Hello, World", and place the cursor at the end of the text.
        ic.mTextFieldValue = TextFieldValue(
            text = "Hello, World",
            selection = TextRange(12)
        )

        assertThat(ic.getTextBeforeCursor(100, 0)).isEqualTo("Hello, World")
        assertThat(ic.getTextAfterCursor(100, 0)).isEqualTo("")
    }

    @Test
    fun getTextBeforeAndAfterCursorTest_maxCharTest() {
        // Set "Hello, World", and place the cursor at the beginning of the text.
        ic.mTextFieldValue = TextFieldValue(
            text = "Hello, World",
            selection = TextRange.Zero
        )

        assertThat(ic.getTextBeforeCursor(5, 0)).isEqualTo("")
        assertThat(ic.getTextAfterCursor(5, 0)).isEqualTo("Hello")

        // Set "Hello, World", and place the cursor between "H" and "e".
        ic.mTextFieldValue = TextFieldValue(
            text = "Hello, World",
            selection = TextRange(1)
        )

        assertThat(ic.getTextBeforeCursor(5, 0)).isEqualTo("H")
        assertThat(ic.getTextAfterCursor(5, 0)).isEqualTo("ello,")

        // Set "Hello, World", and place the cursor at the end of the text.
        ic.mTextFieldValue = TextFieldValue(
            text = "Hello, World",
            selection = TextRange(12)
        )

        assertThat(ic.getTextBeforeCursor(5, 0)).isEqualTo("World")
        assertThat(ic.getTextAfterCursor(5, 0)).isEqualTo("")
    }

    @Test
    fun getSelectedTextTest() {
        // Set "Hello, World", and place the cursor at the beginning of the text.
        ic.mTextFieldValue = TextFieldValue(
            text = "Hello, World",
            selection = TextRange.Zero
        )

        assertThat(ic.getSelectedText(0)).isNull()

        // Set "Hello, World", and place the cursor between "H" and "e".
        ic.mTextFieldValue = TextFieldValue(
            text = "Hello, World",
            selection = TextRange(0, 1)
        )

        assertThat(ic.getSelectedText(0)).isEqualTo("H")

        // Set "Hello, World", and place the cursor at the end of the text.
        ic.mTextFieldValue = TextFieldValue(
            text = "Hello, World",
            selection = TextRange(0, 12)
        )

        assertThat(ic.getSelectedText(0)).isEqualTo("Hello, World")
    }

    @Test
    fun commitTextTest() {
        val captor = argumentCaptor<List<EditCommand>>()

        ic.mTextFieldValue = TextFieldValue(text = "", selection = TextRange.Zero)

        // Inserting "Hello, " into the empty text field.
        assertThat(ic.commitText("Hello, ", 1)).isTrue()

        verify(mCallback, times(1)).onEditCommands(captor.capture())
        val editCommands = captor.lastValue
        assertThat(editCommands.size).isEqualTo(1)
        assertThat(editCommands[0]).isEqualTo(CommitTextCommand("Hello, ", 1))
    }

    @Test
    fun commitTextTest_batchSession() {
        val captor = argumentCaptor<List<EditCommand>>()

        ic.mTextFieldValue = TextFieldValue(text = "", selection = TextRange.Zero)

        // IME set text "Hello, World." with two commitText API within the single batch session.
        // Do not callback to listener during batch session.
        ic.beginBatchEdit()

        assertThat(ic.commitText("Hello, ", 1)).isTrue()
        verify(mCallback, never()).onEditCommands(any())

        assertThat(ic.commitText("World.", 1)).isTrue()
        verify(mCallback, never()).onEditCommands(any())

        ic.endBatchEdit()

        verify(mCallback, times(1)).onEditCommands(captor.capture())
        val editCommands = captor.lastValue
        assertThat(editCommands.size).isEqualTo(2)
        assertThat(editCommands[0]).isEqualTo(CommitTextCommand("Hello, ", 1))
        assertThat(editCommands[1]).isEqualTo(CommitTextCommand("World.", 1))
    }

    @Test
    fun setComposingRegion() {
        val captor = argumentCaptor<List<EditCommand>>()

        ic.mTextFieldValue = TextFieldValue(text = "Hello, World.", selection = TextRange.Zero)

        // Mark first "H" as composition.
        assertThat(ic.setComposingRegion(0, 1)).isTrue()

        verify(mCallback, times(1)).onEditCommands(captor.capture())
        val editCommands = captor.lastValue
        assertThat(editCommands.size).isEqualTo(1)
        assertThat(editCommands[0]).isEqualTo(SetComposingRegionCommand(0, 1))
    }

    @Test
    fun setComposingRegion_batchSession() {
        val captor = argumentCaptor<List<EditCommand>>()

        ic.mTextFieldValue = TextFieldValue(text = "Hello, World", selection = TextRange.Zero)

        // Do not callback to listener during batch session.
        ic.beginBatchEdit()

        assertThat(ic.setComposingRegion(0, 1)).isTrue()
        verify(mCallback, never()).onEditCommands(any())

        assertThat(ic.setComposingRegion(1, 2)).isTrue()
        verify(mCallback, never()).onEditCommands(any())

        ic.endBatchEdit()

        verify(mCallback, times(1)).onEditCommands(captor.capture())
        val editCommands = captor.lastValue
        assertThat(editCommands.size).isEqualTo(2)
        assertThat(editCommands[0]).isEqualTo(SetComposingRegionCommand(0, 1))
        assertThat(editCommands[1]).isEqualTo(SetComposingRegionCommand(1, 2))
    }

    @Test
    fun setComposingTextTest() {
        val captor = argumentCaptor<List<EditCommand>>()

        ic.mTextFieldValue = TextFieldValue(text = "", selection = TextRange.Zero)

        // Inserting "Hello, " into the empty text field.
        assertThat(ic.setComposingText("Hello, ", 1)).isTrue()

        verify(mCallback, times(1)).onEditCommands(captor.capture())
        val editCommands = captor.lastValue
        assertThat(editCommands.size).isEqualTo(1)
        assertThat(editCommands[0]).isEqualTo(SetComposingTextCommand("Hello, ", 1))
    }

    @Test
    fun setComposingTextTest_batchSession() {
        val captor = argumentCaptor<List<EditCommand>>()

        ic.mTextFieldValue = TextFieldValue(text = "", selection = TextRange.Zero)

        // IME set text "Hello, World." with two setComposingText API within the single batch
        // session. Do not callback to listener during batch session.
        ic.beginBatchEdit()

        assertThat(ic.setComposingText("Hello, ", 1)).isTrue()
        verify(mCallback, never()).onEditCommands(any())

        assertThat(ic.setComposingText("World.", 1)).isTrue()
        verify(mCallback, never()).onEditCommands(any())

        ic.endBatchEdit()

        verify(mCallback, times(1)).onEditCommands(captor.capture())
        val editCommands = captor.lastValue
        assertThat(editCommands.size).isEqualTo(2)
        assertThat(editCommands[0]).isEqualTo(SetComposingTextCommand("Hello, ", 1))
        assertThat(editCommands[1]).isEqualTo(SetComposingTextCommand("World.", 1))
    }

    @Test
    fun deleteSurroundingText() {
        val captor = argumentCaptor<List<EditCommand>>()

        ic.mTextFieldValue = TextFieldValue(text = "Hello, World.", selection = TextRange.Zero)

        // Delete first "Hello, " characters
        assertTrue(ic.deleteSurroundingText(0, 6))

        verify(mCallback, times(1)).onEditCommands(captor.capture())
        val editCommands = captor.lastValue
        assertThat(editCommands.size).isEqualTo(1)
        assertThat(editCommands[0]).isEqualTo(DeleteSurroundingTextCommand(0, 6))
    }

    @Test
    fun deleteSurroundingText_batchSession() {
        val captor = argumentCaptor<List<EditCommand>>()

        ic.mTextFieldValue = TextFieldValue(text = "Hello, World", selection = TextRange.Zero)

        // Do not callback to listener during batch session.
        ic.beginBatchEdit()

        assertThat(ic.deleteSurroundingText(0, 6)).isTrue()
        verify(mCallback, never()).onEditCommands(any())

        assertThat(ic.deleteSurroundingText(0, 5)).isTrue()
        verify(mCallback, never()).onEditCommands(any())

        ic.endBatchEdit()

        verify(mCallback, times(1)).onEditCommands(captor.capture())
        val editCommands = captor.lastValue
        assertThat(editCommands.size).isEqualTo(2)
        assertThat(editCommands[0]).isEqualTo(DeleteSurroundingTextCommand(0, 6))
        assertThat(editCommands[1]).isEqualTo(DeleteSurroundingTextCommand(0, 5))
    }

    @Test
    fun deleteSurroundingTextInCodePoints() {
        val captor = argumentCaptor<List<EditCommand>>()

        ic.mTextFieldValue = TextFieldValue(text = "Hello, World.", selection = TextRange.Zero)

        // Delete first "Hello, " characters
        assertThat(ic.deleteSurroundingTextInCodePoints(0, 6)).isTrue()

        verify(mCallback, times(1)).onEditCommands(captor.capture())
        val editCommands = captor.lastValue
        assertThat(editCommands.size).isEqualTo(1)
        assertThat(editCommands[0]).isEqualTo(DeleteSurroundingTextInCodePointsCommand(0, 6))
    }

    @Test
    fun deleteSurroundingTextInCodePoints_batchSession() {
        val captor = argumentCaptor<List<EditCommand>>()

        ic.mTextFieldValue = TextFieldValue(text = "Hello, World", selection = TextRange.Zero)

        // Do not callback to listener during batch session.
        ic.beginBatchEdit()

        assertThat(ic.deleteSurroundingTextInCodePoints(0, 6)).isTrue()
        verify(mCallback, never()).onEditCommands(any())

        assertThat(ic.deleteSurroundingTextInCodePoints(0, 5)).isTrue()
        verify(mCallback, never()).onEditCommands(any())

        ic.endBatchEdit()

        verify(mCallback, times(1)).onEditCommands(captor.capture())
        val editCommands = captor.lastValue
        assertThat(editCommands.size).isEqualTo(2)
        assertThat(editCommands[0]).isEqualTo(DeleteSurroundingTextInCodePointsCommand(0, 6))
        assertThat(editCommands[1]).isEqualTo(DeleteSurroundingTextInCodePointsCommand(0, 5))
    }

    @Test
    fun setSelection() {
        val captor = argumentCaptor<List<EditCommand>>()

        ic.mTextFieldValue = TextFieldValue(text = "Hello, World.", selection = TextRange.Zero)

        // Select "Hello, "
        assertThat(ic.setSelection(0, 6)).isTrue()

        verify(mCallback, times(1)).onEditCommands(captor.capture())
        val editCommands = captor.lastValue
        assertThat(editCommands.size).isEqualTo(1)
        assertThat(editCommands[0]).isEqualTo(SetSelectionCommand(0, 6))
    }

    @Test
    fun setSelection_batchSession() {
        val captor = argumentCaptor<List<EditCommand>>()

        ic.mTextFieldValue = TextFieldValue(text = "Hello, World", selection = TextRange.Zero)

        // Do not callback to listener during batch session.
        ic.beginBatchEdit()

        assertThat(ic.setSelection(0, 6)).isTrue()
        verify(mCallback, never()).onEditCommands(any())

        assertThat(ic.setSelection(6, 11)).isTrue()
        verify(mCallback, never()).onEditCommands(any())

        ic.endBatchEdit()

        verify(mCallback, times(1)).onEditCommands(captor.capture())
        val editCommands = captor.lastValue
        assertThat(editCommands.size).isEqualTo(2)
        assertThat(editCommands[0]).isEqualTo(SetSelectionCommand(0, 6))
        assertThat(editCommands[1]).isEqualTo(SetSelectionCommand(6, 11))
    }

    @Test
    fun finishComposingText() {
        val captor = argumentCaptor<List<EditCommand>>()

        ic.mTextFieldValue = TextFieldValue(text = "Hello, World.", selection = TextRange.Zero)

        // Cancel any ongoing composition. In this example, there is no composition range, but
        // should record the API call
        assertTrue(ic.finishComposingText())

        verify(mCallback, times(1)).onEditCommands(captor.capture())
        val editCommands = captor.lastValue
        assertThat(editCommands.size).isEqualTo(1)
        assertThat(editCommands[0]).isEqualTo(FinishComposingTextCommand())
    }

    @Test
    fun finishComposingText_batchSession() {
        val captor = argumentCaptor<List<EditCommand>>()

        ic.mTextFieldValue = TextFieldValue(text = "Hello, World", selection = TextRange.Zero)

        // Do not callback to listener during batch session.
        ic.beginBatchEdit()

        assertThat(ic.finishComposingText()).isTrue()
        verify(mCallback, never()).onEditCommands(any())

        assertThat(ic.finishComposingText()).isTrue()
        verify(mCallback, never()).onEditCommands(any())

        ic.endBatchEdit()

        verify(mCallback, times(1)).onEditCommands(captor.capture())
        val editCommands = captor.lastValue
        assertThat(editCommands.size).isEqualTo(2)
        assertThat(editCommands[0]).isEqualTo(FinishComposingTextCommand())
        assertThat(editCommands[1]).isEqualTo(FinishComposingTextCommand())
    }

    @Test
    fun mixedAPICalls_batchSession() {
        val captor = argumentCaptor<List<EditCommand>>()

        ic.mTextFieldValue = TextFieldValue(text = "", selection = TextRange.Zero)

        // Do not callback to listener during batch session.
        ic.beginBatchEdit()

        assertThat(ic.setComposingText("Hello, ", 1)).isTrue()
        verify(mCallback, never()).onEditCommands(any())

        assertThat(ic.finishComposingText()).isTrue()
        verify(mCallback, never()).onEditCommands(any())

        assertThat(ic.commitText("World.", 1)).isTrue()
        verify(mCallback, never()).onEditCommands(any())

        assertThat(ic.setSelection(0, 12)).isTrue()
        verify(mCallback, never()).onEditCommands(any())

        assertThat(ic.commitText("", 1)).isTrue()
        verify(mCallback, never()).onEditCommands(any())

        ic.endBatchEdit()

        verify(mCallback, times(1)).onEditCommands(captor.capture())
        val editCommands = captor.lastValue
        assertThat(editCommands.size).isEqualTo(5)
        assertThat(editCommands[0]).isEqualTo(SetComposingTextCommand("Hello, ", 1))
        assertThat(editCommands[1]).isEqualTo(FinishComposingTextCommand())
        assertThat(editCommands[2]).isEqualTo(CommitTextCommand("World.", 1))
        assertThat(editCommands[3]).isEqualTo(SetSelectionCommand(0, 12))
        assertThat(editCommands[4]).isEqualTo(CommitTextCommand("", 1))
    }

    @Test
    fun closeConnection() {
        // Everything is internal and there is nothing to expect.
        // Just make sure it is not crashed by calling method.
        ic.closeConnection()
    }

    @Test
    fun key_event_del_down() {
        val keyEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL)
        val captor = argumentCaptor<KeyEvent>()

        ic.sendKeyEvent(keyEvent)
        verify(mCallback, times(1)).onKeyEvent(captor.capture())
        verify(mCallback, never()).onEditCommands(any())

        val capturedKeyEvent = captor.lastValue
        assertThat(capturedKeyEvent).isEqualTo(keyEvent)
    }

    @Test
    fun key_event_left_up() {
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT))
        verify(mCallback, never()).onEditCommands(any())
    }

    @Test
    fun sendKeyEvent_doesNotCommitKeyEventsWithUnicodeCharEqualTo0() {
        val captor = argumentCaptor<List<EditCommand>>()
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SHIFT_LEFT))
        verify(mCallback, never()).onEditCommands(captor.capture())
    }

    @Test
    fun do_not_callback_empty_edit_ops() {
        ic.beginBatchEdit()
        ic.endBatchEdit()
        verify(mCallback, never()).onEditCommands(any())
    }

    @Test
    fun commitCorrection_returns_true_when_autoCorrect_is_on() {
        val inputConnection = RecordingInputConnection(
            initState = TextFieldValue(),
            eventCallback = mCallback,
            autoCorrect = true
        )
        val anyCorrectionInfo = CorrectionInfo(0, "", "")

        assertThat(inputConnection.commitCorrection(anyCorrectionInfo)).isTrue()
    }

    @Test
    fun commitCorrection_returns_false_when_autoCorrect_is_off() {
        val inputConnection = RecordingInputConnection(
            initState = TextFieldValue(),
            eventCallback = mCallback,
            autoCorrect = false
        )
        val anyCorrectionInfo = CorrectionInfo(0, "", "")

        assertThat(inputConnection.commitCorrection(anyCorrectionInfo)).isFalse()
    }

    @Test
    fun do_not_callback_if_only_readonly_ops() {
        ic.beginBatchEdit()
        ic.getSelectedText(1)
        ic.endBatchEdit()
        verify(mCallback, never()).onEditCommands(any())
    }
}