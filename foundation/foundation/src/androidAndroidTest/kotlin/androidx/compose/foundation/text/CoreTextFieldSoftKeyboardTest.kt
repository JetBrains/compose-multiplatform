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

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.FlakyTest
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class CoreTextFieldSoftKeyboardTest {
    @get:Rule
    val rule = createComposeRule()

    private lateinit var focusManager: FocusManager
    private val timeout = 15_000L
    private val keyboardHelper = KeyboardHelper(rule, timeout)

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    @Test
    fun keyboardShownOnInitialClick() {
        // Arrange.
        rule.setContentForTest {
            CoreTextField(
                value = TextFieldValue("Hello"),
                onValueChange = {},
                modifier = Modifier.testTag("TextField1")
            )
        }

        // Act.
        rule.onNodeWithTag("TextField1").performClick()

        // Assert.
        keyboardHelper.waitForKeyboardVisibility(visible = true)
    }

    @FlakyTest(bugId = 228258574)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    @Test
    fun keyboardShownOnInitialFocus() {
        // Arrange.
        val focusRequester = FocusRequester()
        rule.setContentForTest {
            CoreTextField(
                value = TextFieldValue("Hello"),
                onValueChange = {},
                modifier = Modifier.focusRequester(focusRequester)
            )
        }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        keyboardHelper.waitForKeyboardVisibility(visible = true)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    @Test
    fun keyboardHiddenWhenFocusIsLost() {
        // Arrange.
        val focusRequester = FocusRequester()
        rule.setContentForTest {
            CoreTextField(
                value = TextFieldValue("Hello"),
                onValueChange = {},
                modifier = Modifier.focusRequester(focusRequester)
            )
        }
        // Request focus and wait for keyboard.
        rule.runOnIdle { focusRequester.requestFocus() }
        keyboardHelper.waitForKeyboardVisibility(visible = true)

        // Act.
        rule.runOnIdle { focusManager.clearFocus() }

        // Assert.
        keyboardHelper.waitForKeyboardVisibility(visible = false)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    @Test
    fun keyboardShownAfterDismissingKeyboardAndClickingAgain() {
        // Arrange.
        rule.setContentForTest {
            CoreTextField(
                value = TextFieldValue("Hello"),
                onValueChange = {},
                modifier = Modifier.testTag("TextField1")
            )
        }
        rule.onNodeWithTag("TextField1").performClick()
        keyboardHelper.waitForKeyboardVisibility(visible = true)

        // Act.
        rule.runOnIdle { keyboardHelper.hideKeyboard() }
        keyboardHelper.waitForKeyboardVisibility(visible = false)
        rule.onNodeWithTag("TextField1").performClick()

        // Assert.
        keyboardHelper.waitForKeyboardVisibility(visible = true)
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    @Test
    fun keyboardStaysVisibleWhenMovingFromOneTextFieldToAnother() {
        // Arrange.
        val (focusRequester1, focusRequester2) = FocusRequester.createRefs()
        rule.setContentForTest {
            Column {
                CoreTextField(
                    value = TextFieldValue("Hello"),
                    onValueChange = {},
                    modifier = Modifier.focusRequester(focusRequester1)
                )
                CoreTextField(
                    value = TextFieldValue("Hello"),
                    onValueChange = {},
                    modifier = Modifier.focusRequester(focusRequester2)
                )
            }
        }
        rule.runOnIdle { focusRequester1.requestFocus() }
        keyboardHelper.waitForKeyboardVisibility(visible = true)

        // Act.
        rule.runOnIdle { focusRequester2.requestFocus() }

        // Assert.
        keyboardHelper.waitForKeyboardVisibility(visible = false)
    }

    private fun ComposeContentTestRule.setContentForTest(composable: @Composable () -> Unit) {
        setContent {
            keyboardHelper.view = LocalView.current
            focusManager = LocalFocusManager.current
            composable()
        }
        // We experienced some flakiness in tests if the keyboard was visible at the start of the
        // test. So we make sure that the keyboard is hidden at the start of every test.
        keyboardHelper.hideKeyboardIfShown()
    }
}
