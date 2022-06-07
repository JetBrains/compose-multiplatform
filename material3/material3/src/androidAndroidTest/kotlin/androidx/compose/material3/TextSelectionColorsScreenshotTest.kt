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
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
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
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    @Test
    fun rememberTextSelectionColors() {
        var lightTextSelectionColors: TextSelectionColors? = null
        var darkTextSelectionColors: TextSelectionColors? = null
        var lightPrimary: Color = Color.Unspecified
        var darkPrimary: Color = Color.Unspecified
        rule.setContent {
            val lightColorScheme = lightColorScheme()
            val darkColorScheme = lightColorScheme()
            lightPrimary = lightColorScheme.primary
            darkPrimary = darkColorScheme.primary
            lightTextSelectionColors = rememberTextSelectionColors(lightColorScheme)
            darkTextSelectionColors = rememberTextSelectionColors(darkColorScheme)
        }

        Truth.assertThat(lightTextSelectionColors!!.handleColor).isEqualTo(lightPrimary)
        Truth.assertThat(darkTextSelectionColors!!.handleColor).isEqualTo(darkPrimary)
        Truth.assertThat(lightTextSelectionColors!!.backgroundColor)
            .isEqualTo(lightPrimary.copy(alpha = TextSelectionBackgroundOpacity))
        Truth.assertThat(darkTextSelectionColors!!.backgroundColor)
            .isEqualTo(darkPrimary.copy(alpha = TextSelectionBackgroundOpacity))
    }

    @Test
    fun text_lightThemeSelectionColors() {
        rule.setContent {
            TextTestContent(lightColorScheme())
        }

        rule.onNodeWithText(Text)
            .performTouchInput {
                longClick()
            }

        rule.waitForIdle()

        rule.onNodeWithTag(Tag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "text_lightThemeSelectionColors")
    }

    @Test
    fun text_darkThemeSelectionColors() {
        rule.setContent {
            TextTestContent(darkColorScheme())
        }

        rule.onNodeWithText(Text)
            .performTouchInput {
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
            FilledTextFieldTestContent(lightColorScheme())
        }

        // Click once to focus text field
        rule.onNodeWithText(Text)
            .performTouchInput {
                click()
                longClick()
            }

        rule.waitForIdle()

        rule.onNodeWithTag(Tag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "filledTextField_lightThemeSelectionColors")
    }

    @Test
    fun filledTextField_darkThemeSelectionColors() {
        rule.setContent {
            FilledTextFieldTestContent(darkColorScheme())
        }

        // Click once to focus text field
        rule.onNodeWithText(Text)
            .performTouchInput {
                click()
                longClick()
            }

        rule.waitForIdle()

        rule.onNodeWithTag(Tag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "filledTextField_darkThemeSelectionColors")
    }

    @Test
    fun outlinedTextField_lightThemeSelectionColors() {
        rule.setContent {
            OutlinedTextFieldTestContent(lightColorScheme())
        }

        // Click once to focus text field
        rule.onNodeWithText(Text)
            .performTouchInput {
                click()
                longClick()
            }

        rule.waitForIdle()

        rule.onNodeWithTag(Tag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "outlinedTextField_lightThemeSelectionColors")
    }

    @Test
    fun outlinedTextField_darkThemeSelectionColors() {
        rule.setContent {
            OutlinedTextFieldTestContent(darkColorScheme())
        }

        // Click once to focus text field
        rule.onNodeWithText(Text)
            .performTouchInput {
                click()
                longClick()
            }

        rule.waitForIdle()

        rule.onNodeWithTag(Tag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "outlinedTextField_darkThemeSelectionColors")
    }
}

@Composable
private fun TextTestContent(colorScheme: ColorScheme) {
    MaterialTheme(colorScheme) {
        Surface(Modifier.testTag(Tag)) {
            SelectionContainer {
                Text(Text)
            }
        }
    }
}

@Composable
private fun FilledTextFieldTestContent(colorScheme: ColorScheme) {
    MaterialTheme(colorScheme) {
        Surface(Modifier.testTag(Tag)) {
            TextField(
                value = TextFieldText,
                onValueChange = {},
                modifier = Modifier.requiredWidth(280.dp)
            )
        }
    }
}

@Composable
private fun OutlinedTextFieldTestContent(colorScheme: ColorScheme) {
    MaterialTheme(colorScheme) {
        Surface(Modifier.testTag(Tag)) {
            OutlinedTextField(
                value = TextFieldText,
                onValueChange = {},
                modifier = Modifier.requiredWidth(280.dp)
            )
        }
    }
}

private const val Text = "Selected text"
private val TextFieldText = TextFieldValue(
    text = "Selected text",
    selection = TextRange(0, 8),
    composition = TextRange(0, 8)
)
private const val Tag = "TestTag"