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

import androidx.compose.ui.text.TextRange
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TextFieldValueTest {

    @Test(expected = IllegalArgumentException::class)
    fun throws_exception_for_negative_selection() {
        TextFieldValue(text = "", selection = TextRange(-1))
    }

    @Test(expected = IllegalArgumentException::class)
    fun throws_exception_for_outofbounds_selection() {
        TextFieldValue(text = "a", selection = TextRange("a".length + 1))
    }

    @Test
    fun accepts_selection_end_equal_to_text_length() {
        val text = "a"
        val textRange = TextRange(text.length)
        val textFieldValue = TextFieldValue(text = text, selection = textRange)

        assertThat(textFieldValue.text).isEqualTo(text)
        assertThat(textFieldValue.selection).isEqualTo(textRange)
    }

    @Test(expected = IllegalArgumentException::class)
    fun throws_exception_for_negative_composition() {
        TextFieldValue(text = "", composition = TextRange(-1))
    }

    @Test(expected = IllegalArgumentException::class)
    fun throws_exception_for_outofbounds_composition() {
        TextFieldValue(text = "a", composition = TextRange("a".length + 1))
    }

    @Test
    fun accepts_composition_end_equal_to_text_length() {
        val text = "a"
        val textRange = TextRange(text.length)

        val textFieldValue = TextFieldValue(text = text, composition = textRange)

        assertThat(textFieldValue.text).isEqualTo(text)
        assertThat(textFieldValue.composition).isEqualTo(textRange)
    }
}