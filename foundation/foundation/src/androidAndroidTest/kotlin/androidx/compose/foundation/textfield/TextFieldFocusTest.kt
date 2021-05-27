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

package androidx.compose.foundation.textfield

import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.CoreTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class TextFieldFocusTest {
    @get:Rule
    val rule = createComposeRule()

    @Composable
    private fun TextFieldApp(dataList: List<FocusTestData>) {
        for (data in dataList) {
            val editor = remember { mutableStateOf("") }
            BasicTextField(
                value = editor.value,
                modifier = Modifier
                    .focusRequester(data.focusRequester)
                    .onFocusChanged { data.focused = it.isFocused }
                    .requiredWidth(10.dp),
                onValueChange = {
                    editor.value = it
                }
            )
        }
    }

    data class FocusTestData(val focusRequester: FocusRequester, var focused: Boolean = false)

    @Test
    fun requestFocus() {
        lateinit var testDataList: List<FocusTestData>

        rule.runOnUiThread {
            rule.setContent {
                testDataList = listOf(
                    FocusTestData(FocusRequester()),
                    FocusTestData(FocusRequester()),
                    FocusTestData(FocusRequester())
                )

                TextFieldApp(testDataList)
            }
        }

        rule.runOnIdle { testDataList[0].focusRequester.requestFocus() }

        rule.runOnIdle {
            assertThat(testDataList[0].focused).isTrue()
            assertThat(testDataList[1].focused).isFalse()
            assertThat(testDataList[2].focused).isFalse()
        }

        rule.runOnIdle { testDataList[1].focusRequester.requestFocus() }
        rule.runOnIdle {
            assertThat(testDataList[0].focused).isFalse()
            assertThat(testDataList[1].focused).isTrue()
            assertThat(testDataList[2].focused).isFalse()
        }

        rule.runOnIdle { testDataList[2].focusRequester.requestFocus() }
        rule.runOnIdle {
            assertThat(testDataList[0].focused).isFalse()
            assertThat(testDataList[1].focused).isFalse()
            assertThat(testDataList[2].focused).isTrue()
        }
    }

    @Test
    fun noCrushWhenSwitchingBetweenEnabledFocusedAndDisabledTextField() {
        val enabled = mutableStateOf(true)
        var focused = false
        val tag = "textField"
        rule.setContent {
            CoreTextField(
                value = TextFieldValue(),
                onValueChange = {},
                enabled = enabled.value,
                modifier = Modifier.testTag(tag).onFocusChanged {
                    focused = it.isFocused
                }
            )
        }
        // bring enabled text field into focus
        rule.onNodeWithTag(tag)
            .performClick()
        rule.runOnIdle {
            assertThat(focused).isTrue()
        }

        // make text field disabled
        enabled.value = false
        rule.runOnIdle {
            assertThat(focused).isFalse()
        }

        // make text field enabled again, it must not crash
        enabled.value = true
        rule.runOnIdle {
            assertThat(focused).isFalse()
        }
    }
}
