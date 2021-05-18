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

import android.view.View
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.InputConnection
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.InputEventCallback2
import androidx.compose.ui.text.input.InputMethodManager
import androidx.compose.ui.text.input.RecordingInputConnection
import androidx.compose.ui.text.input.TextFieldValue
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RecordingInputConnectionUpdateTextFieldValueTest {

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
    fun test_update_input_state() {
        val imm: InputMethodManager = mock()
        val view: View = mock()

        val inputState = TextFieldValue(text = "Hello, World.", selection = TextRange.Zero)

        ic.updateInputState(inputState, imm, view)

        verify(imm, times(1)).updateSelection(eq(view), eq(0), eq(0), eq(-1), eq(-1))
        verify(imm, never()).updateExtractedText(any(), any(), any())
    }

    @Test
    fun test_update_input_state_inactive() {
        val imm: InputMethodManager = mock()
        val view: View = mock()

        val previousTextFieldValue = ic.mTextFieldValue
        ic.closeConnection()

        val inputState = TextFieldValue(text = "Hello, World.", selection = TextRange.Zero)
        ic.updateInputState(inputState, imm, view)

        assertThat(ic.mTextFieldValue).isEqualTo(previousTextFieldValue)
        verify(imm, never()).updateSelection(any(), any(), any(), any(), any())
        verify(imm, never()).updateExtractedText(any(), any(), any())
    }

    @Test
    fun test_update_input_state_extracted_text_monitor() {
        val imm: InputMethodManager = mock()
        val view: View = mock()

        ic.getExtractedText(null, InputConnection.GET_EXTRACTED_TEXT_MONITOR)

        val inputState = TextFieldValue(text = "Hello, World.", selection = TextRange.Zero)

        ic.updateInputState(inputState, imm, view)

        verify(imm, times(1)).updateSelection(eq(view), eq(0), eq(0), eq(-1), eq(-1))

        val captor = argumentCaptor<ExtractedText>()

        verify(imm, times(1)).updateExtractedText(any(), any(), captor.capture())

        assertThat(captor.allValues.size).isEqualTo(1)
        assertThat(captor.firstValue.text).isEqualTo("Hello, World.")
        assertThat(captor.firstValue.partialStartOffset).isEqualTo(-1)
        assertThat(captor.firstValue.selectionStart).isEqualTo(0)
        assertThat(captor.firstValue.selectionEnd).isEqualTo(0)
    }
}