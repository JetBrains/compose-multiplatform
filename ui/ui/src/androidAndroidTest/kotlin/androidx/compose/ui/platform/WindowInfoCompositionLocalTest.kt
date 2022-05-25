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

package androidx.compose.ui.platform

import android.view.KeyEvent
import android.view.View
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.setFocusableContent
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS

@MediumTest
@RunWith(AndroidJUnit4::class)
class WindowInfoCompositionLocalTest {
    @get:Rule
    val rule = createComposeRule()

    @Ignore("Flaky Test b/173088588")
    @Test
    fun windowIsFocused_onLaunch() {
        // Arrange.
        lateinit var windowInfo: WindowInfo
        val windowFocusGain = CountDownLatch(1)
        rule.setContent {
            BasicText("Main Window")
            windowInfo = LocalWindowInfo.current
            @Suppress("DEPRECATION")
            WindowFocusObserver { if (it) windowFocusGain.countDown() }
        }

        // Act.
        rule.waitForIdle()

        // Assert.
        windowFocusGain.await(5, SECONDS)
        assertThat(windowInfo.isWindowFocused).isTrue()
    }

    @Test
    fun mainWindowIsNotFocused_whenPopupIsVisible() {
        // Arrange.
        lateinit var mainWindowInfo: WindowInfo
        lateinit var popupWindowInfo: WindowInfo
        val mainWindowFocusLoss = CountDownLatch(1)
        val popupFocusGain = CountDownLatch(1)
        val showPopup = mutableStateOf(false)
        rule.setContent {
            BasicText("Main Window")
            mainWindowInfo = LocalWindowInfo.current
            WindowFocusObserver { if (!it) mainWindowFocusLoss.countDown() }
            if (showPopup.value) {
                Popup(
                    properties = PopupProperties(focusable = true),
                    onDismissRequest = {
                        showPopup.value = false
                    }
                ) {
                    BasicText("Popup Window")
                    popupWindowInfo = LocalWindowInfo.current
                    WindowFocusObserver { if (it) popupFocusGain.countDown() }
                }
            }
        }

        // Act.
        rule.runOnIdle { showPopup.value = true }

        // Assert.
        rule.waitForIdle()
        assertThat(mainWindowFocusLoss.await(5, SECONDS)).isTrue()
        assertThat(popupFocusGain.await(5, SECONDS)).isTrue()
        assertThat(mainWindowInfo.isWindowFocused).isFalse()
        assertThat(popupWindowInfo.isWindowFocused).isTrue()
    }

    @Test
    fun windowIsFocused_whenPopupIsDismissed() {
        // Arrange.
        lateinit var mainWindowInfo: WindowInfo
        var mainWindowFocusGain = CountDownLatch(1)
        val popupFocusGain = CountDownLatch(1)
        val showPopup = mutableStateOf(false)
        rule.setContent {
            BasicText(text = "Main Window")
            mainWindowInfo = LocalWindowInfo.current
            WindowFocusObserver { if (it) mainWindowFocusGain.countDown() }
            if (showPopup.value) {
                Popup(
                    properties = PopupProperties(focusable = true),
                    onDismissRequest = { showPopup.value = false }
                ) {
                    BasicText(text = "Popup Window")
                    WindowFocusObserver { if (it) popupFocusGain.countDown() }
                }
            }
        }
        rule.runOnIdle { showPopup.value = true }
        rule.waitForIdle()
        assertThat(popupFocusGain.await(5, SECONDS)).isTrue()
        mainWindowFocusGain = CountDownLatch(1)

        // Act.
        rule.runOnIdle { showPopup.value = false }

        // Assert.
        rule.waitForIdle()
        assertThat(mainWindowFocusGain.await(5, SECONDS)).isTrue()
        assertThat(mainWindowInfo.isWindowFocused).isTrue()
    }

    @Test
    fun mainWindowIsNotFocused_whenDialogIsVisible() {
        // Arrange.
        lateinit var mainWindowInfo: WindowInfo
        lateinit var dialogWindowInfo: WindowInfo
        val mainWindowFocusLoss = CountDownLatch(1)
        val dialogFocusGain = CountDownLatch(1)
        val showDialog = mutableStateOf(false)
        rule.setContent {
            BasicText("Main Window")
            mainWindowInfo = LocalWindowInfo.current
            WindowFocusObserver { if (!it) mainWindowFocusLoss.countDown() }
            if (showDialog.value) {
                Dialog(onDismissRequest = { showDialog.value = false }) {
                    BasicText("Popup Window")
                    dialogWindowInfo = LocalWindowInfo.current
                    WindowFocusObserver { if (it) dialogFocusGain.countDown() }
                }
            }
        }

        // Act.
        rule.runOnIdle { showDialog.value = true }

        // Assert.
        rule.waitForIdle()
        assertThat(mainWindowFocusLoss.await(5, SECONDS)).isTrue()
        assertThat(dialogFocusGain.await(5, SECONDS)).isTrue()
        assertThat(mainWindowInfo.isWindowFocused).isFalse()
        assertThat(dialogWindowInfo.isWindowFocused).isTrue()
    }

    @Test
    fun windowIsFocused_whenDialogIsDismissed() {
        // Arrange.
        lateinit var mainWindowInfo: WindowInfo
        var mainWindowFocusGain = CountDownLatch(1)
        val dialogFocusGain = CountDownLatch(1)
        val showDialog = mutableStateOf(false)
        rule.setContent {
            BasicText(text = "Main Window")
            mainWindowInfo = LocalWindowInfo.current
            WindowFocusObserver { if (it) mainWindowFocusGain.countDown() }
            if (showDialog.value) {
                Dialog(onDismissRequest = { showDialog.value = false }) {
                    BasicText(text = "Popup Window")
                    WindowFocusObserver { if (it) dialogFocusGain.countDown() }
                }
            }
        }
        rule.runOnIdle { showDialog.value = true }
        rule.waitForIdle()
        assertThat(dialogFocusGain.await(5, SECONDS)).isTrue()
        mainWindowFocusGain = CountDownLatch(1)

        // Act.
        rule.runOnIdle { showDialog.value = false }

        // Assert.
        rule.waitForIdle()
        assertThat(mainWindowFocusGain.await(5, SECONDS)).isTrue()
        assertThat(mainWindowInfo.isWindowFocused).isTrue()
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun windowInfo_providesKeyModifiers() {
        lateinit var mainWindowInfo: WindowInfo
        lateinit var ownerView: View

        var keyModifiers = PointerKeyboardModifiers(0)

        rule.setFocusableContent {
            ownerView = LocalView.current
            mainWindowInfo = LocalWindowInfo.current

            keyModifiers = mainWindowInfo.keyboardModifiers
        }

        assertThat(keyModifiers.packedValue).isEqualTo(0)

        (rule as AndroidComposeTestRule<*, *>).runOnUiThread {
            ownerView.requestFocus()
        }

        rule.runOnIdle {
            val ctrlPressed = KeyEvent(
                0, 0, KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_CTRL_LEFT, 0, KeyEvent.META_CTRL_ON
            )
            ownerView.dispatchKeyEvent(ctrlPressed)
        }

        rule.waitForIdle()
        assertThat(keyModifiers.packedValue).isEqualTo(KeyEvent.META_CTRL_ON)

        rule.runOnIdle {
            val altAndCtrlPressed = KeyEvent(
                0, 0, KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_ALT_LEFT, 0,
                KeyEvent.META_CTRL_ON or KeyEvent.META_ALT_ON
            )
            ownerView.dispatchKeyEvent(altAndCtrlPressed)
        }

        rule.waitForIdle()
        assertThat(keyModifiers.packedValue).isEqualTo(
            KeyEvent.META_CTRL_ON or KeyEvent.META_ALT_ON
        )

        rule.runOnIdle {
            val altUnpressed = KeyEvent(
                0, 0, KeyEvent.ACTION_UP,
                KeyEvent.KEYCODE_ALT_LEFT, 0,
                KeyEvent.META_CTRL_ON
            )
            ownerView.dispatchKeyEvent(altUnpressed)
        }

        rule.waitForIdle()
        assertThat(keyModifiers.packedValue).isEqualTo(KeyEvent.META_CTRL_ON)

        rule.runOnIdle {
            val ctrlUnpressed = KeyEvent(
                0, 0, KeyEvent.ACTION_UP,
                KeyEvent.KEYCODE_CTRL_LEFT, 0, 0
            )
            ownerView.dispatchKeyEvent(ctrlUnpressed)
        }

        rule.waitForIdle()
        assertThat(keyModifiers.packedValue).isEqualTo(0)
    }
}
