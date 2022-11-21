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

package androidx.compose.material.pullrefresh

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.testutils.assertPixelColor
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterialApi::class)
class PullRefreshIndicatorTransformTest {

    @get:Rule
    val rule = createComposeRule()

    // Convert from floats to DP to avoid rounding issues later

    private val IndicatorSize get() = with(rule.density) { 200f.toDp() }
    // Make the box large enough so that when the indicator is offset when not shown, it is still
    // offset to within the bounds of the containing box
    private val ContainingBoxSize = with(rule.density) { 800f.toDp() }
    private val BoxTag = "Box"

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun indicatorClippedWhenNotDisplayed() {
        rule.setContent {
            val state = rememberPullRefreshState(refreshing = false, {})
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .wrapContentSize(Alignment.Center)
                    // Set a larger size so that when offset the indicator will still appear
                    .size(ContainingBoxSize)
                    .testTag(BoxTag),
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier
                    .pullRefreshIndicatorTransform(state)
                    .size(IndicatorSize)
                    .background(Color.Black)
                )
            }
        }
        // The indicator should be fully clipped and invisible
        rule.onNodeWithTag(BoxTag).captureToImage().assertPixels { Color.White }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun indicatorPartiallyClippedWhenPartiallyDisplayed() {
        lateinit var state: PullRefreshState
        rule.setContent {
            // Pull down by 100 pixels (the actual position delta is half of this because the state
            // applies a multiplier)
            state = rememberPullRefreshState(refreshing = false, onRefresh = {}).apply {
                onPull(100f)
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .wrapContentSize(Alignment.Center)
                    // Set a larger size so that when offset the indicator will still appear
                    .size(ContainingBoxSize)
                    .testTag(BoxTag),
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier
                    .pullRefreshIndicatorTransform(state)
                    .size(IndicatorSize)
                    .background(Color.Black)
                )
            }
        }
        // The indicator should be partially clipped
        rule.onNodeWithTag(BoxTag).captureToImage().run {
            val indicatorStart = with(rule.density) { width / 2 - IndicatorSize.toPx() / 2 }.toInt()
            val indicatorXRange = with(rule.density) {
                indicatorStart until (indicatorStart + IndicatorSize.toPx()).toInt()
            }

            val indicatorTop = with(rule.density) { height / 2 - IndicatorSize.toPx() / 2 }.toInt()
            val indicatorYRange = indicatorTop until indicatorTop + state.position.toInt()

            val pixel = toPixelMap()
            for (x in 0 until width) {
                for (y in 0 until height) {
                    val expectedColor = if (x in indicatorXRange && y in indicatorYRange) {
                        Color.Black
                    } else {
                        Color.White
                    }
                    pixel.assertPixelColor(expectedColor, x, y)
                }
            }
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun indicatorNotClippedWhenFullyDisplayed() {
        lateinit var state: PullRefreshState
        rule.setContent {
            // Set refreshing and set the refreshing offset to match the indicator size -
            // this means that the indicator will start to show at an offset of 0 from its normal
            // layout position, since by default it is negatively offset by its height
            state = rememberPullRefreshState(
                refreshing = true,
                onRefresh = {},
                refreshingOffset = IndicatorSize
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .wrapContentSize(Alignment.Center)
                    // Set a larger size so that when offset the indicator will still appear
                    .size(ContainingBoxSize)
                    .testTag(BoxTag),
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier
                    .pullRefreshIndicatorTransform(state)
                    .size(IndicatorSize)
                    .background(Color.Black)
                )
            }
        }
        // The indicator should be fully visible
        rule.onNodeWithTag(BoxTag).captureToImage().run {
            val indicatorStart = with(rule.density) { width / 2 - IndicatorSize.toPx() / 2 }.toInt()
            val indicatorXRange = with(rule.density) {
                indicatorStart until (indicatorStart + IndicatorSize.toPx()).toInt()
            }

            val indicatorTop = with(rule.density) { height / 2 - IndicatorSize.toPx() / 2 }.toInt()
            val indicatorYRange = with(rule.density) {
                indicatorTop until (indicatorTop + IndicatorSize.toPx()).toInt()
            }

            val pixel = toPixelMap()
            for (x in 0 until width) {
                for (y in 0 until height) {
                    val expectedColor = if (x in indicatorXRange && y in indicatorYRange) {
                        Color.Black
                    } else {
                        Color.White
                    }
                    pixel.assertPixelColor(expectedColor, x, y)
                }
            }
        }
    }
}