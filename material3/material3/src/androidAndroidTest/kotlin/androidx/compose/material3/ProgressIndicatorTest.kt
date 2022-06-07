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
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.testutils.assertPixelColor
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ProgressIndicatorTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun determinateLinearProgressIndicator_Progress() {
        val tag = "linear"
        val progress = mutableStateOf(0f)

        rule.setMaterialContent(lightColorScheme()) {
            LinearProgressIndicator(modifier = Modifier.testTag(tag), progress = progress.value)
        }

        rule.onNodeWithTag(tag)
            .assertIsDisplayed()
            .assertRangeInfoEquals(ProgressBarRangeInfo(0f, 0f..1f))

        rule.runOnUiThread {
            progress.value = 0.5f
        }

        rule.onNodeWithTag(tag)
            .assertIsDisplayed()
            .assertRangeInfoEquals(ProgressBarRangeInfo(0.5f, 0f..1f))
    }

    @Test
    fun determinateLinearProgressIndicator_Size() {
        rule
            .setMaterialContentForSizeAssertions {
                LinearProgressIndicator(progress = 0f)
            }
            .assertWidthIsEqualTo(LinearIndicatorWidth)
            .assertHeightIsEqualTo(LinearIndicatorHeight)
    }

    @Test
    fun indeterminateLinearProgressIndicator_Progress() {
        val tag = "linear"

        rule.mainClock.autoAdvance = false
        rule.setMaterialContent(lightColorScheme()) {
            LinearProgressIndicator(modifier = Modifier.testTag(tag))
        }

        rule.mainClock.advanceTimeByFrame() // Kick off the animation

        rule.onNodeWithTag(tag)
            .assertRangeInfoEquals(ProgressBarRangeInfo.Indeterminate)
    }

    @Test
    fun indeterminateLinearProgressIndicator_Size() {
        rule.mainClock.autoAdvance = false
        val contentToTest = rule
            .setMaterialContentForSizeAssertions {
                LinearProgressIndicator()
            }

        rule.mainClock.advanceTimeByFrame() // Kick off the animation

        contentToTest
            .assertWidthIsEqualTo(LinearIndicatorWidth)
            .assertHeightIsEqualTo(LinearIndicatorHeight)
    }

    @Test
    fun determinateCircularProgressIndicator_Progress() {
        val tag = "circular"
        val progress = mutableStateOf(0f)

        rule.setMaterialContent(lightColorScheme()) {
            CircularProgressIndicator(
                modifier = Modifier.testTag(tag),
                progress = progress.value
            )
        }

        rule.onNodeWithTag(tag)
            .assertIsDisplayed()
            .assertRangeInfoEquals(ProgressBarRangeInfo(0f, 0f..1f))

        rule.runOnUiThread {
            progress.value = 0.5f
        }

        rule.onNodeWithTag(tag)
            .assertIsDisplayed()
            .assertRangeInfoEquals(ProgressBarRangeInfo(0.5f, 0f..1f))
    }

    @Test
    fun determinateCircularProgressIndicator_Size() {
        rule
            .setMaterialContentForSizeAssertions {
                CircularProgressIndicator(progress = 0f)
            }
            .assertIsSquareWithSize(CircularIndicatorDiameter)
    }

    @Test
    fun indeterminateCircularProgressIndicator_progress() {
        val tag = "circular"

        rule.mainClock.autoAdvance = false
        rule.setMaterialContent(lightColorScheme()) {
            CircularProgressIndicator(modifier = Modifier.testTag(tag))
        }

        rule.mainClock.advanceTimeByFrame() // Kick off the animation

        rule.onNodeWithTag(tag)
            .assertRangeInfoEquals(ProgressBarRangeInfo.Indeterminate)
    }

    @Test
    fun indeterminateCircularProgressIndicator_Size() {
        rule.mainClock.autoAdvance = false
        val contentToTest = rule
            .setMaterialContentForSizeAssertions {
                CircularProgressIndicator()
            }

        rule.mainClock.advanceTimeByFrame() // Kick off the animation

        contentToTest
            .assertIsSquareWithSize(CircularIndicatorDiameter)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun determinateLinearProgressIndicator_sizeModifier() {
        val expectedWidth = 100.dp
        val expectedHeight = 10.dp
        val expectedSize = with(rule.density) {
            IntSize(expectedWidth.roundToPx(), expectedHeight.roundToPx())
        }
        val tag = "progress_indicator"
        rule.setContent {
            LinearProgressIndicator(
                modifier = Modifier.testTag(tag).size(expectedWidth, expectedHeight),
                progress = 1f,
                color = Color.Blue
            )
        }

        rule.onNodeWithTag(tag)
            .captureToImage()
            .assertPixels(expectedSize = expectedSize) {
                Color.Blue
            }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun indeterminateLinearProgressIndicator_sizeModifier() {
        val expectedWidth = 100.dp
        val expectedHeight = 10.dp
        val expectedSize = with(rule.density) {
            IntSize(expectedWidth.roundToPx(), expectedHeight.roundToPx())
        }
        rule.mainClock.autoAdvance = false
        val tag = "progress_indicator"
        rule.setContent {

            LinearProgressIndicator(
                modifier = Modifier.testTag(tag).size(expectedWidth, expectedHeight),
                color = Color.Blue
            )
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithTag(tag)
            .captureToImage()
            .toPixelMap()
            .let {
                assertEquals(expectedSize.width, it.width)
                assertEquals(expectedSize.height, it.height)
                // Assert on the first pixel column, to make sure that the progress indicator draws
                // to the expect height.
                // We can't assert width as the width dynamically changes during the animation
                for (i in 0 until it.height) {
                    it.assertPixelColor(Color.Blue, 0, i)
                }
            }
    }
}
