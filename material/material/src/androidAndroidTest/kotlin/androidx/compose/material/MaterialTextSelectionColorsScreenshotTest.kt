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

package androidx.compose.material

import android.os.Build
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.height
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performGesture
import androidx.compose.ui.test.width
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.FlakyTest
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Screenshot test for the SelectionColors provided by [MaterialTheme] and used by the selection
 * handle / background.
 *
 * Note: because we cannot screenshot popups, we cannot see the selection handles in the popup,
 * so we can only test the background color here.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class MaterialTextSelectionColorsScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL)

    @Test
    fun rememberTextSelectionColors() {
        var lightTextSelectionColors: TextSelectionColors? = null
        var darkTextSelectionColors: TextSelectionColors? = null
        var lightPrimary: Color = Color.Unspecified
        var darkPrimary: Color = Color.Unspecified
        rule.setContent {
            val lightColors = lightColors()
            val darkColors = darkColors()
            lightPrimary = lightColors.primary
            darkPrimary = darkColors.primary
            lightTextSelectionColors = rememberTextSelectionColors(lightColors)
            darkTextSelectionColors = rememberTextSelectionColors(darkColors)
        }

        Truth.assertThat(lightTextSelectionColors!!.handleColor).isEqualTo(lightPrimary)
        Truth.assertThat(darkTextSelectionColors!!.handleColor).isEqualTo(darkPrimary)
        Truth.assertThat(lightTextSelectionColors!!.backgroundColor)
            .isEqualTo(lightPrimary.copy(alpha = 0.325f))
        Truth.assertThat(darkTextSelectionColors!!.backgroundColor)
            .isEqualTo(darkPrimary.copy(alpha = 0.375f))
    }

    @FlakyTest(bugId = 179770443)
    @Test
    fun text_lightThemeSelectionColors() {
        rule.setContent {
            TextTestContent(lightColors())
        }

        rule.onNodeWithText(Text)
            .performGesture {
                longClick()
            }

        rule.waitForIdle()

        rule.onNodeWithTag(Tag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "text_lightThemeSelectionColors")
    }

    @FlakyTest(bugId = 179770443)
    @Test
    fun text_darkThemeSelectionColors() {
        rule.setContent {
            TextTestContent(darkColors())
        }

        rule.onNodeWithText(Text)
            .performGesture {
                longClick()
            }

        rule.waitForIdle()

        rule.onNodeWithTag(Tag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "text_darkThemeSelectionColors")
    }

    @Test
    fun filledTextField_lightThemeSelectionColors() {
        rule.setContent {
            FilledTextFieldTestContent(lightColors())
        }

        // Click once to focus text field
        rule.onNodeWithText(Text)
            .performGesture {
                click()
                longClick()
            }

        rule.waitForIdle()

        // Long click to start text selection
        rule.onNodeWithText(Text)
            .performGesture {
                longClick(Offset(width / 5f, height / 2f))
            }

        rule.waitForIdle()

        rule.onNodeWithTag(Tag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "filledTextField_lightThemeSelectionColors")
    }

    @Test
    fun filledTextField_darkThemeSelectionColors() {
        rule.setContent {
            FilledTextFieldTestContent(darkColors())
        }

        // Click once to focus text field
        rule.onNodeWithText(Text)
            .performGesture {
                click()
                longClick()
            }

        rule.waitForIdle()

        // Long click to start text selection
        rule.onNodeWithText(Text)
            .performGesture {
                longClick(Offset(width / 5f, height / 2f))
            }

        rule.waitForIdle()

        rule.onNodeWithTag(Tag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "filledTextField_darkThemeSelectionColors")
    }

    @FlakyTest(bugId = 190120675)
    @Test
    fun outlinedTextField_lightThemeSelectionColors() {
        rule.setContent {
            OutlinedTextFieldTestContent(lightColors())
        }

        // Click once to focus text field
        rule.onNodeWithText(Text)
            .performGesture {
                click()
                longClick()
            }

        rule.waitForIdle()

        // Long click to start text selection
        rule.onNodeWithText(Text)
            .performGesture {
                longClick(Offset(width / 5f, height / 2f))
            }

        rule.waitForIdle()

        rule.onNodeWithTag(Tag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "outlinedTextField_lightThemeSelectionColors")
    }

    @Test
    fun outlinedTextField_darkThemeSelectionColors() {
        rule.setContent {
            OutlinedTextFieldTestContent(darkColors())
        }

        // Click once to focus text field
        rule.onNodeWithText(Text)
            .performGesture {
                click()
                longClick()
            }

        rule.waitForIdle()

        // Long click to start text selection
        rule.onNodeWithText(Text)
            .performGesture {
                longClick(Offset(width / 5f, height / 2f))
            }

        rule.waitForIdle()

        rule.onNodeWithTag(Tag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "outlinedTextField_darkThemeSelectionColors")
    }
}

@Composable
private fun TextTestContent(colors: Colors) {
    MaterialTheme(colors) {
        Surface(Modifier.testTag(Tag)) {
            SelectionContainer {
                Text(Text)
            }
        }
    }
}

@Composable
private fun FilledTextFieldTestContent(colors: Colors) {
    MaterialTheme(colors) {
        Surface(Modifier.testTag(Tag)) {
            TextField(value = Text, onValueChange = {}, modifier = Modifier.requiredWidth(280.dp))
        }
    }
}

@Composable
private fun OutlinedTextFieldTestContent(colors: Colors) {
    MaterialTheme(colors) {
        Surface(Modifier.testTag(Tag)) {
            OutlinedTextField(
                value = Text,
                onValueChange = {},
                modifier = Modifier.requiredWidth(280.dp)
            )
        }
    }
}

private const val Text = "Selected text"
private const val Tag = "TestTag"