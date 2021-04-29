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

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PlatformTextInputService
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TextInputService
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class CoreTextFieldInputServiceIntegrationTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun textField_ImeOptions_isPassedTo_platformTextInputService() {
        val platformTextInputService = mock<PlatformTextInputService>()
        val textInputService = TextInputService(platformTextInputService)

        val testTag = "KeyboardOption"
        val value = TextFieldValue("abc")
        val imeOptions = ImeOptions(
            singleLine = true,
            capitalization = KeyboardCapitalization.Words,
            autoCorrect = false,
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Search
        )

        var focused = false

        rule.setContent {
            CompositionLocalProvider(LocalTextInputService provides textInputService) {
                CoreTextField(
                    value = value,
                    imeOptions = imeOptions,
                    modifier = Modifier
                        .testTag(testTag)
                        .onFocusChanged { focused = it.isFocused },
                    onValueChange = {}
                )
            }
        }

        rule.onNodeWithTag(testTag).performClick()

        rule.runOnIdle {
            assertThat(focused).isTrue()

            verify(platformTextInputService, times(1)).startInput(
                eq(value),
                eq(imeOptions),
                any(), // onEditCommand
                any() // onImeActionPerformed
            )
        }
    }
}
