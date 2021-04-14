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

package androidx.compose.foundation.text

import android.os.Build
import androidx.compose.foundation.GOLDEN_UI
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@OptIn(ExperimentalTestApi::class)
class BasicTextScreenshotTest {
    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_UI)

    private val textTag = "text"

    @Test
    fun multiStyleText_setFontWeight() {
        rule.setContent {
            BasicText(
                text = buildAnnotatedString {
                    append("Hello ")
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append("World")
                    pop()
                },
                modifier = Modifier.testTag(textTag),
                style = TextStyle(
                    fontSize = 24.sp,
                    fontStyle = FontStyle.Italic,
                    fontFamily = FontFamily.Monospace
                )
            )
        }
        rule.onNodeWithTag(textTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "multiStyleText_setFontWeight")
    }

    @Test
    fun multiStyleText_setFontFamily() {
        rule.setContent {
            BasicText(
                text = buildAnnotatedString {
                    append("Hello ")
                    pushStyle(SpanStyle(fontFamily = FontFamily.SansSerif))
                    append("World")
                    pop()
                },
                modifier = Modifier.testTag(textTag),
                style = TextStyle(
                    fontSize = 24.sp,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            )
        }
        rule.onNodeWithTag(textTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "multiStyleText_setFontFamily")
    }

    @Test
    fun multiStyleText_setFontStyle() {
        rule.setContent {
            BasicText(
                text = buildAnnotatedString {
                    append("Hello ")
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append("World")
                    pop()
                },
                modifier = Modifier.testTag(textTag),
                style = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            )
        }
        rule.onNodeWithTag(textTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "multiStyleText_setFontStyle")
    }
}