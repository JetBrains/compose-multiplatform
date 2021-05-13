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
import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TextInputServiceAndroid
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TextInputServiceAndroidTest {

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
    }

    @Test
    fun test_fill_editor_info_text() {
        textInputService.startInput(
            value = TextFieldValue(""),
            imeOptions = ImeOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Default
            ),
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertThat((InputType.TYPE_CLASS_TEXT and info.inputType) != 0).isTrue()
            assertThat(
                (EditorInfo.IME_MASK_ACTION and info.imeOptions)
                    == EditorInfo.IME_ACTION_UNSPECIFIED
            ).isTrue()
        }
    }

    @Test
    fun test_fill_editor_info_ascii() {
        textInputService.startInput(
            value = TextFieldValue(""),
            imeOptions = ImeOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Default
            ),
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertThat((InputType.TYPE_CLASS_TEXT and info.inputType) != 0).isTrue()
            assertThat((EditorInfo.IME_FLAG_FORCE_ASCII and info.imeOptions) != 0).isTrue()
            assertThat(
                (EditorInfo.IME_MASK_ACTION and info.imeOptions)
                    == EditorInfo.IME_ACTION_UNSPECIFIED
            ).isTrue()
        }
    }

    @Test
    fun test_fill_editor_info_number() {
        textInputService.startInput(
            value = TextFieldValue(""),
            imeOptions = ImeOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Default
            ),
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertThat((InputType.TYPE_CLASS_NUMBER and info.inputType) != 0).isTrue()
            assertThat(
                (EditorInfo.IME_MASK_ACTION and info.imeOptions)
                    == EditorInfo.IME_ACTION_UNSPECIFIED
            ).isTrue()
        }
    }

    @Test
    fun test_fill_editor_info_phone() {
        textInputService.startInput(
            value = TextFieldValue(""),
            imeOptions = ImeOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Default
            ),
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertThat((InputType.TYPE_CLASS_PHONE and info.inputType) != 0).isTrue()
            assertThat(
                (EditorInfo.IME_MASK_ACTION and info.imeOptions)
                    == EditorInfo.IME_ACTION_UNSPECIFIED
            ).isTrue()
        }
    }

    @Test
    fun test_fill_editor_info_uri() {
        textInputService.startInput(
            value = TextFieldValue(""),
            imeOptions = ImeOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Default
            ),
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertThat((InputType.TYPE_CLASS_TEXT and info.inputType) != 0).isTrue()
            assertThat((InputType.TYPE_TEXT_VARIATION_URI and info.inputType) != 0).isTrue()
            assertThat(
                (EditorInfo.IME_MASK_ACTION and info.imeOptions)
                    == EditorInfo.IME_ACTION_UNSPECIFIED
            ).isTrue()
        }
    }

    @Test
    fun test_fill_editor_info_email() {
        textInputService.startInput(
            value = TextFieldValue(""),
            imeOptions = ImeOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Default
            ),
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertThat((InputType.TYPE_CLASS_TEXT and info.inputType) != 0).isTrue()
            assertThat((InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS and info.inputType) != 0)
                .isTrue()
            assertThat(
                (EditorInfo.IME_MASK_ACTION and info.imeOptions)
                    == EditorInfo.IME_ACTION_UNSPECIFIED
            ).isTrue()
        }
    }

    @Test
    fun test_fill_editor_info_password() {
        textInputService.startInput(
            value = TextFieldValue(""),
            imeOptions = ImeOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Default
            ),
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertThat((InputType.TYPE_CLASS_TEXT and info.inputType) != 0).isTrue()
            assertThat((InputType.TYPE_TEXT_VARIATION_PASSWORD and info.inputType) != 0).isTrue()
            assertThat(
                (EditorInfo.IME_MASK_ACTION and info.imeOptions)
                    == EditorInfo.IME_ACTION_UNSPECIFIED
            ).isTrue()
        }
    }

    @Test
    fun test_fill_editor_info_number_password() {
        textInputService.startInput(
            value = TextFieldValue(""),
            imeOptions = ImeOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Default
            ),
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertThat((InputType.TYPE_CLASS_NUMBER and info.inputType) != 0).isTrue()
            assertThat((InputType.TYPE_NUMBER_VARIATION_PASSWORD and info.inputType) != 0).isTrue()
            assertThat(
                (EditorInfo.IME_MASK_ACTION and info.imeOptions)
                    == EditorInfo.IME_ACTION_UNSPECIFIED
            ).isTrue()
        }
    }

    @Test
    fun test_fill_editor_info_action_none() {
        textInputService.startInput(
            value = TextFieldValue(""),
            imeOptions = ImeOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.None
            ),
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertThat((InputType.TYPE_CLASS_TEXT and info.inputType) != 0).isTrue()
            assertThat((EditorInfo.IME_FLAG_FORCE_ASCII and info.imeOptions) != 0).isTrue()
            assertThat(
                (EditorInfo.IME_MASK_ACTION and info.imeOptions)
                    == EditorInfo.IME_ACTION_NONE
            ).isTrue()
        }
    }

    @Test
    fun test_fill_editor_info_action_go() {
        textInputService.startInput(
            value = TextFieldValue(""),
            imeOptions = ImeOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Go
            ),
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertThat((InputType.TYPE_CLASS_TEXT and info.inputType) != 0).isTrue()
            assertThat((EditorInfo.IME_FLAG_FORCE_ASCII and info.imeOptions) != 0).isTrue()
            assertThat(
                (EditorInfo.IME_MASK_ACTION and info.imeOptions)
                    == EditorInfo.IME_ACTION_GO
            ).isTrue()
        }
    }

    @Test
    fun test_fill_editor_info_action_next() {
        textInputService.startInput(
            value = TextFieldValue(""),
            imeOptions = ImeOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Next
            ),
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertThat((InputType.TYPE_CLASS_TEXT and info.inputType) != 0).isTrue()
            assertThat((EditorInfo.IME_FLAG_FORCE_ASCII and info.imeOptions) != 0).isTrue()
            assertThat(
                (EditorInfo.IME_MASK_ACTION and info.imeOptions)
                    == EditorInfo.IME_ACTION_NEXT
            ).isTrue()
        }
    }

    @Test
    fun test_fill_editor_info_action_previous() {
        textInputService.startInput(
            value = TextFieldValue(""),
            imeOptions = ImeOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Previous
            ),
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertThat((InputType.TYPE_CLASS_TEXT and info.inputType) != 0).isTrue()
            assertThat((EditorInfo.IME_FLAG_FORCE_ASCII and info.imeOptions) != 0).isTrue()
            assertThat(
                (EditorInfo.IME_MASK_ACTION and info.imeOptions)
                    == EditorInfo.IME_ACTION_PREVIOUS
            ).isTrue()
        }
    }

    @Test
    fun test_fill_editor_info_action_search() {
        textInputService.startInput(
            value = TextFieldValue(""),
            imeOptions = ImeOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Search,
            ),
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertThat((InputType.TYPE_CLASS_TEXT and info.inputType) != 0).isTrue()
            assertThat((EditorInfo.IME_FLAG_FORCE_ASCII and info.imeOptions) != 0).isTrue()
            assertThat(
                (EditorInfo.IME_MASK_ACTION and info.imeOptions)
                    == EditorInfo.IME_ACTION_SEARCH
            ).isTrue()
        }
    }

    @Test
    fun test_fill_editor_info_action_send() {
        textInputService.startInput(
            value = TextFieldValue(""),
            imeOptions = ImeOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Send
            ),
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertThat((InputType.TYPE_CLASS_TEXT and info.inputType) != 0).isTrue()
            assertThat((EditorInfo.IME_FLAG_FORCE_ASCII and info.imeOptions) != 0).isTrue()
            assertThat(
                (EditorInfo.IME_MASK_ACTION and info.imeOptions)
                    == EditorInfo.IME_ACTION_SEND
            ).isTrue()
        }
    }

    @Test
    fun test_fill_editor_info_action_done() {
        textInputService.startInput(
            value = TextFieldValue(""),
            imeOptions = ImeOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done
            ),
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertThat((InputType.TYPE_CLASS_TEXT and info.inputType) != 0).isTrue()
            assertThat((EditorInfo.IME_FLAG_FORCE_ASCII and info.imeOptions) != 0).isTrue()
            assertThat(
                (EditorInfo.IME_MASK_ACTION and info.imeOptions)
                    == EditorInfo.IME_ACTION_DONE
            ).isTrue()
        }
    }

    @Test
    fun test_fill_editor_info_multi_line() {
        textInputService.startInput(
            value = TextFieldValue(""),
            imeOptions = ImeOptions(
                singleLine = false,
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done
            ),
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertThat((InputType.TYPE_TEXT_FLAG_MULTI_LINE and info.inputType) == 0).isFalse()
            assertThat((EditorInfo.IME_FLAG_NO_ENTER_ACTION and info.imeOptions) == 0).isTrue()
        }
    }

    @Test
    fun test_fill_editor_info_multi_line_with_default_action() {
        textInputService.startInput(
            value = TextFieldValue(""),
            imeOptions = ImeOptions(
                singleLine = false,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Default
            ),
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertThat((InputType.TYPE_TEXT_FLAG_MULTI_LINE and info.inputType) == 0).isFalse()
            assertThat((EditorInfo.IME_FLAG_NO_ENTER_ACTION and info.imeOptions) == 0).isFalse()
        }
    }

    @Test
    fun test_fill_editor_info_single_line() {
        textInputService.startInput(
            value = TextFieldValue(""),
            imeOptions = ImeOptions(
                singleLine = true,
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done
            ),
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertThat((InputType.TYPE_TEXT_FLAG_MULTI_LINE and info.inputType) == 0).isTrue()
            assertThat((EditorInfo.IME_FLAG_NO_ENTER_ACTION and info.imeOptions) == 0).isTrue()
        }
    }

    @Test
    fun test_fill_editor_info_single_line_changes_ime_from_unspecified_to_done() {
        textInputService.startInput(
            value = TextFieldValue(""),
            imeOptions = ImeOptions(
                singleLine = true,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Default
            ),
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertThat((EditorInfo.IME_ACTION_DONE and info.imeOptions) == 0).isFalse()
            assertThat((EditorInfo.IME_ACTION_UNSPECIFIED and info.imeOptions) == 0).isTrue()
        }
    }

    @Test
    fun test_fill_editor_info_multi_line_not_set_when_input_type_is_not_text() {
        textInputService.startInput(
            value = TextFieldValue(""),
            imeOptions = ImeOptions(
                singleLine = false,
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertThat((InputType.TYPE_TEXT_FLAG_MULTI_LINE and info.inputType) == 0).isTrue()
            assertThat((EditorInfo.IME_FLAG_NO_ENTER_ACTION and info.imeOptions) == 0).isTrue()
        }
    }

    @Test
    fun test_fill_editor_info_capitalization_none() {
        textInputService.startInput(
            value = TextFieldValue(""),
            imeOptions = ImeOptions(
                capitalization = KeyboardCapitalization.None,
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done
            ),
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertThat((InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS and info.inputType) == 0).isTrue()
            assertThat((InputType.TYPE_TEXT_FLAG_CAP_WORDS and info.inputType) == 0).isTrue()
            assertThat((InputType.TYPE_TEXT_FLAG_CAP_SENTENCES and info.inputType) == 0).isTrue()
        }
    }

    @Test
    fun test_fill_editor_info_capitalization_characters() {
        textInputService.startInput(
            value = TextFieldValue(""),
            imeOptions = ImeOptions(
                capitalization = KeyboardCapitalization.Characters,
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done
            ),
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertThat((InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS and info.inputType) == 0).isFalse()
            assertThat((InputType.TYPE_TEXT_FLAG_CAP_WORDS and info.inputType) == 0).isTrue()
            assertThat((InputType.TYPE_TEXT_FLAG_CAP_SENTENCES and info.inputType) == 0).isTrue()
        }
    }

    @Test
    fun test_fill_editor_info_capitalization_words() {
        textInputService.startInput(
            value = TextFieldValue(""),
            imeOptions = ImeOptions(
                capitalization = KeyboardCapitalization.Words,
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done
            ),
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertThat((InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS and info.inputType) == 0).isTrue()
            assertThat((InputType.TYPE_TEXT_FLAG_CAP_WORDS and info.inputType) == 0).isFalse()
            assertThat((InputType.TYPE_TEXT_FLAG_CAP_SENTENCES and info.inputType) == 0).isTrue()
        }
    }

    @Test
    fun test_fill_editor_info_capitalization_sentences() {
        textInputService.startInput(
            value = TextFieldValue(""),
            imeOptions = ImeOptions(
                capitalization = KeyboardCapitalization.Sentences,
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done,
            ),
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertThat((InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS and info.inputType) == 0).isTrue()
            assertThat((InputType.TYPE_TEXT_FLAG_CAP_WORDS and info.inputType) == 0).isTrue()
            assertThat((InputType.TYPE_TEXT_FLAG_CAP_SENTENCES and info.inputType) == 0).isFalse()
        }
    }

    @Test
    fun test_fill_editor_info_capitalization_not_added_when_input_type_is_not_text() {
        textInputService.startInput(
            value = TextFieldValue(""),
            imeOptions = ImeOptions(
                capitalization = KeyboardCapitalization.Sentences,
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertThat((InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS and info.inputType) == 0).isTrue()
            assertThat((InputType.TYPE_TEXT_FLAG_CAP_WORDS and info.inputType) == 0).isTrue()
            assertThat((InputType.TYPE_TEXT_FLAG_CAP_SENTENCES and info.inputType) == 0).isTrue()
        }
    }

    @Test
    fun test_fill_editor_info_auto_correct_on() {
        textInputService.startInput(
            value = TextFieldValue(""),
            imeOptions = ImeOptions(
                autoCorrect = true,
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done
            ),
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertThat((InputType.TYPE_TEXT_FLAG_AUTO_CORRECT and info.inputType) == 0).isFalse()
        }
    }

    @Test
    fun test_fill_editor_info_auto_correct_off() {
        textInputService.startInput(
            value = TextFieldValue(""),
            imeOptions = ImeOptions(
                autoCorrect = false,
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done
            ),
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertThat((InputType.TYPE_TEXT_FLAG_AUTO_CORRECT and info.inputType) == 0).isTrue()
        }
    }

    @Test
    fun autocorrect_not_added_when_input_type_is_not_text() {
        textInputService.startInput(
            value = TextFieldValue(""),
            imeOptions = ImeOptions(
                autoCorrect = true,
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertThat((InputType.TYPE_TEXT_FLAG_AUTO_CORRECT and info.inputType) == 0).isTrue()
        }
    }

    @Test
    fun initial_default_selection_info_is_set() {
        textInputService.startInput(
            value = TextFieldValue(),
            imeOptions = ImeOptions.Default,
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertThat(info.initialSelStart).isEqualTo(0)
            assertThat(info.initialSelEnd).isEqualTo(0)
        }
    }

    @Test
    fun initial_selection_info_is_set() {
        val selection = TextRange(1, 2)
        textInputService.startInput(
            value = TextFieldValue("abc", selection),
            imeOptions = ImeOptions.Default,
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertThat(info.initialSelStart).isEqualTo(selection.start)
            assertThat(info.initialSelEnd).isEqualTo(selection.end)
        }
    }
}