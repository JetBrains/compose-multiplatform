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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.CoreTextField
import androidx.compose.foundation.text.KeyboardHelper
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Dialog
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
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
}
