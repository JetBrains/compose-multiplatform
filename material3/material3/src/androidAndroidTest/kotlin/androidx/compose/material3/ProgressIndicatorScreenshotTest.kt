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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
class ProgressIndicatorScreenshotTest {
    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    private val wrap = Modifier.wrapContentSize(Alignment.TopStart)
    private val wrapperTestTag = "progressIndicatorWrapper"

    @Test
    fun linearProgressIndicator_lightTheme_definite() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                LinearProgressIndicator(progress = 0.5f)
            }
        }
        assertIndicatorAgainstGolden("linearProgressIndicator_lightTheme_definite")
    }

    @Test
    fun linearProgressIndicator_lightTheme_indefinite() {
        rule.mainClock.autoAdvance = false
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                LinearProgressIndicator()
            }
        }
        rule.mainClock.advanceTimeBy(500)
        assertIndicatorAgainstGolden("linearProgressIndicator_lightTheme_indefinite")
    }

    @Test
    fun linearProgressIndicator_darkTheme_definite() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                LinearProgressIndicator(progress = 0.5f)
            }
        }
        assertIndicatorAgainstGolden("linearProgressIndicator_darkTheme_definite")
    }

    @Test
    fun circularProgressIndicator_lightTheme_definite() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                CircularProgressIndicator(progress = 0.5f)
            }
        }
        assertIndicatorAgainstGolden("circularProgressIndicator_lightTheme_definite")
    }

    @Test
    fun circularProgressIndicator_lightTheme_indefinite() {
        rule.mainClock.autoAdvance = false
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                CircularProgressIndicator()
            }
        }
        rule.mainClock.advanceTimeBy(500)
        assertIndicatorAgainstGolden("circularProgressIndicator_lightTheme_indefinite")
    }

    @Test
    fun circularProgressIndicator_darkTheme_definite() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                CircularProgressIndicator(progress = 0.5f)
            }
        }
        assertIndicatorAgainstGolden("circularProgressIndicator_darkTheme_definite")
    }

    private fun assertIndicatorAgainstGolden(goldenName: String) {
        rule.onNodeWithTag(wrapperTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenName)
    }
}