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

import androidx.compose.foundation.Text
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Providers
import androidx.compose.runtime.ambientOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.test.espresso.Espresso
import androidx.test.filters.FlakyTest
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.UiDevice
import androidx.ui.test.assertIsDisplayed
import androidx.ui.test.createComposeRule
import androidx.ui.test.onNodeWithText
import androidx.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

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
                    Text(defaultText)
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
                    Text(
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
                    Text(defaultText)
                }
            }
        }

        rule.onNodeWithText(defaultText).assertIsDisplayed()

        // Click outside the dialog to dismiss it
        val outsideX = 0
        val outsideY = rule.displaySize.height / 2
        UiDevice.getInstance(getInstrumentation()).click(outsideX, outsideY)

        rule.onNodeWithText(defaultText).assertDoesNotExist()
    }

    @Test
    fun dialogTest_isNotDismissed_whenNotSpecified() {
        rule.setContent {
            val showDialog = remember { mutableStateOf(true) }

            if (showDialog.value) {
                Dialog(onDismissRequest = {}) {
                    Text(defaultText)
                }
            }
        }

        rule.onNodeWithText(defaultText).assertIsDisplayed()

        // Click outside the dialog to try to dismiss it
        val outsideX = 0
        val outsideY = rule.displaySize.height / 2
        UiDevice.getInstance(getInstrumentation()).click(outsideX, outsideY)

        // The Dialog should still be visible
        rule.onNodeWithText(defaultText).assertIsDisplayed()
    }

    @Test
    fun dialogTest_isDismissed_whenSpecified_backButtonPressed() {
        rule.setContent {
            val showDialog = remember { mutableStateOf(true) }

            if (showDialog.value) {
                Dialog(
                    onDismissRequest = {
                        showDialog.value = false
                    }
                ) {
                    Text(defaultText)
                }
            }
        }

        rule.onNodeWithText(defaultText).assertIsDisplayed()

        // Click the back button to dismiss the Dialog
        Espresso.pressBack()

        rule.onNodeWithText(defaultText).assertDoesNotExist()
    }

    // TODO(pavlis): Espresso loses focus on the dialog after back press. That makes the
    // subsequent query to fails.
    @Ignore
    @Test
    fun dialogTest_isNotDismissed_whenNotSpecified_backButtonPressed() {
        rule.setContent {
            val showDialog = remember { mutableStateOf(true) }

            if (showDialog.value) {
                Dialog(onDismissRequest = {}) {
                    Text(defaultText)
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