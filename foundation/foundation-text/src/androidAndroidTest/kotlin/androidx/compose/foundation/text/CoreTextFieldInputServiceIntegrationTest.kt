/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.foundation.text

import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.ExperimentalFocus
import androidx.compose.ui.focus.isFocused
import androidx.compose.ui.focusObserver
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PlatformTextInputService
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TextInputService
import androidx.compose.ui.text.input.textInputServiceFactory
import androidx.test.filters.LargeTest
import androidx.ui.test.createComposeRule
import androidx.ui.test.onNodeWithTag
import androidx.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(
    ExperimentalTextApi::class,
    InternalTextApi::class,
    ExperimentalFocus::class
)
@LargeTest
@RunWith(JUnit4::class)
class CoreTextFieldInputServiceIntegrationTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun textField_KeyboardOptions_isPassedTo_platformTextInputService() {
        val platformTextInputService = mock<PlatformTextInputService>()
        @Suppress("DEPRECATION_ERROR")
        textInputServiceFactory = { TextInputService(platformTextInputService) }

        val testTag = "KeyboardOption"
        val value = TextFieldValue("abc")
        val keyboardOptions = KeyboardOptions(
            singleLine = true,
            capitalization = KeyboardCapitalization.Words,
            autoCorrect = false
        )
        val keyboardType = KeyboardType.Phone
        val imeAction = ImeAction.Search
        var focused = false

        rule.setContent {
            CoreTextField(
                value = value,
                keyboardOptions = keyboardOptions,
                keyboardType = keyboardType,
                imeAction = imeAction,
                modifier = Modifier
                    .testTag(testTag)
                    .focusObserver { focused = it.isFocused },
                onValueChange = {}
            )
        }

        rule.onNodeWithTag(testTag).performClick()

        rule.runOnIdle {
            assertThat(focused).isTrue()

            verify(platformTextInputService, times(1)).startInput(
                eq(value),
                eq(keyboardType),
                eq(imeAction),
                eq(keyboardOptions),
                any(), // onEditCommand
                any() // onImeActionPerformed
            )
        }
    }
}
