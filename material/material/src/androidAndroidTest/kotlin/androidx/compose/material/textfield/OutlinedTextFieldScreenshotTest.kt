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

package androidx.compose.material.textfield

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.GOLDEN_MATERIAL
import androidx.compose.material.LocalContentColor
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.setMaterialContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.center
import androidx.compose.ui.test.down
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.move
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performGesture
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.up
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.FlakyTest
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class OutlinedTextFieldScreenshotTest {
    private val TextFieldTag = "OutlinedTextField"

    private val longText = TextFieldValue(
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do " +
            "eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam," +
            " quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. " +
            "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu " +
            "fugiat nulla pariatur."
    )

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL)

    @Test
    fun outlinedTextField_withInput() {
        rule.setMaterialContent {
            Box(Modifier.semantics(mergeDescendants = true) {}.testTag(TextFieldTag)) {
                OutlinedTextField(
                    value = "Text",
                    onValueChange = {},
                    label = { Text("Label") },
                    modifier = Modifier.requiredWidth(280.dp)
                )
            }
        }

        assertAgainstGolden("outlined_textField_withInput")
    }

    @Test
    fun outlinedTextField_notFocused() {
        rule.setMaterialContent {
            Box(Modifier.semantics(mergeDescendants = true) {}.testTag(TextFieldTag)) {
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    label = { Text("Label") },
                    modifier = Modifier.requiredWidth(280.dp)
                )
            }
        }

        assertAgainstGolden("outlined_textField_not_focused")
    }

    @Test
    fun outlinedTextField_focused() {
        rule.setMaterialContent {
            Box(Modifier.semantics(mergeDescendants = true) {}.testTag(TextFieldTag)) {
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    label = { Text("Label") },
                    modifier = Modifier.requiredWidth(280.dp)
                )
            }
        }

        rule.onNodeWithTag(TextFieldTag).focus()

        assertAgainstGolden("outlined_textField_focused")
    }

    @Test
    fun outlinedTextField_focused_rtl() {
        rule.setMaterialContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Box(Modifier.semantics(mergeDescendants = true) {}.testTag(TextFieldTag)) {
                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        label = { Text("Label") },
                        modifier = Modifier.requiredWidth(280.dp)
                    )
                }
            }
        }

        rule.onNodeWithTag(TextFieldTag).focus()

        assertAgainstGolden("outlined_textField_focused_rtl")
    }

    @Test
    fun outlinedTextField_error_focused() {
        rule.setMaterialContent {
            Box(Modifier.semantics(mergeDescendants = true) {}.testTag(TextFieldTag)) {
                OutlinedTextField(
                    value = "Input",
                    onValueChange = {},
                    label = { Text("Label") },
                    isError = true,
                    modifier = Modifier.requiredWidth(280.dp)
                )
            }
        }

        rule.onNodeWithTag(TextFieldTag).focus()

        assertAgainstGolden("outlined_textField_focused_errorState")
    }

    @Test
    fun outlinedTextField_error_notFocused() {
        rule.setMaterialContent {
            Box(Modifier.semantics(mergeDescendants = true) {}.testTag(TextFieldTag)) {
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    label = { Text("Label") },
                    isError = true,
                    modifier = Modifier.requiredWidth(280.dp)
                )
            }
        }

        assertAgainstGolden("outlined_textField_notFocused_errorState")
    }

    @Test
    fun outlinedTextField_textColor_fallbackToContentColor() {
        rule.setMaterialContent {
            CompositionLocalProvider(LocalContentColor provides Color.Magenta) {
                OutlinedTextField(
                    value = "Hello, world!",
                    onValueChange = {},
                    modifier = Modifier.testTag(TextFieldTag).requiredWidth(280.dp)
                )
            }
        }

        assertAgainstGolden("outlined_textField_textColor_defaultContentColor")
    }

    @Test
    fun outlinedTextField_multiLine_withLabel_textAlignedToTop() {
        rule.setMaterialContent {
            OutlinedTextField(
                value = "Text",
                onValueChange = {},
                label = { Text("Label") },
                modifier = Modifier.requiredHeight(300.dp)
                    .requiredWidth(280.dp)
                    .testTag(TextFieldTag)
            )
        }

        assertAgainstGolden("outlined_textField_multiLine_withLabel_textAlignedToTop")
    }

    @Test
    fun outlinedTextField_multiLine_withoutLabel_textAlignedToTop() {
        rule.setMaterialContent {
            OutlinedTextField(
                value = "Text",
                onValueChange = {},
                modifier = Modifier.requiredHeight(300.dp)
                    .requiredWidth(280.dp)
                    .testTag(TextFieldTag)
            )
        }

        assertAgainstGolden("outlined_textField_multiLine_withoutLabel_textAlignedToTop")
    }

    @Test
    fun outlinedTextField_multiLine_withLabel_placeholderAlignedToTop() {
        rule.setMaterialContent {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                label = { Text("Label") },
                placeholder = { Text("placeholder") },
                modifier = Modifier.requiredHeight(300.dp)
                    .requiredWidth(280.dp)
                    .testTag(TextFieldTag)
            )
        }

        rule.onNodeWithTag(TextFieldTag).focus()

        assertAgainstGolden("outlined_textField_multiLine_withLabel_placeholderAlignedToTop")
    }

    @Test
    fun outlinedTextField_multiLine_withoutLabel_placeholderAlignedToTop() {
        rule.setMaterialContent {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                placeholder = { Text("placeholder") },
                modifier = Modifier.requiredHeight(300.dp)
                    .requiredWidth(280.dp)
                    .testTag(TextFieldTag)
            )
        }

        rule.onNodeWithTag(TextFieldTag).focus()

        assertAgainstGolden("outlined_textField_multiLine_withoutLabel_placeholderAlignedToTop")
    }

    @Test
    fun outlinedTextField_multiLine_labelAlignedToTop() {
        rule.setMaterialContent {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                label = { Text("Label") },
                modifier = Modifier.requiredHeight(300.dp)
                    .requiredWidth(280.dp)
                    .testTag(TextFieldTag)
            )
        }

        assertAgainstGolden("outlined_textField_multiLine_labelAlignedToTop")
    }

    @Test
    fun outlinedTextField_singleLine_withLabel_textAlignedToTop() {
        rule.setMaterialContent {
            OutlinedTextField(
                value = "Text",
                onValueChange = {},
                singleLine = true,
                label = { Text("Label") },
                modifier = Modifier.testTag(TextFieldTag).requiredWidth(280.dp)
            )
        }

        assertAgainstGolden("outlined_textField_singleLine_withLabel_textAlignedToTop")
    }

    @Test
    fun outlinedTextField_singleLine_withoutLabel_textCenteredVertically() {
        rule.setMaterialContent {
            OutlinedTextField(
                value = "Text",
                onValueChange = {},
                singleLine = true,
                modifier = Modifier.testTag(TextFieldTag).requiredWidth(280.dp)
            )
        }

        assertAgainstGolden("outlined_textField_singleLine_withoutLabel_textCenteredVertically")
    }

    @Test
    fun outlinedTextField_singleLine_withLabel_placeholderAlignedToTop() {
        rule.setMaterialContent {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                placeholder = { Text("placeholder") },
                label = { Text("Label") },
                singleLine = true,
                modifier = Modifier.testTag(TextFieldTag).requiredWidth(280.dp)
            )
        }

        rule.onNodeWithTag(TextFieldTag).focus()

        assertAgainstGolden("outlined_textField_singleLine_withLabel_placeholderAlignedToTop")
    }

    @Test
    fun outlinedTextField_singleLine_withoutLabel_placeholderCenteredVertically() {
        rule.setMaterialContent {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                placeholder = { Text("placeholder") },
                singleLine = true,
                modifier = Modifier.testTag(TextFieldTag).requiredWidth(280.dp)
            )
        }

        rule.onNodeWithTag(TextFieldTag).focus()

        assertAgainstGolden(
            "outlined_textField_singleLine_withoutLabel_placeholderCenteredVertically"
        )
    }

    @Test
    fun outlinedTextField_singleLine_labelCenteredVetically() {
        rule.setMaterialContent {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                label = { Text("Label") },
                modifier = Modifier.testTag(TextFieldTag).requiredWidth(280.dp)
            )
        }

        assertAgainstGolden("outlined_textField_singleLine_labelCenteredVetically")
    }

    @Test
    fun outlinedTextField_disabled() {
        rule.setMaterialContent {
            Box(Modifier.semantics(mergeDescendants = true) {}.testTag(TextFieldTag)) {
                OutlinedTextField(
                    value = TextFieldValue("Text"),
                    onValueChange = {},
                    singleLine = true,
                    enabled = false,
                    modifier = Modifier.requiredWidth(280.dp)
                )
            }
        }

        assertAgainstGolden("outlinedTextField_disabled")
    }

    @Test
    fun outlinedTextField_disabled_notFocusable() {
        rule.setMaterialContent {
            Box(Modifier.semantics(mergeDescendants = true) {}.testTag(TextFieldTag)) {
                OutlinedTextField(
                    value = TextFieldValue("Text"),
                    onValueChange = {},
                    singleLine = true,
                    enabled = false,
                    modifier = Modifier.requiredWidth(280.dp)
                )
            }
        }

        rule.onNodeWithTag(TextFieldTag).focus()

        assertAgainstGolden("outlinedTextField_disabled_notFocusable")
    }

    @Test
    fun outlinedTextField_disabled_notScrolled() {
        rule.setMaterialContent {
            Box(Modifier.semantics(mergeDescendants = true) {}.testTag(TextFieldTag)) {
                OutlinedTextField(
                    value = longText,
                    onValueChange = { },
                    singleLine = true,
                    modifier = Modifier.requiredWidth(300.dp),
                    enabled = false
                )
            }
        }

        rule.mainClock.autoAdvance = false

        rule.onNodeWithTag(TextFieldTag).performGesture { swipeLeft() }

        // wait for swipe to finish
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(250)

        assertAgainstGolden("outlinedTextField_disabled_notScrolled")
    }

    @Test
    fun outlinedTextField_readOnly() {
        rule.setMaterialContent {
            OutlinedTextField(
                value = TextFieldValue("Text"),
                onValueChange = {},
                modifier = Modifier.testTag(TextFieldTag).requiredWidth(280.dp),
                enabled = true,
                readOnly = true
            )
        }

        assertAgainstGolden("outlinedTextField_readOnly")
    }

    @Test
    fun outlinedTextField_readOnly_focused() {
        rule.setMaterialContent {
            OutlinedTextField(
                value = TextFieldValue("Text"),
                onValueChange = {},
                modifier = Modifier.testTag(TextFieldTag).requiredWidth(280.dp),
                enabled = true,
                readOnly = true
            )
        }

        rule.onNodeWithTag(TextFieldTag).focus()

        assertAgainstGolden("outlinedTextField_readOnly_focused")
    }

    @FlakyTest(bugId = 178510985)
    @Test
    fun outlinedTextField_readOnly_scrolled() {
        rule.setMaterialContent {
            OutlinedTextField(
                value = longText,
                onValueChange = { },
                modifier = Modifier.testTag(TextFieldTag).requiredWidth(300.dp),
                singleLine = true,
                enabled = true,
                readOnly = true
            )
        }

        rule.mainClock.autoAdvance = false

        rule.onNodeWithTag(TextFieldTag).performGesture { swipeLeft() }

        // wait for swipe to finish
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(250)

        assertAgainstGolden("outlinedTextField_readOnly_scrolled")
    }

    @Test
    fun outlinedTextField_textCenterAligned() {
        rule.setMaterialContent {
            OutlinedTextField(
                value = "Hello world",
                onValueChange = {},
                modifier = Modifier.width(300.dp).testTag(TextFieldTag),
                textStyle = TextStyle(textAlign = TextAlign.Center),
                singleLine = true
            )
        }

        assertAgainstGolden("outlinedTextField_textCenterAligned")
    }

    @Test
    fun outlinedTextField_textAlignedToEnd() {
        rule.setMaterialContent {
            OutlinedTextField(
                value = "Hello world",
                onValueChange = {},
                modifier = Modifier.fillMaxWidth().testTag(TextFieldTag),
                textStyle = TextStyle(textAlign = TextAlign.End),
                singleLine = true
            )
        }

        assertAgainstGolden("outlinedTextField_textAlignedToEnd")
    }

    private fun SemanticsNodeInteraction.focus() {
        // split click into (down) and (move, up) to enforce a composition in between
        this.performGesture { down(center) }.performGesture { move(); up() }
    }

    private fun assertAgainstGolden(goldenIdentifier: String) {
        rule.onNodeWithTag(TextFieldTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenIdentifier)
    }
}