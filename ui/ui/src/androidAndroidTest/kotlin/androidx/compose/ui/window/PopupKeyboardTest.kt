/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.window

import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsAnimation
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.background
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(AndroidJUnit4::class)
class PopupKeyboardTest {
    @get:Rule
    val rule = createAndroidComposeRule<TestActivity>()

    private lateinit var view: View
    private val timeout = 10_000L

    private val testTag = "testedPopup"
    private val TFTag = "TextField"

    @OptIn(ExperimentalComposeUiApi::class)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    @Test
    fun flagAltFocusableIMForNotFocusableDoesNotCloseKeyboardTest() {
        rule.setContentForTest {
            BoxWithAnchorAndPopupForTest(
                testTag,
                TFTag,
                PopupProperties(
                    focusable = false,
                    updateAndroidWindowManagerFlags = { flags ->
                        flags or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
                    }
                )
            )
        }

        // Popup should not exist
        rule.onNodeWithTag(testTag).assertDoesNotExist()

        // Click on the TextField
        rule.onNodeWithTag(TFTag).performClick()

        // Popup should be visible
        rule.onNodeWithTag(testTag).assertIsDisplayed()
        view.waitUntil(timeout) { view.isSoftwareKeyboardShown() }

        rule.runOnIdle { Truth.assertThat(view.isSoftwareKeyboardShown()).isTrue() }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    @Test
    fun flagAltFocusableIMForFocusableDoesNotCloseKeyboardTest() {
        rule.setContentForTest {
            BoxWithAnchorAndPopupForTest(
                testTag,
                TFTag,
                PopupProperties(
                    focusable = true,
                    updateAndroidWindowManagerFlags = { flags ->
                        flags or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
                    }
                )
            )
        }

        // Popup should not be visible
        rule.onNodeWithTag(testTag).assertDoesNotExist()

        // Click on the TextField
        rule.onNodeWithTag(TFTag).performClick()

        // Popup should be visible
        rule.onNodeWithTag(testTag).assertIsDisplayed()
        view.waitUntil(timeout) { view.isSoftwareKeyboardShown() }

        rule.runOnIdle {
            Truth.assertThat(view.isSoftwareKeyboardShown()).isTrue()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun ComposeContentTestRule.setContentForTest(composable: @Composable () -> Unit) {
        setContent {
            view = LocalView.current
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

@RequiresApi(Build.VERSION_CODES.R)
private fun View.waitUntil(timeoutMillis: Long, condition: () -> Boolean) {
    val latch = CountDownLatch(1)
    rootView.setWindowInsetsAnimationCallback(
        InsetAnimationCallback {
            if (condition()) {
                latch.countDown()
            }
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

@Composable
fun BoxWithAnchorAndPopupForTest(
    popupTag: String,
    textFieldTag: String,
    popupProperties: PopupProperties,
) {
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }
    val isTextFieldFocused by interactionSource.collectIsFocusedAsState()

    Box {
        BasicTextField(
            "test",
            {},
            modifier = Modifier.focusRequester(focusRequester).testTag(textFieldTag),
            interactionSource = interactionSource
        )
        if (isTextFieldFocused) {
            Popup(
                alignment = Alignment.Center,
                onDismissRequest = { },
                properties = popupProperties
            ) {
                Box(Modifier.background(Color.Red).size(50.dp).testTag(popupTag))
            }
        }
    }
}