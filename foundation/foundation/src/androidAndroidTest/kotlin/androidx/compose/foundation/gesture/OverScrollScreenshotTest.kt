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

package androidx.compose.foundation.gesture

import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.GOLDEN_UI
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.LocalOverScrollConfiguration
import androidx.compose.foundation.gestures.OverScrollConfiguration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.testutils.AnimationDurationScaleRule
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@OptIn(ExperimentalTestApi::class)
class OverScrollScreenshotTest {
    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_UI)

    @get:Rule
    val animationScaleRule: AnimationDurationScaleRule =
        AnimationDurationScaleRule.createForAllTests(1f)

    val overScrollTag = "overScrollTag"

    @Test
    @Ignore("b/197325932 no animations in screenshot tests")
    fun overscroll_dragTop() {
        animationScaleRule.setAnimationDurationScale(1f)
        rule.setContent {
            VerticalScrollable()
        }

        rule.onNodeWithTag(overScrollTag)
            .performTouchInput {
                down(Offset(centerX + width / 2 - 10, centerY))
                moveBy(Offset(0f, 500f))
                repeat(5) {
                    moveBy(Offset(0f, 200f))
                }
            }

        rule.onNodeWithTag(overScrollTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "overscroll_top_origin")
    }

    @Test
    @Ignore("b/197325932 no animations in screenshot tests")
    fun overscroll_dragBottom() {
        animationScaleRule.setAnimationDurationScale(1f)
        rule.setContent {
            VerticalScrollable()
        }

        rule.onNodeWithTag(overScrollTag)
            .performTouchInput {
                down(Offset(centerX + width / 2 - 10, centerY))
                moveBy(Offset(0f, -500f))
                repeat(5) {
                    moveBy(Offset(0f, -200f))
                }
            }

        rule.onNodeWithTag(overScrollTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "overscroll_bottom_origin")
    }

    @Test
    @Ignore("b/197325932 no animations in screenshot tests")
    fun overscroll_dragLeft() {
        animationScaleRule.setAnimationDurationScale(1f)
        rule.setContent {
            HorizontalScrollable()
        }

        rule.onNodeWithTag(overScrollTag)
            .performTouchInput {
                down(Offset(centerX, centerY + height / 2 - 10))
                moveBy(Offset(500f, 0f))
                repeat(5) {
                    moveBy(Offset(200f, 0f))
                }
            }

        rule.onNodeWithTag(overScrollTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "overscroll_left_origin")
    }

    @Test
    @Ignore("b/197325932 no animations in screenshot tests")
    fun overscroll_dragRight() {
        animationScaleRule.setAnimationDurationScale(1f)
        rule.setContent {
            HorizontalScrollable()
        }

        rule.onNodeWithTag(overScrollTag)
            .performTouchInput {
                down(Offset(centerX, centerY + height / 2 - 10))
                moveBy(Offset(-500f, 0f))
                repeat(5) {
                    moveBy(Offset(-200f, 0f))
                }
            }

        rule.onNodeWithTag(overScrollTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "overscroll_right_origin")
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun VerticalScrollable() {
        CompositionLocalProvider(
            LocalOverScrollConfiguration provides OverScrollConfiguration(
                glowColor = Color.Red,
                drawPadding = PaddingValues(10.dp)
            )
        ) {
            Box(
                Modifier
                    .wrapContentSize(Alignment.TopStart)
            ) {
                LazyColumn(
                    Modifier
                        .border(5.dp, Color.Black)
                        .size(width = 400.dp, height = 200.dp)
                        .testTag(overScrollTag)
                ) {
                    items(7) {
                        Box(Modifier.size(400.dp, 50.dp))
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun HorizontalScrollable() {
        CompositionLocalProvider(
            LocalOverScrollConfiguration provides OverScrollConfiguration(
                glowColor = Color.Red,
                drawPadding = PaddingValues(10.dp)
            )
        ) {
            Box(
                Modifier
                    .wrapContentSize(Alignment.TopStart)
            ) {
                LazyRow(
                    Modifier
                        .border(5.dp, Color.Black)
                        .size(width = 200.dp, height = 400.dp)
                        .testTag(overScrollTag)
                ) {
                    items(7) {
                        Box(Modifier.size(50.dp, 400.dp))
                    }
                }
            }
        }
    }
}