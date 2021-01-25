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
package androidx.compose.ui.window

import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Providers
import androidx.compose.runtime.ambientOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.height
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.FlakyTest
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.UiDevice
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class DialogUiTest {
    @get:Rule
    val rule = createComposeRule()

    private val defaultText = "dialogText"

    @Test
    fun dialogTest_isShowingContent() {
        rule.setContent {
            val showDialog = remember { mutableStateOf(true) }

            if (showDialog.value) {
                Dialog(onDismissRequest = {}) {
                    BasicText(defaultText)
                }
            }
        }

        rule.onNodeWithText(defaultText).assertIsDisplayed()
    }

    @Test
    @FlakyTest(bugId = 159364185)
    fun dialogTest_isNotDismissed_whenClicked() {
        val textBeforeClick = "textBeforeClick"
        val textAfterClick = "textAfterClick"

        rule.setContent {
            val showDialog = remember { mutableStateOf(true) }
            val text = remember { mutableStateOf(textBeforeClick) }

            if (showDialog.value) {
                Dialog(
                    onDismissRequest = {
                        showDialog.value = false
                    }
                ) {
                    BasicText(
                        text = text.value,
                        modifier = Modifier.clickable {
                            text.value = textAfterClick
                        }
                    )
                }
            }
        }

        rule.onNodeWithText(textBeforeClick)
            .assertIsDisplayed()
            // Click inside the dialog
            .performClick()

        // Check that the Clickable was pressed and that the Dialog is still visible, but with
        // the new text
        rule.onNodeWithText(textBeforeClick).assertDoesNotExist()
        rule.onNodeWithText(textAfterClick).assertIsDisplayed()
    }

    @Test
    fun dialogTest_isDismissed_whenSpecified() {
        rule.setContent {
            val showDialog = remember { mutableStateOf(true) }

            if (showDialog.value) {
                Dialog(
                    onDismissRequest = {
                        showDialog.value = false
                    }
                ) {
                    BasicText(defaultText)
                }
            }
        }

        rule.onNodeWithText(defaultText).assertIsDisplayed()

        // Click outside the dialog to dismiss it
        val outsideX = 0
        val outsideY = with(rule.density) {
            rule.onAllNodes(isRoot()).onFirst().getUnclippedBoundsInRoot().height.toIntPx() / 2
        }
        UiDevice.getInstance(getInstrumentation()).click(outsideX, outsideY)

        rule.onNodeWithText(defaultText).assertDoesNotExist()
    }

    @Test
    fun dialogTest_isNotDismissed_whenNotSpecified() {
        rule.setContent {
            val showDialog = remember { mutableStateOf(true) }

            if (showDialog.value) {
                Dialog(onDismissRequest = {}) {
                    BasicText(defaultText)
                }
            }
        }

        rule.onNodeWithText(defaultText).assertIsDisplayed()

        // Click outside the dialog to try to dismiss it
        val outsideX = 0
        val outsideY = with(rule.density) {
            rule.onAllNodes(isRoot()).onFirst().getUnclippedBoundsInRoot().height.toIntPx() / 2
        }
        UiDevice.getInstance(getInstrumentation()).click(outsideX, outsideY)

        // The Dialog should still be visible
        rule.onNodeWithText(defaultText).assertIsDisplayed()
    }

    @Test
    fun dialogTest_isNotDismissed_whenDismissOnClickOutsideIsFalse() {
        rule.setContent {
            val showDialog = remember { mutableStateOf(true) }

            if (showDialog.value) {
                Dialog(
                    onDismissRequest = {
                        showDialog.value = false
                    },
                    properties = AndroidDialogProperties(dismissOnClickOutside = false)
                ) {
                    BasicText(defaultText)
                }
            }
        }

        rule.onNodeWithText(defaultText).assertIsDisplayed()

        // Click outside the dialog to try to dismiss it
        val outsideX = 0
        val outsideY = with(rule.density) {
            rule.onAllNodes(isRoot()).onFirst().getUnclippedBoundsInRoot().height.toIntPx() / 2
        }
        UiDevice.getInstance(getInstrumentation()).click(outsideX, outsideY)

        // The Dialog should still be visible
        rule.onNodeWithText(defaultText).assertIsDisplayed()
    }

    @FlakyTest(bugId = 159364185)
    fun dialogTest_isDismissed_whenSpecified_backButtonPressed() {
        rule.setContent {
            val showDialog = remember { mutableStateOf(true) }

            if (showDialog.value) {
                Dialog(
                    onDismissRequest = {
                        showDialog.value = false
                    }
                ) {
                    BasicText(defaultText)
                }
            }
        }

        rule.onNodeWithText(defaultText).assertIsDisplayed()

        // Click the back button to dismiss the Dialog
        Espresso.pressBack()

        rule.onNodeWithText(defaultText).assertDoesNotExist()
    }

    @FlakyTest(bugId = 159364185)
    fun dialogTest_isNotDismissed_whenNotSpecified_backButtonPressed() {
        rule.setContent {
            val showDialog = remember { mutableStateOf(true) }

            if (showDialog.value) {
                Dialog(onDismissRequest = {}) {
                    BasicText(defaultText)
                }
            }
        }

        rule.onNodeWithText(defaultText).assertIsDisplayed()

        // Click the back button to try to dismiss the dialog
        Espresso.pressBack()

        // The Dialog should still be visible
        rule.onNodeWithText(defaultText).assertIsDisplayed()
    }

    @FlakyTest(bugId = 159364185)
    fun dialogTest_isNotDismissed_whenDismissOnBackPressIsFalse() {
        rule.setContent {
            val showDialog = remember { mutableStateOf(true) }

            if (showDialog.value) {
                Dialog(
                    onDismissRequest = {
                        showDialog.value = false
                    },
                    properties = AndroidDialogProperties(dismissOnBackPress = false)
                ) {
                    BasicText(defaultText)
                }
            }
        }

        rule.onNodeWithText(defaultText).assertIsDisplayed()

        // Click the back button to try to dismiss the dialog
        Espresso.pressBack()

        // The Dialog should still be visible
        rule.onNodeWithText(defaultText).assertIsDisplayed()
    }

    @Test
    fun dialog_preservesAmbients() {
        val ambient = ambientOf<Float>()
        var value = 0f
        rule.setContent {
            Providers(ambient provides 1f) {
                Dialog(onDismissRequest = {}) {
                    value = ambient.current
                }
            }
        }
        rule.runOnIdle {
            assertEquals(1f, value)
        }
    }
}