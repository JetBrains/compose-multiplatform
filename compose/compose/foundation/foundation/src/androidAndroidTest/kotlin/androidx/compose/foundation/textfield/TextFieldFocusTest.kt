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

import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyEvent
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.CoreTextField
import androidx.compose.foundation.text.KeyboardHelper
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.NativeKeyEvent
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyPress
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Dialog
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class TextFieldFocusTest {
    @get:Rule
    val rule = createComposeRule()

    private val keyboardHelper = KeyboardHelper(rule)

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

        rule.setContent {
            testDataList = listOf(
                FocusTestData(FocusRequester()),
                FocusTestData(FocusRequester()),
                FocusTestData(FocusRequester())
            )

            TextFieldApp(testDataList)
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
    fun noCrashWhenSwitchingBetweenEnabledFocusedAndDisabledTextField() {
        val enabled = mutableStateOf(true)
        var focused = false
        val tag = "textField"
        rule.setContent {
            CoreTextField(
                value = TextFieldValue(),
                onValueChange = {},
                enabled = enabled.value,
                modifier = Modifier
                    .testTag(tag)
                    .onFocusChanged {
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

    @Test
    fun wholeDecorationBox_isBroughtIntoView_whenFocused() {
        var outerCoordinates: LayoutCoordinates? = null
        var innerCoordinates: LayoutCoordinates? = null
        val focusRequester = FocusRequester()
        rule.setContent {
            Column(
                Modifier
                    .height(100.dp)
                    .onPlaced { outerCoordinates = it }
                    .verticalScroll(rememberScrollState())
            ) {
                // Place the text field way out of the viewport.
                Spacer(Modifier.height(10000.dp))
                CoreTextField(
                    value = TextFieldValue(),
                    onValueChange = {},
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .onPlaced { innerCoordinates = it },
                    decorationBox = { innerTextField ->
                        Box(Modifier.padding(20.dp)) {
                            innerTextField()
                        }
                    }
                )
            }
        }

        rule.runOnIdle {
            // Text field should start completely clipped.
            assertThat(
                outerCoordinates!!.localBoundingBoxOf(
                    innerCoordinates!!,
                    clipBounds = true
                ).size
            ).isEqualTo(Size.Zero)

            focusRequester.requestFocus()
        }

        rule.runOnIdle {
            // Text field should be completely visible.
            assertThat(
                outerCoordinates!!.localBoundingBoxOf(
                    innerCoordinates!!,
                    clipBounds = true
                ).size
            ).isEqualTo(innerCoordinates!!.size.toSize())
        }
    }

    @Test
    fun keyboardIsShown_forFieldInActivity_whenFocusRequestedImmediately_fromLaunchedEffect() {
        keyboardIsShown_whenFocusRequestedImmediately_fromEffect(
            runEffect = {
                LaunchedEffect(Unit) {
                    it()
                }
            }
        )
    }

    @Test
    fun keyboardIsShown_forFieldInActivity_whenFocusRequestedImmediately_fromDisposableEffect() {
        keyboardIsShown_whenFocusRequestedImmediately_fromEffect(
            runEffect = {
                DisposableEffect(Unit) {
                    it()
                    onDispose {}
                }
            }
        )
    }

    // TODO(b/229378542) We can't accurately detect IME visibility from dialogs before API 30 so
    //  this test can't assert.
    @SdkSuppress(minSdkVersion = 30)
    @Test
    fun keyboardIsShown_forFieldInDialog_whenFocusRequestedImmediately_fromLaunchedEffect() {
        keyboardIsShown_whenFocusRequestedImmediately_fromEffect(
            runEffect = {
                LaunchedEffect(Unit) {
                    it()
                }
            },
            wrapContent = {
                Dialog(onDismissRequest = {}, content = it)
            }
        )
    }

    // TODO(b/229378542) We can't accurately detect IME visibility from dialogs before API 30 so
    //  this test can't assert.
    @SdkSuppress(minSdkVersion = 30)
    @Test
    fun keyboardIsShown_forFieldInDialog_whenFocusRequestedImmediately_fromDisposableEffect() {
        keyboardIsShown_whenFocusRequestedImmediately_fromEffect(
            runEffect = {
                DisposableEffect(Unit) {
                    it()
                    onDispose {}
                }
            },
            wrapContent = {
                Dialog(onDismissRequest = {}, content = it)
            }
        )
    }

    private fun keyboardIsShown_whenFocusRequestedImmediately_fromEffect(
        runEffect: @Composable (body: () -> Unit) -> Unit,
        wrapContent: @Composable (@Composable () -> Unit) -> Unit = { it() }
    ) {
        val focusRequester = FocusRequester()
        val keyboardHelper = KeyboardHelper(rule)

        rule.setContent {
            wrapContent {
                keyboardHelper.initialize()

                runEffect {
                    assertThat(keyboardHelper.isSoftwareKeyboardShown()).isFalse()
                    focusRequester.requestFocus()
                }

                BasicTextField(
                    value = "",
                    onValueChange = {},
                    modifier = Modifier.focusRequester(focusRequester)
                )
            }
        }

        keyboardHelper.waitForKeyboardVisibility(visible = true)

        // Ensure the keyboard doesn't leak in to the next test. Can't do this at the start of the
        // test since the KeyboardHelper won't be initialized until composition runs, and this test
        // is checking behavior that all happens on the first frame.
        keyboardHelper.hideKeyboard()
        keyboardHelper.waitForKeyboardVisibility(visible = false)
    }

    @Test
    fun basicTextField_checkFocusNavigation_onDPadLeft() {
        setupAndEnableBasicTextField()
        inputSingleLineTextInBasicTextField()

        // Dismiss keyboard on back press
        keyPressOnVirtualKeyboard(NativeKeyEvent.KEYCODE_BACK)
        rule.waitForIdle()

        // Move focus to the focusable element on left
        keyPressOnPhysicalKeyboard(rule, NativeKeyEvent.KEYCODE_DPAD_LEFT)

        // Check if the element to the left of text field gains focus
        rule.onNodeWithTag("test-button-left").assertIsFocused()
    }

    @Test
    fun basicTextField_checkFocusNavigation_onDPadRight() {
        setupAndEnableBasicTextField()
        inputSingleLineTextInBasicTextField()

        // Dismiss keyboard on back press
        keyPressOnVirtualKeyboard(NativeKeyEvent.KEYCODE_BACK)
        rule.waitForIdle()

        // Move focus to the focusable element on right
        keyPressOnPhysicalKeyboard(rule, NativeKeyEvent.KEYCODE_DPAD_RIGHT)

        // Check if the element to the right of text field gains focus
        rule.onNodeWithTag("test-button-right").assertIsFocused()
    }

    @Test
    fun basicTextField_checkFocusNavigation_onDPadUp() {
        setupAndEnableBasicTextField()
        inputMultilineTextInBasicTextField()

        // Dismiss keyboard on back press
        keyPressOnVirtualKeyboard(NativeKeyEvent.KEYCODE_BACK)
        rule.waitForIdle()

        // Move focus to the focusable element on top
        keyPressOnPhysicalKeyboard(rule, NativeKeyEvent.KEYCODE_DPAD_UP)

        // Check if the element on the top of text field gains focus
        rule.onNodeWithTag("test-button-top").assertIsFocused()
    }

    @Test
    fun basicTextField_checkFocusNavigation_onDPadDown() {
        setupAndEnableBasicTextField()
        inputMultilineTextInBasicTextField()

        // Dismiss keyboard on back press
        keyPressOnVirtualKeyboard(NativeKeyEvent.KEYCODE_BACK)
        rule.waitForIdle()

        // Move focus to the focusable element on bottom
        keyPressOnPhysicalKeyboard(rule, NativeKeyEvent.KEYCODE_DPAD_DOWN)

        // Check if the element to the bottom of text field gains focus
        rule.onNodeWithTag("test-button-bottom").assertIsFocused()
    }

    @Ignore // b/264919150
    @Test
    fun basicTextField_checkKeyboardShown_onDPadCenter() {
        setupAndEnableBasicTextField()
        inputSingleLineTextInBasicTextField()

        // Dismiss keyboard on back press
        keyPressOnVirtualKeyboard(NativeKeyEvent.KEYCODE_BACK)
        keyboardHelper.waitForKeyboardVisibility(false)
        rule.runOnIdle {
            assertThat(keyboardHelper.isSoftwareKeyboardShown()).isFalse()
        }

        // Check if keyboard is enabled on Dpad center key press
        keyPressOnPhysicalKeyboard(rule, NativeKeyEvent.KEYCODE_DPAD_CENTER)
        keyboardHelper.waitForKeyboardVisibility(true)
        rule.runOnIdle {
            assertThat(keyboardHelper.isSoftwareKeyboardShown()).isTrue()
        }
    }

    @Test
    fun basicTextField_handlesInvalidDevice() {
        setupAndEnableBasicTextField()
        inputSingleLineTextInBasicTextField()

        // -2 shouldn't be a valid device – we verify this below by asserting the device in the
        // event is actually null.
        val invalidDeviceId = -2
        val keyCode = NativeKeyEvent.KEYCODE_DPAD_CENTER
        val keyEventDown = KeyEvent(
            SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
            KeyEvent.ACTION_DOWN, keyCode, 0, 0, invalidDeviceId, 0
        )
        assertThat(keyEventDown.device).isNull()
        rule.onRoot().performKeyPress(androidx.compose.ui.input.key.KeyEvent(keyEventDown))
        rule.waitForIdle()
        val keyEventUp = KeyEvent(
            SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
            KeyEvent.ACTION_UP, keyCode, 0, 0, invalidDeviceId, 0
        )
        rule.onRoot().performKeyPress(androidx.compose.ui.input.key.KeyEvent(keyEventUp))
        rule.waitForIdle()
    }

    private fun setupAndEnableBasicTextField() {
        setupContent()

        rule.onNodeWithTag("test-text-field-1").assertIsFocused()
    }

    private fun inputSingleLineTextInBasicTextField() {
        // Input "abc"
        keyPressOnVirtualKeyboard(NativeKeyEvent.KEYCODE_A)
        rule.waitForIdle()
        keyPressOnVirtualKeyboard(NativeKeyEvent.KEYCODE_B)
        rule.waitForIdle()
        keyPressOnVirtualKeyboard(NativeKeyEvent.KEYCODE_C)
        rule.waitForIdle()
    }

    private fun inputMultilineTextInBasicTextField() {
        // Input "a\nb\nc"
        keyPressOnVirtualKeyboard(NativeKeyEvent.KEYCODE_A)
        rule.waitForIdle()
        keyPressOnVirtualKeyboard(NativeKeyEvent.KEYCODE_ENTER)
        rule.waitForIdle()
        keyPressOnVirtualKeyboard(NativeKeyEvent.KEYCODE_B)
        rule.waitForIdle()
        keyPressOnVirtualKeyboard(NativeKeyEvent.KEYCODE_ENTER)
        rule.waitForIdle()
        keyPressOnVirtualKeyboard(NativeKeyEvent.KEYCODE_C)
        rule.waitForIdle()
    }

    private fun setupContent() {
        rule.setContent {
            keyboardHelper.initialize()
            Column() {
                Row(horizontalArrangement = Arrangement.Center) {
                    TestFocusableElement(id = "top")
                }
                Row() {
                    TestFocusableElement(id = "left")
                    TestBasicTextField(id = "1", requestFocus = true)
                    TestFocusableElement(id = "right")
                }
                Row(horizontalArrangement = Arrangement.Center) {
                    TestFocusableElement(id = "bottom")
                }
            }
        }
        rule.waitForIdle()
    }

    @Composable
    private fun TestFocusableElement(id: String) {
        var isFocused by remember {
            mutableStateOf(false)
        }
        BasicText(
            text = "test-button-$id",
            modifier = Modifier
                .testTag("test-button-$id")
                .padding(10.dp)
                .onFocusChanged {
                    isFocused = it.hasFocus
                }
                .focusable()
                .border(2.dp, if (isFocused) Color.Green else Color.Cyan)
        )
    }

    @Composable
    private fun TestBasicTextField(
        id: String,
        requestFocus: Boolean = false
    ) {
        var textInput by remember {
            mutableStateOf("")
        }
        var isFocused by remember {
            mutableStateOf(false)
        }
        val focusRequester = remember {
            FocusRequester()
        }
        val modifier = if (requestFocus) Modifier.focusRequester(focusRequester) else Modifier

        BasicTextField(
            value = textInput,
            onValueChange = {
                textInput = it
            },
            modifier = modifier
                .testTag("test-text-field-$id")
                .padding(10.dp)
                .onFocusChanged {
                    isFocused = it.isFocused || it.hasFocus
                }
                .border(2.dp, if (isFocused) Color.Red else Color.Transparent)
        )

        LaunchedEffect(requestFocus, focusRequester) {
            if (requestFocus) focusRequester.requestFocus()
        }
    }

    // Triggers a key press on the root node from a non-virtual device
    private fun keyPressOnPhysicalKeyboard(
        rule: ComposeContentTestRule,
        keyCode: Int,
        count: Int = 1
    ) {
        repeat(count) {
            val deviceId = InputDevice.getDeviceIds().first { id ->
                InputDevice.getDevice(id)?.isVirtual?.not() ?: false
            }
            val keyEventDown = KeyEvent(
                SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
                KeyEvent.ACTION_DOWN, keyCode, 0, 0, deviceId, 0
            )
            rule.onRoot().performKeyPress(androidx.compose.ui.input.key.KeyEvent(keyEventDown))
            rule.waitForIdle()
            val keyEventUp = KeyEvent(
                SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
                KeyEvent.ACTION_UP, keyCode, 0, 0, deviceId, 0
            )
            rule.onRoot().performKeyPress(androidx.compose.ui.input.key.KeyEvent(keyEventUp))
        }
    }

    // Triggers a key press on the virtual keyboard
    private fun keyPressOnVirtualKeyboard(keyCode: Int, count: Int = 1) {
        repeat(count) {
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(keyCode)
        }
    }
}
