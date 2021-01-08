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
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsAnimation
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.AmbientFocusManager
import androidx.compose.ui.platform.AmbientView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@OptIn(InternalTextApi::class)
class CoreTextFieldSoftKeyboardTest {

    @get:Rule
    val rule = createComposeRule()

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    @Test
    fun keyboardShownOnInitialClick() {
        // Arrange.
        lateinit var view: View
        rule.setContent {
            view = AmbientView.current
            CoreTextField(
                value = TextFieldValue("Hello"),
                onValueChange = {},
                modifier = Modifier.testTag("TextField1")
            )
        }
        view.ensureKeyboardIsHidden()

        // Act.
        view.runAndWaitUntil({ view.isSoftwareKeyboardShown() }) {
            rule.onNodeWithTag("TextField1").performClick()
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    @Test
    fun keyboardShownOnInitialFocus() {
        // Arrange.
        val focusRequester = FocusRequester()
        lateinit var view: View
        rule.setContent {
            view = AmbientView.current
            CoreTextField(
                value = TextFieldValue("Hello"),
                onValueChange = {},
                modifier = Modifier.focusRequester(focusRequester)
            )
        }
        view.ensureKeyboardIsHidden()

        // Act.
        view.runAndWaitUntil({ view.isSoftwareKeyboardShown() }) {
            rule.runOnIdle { focusRequester.requestFocus() }
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    @Test
    fun keyboardHiddenWhenFocusIsLost() {
        // Arrange.
        lateinit var focusManager: FocusManager
        lateinit var view: View
        val focusRequester = FocusRequester()
        rule.setContent {
            view = AmbientView.current
            focusManager = AmbientFocusManager.current
            CoreTextField(
                value = TextFieldValue("Hello"),
                onValueChange = {},
                modifier = Modifier.focusRequester(focusRequester)
            )
        }
        view.ensureKeyboardIsHidden()
        // Request focus and wait for keyboard.
        view.runAndWaitUntil({ view.isSoftwareKeyboardShown() }) {
            rule.runOnIdle { focusRequester.requestFocus() }
        }

        // Act.
        view.runAndWaitUntil({ !view.isSoftwareKeyboardShown() }) {
            rule.runOnIdle { focusManager.clearFocus() }
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    @Test
    fun keyboardShownAfterDismissingKeyboardAndClickingAgain() {
        // Arrange.
        lateinit var view: View
        rule.setContent {
            view = AmbientView.current
            CoreTextField(
                value = TextFieldValue("Hello"),
                onValueChange = {},
                modifier = Modifier.testTag("TextField1")
            )
        }
        view.ensureKeyboardIsHidden()
        view.runAndWaitUntil({ view.isSoftwareKeyboardShown() }) {
            rule.onNodeWithTag("TextField1").performClick()
        }
        view.runAndWaitUntil({ !view.isSoftwareKeyboardShown() }) {
            rule.runOnIdle { view.hideKeyboard() }
        }

        // Act.
        view.runAndWaitUntil({ view.isSoftwareKeyboardShown() }) {
            rule.onNodeWithTag("TextField1").performClick()
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    @Test
    fun keyboardStaysVisibleWhenMovingFromOneTextFieldToAnother() {
        // Arrange.
        val (focusRequester1, focusRequester2) = FocusRequester.createRefs()
        lateinit var view: View
        rule.setContent {
            view = AmbientView.current
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
        view.ensureKeyboardIsHidden()
        view.runAndWaitUntil({ view.isSoftwareKeyboardShown() }) {
            rule.runOnIdle { focusRequester1.requestFocus() }
        }

        // Act.
        view.runAndWaitUntil({ !view.isSoftwareKeyboardShown() }) {
            rule.runOnIdle { focusRequester2.requestFocus() }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun View.runAndWaitUntil(condition: () -> Boolean, block: () -> Unit) {
        var conditionPassed = false
        rule.runOnIdle {
            rootView.setWindowInsetsAnimationCallback(
                InsetAnimationCallback {
                    if (condition()) { conditionPassed = true }
                }
            )
        }
        rule.waitForIdle()
        block()
        rule.waitUntil(15_000) { conditionPassed }
    }

    // We experienced some flakiness in tests if the keyboard was visible at the start of the test.
    // This function makes sure the keyboard is hidden at the start of every test.
    @RequiresApi(Build.VERSION_CODES.R)
    private fun View.ensureKeyboardIsHidden() {
        rule.waitForIdle()
        if (isSoftwareKeyboardShown()) {
            runAndWaitUntil({ !isSoftwareKeyboardShown() }) {
                rule.runOnIdle { hideKeyboard() }
            }
        }
        rule.waitForIdle()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private class InsetAnimationCallback(val block: () -> Unit) :
        WindowInsetsAnimation.Callback(DISPATCH_MODE_CONTINUE_ON_SUBTREE) {

        override fun onProgress(
            insets: WindowInsets,
            runningAnimations: MutableList<WindowInsetsAnimation>
        ) = insets

        override fun onEnd(animation: WindowInsetsAnimation) {
            block()
            super.onEnd(animation)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.R)
private fun View.isSoftwareKeyboardShown(): Boolean {
    return rootWindowInsets != null && rootWindowInsets.isVisible(WindowInsets.Type.ime())
}

@RequiresApi(Build.VERSION_CODES.R)
private fun View.hideKeyboard() {
    windowInsetsController?.hide(WindowInsets.Type.ime())
}
