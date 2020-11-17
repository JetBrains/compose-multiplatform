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

    @Test
    fun aligns_selection_to_the_text_length() {
        val text = "a"
        val textFieldValue = TextFieldValue(text = text, selection = TextRange(text.length + 1))
        assertThat(textFieldValue.selection.collapsed).isTrue()
        assertThat(textFieldValue.selection.max).isEqualTo(textFieldValue.text.length)
    }

    @Test
    fun keep_selection_that_is_less_than_text_length() {
        val text = "a bc"
        val selection = TextRange(0, "a".length)

        val textFieldValue = TextFieldValue(text = text, selection = selection)

        assertThat(textFieldValue.text).isEqualTo(text)
        assertThat(textFieldValue.selection).isEqualTo(selection)
    }

    @Test(expected = IllegalArgumentException::class)
    fun throws_exception_for_negative_composition() {
        TextFieldValue(text = "", composition = TextRange(-1))
    }

    @Test
    fun aligns_composition_to_text_length() {
        val text = "a"
        val textFieldValue = TextFieldValue(text = text, composition = TextRange(text.length + 1))
        assertThat(textFieldValue.composition?.collapsed).isTrue()
        assertThat(textFieldValue.composition?.max).isEqualTo(textFieldValue.text.length)
    }

    @Test
    fun keep_composition_that_is_less_than_text_length() {
        val text = "a bc"
        val composition = TextRange(0, "a".length)

        val textFieldValue = TextFieldValue(text = text, composition = composition)

        assertThat(textFieldValue.text).isEqualTo(text)
        assertThat(textFieldValue.composition).isEqualTo(composition)
    }

    @Test
    fun equals_returns_true_for_same_instance() {
        val textFieldValue = TextFieldValue(
            text = "a",
            selection = TextRange(1),
            composition = TextRange(2)
        )

        assertThat(textFieldValue).isEqualTo(textFieldValue)
    }

    @Test
    fun equals_returns_true_for_same_object() {
        val textFieldValue = TextFieldValue(
            text = "a",
            selection = TextRange(1),
            composition = TextRange(2)
        )

        assertThat(textFieldValue).isEqualTo(textFieldValue.copy())
    }

    @Test
    fun copy_sets_text_correctly() {
        val textFieldValue = TextFieldValue(
            text = "a",
            selection = TextRange(1),
            composition = TextRange(2)
        )

        val expected = TextFieldValue(
            text = "b",
            selection = textFieldValue.selection,
            composition = textFieldValue.composition
        )

        assertThat(textFieldValue.copy(text = "b")).isEqualTo(expected)
    }

    @Test
    fun copy_sets_selection_correctly() {
        val textFieldValue = TextFieldValue(
            text = "a",
            selection = TextRange(1),
            composition = TextRange(2)
        )

        val expected = TextFieldValue(
            text = textFieldValue.text,
            selection = TextRange.Zero,
            composition = textFieldValue.composition
        )

        assertThat(textFieldValue.copy(selection = TextRange.Zero)).isEqualTo(expected)
    }

    @Test
    fun text_and_selection_parameter_constructor_has_null_composition() {
        val textFieldValue = TextFieldValue(
            text = "a",
            selection = TextRange(1)
        )

        assertThat(textFieldValue.composition).isNull()
    }
}