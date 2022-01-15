/*
 * Copyright 2022 The Android Open Source Project
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

import android.view.inputmethod.CursorAnchorInfo
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class CursorAnchorInfoBuilderTest {

    @Test
    fun testSelectionDefault() {
        val textFieldValue = TextFieldValue()
        val builder = CursorAnchorInfo.Builder()

        val cursorAnchorInfo = builder.build(textFieldValue)

        assertThat(cursorAnchorInfo.selectionStart).isEqualTo(0)
        assertThat(cursorAnchorInfo.selectionEnd).isEqualTo(0)
    }

    @Test
    fun testSelectionCursor() {
        val textFieldValue = TextFieldValue("abc", selection = TextRange(2))
        val builder = CursorAnchorInfo.Builder()

        val cursorAnchorInfo = builder.build(textFieldValue)

        assertThat(cursorAnchorInfo.selectionStart).isEqualTo(2)
        assertThat(cursorAnchorInfo.selectionEnd).isEqualTo(2)
    }

    @Test
    fun testSelectionRange() {
        val textFieldValue = TextFieldValue("abc", selection = TextRange(1, 2))
        val builder = CursorAnchorInfo.Builder()

        val cursorAnchorInfo = builder.build(textFieldValue)

        assertThat(cursorAnchorInfo.selectionStart).isEqualTo(1)
        assertThat(cursorAnchorInfo.selectionEnd).isEqualTo(2)
    }

    @Test
    fun testCompositionNone() {
        val textFieldValue = TextFieldValue(composition = null)
        val builder = CursorAnchorInfo.Builder()

        val cursorAnchorInfo = builder.build(textFieldValue)

        assertThat(cursorAnchorInfo.composingTextStart).isEqualTo(-1)
        assertThat(cursorAnchorInfo.composingText).isNull()
    }

    @Test
    fun testCompositionCoveringAllString() {
        val text = "abc"
        val textFieldValue = TextFieldValue(text, composition = TextRange(0, text.length))
        val builder = CursorAnchorInfo.Builder()

        val cursorAnchorInfo = builder.build(textFieldValue)

        assertThat(cursorAnchorInfo.composingTextStart).isEqualTo(0)
        assertThat(cursorAnchorInfo.composingText.toString()).isEqualTo(text)
    }

    @Test
    fun testCompositionCoveringPortionOfString() {
        val word1 = "123 "
        val word2 = "456"
        val word3 = " 789"
        val textFieldValue = TextFieldValue(
            word1 + word2 + word3,
            composition = TextRange(word1.length, (word1 + word2).length)
        )
        val builder = CursorAnchorInfo.Builder()

        val cursorAnchorInfo = builder.build(textFieldValue)

        assertThat(cursorAnchorInfo.composingTextStart).isEqualTo(word1.length)
        assertThat(cursorAnchorInfo.composingText.toString()).isEqualTo(word2)
    }

    @Test
    fun testResetsBetweenExecutions() {
        val text = "abc"
        val textFieldValue = TextFieldValue(text, composition = TextRange(0, text.length))
        val builder = CursorAnchorInfo.Builder()

        val cursorAnchorInfo = builder.build(textFieldValue)

        assertThat(cursorAnchorInfo.composingText.toString()).isEqualTo(text)
        assertThat(cursorAnchorInfo.composingTextStart).isEqualTo(textFieldValue.composition!!.min)

        val cursorAnchorInfo1 = builder.build(TextFieldValue("abcd"))

        assertThat(cursorAnchorInfo1.composingText).isNull()
        assertThat(cursorAnchorInfo1.composingTextStart).isEqualTo(-1)
    }
}