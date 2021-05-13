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

import android.os.Bundle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.RecordingInputConnection
import androidx.compose.ui.text.input.TextFieldValue
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RecordingInputConnectionInactiveTest {
    private val ic = defaultInputConnection()

    @Before
    fun setup() {
        ic.closeConnection()
    }

    @Test
    fun deleteSurroundingText() {
        assertThat(ic.deleteSurroundingText(0, 1)).isFalse()
    }

    @Test
    fun deleteSurroundingTextInCodePoints() {
        assertThat(ic.deleteSurroundingTextInCodePoints(0, 1)).isFalse()
    }

    @Test
    fun setComposingText() {
        assertThat(ic.setComposingText(mock(), 0)).isFalse()
    }

    @Test
    fun setComposingRegion() {
        assertThat(ic.setComposingRegion(0, 1)).isFalse()
    }

    @Test
    fun finishComposingText() {
        assertThat(ic.finishComposingText()).isFalse()
    }

    @Test
    fun commitText() {
        assertThat(ic.commitText(mock(), 1)).isFalse()
    }

    @Test
    fun commitCompletion() {
        assertThat(ic.commitCompletion(mock())).isFalse()
    }

    @Test
    fun commitCorrection() {
        assertThat(ic.commitCorrection(mock())).isFalse()
    }

    @Test
    fun setSelection() {
        assertThat(ic.setSelection(0, 1)).isFalse()
    }

    @Test
    fun performEditorAction() {
        assertThat(ic.performEditorAction(0)).isFalse()
    }

    @Test
    fun performContextMenuAction() {
        assertThat(ic.performContextMenuAction(0)).isFalse()
    }

    @Test
    fun beginBatchEdit() {
        assertThat(ic.beginBatchEdit()).isFalse()
    }

    @Test
    fun endBatchEdit() {
        val tmpIc = defaultInputConnection()
        assertThat(tmpIc.beginBatchEdit()).isTrue()
        tmpIc.closeConnection()

        assertThat(tmpIc.endBatchEdit()).isFalse()
    }

    @Test
    fun sendKeyEvent() {
        assertThat(ic.sendKeyEvent(mock())).isFalse()
    }

    @Test
    fun clearMetaKeyStates() {
        assertThat(ic.clearMetaKeyStates(0)).isFalse()
    }

    @Test
    fun reportFullscreenMode() {
        assertThat(ic.reportFullscreenMode(false)).isFalse()
    }

    @Test
    fun performPrivateCommand() {
        assertThat(ic.performPrivateCommand("", Bundle())).isFalse()
    }

    @Test
    fun requestCursorUpdates() {
        assertThat(ic.requestCursorUpdates(0)).isFalse()
    }

    @Test
    fun commitContent() {
        assertThat(ic.commitContent(mock(), 0, mock())).isFalse()
    }

    private fun defaultInputConnection() = RecordingInputConnection(
        initState = TextFieldValue("", TextRange.Zero),
        eventCallback = mock(),
        autoCorrect = true
    )

// The following InputConnection functions does not have well defined inactive input
// connection behavior, keeping them commented as a reference.
//    @Test
//    fun getTextBeforeCursor(){
//        assertThat(ic.getTextBeforeCursor(mock(), mock())).isEqualTo("")
//    }
//
//    @Test
//    fun getTextAfterCursor() {
//        assertThat(ic.getTextAfterCursor(mock(), mock())).isEqualTo("")
//    }
//
//    @Test
//    fun getSelectedText() {
//        assertThat(ic.getSelectedText(mock(), mock())).isEqualTo("")
//    }
//
//    @Test
//    fun getCursorCapsMode() {
//        assertThat(ic.getCursorCapsMode(mock())).isEqualTo(0)
//    }
//    @Test
//    fun getExtractedText() {
//        assertThat(ic.getExtractedText(mock(), mock())).isEqualTo(0)
//    }
//    @Test
//    fun getHandler() {
//        assertThat(ic.handler).isFalse()
//    }
}