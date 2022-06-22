/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.material3

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterial3Api::class)
@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class TextFieldScreenshotTest {
    private val TextFieldTag = "TextField"
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
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    @Test
    fun textField_withInput() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.semantics(mergeDescendants = true) {}.testTag(TextFieldTag)) {
                val text = "Text"
                TextField(
                    value = TextFieldValue(text = text, selection = TextRange(text.length)),
                    onValueChange = {},
                    label = { Text("Label") },
                    modifier = Modifier.requiredWidth(280.dp)
                )
            }
        }

        assertAgainstGolden("filled_textField_withInput")
    }

    @Test
    fun textField_notFocused() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.semantics(mergeDescendants = true) {}.testTag(TextFieldTag)) {
                TextField(
                    value = "",
                    onValueChange = {},
                    label = { Text("Label") },
                    modifier = Modifier.requiredWidth(280.dp)
                )
            }
        }

        assertAgainstGolden("filled_textField_not_focused")
    }

    @Test
    fun textField_focused() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.semantics(mergeDescendants = true) {}.testTag(TextFieldTag)) {
                TextField(
                    value = "",
                    onValueChange = {},
                    label = { Text("Label") },
                    modifier = Modifier.requiredWidth(280.dp)
                )
            }
        }

        rule.onNodeWithTag(TextFieldTag).focus()

        assertAgainstGolden("filled_textField_focused")
    }

    @Test
    fun textField_focused_rtl() {
        rule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Box(Modifier.semantics(mergeDescendants = true) {}.testTag(TextFieldTag)) {
                    TextField(
                        value = "",
                        onValueChange = {},
                        label = { Text("Label") },
                        modifier = Modifier.requiredWidth(280.dp)
                    )
                }
            }
        }

        rule.onNodeWithTag(TextFieldTag).focus()

        assertAgainstGolden("filled_textField_focused_rtl")
    }

    @Test
    fun textField_error_focused() {
        rule.setMaterialContent(lightColorScheme()) {
            val text = "Input"
            TextField(
                value = TextFieldValue(text = text, selection = TextRange(text.length)),
                onValueChange = {},
                label = { Text("Label") },
                isError = true,
                modifier = Modifier.requiredWidth(280.dp).testTag(TextFieldTag)
            )
        }

        rule.onNodeWithTag(TextFieldTag).focus()

        assertAgainstGolden("filled_textField_focused_errorState")
    }

    @Test
    fun textField_error_notFocused() {
        rule.setMaterialContent(lightColorScheme()) {
            TextField(
                value = "",
                onValueChange = {},
                label = { Text("Label") },
                isError = true,
                modifier = Modifier.requiredWidth(280.dp).testTag(TextFieldTag)
            )
        }

        assertAgainstGolden("filled_textField_notFocused_errorState")
    }

    @Test
    fun textField_textColor_customTextColor() {
        rule.setMaterialContent(lightColorScheme()) {
            val text = "Hello, world!"
            TextField(
                value = TextFieldValue(text = text, selection = TextRange(text.length)),
                onValueChange = {},
                modifier = Modifier.requiredWidth(280.dp).testTag(TextFieldTag),
                colors = TextFieldDefaults.textFieldColors(textColor = Color.Green)
            )
        }

        assertAgainstGolden("filled_textField_textColor_customTextColor")
    }

    @Test
    fun textField_multiLine_withLabel_textAlignedToTop() {
        rule.setMaterialContent(lightColorScheme()) {
            val text = "Text"
            TextField(
                value = TextFieldValue(text = text, selection = TextRange(text.length)),
                onValueChange = {},
                label = { Text("Label") },
                modifier = Modifier.requiredHeight(300.dp)
                    .requiredWidth(280.dp)
                    .testTag(TextFieldTag)
            )
        }

        assertAgainstGolden("filled_textField_multiLine_withLabel_textAlignedToTop")
    }

    @Test
    fun textField_multiLine_withoutLabel_textAlignedToTop() {
        rule.setMaterialContent(lightColorScheme()) {
            val text = "Text"
            TextField(
                value = TextFieldValue(text = text, selection = TextRange(text.length)),
                onValueChange = {},
                modifier = Modifier.requiredHeight(300.dp)
                    .requiredWidth(280.dp)
                    .testTag(TextFieldTag)
            )
        }

        assertAgainstGolden("filled_textField_multiLine_withoutLabel_textAlignedToTop")
    }

    @Test
    fun textField_multiLine_withLabel_placeholderAlignedToTop() {
        rule.setMaterialContent(lightColorScheme()) {
            TextField(
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

        assertAgainstGolden("filled_textField_multiLine_withLabel_placeholderAlignedToTop")
    }

    @Test
    fun textField_multiLine_withoutLabel_placeholderAlignedToTop() {
        rule.setMaterialContent(lightColorScheme()) {
            TextField(
                value = "",
                onValueChange = {},
                placeholder = { Text("placeholder") },
                modifier = Modifier.requiredHeight(300.dp)
                    .requiredWidth(280.dp)
                    .testTag(TextFieldTag)
            )
        }

        rule.onNodeWithTag(TextFieldTag).focus()

        assertAgainstGolden("filled_textField_multiLine_withoutLabel_placeholderAlignedToTop")
    }

    @Test
    fun textField_multiLine_labelAlignedToTop() {
        rule.setMaterialContent(lightColorScheme()) {
            TextField(
                value = "",
                onValueChange = {},
                label = { Text("Label") },
                modifier = Modifier.requiredHeight(300.dp)
                    .requiredWidth(280.dp)
                    .testTag(TextFieldTag)
            )
        }

        assertAgainstGolden("filled_textField_multiLine_labelAlignedToTop")
    }

    @Test
    fun textField_singleLine_withLabel_textAlignedToTop() {
        rule.setMaterialContent(lightColorScheme()) {
            val text = "Text"
            TextField(
                value = TextFieldValue(text = text, selection = TextRange(text.length)),
                onValueChange = {},
                singleLine = true,
                label = { Text("Label") },
                modifier = Modifier.requiredWidth(280.dp).testTag(TextFieldTag)
            )
        }

        assertAgainstGolden("filled_textField_singleLine_withLabel_textAlignedToTop")
    }

    @Test
    fun textField_singleLine_withoutLabel_textCenteredVertically() {
        rule.setMaterialContent(lightColorScheme()) {
            val text = "Text"
            TextField(
                value = TextFieldValue(text = text, selection = TextRange(text.length)),
                onValueChange = {},
                singleLine = true,
                modifier = Modifier.requiredWidth(280.dp).testTag(TextFieldTag)
            )
        }

        assertAgainstGolden("filled_textField_singleLine_withoutLabel_textCenteredVertically")
    }

    @Test
    fun textField_singleLine_withLabel_placeholderAlignedToTop() {
        rule.setMaterialContent(lightColorScheme()) {
            TextField(
                value = "",
                onValueChange = {},
                placeholder = { Text("placeholder") },
                label = { Text("Label") },
                singleLine = true,
                modifier = Modifier.requiredWidth(280.dp).testTag(TextFieldTag)
            )
        }

        rule.onNodeWithTag(TextFieldTag).focus()

        assertAgainstGolden("filled_textField_singleLine_withLabel_placeholderAlignedToTop")
    }

    @Test
    fun textField_singleLine_withoutLabel_placeholderCenteredVertically() {
        rule.setMaterialContent(lightColorScheme()) {
            TextField(
                value = "",
                onValueChange = {},
                placeholder = { Text("placeholder") },
                singleLine = true,
                modifier = Modifier.requiredWidth(280.dp).testTag(TextFieldTag)
            )
        }

        rule.onNodeWithTag(TextFieldTag).focus()

        assertAgainstGolden(
            "filled_textField_singleLine_withoutLabel_placeholderCenteredVertically"
        )
    }

    @Test
    fun textField_singleLine_labelCenteredVertically() {
        rule.setMaterialContent(lightColorScheme()) {
            TextField(
                value = "",
                onValueChange = {},
                label = { Text("Label") },
                modifier = Modifier.requiredWidth(280.dp).testTag(TextFieldTag)
            )
        }

        assertAgainstGolden("filled_textField_singleLine_labelCenteredVetically")
    }

    @Test
    fun textField_disabled() {
        rule.setMaterialContent(lightColorScheme()) {
            TextField(
                value = TextFieldValue("Text"),
                onValueChange = {},
                modifier = Modifier.requiredWidth(280.dp).testTag(TextFieldTag),
                singleLine = true,
                enabled = false
            )
        }

        assertAgainstGolden("textField_disabled")
    }

    @Test
    fun textField_disabled_notFocusable() {
        rule.setMaterialContent(lightColorScheme()) {
            TextField(
                value = TextFieldValue("Text"),
                onValueChange = {},
                singleLine = true,
                modifier = Modifier.requiredWidth(280.dp).testTag(TextFieldTag),
                enabled = false
            )
        }

        rule.onNodeWithTag(TextFieldTag).focus()

        assertAgainstGolden("textField_disabled_notFocusable")
    }

    @Test
    fun textField_disabled_notScrolled() {
        rule.setMaterialContent(lightColorScheme()) {
            TextField(
                value = longText,
                onValueChange = { },
                singleLine = true,
                modifier = Modifier.testTag(TextFieldTag).requiredWidth(300.dp),
                enabled = false
            )
        }

        rule.mainClock.autoAdvance = false

        rule.onNodeWithTag(TextFieldTag).performTouchInput { swipeLeft() }

        // wait for swipe to finish
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(250)

        assertAgainstGolden("textField_disabled_notScrolled")
    }

    @Test
    fun textField_readOnly() {
        rule.setMaterialContent(lightColorScheme()) {
            TextField(
                value = TextFieldValue("Text"),
                onValueChange = {},
                modifier = Modifier.requiredWidth(280.dp).testTag(TextFieldTag),
                enabled = true,
                readOnly = true
            )
        }

        assertAgainstGolden("textField_readOnly")
    }

    @Test
    fun textField_readOnly_focused() {
        rule.setMaterialContent(lightColorScheme()) {
            TextField(
                value = TextFieldValue("Text"),
                onValueChange = {},
                modifier = Modifier.requiredWidth(280.dp).testTag(TextFieldTag),
                enabled = true,
                readOnly = true
            )
        }

        rule.onNodeWithTag(TextFieldTag).focus()

        assertAgainstGolden("textField_readOnly_focused")
    }

    @Test
    fun textField_readOnly_scrolled() {
        rule.setMaterialContent(lightColorScheme()) {
            TextField(
                value = longText,
                onValueChange = { },
                modifier = Modifier.testTag(TextFieldTag).requiredWidth(300.dp),
                singleLine = true,
                enabled = true,
                readOnly = true
            )
        }
        rule.mainClock.autoAdvance = false

        rule.onNodeWithTag(TextFieldTag).performTouchInput { swipeLeft() }

        // wait for swipe to finish
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(250)

        assertAgainstGolden("textField_readOnly_scrolled")
    }

    @Test
    fun textField_textCenterAligned() {
        rule.setMaterialContent(lightColorScheme()) {
            val text = "Hello world"
            TextField(
                value = TextFieldValue(text = text, selection = TextRange(text.length)),
                onValueChange = {},
                modifier = Modifier.width(300.dp).testTag(TextFieldTag),
                textStyle = TextStyle(textAlign = TextAlign.Center),
                singleLine = true
            )
        }

        assertAgainstGolden("textField_textCenterAligned")
    }

    @Test
    fun textField_textAlignedToEnd() {
        rule.setMaterialContent(lightColorScheme()) {
            val text = "Hello world"
            TextField(
                value = TextFieldValue(text = text, selection = TextRange(text.length)),
                onValueChange = {},
                modifier = Modifier.fillMaxWidth().testTag(TextFieldTag),
                textStyle = TextStyle(textAlign = TextAlign.End),
                singleLine = true
            )
        }

        assertAgainstGolden("textField_textAlignedToEnd")
    }

    @Test
    fun textField_leadingTrailingIcons() {
        rule.setMaterialContent(lightColorScheme()) {
            TextField(
                value = "",
                onValueChange = {},
                label = { Text("Label") },
                modifier = Modifier.width(300.dp).testTag(TextFieldTag),
                leadingIcon = { Icon(Icons.Default.Call, null) },
                trailingIcon = { Icon(Icons.Default.Clear, null) }
            )
        }

        assertAgainstGolden("textField_leadingTrailingIcons")
    }

    @Test
    fun textField_leadingTrailingIcons_error() {
        rule.setMaterialContent(lightColorScheme()) {
            TextField(
                value = "",
                onValueChange = {},
                label = { Text("Label") },
                modifier = Modifier.width(300.dp).testTag(TextFieldTag),
                leadingIcon = { Icon(Icons.Default.Call, null) },
                trailingIcon = { Icon(Icons.Default.Clear, null) },
                isError = true
            )
        }

        assertAgainstGolden("textField_leadingTrailingIcons_error")
    }

    @Test
    fun textField_withInput_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(Modifier.semantics(mergeDescendants = true) {}.testTag(TextFieldTag)) {
                val text = "Text"
                TextField(
                    value = TextFieldValue(text = text, selection = TextRange(text.length)),
                    onValueChange = {},
                    label = { Text("Label") },
                    modifier = Modifier.requiredWidth(280.dp)
                )
            }
        }

        assertAgainstGolden("filled_textField_withInput_dark")
    }

    @Test
    fun textField_focused_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(Modifier.semantics(mergeDescendants = true) {}.testTag(TextFieldTag)) {
                TextField(
                    value = "",
                    onValueChange = {},
                    label = { Text("Label") },
                    modifier = Modifier.requiredWidth(280.dp)
                )
            }
        }

        rule.onNodeWithTag(TextFieldTag).focus()

        assertAgainstGolden("filled_textField_focused_dark")
    }

    @Test
    fun textField_error_focused_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            val text = "Input"
            TextField(
                value = TextFieldValue(text = text, selection = TextRange(text.length)),
                onValueChange = {},
                label = { Text("Label") },
                isError = true,
                modifier = Modifier.requiredWidth(280.dp).testTag(TextFieldTag)
            )
        }

        rule.onNodeWithTag(TextFieldTag).focus()

        assertAgainstGolden("filled_textField_focused_errorState_dark")
    }

    @Test
    fun textField_disabled_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            TextField(
                value = TextFieldValue("Text"),
                onValueChange = {},
                modifier = Modifier.requiredWidth(280.dp).testTag(TextFieldTag),
                singleLine = true,
                enabled = false
            )
        }

        assertAgainstGolden("textField_disabled_dark")
    }

    @Test
    fun textField_leadingTrailingIcons_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            TextField(
                value = "",
                onValueChange = {},
                label = { Text("Label") },
                modifier = Modifier.width(300.dp).testTag(TextFieldTag),
                leadingIcon = { Icon(Icons.Default.Call, null) },
                trailingIcon = { Icon(Icons.Default.Clear, null) }
            )
        }

        assertAgainstGolden("textField_leadingTrailingIcons_dark")
    }
    private fun SemanticsNodeInteraction.focus() {
        // split click into (down) and (move, up) to enforce a composition in between
        this.performTouchInput { down(center) }.performTouchInput { move(); up() }
    }

    private fun assertAgainstGolden(goldenIdentifier: String) {
        rule.onNodeWithTag(TextFieldTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenIdentifier)
    }
}