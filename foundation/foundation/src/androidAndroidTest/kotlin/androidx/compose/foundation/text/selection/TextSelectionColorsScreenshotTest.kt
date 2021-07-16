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

package androidx.compose.foundation.text.selection

import android.os.Build
import androidx.compose.foundation.GOLDEN_UI
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.center
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performGesture
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Screenshot test for [TextSelectionColors] used by the selection handle / background.
 *
 * Note: because we cannot screenshot popups, we cannot see the selection handles in the popup,
 * so instead we just draw them manually so we can at least compare the shape and color.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class TextSelectionColorsScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_UI)

    @Test
    fun text_defaultSelectionColors() {
        rule.setContent {
            TextTestContent(textSelectionColors = LocalTextSelectionColors.current)
        }

        rule.onNodeWithText(Text)
            .performGesture {
                longClick(center)
            }

        rule.waitForIdle()

        rule.onNodeWithTag(Tag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "text_defaultSelectionColors")
    }

    @Test
    fun text_customSelectionColors() {
        rule.setContent {
            TextTestContent(
                textSelectionColors = TextSelectionColors(
                    handleColor = Color(0xFFFFB7B2),
                    backgroundColor = Color(0xFFB5EAD7).copy(alpha = 0.4f),
                )
            )
        }

        rule.onNodeWithText(Text)
            .performGesture {
                longClick(center)
            }

        rule.waitForIdle()

        rule.onNodeWithTag(Tag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "text_customSelectionColors")
    }

    @Test
    fun textField_defaultSelectionColors() {
        rule.setContent {
            TextFieldTestContent(textSelectionColors = LocalTextSelectionColors.current)
        }

        // Click once to focus text field
        rule.onNodeWithText(Text)
            .performGesture {
                click()
                longClick()
            }

        rule.waitForIdle()

        rule.onNodeWithTag(Tag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "textField_defaultSelectionColors")
    }

    @Test
    fun textField_customSelectionColors() {
        rule.setContent {
            TextFieldTestContent(
                textSelectionColors = TextSelectionColors(
                    handleColor = Color(0xFFFFB7B2),
                    backgroundColor = Color(0xFFB5EAD7).copy(alpha = 0.4f),
                )
            )
        }

        // Click once to focus text field
        rule.onNodeWithText(Text)
            .performGesture {
                click()
                longClick()
            }

        rule.waitForIdle()

        rule.onNodeWithTag(Tag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "textField_customSelectionColors")
    }
}

@Composable
private fun TextTestContent(textSelectionColors: TextSelectionColors) {
    CompositionLocalProvider(LocalTextSelectionColors provides textSelectionColors) {
        Row(Modifier.testTag(Tag), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            // Manually draw selection handles as we cannot screenshot the ones drawn in the popup
            DefaultSelectionHandle(
                modifier = Modifier,
                isStartHandle = true,
                direction = ResolvedTextDirection.Ltr,
                handlesCrossed = false
            )

            SelectionContainer {
                BasicText(Text)
            }

            DefaultSelectionHandle(
                modifier = Modifier,
                isStartHandle = false,
                direction = ResolvedTextDirection.Ltr,
                handlesCrossed = false
            )
        }
    }
}

@Composable
private fun TextFieldTestContent(textSelectionColors: TextSelectionColors) {
    CompositionLocalProvider(LocalTextSelectionColors provides textSelectionColors) {
        Box(Modifier.testTag(Tag)) {
            BasicTextField(value = TextFieldText, onValueChange = {})
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
