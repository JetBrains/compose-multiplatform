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
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TextInputServiceAndroid
import androidx.test.filters.SmallTest
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalTextApi::class)
@SmallTest
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
            TextFieldValue(""),
            KeyboardType.Text,
            ImeAction.Unspecified,
            KeyboardOptions.Default,
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertTrue((InputType.TYPE_CLASS_TEXT and info.inputType) != 0)
            assertTrue(
                (EditorInfo.IME_MASK_ACTION and info.imeOptions)
                    == EditorInfo.IME_ACTION_UNSPECIFIED
            )
        }
    }

    @Test
    fun test_fill_editor_info_ascii() {
        textInputService.startInput(
            TextFieldValue(""),
            KeyboardType.Ascii,
            ImeAction.Unspecified,
            KeyboardOptions.Default,
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertTrue((InputType.TYPE_CLASS_TEXT and info.inputType) != 0)
            assertTrue((EditorInfo.IME_FLAG_FORCE_ASCII and info.imeOptions) != 0)
            assertTrue(
                (EditorInfo.IME_MASK_ACTION and info.imeOptions)
                    == EditorInfo.IME_ACTION_UNSPECIFIED
            )
        }
    }

    @Test
    fun test_fill_editor_info_number() {
        textInputService.startInput(
            TextFieldValue(""),
            KeyboardType.Number,
            ImeAction.Unspecified,
            KeyboardOptions.Default,
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertTrue((InputType.TYPE_CLASS_NUMBER and info.inputType) != 0)
            assertTrue(
                (EditorInfo.IME_MASK_ACTION and info.imeOptions)
                    == EditorInfo.IME_ACTION_UNSPECIFIED
            )
        }
    }

    @Test
    fun test_fill_editor_info_phone() {
        textInputService.startInput(
            TextFieldValue(""),
            KeyboardType.Phone,
            ImeAction.Unspecified,
            KeyboardOptions.Default,
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertTrue((InputType.TYPE_CLASS_PHONE and info.inputType) != 0)
            assertTrue(
                (EditorInfo.IME_MASK_ACTION and info.imeOptions)
                    == EditorInfo.IME_ACTION_UNSPECIFIED
            )
        }
    }

    @Test
    fun test_fill_editor_info_uri() {
        textInputService.startInput(
            TextFieldValue(""),
            KeyboardType.Uri,
            ImeAction.Unspecified,
            KeyboardOptions.Default,
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertTrue((InputType.TYPE_CLASS_TEXT and info.inputType) != 0)
            assertTrue((InputType.TYPE_TEXT_VARIATION_URI and info.inputType) != 0)
            assertTrue(
                (EditorInfo.IME_MASK_ACTION and info.imeOptions)
                    == EditorInfo.IME_ACTION_UNSPECIFIED
            )
        }
    }

    @Test
    fun test_fill_editor_info_email() {
        textInputService.startInput(
            TextFieldValue(""),
            KeyboardType.Email,
            ImeAction.Unspecified,
            KeyboardOptions.Default,
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertTrue((InputType.TYPE_CLASS_TEXT and info.inputType) != 0)
            assertTrue((InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS and info.inputType) != 0)
            assertTrue(
                (EditorInfo.IME_MASK_ACTION and info.imeOptions)
                    == EditorInfo.IME_ACTION_UNSPECIFIED
            )
        }
    }

    @Test
    fun test_fill_editor_info_password() {
        textInputService.startInput(
            TextFieldValue(""),
            KeyboardType.Password,
            ImeAction.Unspecified,
            KeyboardOptions.Default,
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertTrue((InputType.TYPE_CLASS_TEXT and info.inputType) != 0)
            assertTrue((InputType.TYPE_TEXT_VARIATION_PASSWORD and info.inputType) != 0)
            assertTrue(
                (EditorInfo.IME_MASK_ACTION and info.imeOptions)
                    == EditorInfo.IME_ACTION_UNSPECIFIED
            )
        }
    }

    @Test
    fun test_fill_editor_info_number_password() {
        textInputService.startInput(
            TextFieldValue(""),
            KeyboardType.NumberPassword,
            ImeAction.Unspecified,
            KeyboardOptions.Default,
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertTrue((InputType.TYPE_CLASS_NUMBER and info.inputType) != 0)
            assertTrue((InputType.TYPE_NUMBER_VARIATION_PASSWORD and info.inputType) != 0)
            assertTrue(
                (EditorInfo.IME_MASK_ACTION and info.imeOptions)
                    == EditorInfo.IME_ACTION_UNSPECIFIED
            )
        }
    }

    @Test
    fun test_fill_editor_info_action_none() {
        textInputService.startInput(
            TextFieldValue(""),
            KeyboardType.Ascii,
            ImeAction.NoAction,
            KeyboardOptions.Default,
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertTrue((InputType.TYPE_CLASS_TEXT and info.inputType) != 0)
            assertTrue((EditorInfo.IME_FLAG_FORCE_ASCII and info.imeOptions) != 0)
            assertTrue(
                (EditorInfo.IME_MASK_ACTION and info.imeOptions)
                    == EditorInfo.IME_ACTION_NONE
            )
        }
    }

    @Test
    fun test_fill_editor_info_action_go() {
        textInputService.startInput(
            TextFieldValue(""),
            KeyboardType.Ascii,
            ImeAction.Go,
            KeyboardOptions.Default,
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertTrue((InputType.TYPE_CLASS_TEXT and info.inputType) != 0)
            assertTrue((EditorInfo.IME_FLAG_FORCE_ASCII and info.imeOptions) != 0)
            assertTrue(
                (EditorInfo.IME_MASK_ACTION and info.imeOptions)
                    == EditorInfo.IME_ACTION_GO
            )
        }
    }

    @Test
    fun test_fill_editor_info_action_next() {
        textInputService.startInput(
            TextFieldValue(""),
            KeyboardType.Ascii,
            ImeAction.Next,
            KeyboardOptions.Default,
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertTrue((InputType.TYPE_CLASS_TEXT and info.inputType) != 0)
            assertTrue((EditorInfo.IME_FLAG_FORCE_ASCII and info.imeOptions) != 0)
            assertTrue(
                (EditorInfo.IME_MASK_ACTION and info.imeOptions)
                    == EditorInfo.IME_ACTION_NEXT
            )
        }
    }

    @Test
    fun test_fill_editor_info_action_previous() {
        textInputService.startInput(
            TextFieldValue(""),
            KeyboardType.Ascii,
            ImeAction.Previous,
            KeyboardOptions.Default,
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertTrue((InputType.TYPE_CLASS_TEXT and info.inputType) != 0)
            assertTrue((EditorInfo.IME_FLAG_FORCE_ASCII and info.imeOptions) != 0)
            assertTrue(
                (EditorInfo.IME_MASK_ACTION and info.imeOptions)
                    == EditorInfo.IME_ACTION_PREVIOUS
            )
        }
    }

    @Test
    fun test_fill_editor_info_action_search() {
        textInputService.startInput(
            TextFieldValue(""),
            KeyboardType.Ascii,
            ImeAction.Search,
            KeyboardOptions.Default,
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertTrue((InputType.TYPE_CLASS_TEXT and info.inputType) != 0)
            assertTrue((EditorInfo.IME_FLAG_FORCE_ASCII and info.imeOptions) != 0)
            assertTrue(
                (EditorInfo.IME_MASK_ACTION and info.imeOptions)
                    == EditorInfo.IME_ACTION_SEARCH
            )
        }
    }

    @Test
    fun test_fill_editor_info_action_send() {
        textInputService.startInput(
            TextFieldValue(""),
            KeyboardType.Ascii,
            ImeAction.Send,
            KeyboardOptions.Default,
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertTrue((InputType.TYPE_CLASS_TEXT and info.inputType) != 0)
            assertTrue((EditorInfo.IME_FLAG_FORCE_ASCII and info.imeOptions) != 0)
            assertTrue(
                (EditorInfo.IME_MASK_ACTION and info.imeOptions)
                    == EditorInfo.IME_ACTION_SEND
            )
        }
    }

    @Test
    fun test_fill_editor_info_action_done() {
        textInputService.startInput(
            TextFieldValue(""),
            KeyboardType.Ascii,
            ImeAction.Done,
            KeyboardOptions.Default,
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertTrue((InputType.TYPE_CLASS_TEXT and info.inputType) != 0)
            assertTrue((EditorInfo.IME_FLAG_FORCE_ASCII and info.imeOptions) != 0)
            assertTrue(
                (EditorInfo.IME_MASK_ACTION and info.imeOptions)
                    == EditorInfo.IME_ACTION_DONE
            )
        }
    }

    @Test
    fun test_fill_editor_info_multi_line() {
        textInputService.startInput(
            TextFieldValue(""),
            KeyboardType.Ascii,
            ImeAction.Done,
            KeyboardOptions(singleLine = false),
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertFalse((InputType.TYPE_TEXT_FLAG_MULTI_LINE and info.inputType) == 0)
            assertFalse((EditorInfo.IME_FLAG_NO_ENTER_ACTION and info.imeOptions) == 0)
        }
    }

    @Test
    fun test_fill_editor_info_single_line() {
        textInputService.startInput(
            TextFieldValue(""),
            KeyboardType.Ascii,
            ImeAction.Done,
            KeyboardOptions(singleLine = true),
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertTrue((InputType.TYPE_TEXT_FLAG_MULTI_LINE and info.inputType) == 0)
            assertTrue((EditorInfo.IME_FLAG_NO_ENTER_ACTION and info.imeOptions) == 0)
        }
    }

    @Test
    fun test_fill_editor_info_single_line_changes_ime_from_unspecified_to_done() {
        textInputService.startInput(
            TextFieldValue(""),
            KeyboardType.Text,
            ImeAction.Unspecified,
            KeyboardOptions(singleLine = true),
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertFalse((EditorInfo.IME_ACTION_DONE and info.imeOptions) == 0)
            assertTrue((EditorInfo.IME_ACTION_UNSPECIFIED and info.imeOptions) == 0)
        }
    }

    @Test
    fun test_fill_editor_info_multi_line_not_set_when_input_type_is_not_text() {
        textInputService.startInput(
            TextFieldValue(""),
            KeyboardType.Number,
            ImeAction.Done,
            KeyboardOptions(singleLine = false),
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertTrue((InputType.TYPE_TEXT_FLAG_MULTI_LINE and info.inputType) == 0)
            assertFalse((EditorInfo.IME_FLAG_NO_ENTER_ACTION and info.imeOptions) == 0)
        }
    }

    @Test
    fun test_fill_editor_info_capitalization_none() {
        textInputService.startInput(
            TextFieldValue(""),
            KeyboardType.Ascii,
            ImeAction.Done,
            KeyboardOptions(capitalization = KeyboardCapitalization.None),
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertTrue((InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS and info.inputType) == 0)
            assertTrue((InputType.TYPE_TEXT_FLAG_CAP_WORDS and info.inputType) == 0)
            assertTrue((InputType.TYPE_TEXT_FLAG_CAP_SENTENCES and info.inputType) == 0)
        }
    }

    @Test
    fun test_fill_editor_info_capitalization_characters() {
        textInputService.startInput(
            TextFieldValue(""),
            KeyboardType.Ascii,
            ImeAction.Done,
            KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertFalse((InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS and info.inputType) == 0)
            assertTrue((InputType.TYPE_TEXT_FLAG_CAP_WORDS and info.inputType) == 0)
            assertTrue((InputType.TYPE_TEXT_FLAG_CAP_SENTENCES and info.inputType) == 0)
        }
    }

    @Test
    fun test_fill_editor_info_capitalization_words() {
        textInputService.startInput(
            TextFieldValue(""),
            KeyboardType.Ascii,
            ImeAction.Done,
            KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertTrue((InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS and info.inputType) == 0)
            assertFalse((InputType.TYPE_TEXT_FLAG_CAP_WORDS and info.inputType) == 0)
            assertTrue((InputType.TYPE_TEXT_FLAG_CAP_SENTENCES and info.inputType) == 0)
        }
    }

    @Test
    fun test_fill_editor_info_capitalization_sentences() {
        textInputService.startInput(
            TextFieldValue(""),
            KeyboardType.Ascii,
            ImeAction.Done,
            KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertTrue((InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS and info.inputType) == 0)
            assertTrue((InputType.TYPE_TEXT_FLAG_CAP_WORDS and info.inputType) == 0)
            assertFalse((InputType.TYPE_TEXT_FLAG_CAP_SENTENCES and info.inputType) == 0)
        }
    }

    @Test
    fun test_fill_editor_info_capitalization_not_added_when_input_type_is_not_text() {
        textInputService.startInput(
            TextFieldValue(""),
            KeyboardType.Number,
            ImeAction.Done,
            KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertTrue((InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS and info.inputType) == 0)
            assertTrue((InputType.TYPE_TEXT_FLAG_CAP_WORDS and info.inputType) == 0)
            assertTrue((InputType.TYPE_TEXT_FLAG_CAP_SENTENCES and info.inputType) == 0)
        }
    }

    @Test
    fun test_fill_editor_info_auto_correct_on() {
        textInputService.startInput(
            TextFieldValue(""),
            KeyboardType.Ascii,
            ImeAction.Done,
            KeyboardOptions(autoCorrect = true),
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertFalse((InputType.TYPE_TEXT_FLAG_AUTO_CORRECT and info.inputType) == 0)
        }
    }

    @Test
    fun test_fill_editor_info_auto_correct_off() {
        textInputService.startInput(
            TextFieldValue(""),
            KeyboardType.Ascii,
            ImeAction.Done,
            KeyboardOptions(autoCorrect = false),
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertTrue((InputType.TYPE_TEXT_FLAG_AUTO_CORRECT and info.inputType) == 0)
        }
    }

    @Test
    fun autocorrect_not_added_when_input_type_is_not_text() {
        textInputService.startInput(
            TextFieldValue(""),
            KeyboardType.Number,
            ImeAction.Done,
            KeyboardOptions(autoCorrect = true),
            onEditCommand = {},
            onImeActionPerformed = {}
        )

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertTrue((InputType.TYPE_TEXT_FLAG_AUTO_CORRECT and info.inputType) == 0)
        }
    }
}