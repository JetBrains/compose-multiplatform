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

import android.text.InputType
import android.view.inputmethod.EditorInfo
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.update
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class EditorInfoTest {

    @Test
    fun test_fill_editor_info_text() {
        val info = EditorInfo()
        info.update(
            ImeOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Default
            )
        )

        assertThat((InputType.TYPE_CLASS_TEXT and info.inputType) != 0).isTrue()
        assertThat(
            (EditorInfo.IME_MASK_ACTION and info.imeOptions)
                == EditorInfo.IME_ACTION_UNSPECIFIED
        ).isTrue()
    }

    @Test
    fun test_fill_editor_info_ascii() {
        val info = EditorInfo()
        info.update(
            ImeOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Default
            )
        )

        assertThat((InputType.TYPE_CLASS_TEXT and info.inputType) != 0).isTrue()
        assertThat((EditorInfo.IME_FLAG_FORCE_ASCII and info.imeOptions) != 0).isTrue()
        assertThat(
            (EditorInfo.IME_MASK_ACTION and info.imeOptions)
                == EditorInfo.IME_ACTION_UNSPECIFIED
        ).isTrue()
    }

    @Test
    fun test_fill_editor_info_number() {
        val info = EditorInfo()
        info.update(
            ImeOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Default
            )
        )

        assertThat((InputType.TYPE_CLASS_NUMBER and info.inputType) != 0).isTrue()
        assertThat(
            (EditorInfo.IME_MASK_ACTION and info.imeOptions)
                == EditorInfo.IME_ACTION_UNSPECIFIED
        ).isTrue()
    }

    @Test
    fun test_fill_editor_info_phone() {
        val info = EditorInfo()
        info.update(
            ImeOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Default
            )
        )

        assertThat((InputType.TYPE_CLASS_PHONE and info.inputType) != 0).isTrue()
        assertThat(
            (EditorInfo.IME_MASK_ACTION and info.imeOptions)
                == EditorInfo.IME_ACTION_UNSPECIFIED
        ).isTrue()
    }

    @Test
    fun test_fill_editor_info_uri() {
        val info = EditorInfo()
        info.update(
            ImeOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Default
            )
        )

        assertThat((InputType.TYPE_CLASS_TEXT and info.inputType) != 0).isTrue()
        assertThat((InputType.TYPE_TEXT_VARIATION_URI and info.inputType) != 0).isTrue()
        assertThat(
            (EditorInfo.IME_MASK_ACTION and info.imeOptions)
                == EditorInfo.IME_ACTION_UNSPECIFIED
        ).isTrue()
    }

    @Test
    fun test_fill_editor_info_email() {
        val info = EditorInfo()
        info.update(
            ImeOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Default
            )
        )

        assertThat((InputType.TYPE_CLASS_TEXT and info.inputType) != 0).isTrue()
        assertThat((InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS and info.inputType) != 0)
            .isTrue()
        assertThat(
            (EditorInfo.IME_MASK_ACTION and info.imeOptions)
                == EditorInfo.IME_ACTION_UNSPECIFIED
        ).isTrue()
    }

    @Test
    fun test_fill_editor_info_password() {
        val info = EditorInfo()
        info.update(
            ImeOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Default
            )
        )

        assertThat((InputType.TYPE_CLASS_TEXT and info.inputType) != 0).isTrue()
        assertThat((InputType.TYPE_TEXT_VARIATION_PASSWORD and info.inputType) != 0).isTrue()
        assertThat(
            (EditorInfo.IME_MASK_ACTION and info.imeOptions)
                == EditorInfo.IME_ACTION_UNSPECIFIED
        ).isTrue()
    }

    @Test
    fun test_fill_editor_info_number_password() {
        val info = EditorInfo()
        info.update(
            ImeOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Default
            )
        )

        assertThat((InputType.TYPE_CLASS_NUMBER and info.inputType) != 0).isTrue()
        assertThat((InputType.TYPE_NUMBER_VARIATION_PASSWORD and info.inputType) != 0).isTrue()
        assertThat(
            (EditorInfo.IME_MASK_ACTION and info.imeOptions)
                == EditorInfo.IME_ACTION_UNSPECIFIED
        ).isTrue()
    }

    @Test
    fun test_fill_editor_info_decimal_number() {
        val info = EditorInfo()
        info.update(
            ImeOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Default
            )
        )

        assertThat((InputType.TYPE_CLASS_NUMBER and info.inputType) != 0).isTrue()
        assertThat((InputType.TYPE_NUMBER_FLAG_DECIMAL and info.inputType) != 0).isTrue()
        assertThat(
            (EditorInfo.IME_MASK_ACTION and info.imeOptions)
                == EditorInfo.IME_ACTION_UNSPECIFIED
        ).isTrue()
    }

    @Test
    fun test_fill_editor_info_action_none() {
        val info = EditorInfo()
        info.update(
            ImeOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.None
            )
        )

        assertThat((InputType.TYPE_CLASS_TEXT and info.inputType) != 0).isTrue()
        assertThat((EditorInfo.IME_FLAG_FORCE_ASCII and info.imeOptions) != 0).isTrue()
        assertThat(
            (EditorInfo.IME_MASK_ACTION and info.imeOptions)
                == EditorInfo.IME_ACTION_NONE
        ).isTrue()
    }

    @Test
    fun test_fill_editor_info_action_go() {
        val info = EditorInfo()
        info.update(
            ImeOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Go
            )
        )

        assertThat((InputType.TYPE_CLASS_TEXT and info.inputType) != 0).isTrue()
        assertThat((EditorInfo.IME_FLAG_FORCE_ASCII and info.imeOptions) != 0).isTrue()
        assertThat(
            (EditorInfo.IME_MASK_ACTION and info.imeOptions)
                == EditorInfo.IME_ACTION_GO
        ).isTrue()
    }

    @Test
    fun test_fill_editor_info_action_next() {
        val info = EditorInfo()
        info.update(
            ImeOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Next
            )
        )

        assertThat((InputType.TYPE_CLASS_TEXT and info.inputType) != 0).isTrue()
        assertThat((EditorInfo.IME_FLAG_FORCE_ASCII and info.imeOptions) != 0).isTrue()
        assertThat(
            (EditorInfo.IME_MASK_ACTION and info.imeOptions)
                == EditorInfo.IME_ACTION_NEXT
        ).isTrue()
    }

    @Test
    fun test_fill_editor_info_action_previous() {
        val info = EditorInfo()
        info.update(
            ImeOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Previous
            )
        )

        assertThat((InputType.TYPE_CLASS_TEXT and info.inputType) != 0).isTrue()
        assertThat((EditorInfo.IME_FLAG_FORCE_ASCII and info.imeOptions) != 0).isTrue()
        assertThat(
            (EditorInfo.IME_MASK_ACTION and info.imeOptions)
                == EditorInfo.IME_ACTION_PREVIOUS
        ).isTrue()
    }

    @Test
    fun test_fill_editor_info_action_search() {
        val info = EditorInfo()
        info.update(
            ImeOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Search,
            )
        )

        assertThat((InputType.TYPE_CLASS_TEXT and info.inputType) != 0).isTrue()
        assertThat((EditorInfo.IME_FLAG_FORCE_ASCII and info.imeOptions) != 0).isTrue()
        assertThat(
            (EditorInfo.IME_MASK_ACTION and info.imeOptions)
                == EditorInfo.IME_ACTION_SEARCH
        ).isTrue()
    }

    @Test
    fun test_fill_editor_info_action_send() {
        val info = EditorInfo()
        info.update(
            ImeOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Send
            )
        )

        assertThat((InputType.TYPE_CLASS_TEXT and info.inputType) != 0).isTrue()
        assertThat((EditorInfo.IME_FLAG_FORCE_ASCII and info.imeOptions) != 0).isTrue()
        assertThat(
            (EditorInfo.IME_MASK_ACTION and info.imeOptions)
                == EditorInfo.IME_ACTION_SEND
        ).isTrue()
    }

    @Test
    fun test_fill_editor_info_action_done() {
        val info = EditorInfo()
        info.update(
            ImeOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done
            )
        )

        assertThat((InputType.TYPE_CLASS_TEXT and info.inputType) != 0).isTrue()
        assertThat((EditorInfo.IME_FLAG_FORCE_ASCII and info.imeOptions) != 0).isTrue()
        assertThat(
            (EditorInfo.IME_MASK_ACTION and info.imeOptions)
                == EditorInfo.IME_ACTION_DONE
        ).isTrue()
    }

    @Test
    fun test_fill_editor_info_multi_line() {
        val info = EditorInfo()
        info.update(
            ImeOptions(
                singleLine = false,
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done
            )
        )

        assertThat((InputType.TYPE_TEXT_FLAG_MULTI_LINE and info.inputType) == 0).isFalse()
        assertThat((EditorInfo.IME_FLAG_NO_ENTER_ACTION and info.imeOptions) == 0).isTrue()
    }

    @Test
    fun test_fill_editor_info_multi_line_with_default_action() {
        val info = EditorInfo()
        info.update(
            ImeOptions(
                singleLine = false,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Default
            )
        )

        assertThat((InputType.TYPE_TEXT_FLAG_MULTI_LINE and info.inputType) == 0).isFalse()
        assertThat((EditorInfo.IME_FLAG_NO_ENTER_ACTION and info.imeOptions) == 0).isFalse()
    }

    @Test
    fun test_fill_editor_info_single_line() {
        val info = EditorInfo()
        info.update(
            ImeOptions(
                singleLine = true,
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done
            )
        )

        assertThat((InputType.TYPE_TEXT_FLAG_MULTI_LINE and info.inputType) == 0).isTrue()
        assertThat((EditorInfo.IME_FLAG_NO_ENTER_ACTION and info.imeOptions) == 0).isTrue()
    }

    @Test
    fun test_fill_editor_info_single_line_changes_ime_from_unspecified_to_done() {
        val info = EditorInfo()
        info.update(
            ImeOptions(
                singleLine = true,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Default
            )
        )

        assertThat((EditorInfo.IME_ACTION_DONE and info.imeOptions) == 0).isFalse()
        assertThat((EditorInfo.IME_ACTION_UNSPECIFIED and info.imeOptions) == 0).isTrue()
    }

    @Test
    fun test_fill_editor_info_multi_line_not_set_when_input_type_is_not_text() {
        val info = EditorInfo()
        info.update(
            ImeOptions(
                singleLine = false,
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            )
        )

        assertThat((InputType.TYPE_TEXT_FLAG_MULTI_LINE and info.inputType) == 0).isTrue()
        assertThat((EditorInfo.IME_FLAG_NO_ENTER_ACTION and info.imeOptions) == 0).isTrue()
    }

    @Test
    fun test_fill_editor_info_capitalization_none() {
        val info = EditorInfo()
        info.update(
            ImeOptions(
                capitalization = KeyboardCapitalization.None,
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done
            )
        )

        assertThat((InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS and info.inputType) == 0).isTrue()
        assertThat((InputType.TYPE_TEXT_FLAG_CAP_WORDS and info.inputType) == 0).isTrue()
        assertThat((InputType.TYPE_TEXT_FLAG_CAP_SENTENCES and info.inputType) == 0).isTrue()
    }

    @Test
    fun test_fill_editor_info_capitalization_characters() {
        val info = EditorInfo()
        info.update(
            ImeOptions(
                capitalization = KeyboardCapitalization.Characters,
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done
            )
        )

        assertThat((InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS and info.inputType) == 0).isFalse()
        assertThat((InputType.TYPE_TEXT_FLAG_CAP_WORDS and info.inputType) == 0).isTrue()
        assertThat((InputType.TYPE_TEXT_FLAG_CAP_SENTENCES and info.inputType) == 0).isTrue()
    }

    @Test
    fun test_fill_editor_info_capitalization_words() {
        val info = EditorInfo()
        info.update(
            ImeOptions(
                capitalization = KeyboardCapitalization.Words,
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done
            )
        )

        assertThat((InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS and info.inputType) == 0).isTrue()
        assertThat((InputType.TYPE_TEXT_FLAG_CAP_WORDS and info.inputType) == 0).isFalse()
        assertThat((InputType.TYPE_TEXT_FLAG_CAP_SENTENCES and info.inputType) == 0).isTrue()
    }

    @Test
    fun test_fill_editor_info_capitalization_sentences() {
        val info = EditorInfo()
        info.update(
            ImeOptions(
                capitalization = KeyboardCapitalization.Sentences,
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done,
            )
        )

        assertThat((InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS and info.inputType) == 0).isTrue()
        assertThat((InputType.TYPE_TEXT_FLAG_CAP_WORDS and info.inputType) == 0).isTrue()
        assertThat((InputType.TYPE_TEXT_FLAG_CAP_SENTENCES and info.inputType) == 0).isFalse()
    }

    @Test
    fun test_fill_editor_info_capitalization_not_added_when_input_type_is_not_text() {
        val info = EditorInfo()
        info.update(
            ImeOptions(
                capitalization = KeyboardCapitalization.Sentences,
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            )
        )

        assertThat((InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS and info.inputType) == 0).isTrue()
        assertThat((InputType.TYPE_TEXT_FLAG_CAP_WORDS and info.inputType) == 0).isTrue()
        assertThat((InputType.TYPE_TEXT_FLAG_CAP_SENTENCES and info.inputType) == 0).isTrue()
    }

    @Test
    fun test_fill_editor_info_auto_correct_on() {
        val info = EditorInfo()
        info.update(
            ImeOptions(
                autoCorrect = true,
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done
            )
        )

        assertThat((InputType.TYPE_TEXT_FLAG_AUTO_CORRECT and info.inputType) == 0).isFalse()
    }

    @Test
    fun test_fill_editor_info_auto_correct_off() {
        val info = EditorInfo()
        info.update(
            ImeOptions(
                autoCorrect = false,
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done
            )
        )

        assertThat((InputType.TYPE_TEXT_FLAG_AUTO_CORRECT and info.inputType) == 0).isTrue()
    }

    @Test
    fun autocorrect_not_added_when_input_type_is_not_text() {
        val info = EditorInfo()
        info.update(
            ImeOptions(
                autoCorrect = true,
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            )
        )

        assertThat((InputType.TYPE_TEXT_FLAG_AUTO_CORRECT and info.inputType) == 0).isTrue()
    }

    @Test
    fun initial_default_selection_info_is_set() {
        val info = EditorInfo()
        info.update(ImeOptions.Default)

        assertThat(info.initialSelStart).isEqualTo(0)
        assertThat(info.initialSelEnd).isEqualTo(0)
    }

    @Test
    fun initial_selection_info_is_set() {
        val selection = TextRange(1, 2)
        val info = EditorInfo()
        info.update(ImeOptions.Default, TextFieldValue("abc", selection))

        assertThat(info.initialSelStart).isEqualTo(selection.start)
        assertThat(info.initialSelEnd).isEqualTo(selection.end)
    }

    private fun EditorInfo.update(imeOptions: ImeOptions) {
        this.update(imeOptions, TextFieldValue())
    }
}