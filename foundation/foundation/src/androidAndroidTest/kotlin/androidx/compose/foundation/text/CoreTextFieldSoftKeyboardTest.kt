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
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@LargeTest
@RunWith(AndroidJUnit4::class)
class CoreTextFieldSoftKeyboardTest {
    @get:Rule
    val rule = createComposeRule()

    private lateinit var view: View
    private lateinit var focusManager: FocusManager
    private val timeout = 15_000L

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
        view.waitUntil(timeout) { view.isSoftwareKeyboardShown() }
    }

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
        view.waitUntil(timeout) { view.isSoftwareKeyboardShown() }
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
        view.waitUntil(timeout) { view.isSoftwareKeyboardShown() }

        // Act.
        rule.runOnIdle { focusManager.clearFocus() }

        // Assert.
        view.waitUntil(timeout) { !view.isSoftwareKeyboardShown() }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    @Test
    fun keyboardShownAfterDismissingKeyboardAndClickingAgain() {
        // Arrange.
        rule.setContentForTest {
            view = LocalView.current
            CoreTextField(
                value = TextFieldValue("Hello"),
                onValueChange = {},
                modifier = Modifier.testTag("TextField1")
            )
        }
        rule.onNodeWithTag("TextField1").performClick()
        view.waitUntil(timeout) { view.isSoftwareKeyboardShown() }

        // Act.
        rule.runOnIdle { view.hideKeyboard() }
        view.waitUntil(timeout) { !view.isSoftwareKeyboardShown() }
        rule.onNodeWithTag("TextField1").performClick()

        // Assert.
        view.waitUntil(timeout) { view.isSoftwareKeyboardShown() }
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
        view.waitUntil(timeout) { view.isSoftwareKeyboardShown() }

        // Act.
        rule.runOnIdle { focusRequester2.requestFocus() }

        // Assert.
        view.waitUntil(timeout) { !view.isSoftwareKeyboardShown() }
    }

    private fun ComposeContentTestRule.setContentForTest(composable: @Composable () -> Unit) {
        setContent {
            view = LocalView.current
            focusManager = LocalFocusManager.current
            composable()
        }
        // We experienced some flakiness in tests if the keyboard was visible at the start of the
        // test. So we make sure that the keyboard is hidden at the start of every test.
        runOnIdle {
            if (view.isSoftwareKeyboardShown()) {
                view.hideKeyboard()
                view.waitUntil(timeout) { !view.isSoftwareKeyboardShown() }
            }
        }
    }
}

private fun View.waitUntil(timeoutMillis: Long, condition: () -> Boolean) {
    val latch = CountDownLatch(1)
    rootView.setWindowInsetsAnimationCallback(
        InsetAnimationCallback {
            if (condition()) { latch.countDown() }
        }
    )
    latch.await(timeoutMillis, TimeUnit.MILLISECONDS)
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

@RequiresApi(Build.VERSION_CODES.R)
private fun View.isSoftwareKeyboardShown(): Boolean {
    return rootWindowInsets != null && rootWindowInsets.isVisible(WindowInsets.Type.ime())
}

@RequiresApi(Build.VERSION_CODES.R)
private fun View.hideKeyboard() {
    windowInsetsController?.hide(WindowInsets.Type.ime())
}
