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

import android.content.Context
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TextInputServiceAndroid
import androidx.compose.ui.text.input.buildTextFieldValue
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalTextApi::class)
@RunWith(JUnit4::class)
class TextInputServiceAndroidOnStateUpdateTest {

    private lateinit var textInputService: TextInputServiceAndroid
    private lateinit var imm: InputMethodManager

    @Before
    fun setup() {
        imm = mock()
        val view: View = mock()
        val context: Context = mock()
        whenever(context.getSystemService(eq(Context.INPUT_METHOD_SERVICE))).thenReturn(imm)
        whenever(view.context).thenReturn(context)
        textInputService = TextInputServiceAndroid(view)
        textInputService.startInput(
            value = TextFieldValue(""),
            imeOptions = ImeOptions.Default,
            onEditCommand = {},
            onImeActionPerformed = {}
        )
        textInputService.createInputConnection(EditorInfo())
    }

    @Test
    fun onUpdateState_resetInputCalled_whenOnlyTextChanged() {
        textInputService.onStateUpdated(
            oldValue = TextFieldValue("a"),
            newValue = TextFieldValue("b")
        )

        verify(imm, times(1)).restartInput(any())
        verify(imm, never()).updateSelection(any(), any(), any(), any(), any())
    }

    @OptIn(InternalTextApi::class)
    @Test
    fun onUpdateState_resetInputCalled_whenOnlyCompositionChanged() {
        textInputService.onStateUpdated(
            oldValue = buildTextFieldValue("a", TextRange.Zero, TextRange.Zero),
            newValue = buildTextFieldValue("a", TextRange.Zero, null)
        )

        verify(imm, times(1)).restartInput(any())
        verify(imm, never()).updateSelection(any(), any(), any(), any(), any())
    }

    @InternalTextApi
    @Test
    fun onUpdateState_updateSelectionCalled_whenOnlySelectionChanged() {
        textInputService.onStateUpdated(
            oldValue = buildTextFieldValue("a", TextRange.Zero, null),
            newValue = buildTextFieldValue("a", TextRange(1), null)
        )

        verify(imm, never()).restartInput(any())
        verify(imm, times(1)).updateSelection(any(), any(), any(), any(), any())
    }

    @OptIn(InternalTextApi::class)
    @Test
    fun onUpdateState_resetInputNotCalled_whenSelectionAndCompositionChanged() {
        textInputService.onStateUpdated(
            oldValue = buildTextFieldValue("a", TextRange.Zero, TextRange.Zero),
            newValue = buildTextFieldValue("a", TextRange(1), null)
        )

        verify(imm, never()).restartInput(any())
        verify(imm, times(1)).updateSelection(any(), any(), any(), any(), any())
    }

    @Test
    fun onUpdateState_resetInputNotCalled_whenValuesAreSame() {
        textInputService.onStateUpdated(
            oldValue = TextFieldValue("a"),
            newValue = TextFieldValue("a")
        )

        verify(imm, never()).restartInput(any())
        verify(imm, never()).updateSelection(any(), any(), any(), any(), any())
    }
}