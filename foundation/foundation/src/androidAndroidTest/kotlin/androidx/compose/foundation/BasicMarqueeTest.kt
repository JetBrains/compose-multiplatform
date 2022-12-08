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

package androidx.compose.foundation

import android.os.Build
import androidx.compose.foundation.MarqueeAnimationMode.Companion.Immediately
import androidx.compose.foundation.MarqueeAnimationMode.Companion.WhileFocused
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.AnimationDurationScaleRule
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFoundationApi::class)
@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class BasicMarqueeTest {
    private companion object {
        private const val FrameDelay = 16

        private val Color1 = Color.Green
        private val Color2 = Color.Red
        private val BackgroundColor = Color.White
    }

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val animationScaleRule: AnimationDurationScaleRule =
        AnimationDurationScaleRule.createForAllTests(1f)

    /**
     * Converts pxPerFrame to dps per second. The frame delay is 16ms, which means there are
     * actually slightly more than 60 frames in a second (62.5).
     */
    private val Int.pxPerFrame: Dp
        get() = toDp() * (1000 / FrameDelay.toFloat())

    private fun Int.toDp() = with(rule.density) { this@toDp.toDp() }

    @Test
    fun initialState() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            Row(
                Modifier
                    .width(100.toDp())
                    .basicMarquee()
            ) {
                Box(
                    Modifier
                        .size(100.toDp())
                        .background(Color1)
                )
                Box(
                    Modifier
                        .size(100.toDp())
                        .background(Color2)
                )
            }
        }

        rule.onRoot().captureToImage()
            .assertPixels { Color1 }
    }

    @Test
    fun animationDisabled() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            Row(
                Modifier
                    .width(100.toDp())
                    .basicMarquee(
                        iterations = 0,
                        animationMode = Immediately,
                        initialDelayMillis = 0
                    )
            ) {
                Box(
                    Modifier
                        .size(100.toDp())
                        .background(Color1)
                )
                Box(
                    Modifier
                        .size(100.toDp())
                        .background(Color2)
                )
            }
        }

        // Color2 should never show up.
        repeat(5) {
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
            rule.onRoot().captureToImage()
                .assertPixels { Color1 }
        }
    }

    @Test
    fun animates_singleIteration_noSpace() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            Row(
                Modifier
                    .width(100.toDp())
                    .background(BackgroundColor)
                    .basicMarquee(
                        iterations = 1,
                        velocity = 10.pxPerFrame,
                        animationMode = Immediately,
                        initialDelayMillis = 0,
                        spacing = MarqueeSpacing(0.toDp())
                    )
            ) {
                Box(
                    Modifier
                        .size(100.toDp())
                        .background(Color1)
                )
                Box(
                    Modifier
                        .size(100.toDp())
                        .background(Color2)
                )
            }
        }

        rule.onRoot().captureToImage()
            .assertPixels(expectedSize = IntSize(100, 100)) { Color1 }

        // First stage of animation: show all the content.
        repeat(30) { frameNum ->
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
            val image = rule.onRoot().captureToImage()
            val edge1 = image.findFirstColorEdge(Color1, Color2)
            val edge2 = image.findFirstColorEdge(Color2, Color1)
            val expectedEdge1 = 100 - (frameNum * 10)
            val expectedEdge2 = 100 - ((frameNum - 10) * 10)

            when {
                frameNum == 0 -> {
                    assertThat(edge1).isEqualTo(-1)
                    assertThat(edge2).isEqualTo(-1)
                }

                frameNum < 10 -> {
                    assertThat(edge1).isEqualTo(expectedEdge1)
                    assertThat(edge2).isEqualTo(-1)
                }

                frameNum == 10 -> {
                    assertThat(edge1).isEqualTo(-1)
                    assertThat(edge2).isEqualTo(-1)
                }

                frameNum < 20 -> {
                    assertThat(edge1).isEqualTo(-1)
                    assertThat(edge2).isEqualTo(expectedEdge2)
                }

                else -> {
                    // Nothing should happen after the animation finishes.
                    assertThat(edge1).isEqualTo(-1)
                    assertThat(edge2).isEqualTo(-1)
                }
            }
        }
    }

    @Test
    fun animates_singleIteration_fixedSpace() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            Row(
                Modifier
                    .width(100.toDp())
                    .background(BackgroundColor)
                    .basicMarquee(
                        iterations = 1,
                        velocity = 10.pxPerFrame,
                        animationMode = Immediately,
                        initialDelayMillis = 0,
                        spacing = MarqueeSpacing(10.toDp())
                    )
            ) {
                Box(
                    Modifier
                        .size(100.toDp())
                        .background(Color1)
                )
                Box(
                    Modifier
                        .size(100.toDp())
                        .background(Color2)
                )
            }
        }

        rule.onRoot().captureToImage()
            .assertPixels(expectedSize = IntSize(100, 100)) { Color1 }

        // First stage of animation: show all the content.
        repeat(30) { frameNum ->
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
            val image = rule.onRoot().captureToImage()
            val edge1 = image.findFirstColorEdge(Color1, Color2)
            val edge2 = image.findFirstColorEdge(Color2, BackgroundColor)
            val edge3 = image.findFirstColorEdge(BackgroundColor, Color1)
            val expectedEdge1 = 100 - (frameNum * 10)
            val expectedEdge2 = 100 - ((frameNum - 10) * 10)
            val expectedEdge3 = 100 - ((frameNum - 11) * 10)

            when {
                frameNum == 0 -> {
                    assertThat(edge1).isEqualTo(-1)
                    assertThat(edge2).isEqualTo(-1)
                    assertThat(edge3).isEqualTo(-1)
                }

                frameNum < 10 -> {
                    assertThat(edge1).isEqualTo(expectedEdge1)
                    assertThat(edge2).isEqualTo(-1)
                    assertThat(edge3).isEqualTo(-1)
                }

                frameNum == 10 -> {
                    assertThat(edge1).isEqualTo(-1)
                    assertThat(edge2).isEqualTo(-1)
                    assertThat(edge3).isEqualTo(-1)
                }

                frameNum == 11 -> {
                    assertThat(edge1).isEqualTo(-1)
                    assertThat(edge2).isEqualTo(expectedEdge2)
                    assertThat(edge3).isEqualTo(-1)
                }

                frameNum < 20 -> {
                    assertThat(edge1).isEqualTo(-1)
                    assertThat(edge2).isEqualTo(expectedEdge2)
                    assertThat(edge3).isEqualTo(expectedEdge3)
                }

                frameNum == 20 -> {
                    assertThat(edge1).isEqualTo(-1)
                    assertThat(edge2).isEqualTo(-1)
                    assertThat(edge3).isEqualTo(expectedEdge3)
                }

                else -> {
                    // Nothing should happen after the animation finishes.
                    assertThat(edge1).isEqualTo(-1)
                    assertThat(edge2).isEqualTo(-1)
                    assertThat(edge3).isEqualTo(-1)
                }
            }
        }
    }

    @Test
    fun animates_singleIteration_fractionOfSpace() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            Row(
                Modifier
                    .width(100.toDp())
                    .background(BackgroundColor)
                    .basicMarquee(
                        iterations = 1,
                        velocity = 10.pxPerFrame,
                        animationMode = Immediately,
                        initialDelayMillis = 0,
                        spacing = MarqueeSpacing.fractionOfContainer(0.1f)
                    )
            ) {
                Box(
                    Modifier
                        .size(100.toDp())
                        .background(Color1)
                )
                Box(
                    Modifier
                        .size(100.toDp())
                        .background(Color2)
                )
            }
        }

        rule.onRoot().captureToImage()
            .assertPixels(expectedSize = IntSize(100, 100)) { Color1 }

        // First stage of animation: show all the content.
        repeat(30) { frameNum ->
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
            val image = rule.onRoot().captureToImage()
            val edge1 = image.findFirstColorEdge(Color1, Color2)
            val edge2 = image.findFirstColorEdge(Color2, BackgroundColor)
            val edge3 = image.findFirstColorEdge(BackgroundColor, Color1)
            val expectedEdge1 = 100 - (frameNum * 10)
            val expectedEdge2 = 100 - ((frameNum - 10) * 10)
            val expectedEdge3 = 100 - ((frameNum - 11) * 10)

            when {
                frameNum == 0 -> {
                    assertThat(edge1).isEqualTo(-1)
                    assertThat(edge2).isEqualTo(-1)
                    assertThat(edge3).isEqualTo(-1)
                }

                frameNum < 10 -> {
                    assertThat(edge1).isEqualTo(expectedEdge1)
                    assertThat(edge2).isEqualTo(-1)
                    assertThat(edge3).isEqualTo(-1)
                }

                frameNum == 10 -> {
                    assertThat(edge1).isEqualTo(-1)
                    assertThat(edge2).isEqualTo(-1)
                    assertThat(edge3).isEqualTo(-1)
                }

                frameNum == 11 -> {
                    assertThat(edge1).isEqualTo(-1)
                    assertThat(edge2).isEqualTo(expectedEdge2)
                    assertThat(edge3).isEqualTo(-1)
                }

                frameNum < 20 -> {
                    assertThat(edge1).isEqualTo(-1)
                    assertThat(edge2).isEqualTo(expectedEdge2)
                    assertThat(edge3).isEqualTo(expectedEdge3)
                }

                frameNum == 20 -> {
                    assertThat(edge1).isEqualTo(-1)
                    assertThat(edge2).isEqualTo(-1)
                    assertThat(edge3).isEqualTo(expectedEdge3)
                }

                else -> {
                    // Nothing should happen after the animation finishes.
                    assertThat(edge1).isEqualTo(-1)
                    assertThat(edge2).isEqualTo(-1)
                    assertThat(edge3).isEqualTo(-1)
                }
            }
        }
    }

    @Test
    fun animates_twoIterations_noInitialDelay_noDelay() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            Row(
                Modifier
                    .width(100.toDp())
                    .background(BackgroundColor)
                    .basicMarquee(
                        iterations = 2,
                        velocity = 10.pxPerFrame,
                        animationMode = Immediately,
                        initialDelayMillis = 0,
                        delayMillis = 0,
                        spacing = MarqueeSpacing(0.toDp())
                    )
            ) {
                Box(
                    Modifier
                        .size(100.toDp())
                        .background(Color1)
                )
                Box(
                    Modifier
                        .size(100.toDp())
                        .background(Color2)
                )
            }
        }

        rule.onRoot().captureToImage()
            .assertPixels(expectedSize = IntSize(100, 100)) { Color1 }

        // First iteration.
        repeat(20) { frameNum ->
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
            val image = rule.onRoot().captureToImage()
            val edge1 = image.findFirstColorEdge(Color1, Color2)
            val edge2 = image.findFirstColorEdge(Color2, Color1)
            val expectedEdge1 = 100 - (frameNum * 10)
            val expectedEdge2 = 100 - ((frameNum - 10) * 10)

            when {
                frameNum == 0 -> {
                    assertThat(edge1).isEqualTo(-1)
                    assertThat(edge2).isEqualTo(-1)
                }

                frameNum < 10 -> {
                    assertThat(edge1).isEqualTo(expectedEdge1)
                    assertThat(edge2).isEqualTo(-1)
                }

                frameNum == 10 -> {
                    assertThat(edge1).isEqualTo(-1)
                    assertThat(edge2).isEqualTo(-1)
                }

                frameNum < 20 -> {
                    assertThat(edge1).isEqualTo(-1)
                    assertThat(edge2).isEqualTo(expectedEdge2)
                }
            }
        }

        // Second iteration.
        repeat(20) { frameNum ->
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
            val image = rule.onRoot().captureToImage()
            val edge1 = image.findFirstColorEdge(Color1, Color2)
            val edge2 = image.findFirstColorEdge(Color2, Color1)
            val expectedEdge1 = 100 - (frameNum * 10)
            val expectedEdge2 = 100 - ((frameNum - 10) * 10)

            when {
                frameNum == 0 -> {
                    assertThat(edge1).isEqualTo(-1)
                    assertThat(edge2).isEqualTo(-1)
                }

                frameNum < 10 -> {
                    assertThat(edge1).isEqualTo(expectedEdge1)
                    assertThat(edge2).isEqualTo(-1)
                }

                frameNum == 10 -> {
                    assertThat(edge1).isEqualTo(-1)
                    assertThat(edge2).isEqualTo(-1)
                }

                frameNum < 20 -> {
                    assertThat(edge1).isEqualTo(-1)
                    assertThat(edge2).isEqualTo(expectedEdge2)
                }
            }
        }

        // Nothing should happen after the animation finishes.
        repeat(5) {
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
            val image = rule.onRoot().captureToImage()
            val edge1 = image.findFirstColorEdge(Color1, Color2)
            val edge2 = image.findFirstColorEdge(Color2, Color1)
            assertThat(edge1).isEqualTo(-1)
            assertThat(edge2).isEqualTo(-1)
        }
    }

    @Test
    fun animates_twoIterations_initialDelay_noDelay() {
        val initialFrameDelay = 2
        rule.mainClock.autoAdvance = false
        rule.setContent {
            Row(
                Modifier
                    .width(100.toDp())
                    .background(BackgroundColor)
                    .basicMarquee(
                        iterations = 2,
                        velocity = 10.pxPerFrame,
                        animationMode = Immediately,
                        initialDelayMillis = FrameDelay * initialFrameDelay,
                        delayMillis = 0,
                        spacing = MarqueeSpacing(0.toDp())
                    )
            ) {
                Box(
                    Modifier
                        .size(100.toDp())
                        .background(Color1)
                )
                Box(
                    Modifier
                        .size(100.toDp())
                        .background(Color2)
                )
            }
        }

        rule.onRoot().captureToImage()
            .assertPixels(expectedSize = IntSize(100, 100)) { Color1 }

        repeat(initialFrameDelay) {
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
            val image = rule.onRoot().captureToImage()
            val edge1 = image.findFirstColorEdge(Color1, Color2)
            val edge2 = image.findFirstColorEdge(Color2, Color1)

            assertThat(edge1).isEqualTo(-1)
            assertThat(edge2).isEqualTo(-1)
        }

        // First iteration.
        repeat(20) { frameNum ->
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
            val image = rule.onRoot().captureToImage()
            val edge1 = image.findFirstColorEdge(Color1, Color2)
            val edge2 = image.findFirstColorEdge(Color2, Color1)
            val expectedEdge1 = 100 - (frameNum * 10)
            val expectedEdge2 = 100 - ((frameNum - 10) * 10)

            when {
                frameNum == 0 -> {
                    assertThat(edge1).isEqualTo(-1)
                    assertThat(edge2).isEqualTo(-1)
                }

                frameNum < 10 -> {
                    assertThat(edge1).isEqualTo(expectedEdge1)
                    assertThat(edge2).isEqualTo(-1)
                }

                frameNum == 10 -> {
                    assertThat(edge1).isEqualTo(-1)
                    assertThat(edge2).isEqualTo(-1)
                }

                frameNum < 20 -> {
                    assertThat(edge1).isEqualTo(-1)
                    assertThat(edge2).isEqualTo(expectedEdge2)
                }
            }
        }

        // Second iteration.
        repeat(20) { frameNum ->
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
            val image = rule.onRoot().captureToImage()
            val edge1 = image.findFirstColorEdge(Color1, Color2)
            val edge2 = image.findFirstColorEdge(Color2, Color1)
            val expectedEdge1 = 100 - (frameNum * 10)
            val expectedEdge2 = 100 - ((frameNum - 10) * 10)

            when {
                frameNum == 0 -> {
                    assertThat(edge1).isEqualTo(-1)
                    assertThat(edge2).isEqualTo(-1)
                }

                frameNum < 10 -> {
                    assertThat(edge1).isEqualTo(expectedEdge1)
                    assertThat(edge2).isEqualTo(-1)
                }

                frameNum == 10 -> {
                    assertThat(edge1).isEqualTo(-1)
                    assertThat(edge2).isEqualTo(-1)
                }

                frameNum < 20 -> {
                    assertThat(edge1).isEqualTo(-1)
                    assertThat(edge2).isEqualTo(expectedEdge2)
                }
            }
        }

        // Nothing should happen after the animation finishes.
        repeat(5) {
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
            val image = rule.onRoot().captureToImage()
            val edge1 = image.findFirstColorEdge(Color1, Color2)
            val edge2 = image.findFirstColorEdge(Color2, Color1)
            assertThat(edge1).isEqualTo(-1)
            assertThat(edge2).isEqualTo(-1)
        }
    }

    @Test
    fun animates_twoIterations_noInitialDelay_delay() {
        val frameDelay = 2
        rule.mainClock.autoAdvance = false
        rule.setContent {
            Row(
                Modifier
                    .width(100.toDp())
                    .background(BackgroundColor)
                    .basicMarquee(
                        iterations = 2,
                        velocity = 10.pxPerFrame,
                        animationMode = Immediately,
                        initialDelayMillis = 0,
                        delayMillis = FrameDelay * frameDelay,
                        spacing = MarqueeSpacing(0.toDp())
                    )
            ) {
                Box(
                    Modifier
                        .size(100.toDp())
                        .background(Color1)
                )
                Box(
                    Modifier
                        .size(100.toDp())
                        .background(Color2)
                )
            }
        }

        rule.onRoot().captureToImage()
            .assertPixels(expectedSize = IntSize(100, 100)) { Color1 }

        // First iteration.
        repeat(20) { frameNum ->
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
            val image = rule.onRoot().captureToImage()
            val edge1 = image.findFirstColorEdge(Color1, Color2)
            val edge2 = image.findFirstColorEdge(Color2, Color1)
            val expectedEdge1 = 100 - (frameNum * 10)
            val expectedEdge2 = 100 - ((frameNum - 10) * 10)

            when {
                frameNum == 0 -> {
                    assertThat(edge1).isEqualTo(-1)
                    assertThat(edge2).isEqualTo(-1)
                }

                frameNum < 10 -> {
                    assertThat(edge1).isEqualTo(expectedEdge1)
                    assertThat(edge2).isEqualTo(-1)
                }

                frameNum == 10 -> {
                    assertThat(edge1).isEqualTo(-1)
                    assertThat(edge2).isEqualTo(-1)
                }

                frameNum < 20 -> {
                    assertThat(edge1).isEqualTo(-1)
                    assertThat(edge2).isEqualTo(expectedEdge2)
                }
            }
        }

        repeat(frameDelay) {
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
            val image = rule.onRoot().captureToImage()
            val edge1 = image.findFirstColorEdge(Color1, Color2)
            val edge2 = image.findFirstColorEdge(Color2, Color1)

            assertThat(edge1).isEqualTo(-1)
            assertThat(edge2).isEqualTo(-1)
        }

        // Second iteration.
        repeat(20) { frameNum ->
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
            val image = rule.onRoot().captureToImage()
            val edge1 = image.findFirstColorEdge(Color1, Color2)
            val edge2 = image.findFirstColorEdge(Color2, Color1)
            val expectedEdge1 = 100 - (frameNum * 10)
            val expectedEdge2 = 100 - ((frameNum - 10) * 10)

            when {
                frameNum == 0 -> {
                    assertThat(edge1).isEqualTo(-1)
                    assertThat(edge2).isEqualTo(-1)
                }

                frameNum < 10 -> {
                    assertThat(edge1).isEqualTo(expectedEdge1)
                    assertThat(edge2).isEqualTo(-1)
                }

                frameNum == 10 -> {
                    assertThat(edge1).isEqualTo(-1)
                    assertThat(edge2).isEqualTo(-1)
                }

                frameNum < 20 -> {
                    assertThat(edge1).isEqualTo(-1)
                    assertThat(edge2).isEqualTo(expectedEdge2)
                }
            }
        }

        // Nothing should happen after the animation finishes.
        repeat(5) {
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
            val image = rule.onRoot().captureToImage()
            val edge1 = image.findFirstColorEdge(Color1, Color2)
            val edge2 = image.findFirstColorEdge(Color2, Color1)
            assertThat(edge1).isEqualTo(-1)
            assertThat(edge2).isEqualTo(-1)
        }
    }

    @Test
    fun animates_twoIterations_initialDelay_delay() {
        val initialFrameDelay = 2
        val frameDelay = 3
        rule.mainClock.autoAdvance = false
        rule.setContent {
            Row(
                Modifier
                    .width(100.toDp())
                    .background(BackgroundColor)
                    .basicMarquee(
                        iterations = 2,
                        velocity = 10.pxPerFrame,
                        animationMode = Immediately,
                        initialDelayMillis = FrameDelay * initialFrameDelay,
                        delayMillis = FrameDelay * frameDelay,
                        spacing = MarqueeSpacing(0.toDp())
                    )
            ) {
                Box(
                    Modifier
                        .size(100.toDp())
                        .background(Color1)
                )
                Box(
                    Modifier
                        .size(100.toDp())
                        .background(Color2)
                )
            }
        }

        rule.onRoot().captureToImage()
            .assertPixels(expectedSize = IntSize(100, 100)) { Color1 }

        repeat(initialFrameDelay) {
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
            val image = rule.onRoot().captureToImage()
            val edge1 = image.findFirstColorEdge(Color1, Color2)
            val edge2 = image.findFirstColorEdge(Color2, Color1)

            assertThat(edge1).isEqualTo(-1)
            assertThat(edge2).isEqualTo(-1)
        }

        // First iteration.
        repeat(20) { frameNum ->
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
            val image = rule.onRoot().captureToImage()
            val edge1 = image.findFirstColorEdge(Color1, Color2)
            val edge2 = image.findFirstColorEdge(Color2, Color1)
            val expectedEdge1 = 100 - (frameNum * 10)
            val expectedEdge2 = 100 - ((frameNum - 10) * 10)

            when {
                frameNum == 0 -> {
                    assertThat(edge1).isEqualTo(-1)
                    assertThat(edge2).isEqualTo(-1)
                }

                frameNum < 10 -> {
                    assertThat(edge1).isEqualTo(expectedEdge1)
                    assertThat(edge2).isEqualTo(-1)
                }

                frameNum == 10 -> {
                    assertThat(edge1).isEqualTo(-1)
                    assertThat(edge2).isEqualTo(-1)
                }

                frameNum < 20 -> {
                    assertThat(edge1).isEqualTo(-1)
                    assertThat(edge2).isEqualTo(expectedEdge2)
                }
            }
        }

        repeat(frameDelay) {
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
            val image = rule.onRoot().captureToImage()
            val edge1 = image.findFirstColorEdge(Color1, Color2)
            val edge2 = image.findFirstColorEdge(Color2, Color1)

            assertThat(edge1).isEqualTo(-1)
            assertThat(edge2).isEqualTo(-1)
        }

        // Second iteration.
        repeat(20) { frameNum ->
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
            val image = rule.onRoot().captureToImage()
            val edge1 = image.findFirstColorEdge(Color1, Color2)
            val edge2 = image.findFirstColorEdge(Color2, Color1)
            val expectedEdge1 = 100 - (frameNum * 10)
            val expectedEdge2 = 100 - ((frameNum - 10) * 10)

            when {
                frameNum == 0 -> {
                    assertThat(edge1).isEqualTo(-1)
                    assertThat(edge2).isEqualTo(-1)
                }

                frameNum < 10 -> {
                    assertThat(edge1).isEqualTo(expectedEdge1)
                    assertThat(edge2).isEqualTo(-1)
                }

                frameNum == 10 -> {
                    assertThat(edge1).isEqualTo(-1)
                    assertThat(edge2).isEqualTo(-1)
                }

                frameNum < 20 -> {
                    assertThat(edge1).isEqualTo(-1)
                    assertThat(edge2).isEqualTo(expectedEdge2)
                }
            }
        }

        // Nothing should happen after the animation finishes.
        repeat(5) {
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
            val image = rule.onRoot().captureToImage()
            val edge1 = image.findFirstColorEdge(Color1, Color2)
            val edge2 = image.findFirstColorEdge(Color2, Color1)
            assertThat(edge1).isEqualTo(-1)
            assertThat(edge2).isEqualTo(-1)
        }
    }

    @Test
    fun animates_negativeVelocity() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            Row(
                Modifier
                    .width(100.toDp())
                    .background(BackgroundColor)
                    .basicMarquee(
                        iterations = 1,
                        velocity = (-10).pxPerFrame,
                        animationMode = Immediately,
                        initialDelayMillis = 0,
                        spacing = MarqueeSpacing(0.toDp())
                    )
            ) {
                Box(
                    Modifier
                        .size(100.toDp())
                        .background(Color1)
                )
                Box(
                    Modifier
                        .size(100.toDp())
                        .background(Color2)
                )
            }
        }

        rule.onRoot().captureToImage()
            .assertPixels(expectedSize = IntSize(100, 100)) { Color1 }

        // First stage of animation: show all the content.
        repeat(30) { frameNum ->
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
            val image = rule.onRoot().captureToImage()
            val edge1 = image.findFirstColorEdge(Color2, Color1)
            val edge2 = image.findFirstColorEdge(Color1, Color2)
            val expectedEdge1 = frameNum * 10
            val expectedEdge2 = (frameNum - 10) * 10

            when {
                frameNum == 0 -> {
                    assertThat(edge1).isEqualTo(-1)
                    assertThat(edge2).isEqualTo(-1)
                }

                frameNum < 10 -> {
                    assertThat(edge1).isEqualTo(expectedEdge1)
                    assertThat(edge2).isEqualTo(-1)
                }

                frameNum == 10 -> {
                    assertThat(edge1).isEqualTo(-1)
                    assertThat(edge2).isEqualTo(-1)
                }

                frameNum < 20 -> {
                    assertThat(edge1).isEqualTo(-1)
                    assertThat(edge2).isEqualTo(expectedEdge2)
                }

                else -> {
                    // Nothing should happen after the animation finishes.
                    assertThat(edge1).isEqualTo(-1)
                    assertThat(edge2).isEqualTo(-1)
                }
            }
        }
    }

    @Test
    fun animates_rtl() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Row(
                    Modifier
                        .width(100.toDp())
                        .background(BackgroundColor)
                        .basicMarquee(
                            iterations = 1,
                            velocity = 10.pxPerFrame,
                            animationMode = Immediately,
                            initialDelayMillis = 0,
                            spacing = MarqueeSpacing(0.toDp())
                        )
                ) {
                    Box(
                        Modifier
                            .size(100.toDp())
                            .background(Color1)
                    )
                    Box(
                        Modifier
                            .size(100.toDp())
                            .background(Color2)
                    )
                }
            }
        }

        rule.onRoot().captureToImage()
            .assertPixels(expectedSize = IntSize(100, 100)) { Color2 }

        // First stage of animation: show all the content.
        repeat(30) { frameNum ->
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
            val image = rule.onRoot().captureToImage()
            val edge1 = image.findFirstColorEdge(Color1, Color2)
            val edge2 = image.findFirstColorEdge(Color2, Color1)
            val expectedEdge1 = frameNum * 10
            val expectedEdge2 = (frameNum - 10) * 10

            when {
                frameNum == 0 -> {
                    assertThat(edge1).isEqualTo(-1)
                    assertThat(edge2).isEqualTo(-1)
                }

                frameNum < 10 -> {
                    assertThat(edge1).isEqualTo(expectedEdge1)
                    assertThat(edge2).isEqualTo(-1)
                }

                frameNum == 10 -> {
                    assertThat(edge1).isEqualTo(-1)
                    assertThat(edge2).isEqualTo(-1)
                }

                frameNum < 20 -> {
                    assertThat(edge1).isEqualTo(-1)
                    assertThat(edge2).isEqualTo(expectedEdge2)
                }

                else -> {
                    // Nothing should happen after the animation finishes.
                    assertThat(edge1).isEqualTo(-1)
                    assertThat(edge2).isEqualTo(-1)
                }
            }
        }
    }

    @Test
    fun animationMode_onlyWhileFocused() {
        val focusRequester = FocusRequester()
        lateinit var focusManager: FocusManager
        rule.mainClock.autoAdvance = false
        rule.setContent {
            focusManager = LocalFocusManager.current
            Row(
                Modifier
                    .width(100.toDp())
                    .background(BackgroundColor)
                    .basicMarquee(
                        iterations = 1,
                        velocity = 10.pxPerFrame,
                        animationMode = WhileFocused,
                        initialDelayMillis = 0,
                        spacing = MarqueeSpacing(0.toDp())
                    )
                    .focusRequester(focusRequester)
                    .focusable()
            ) {
                Box(
                    Modifier
                        .size(100.toDp())
                        .background(Color1)
                )
                Box(
                    Modifier
                        .size(100.toDp())
                        .background(Color2)
                )
            }
        }

        // Nothing should happen before focusing.
        repeat(10) {
            rule.mainClock.advanceTimeByFrame()
            rule.onRoot().captureToImage()
                .assertPixels(expectedSize = IntSize(100, 100)) { Color1 }
        }

        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Animation should start on next frame.
        repeat(5) { frameNum ->
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
            val image = rule.onRoot().captureToImage()
            val edge1 = image.findFirstColorEdge(Color1, Color2)
            val expectedEdge1 = 100 - (frameNum * 10)

            when (frameNum) {
                0 -> {
                    assertThat(edge1).isEqualTo(-1)
                }

                else -> {
                    assertThat(edge1).isEqualTo(expectedEdge1)
                }
            }
        }

        rule.runOnIdle {
            focusManager.clearFocus()
        }

        // Losing focus should cancel the animation and reset the offset.
        repeat(5) {
            rule.mainClock.advanceTimeByFrame()
            rule.onRoot().captureToImage()
                .assertPixels(expectedSize = IntSize(100, 100)) { Color1 }
        }
    }

    @Test
    fun drawCounts() {
        var outerDraws = 0
        var innerDraws = 0
        val iterations = 10

        rule.mainClock.autoAdvance = false
        rule.setContent {
            Row(
                Modifier
                    .drawBehind { outerDraws++ }
                    .width(100.toDp())
                    .basicMarquee(
                        iterations = 1,
                        velocity = 10.pxPerFrame,
                        animationMode = Immediately,
                        initialDelayMillis = 0,
                        spacing = MarqueeSpacing(0.dp)
                    )
                    .drawBehind { innerDraws++ }
            ) {
                Box(
                    Modifier
                        .size(100.toDp())
                        .background(Color1)
                )
                Box(
                    Modifier
                        .size(100.toDp())
                        .background(Color2)
                )
            }
        }

        repeat(iterations) {
            rule.mainClock.advanceTimeByFrame()
        }

        rule.runOnIdle {
            assertThat(outerDraws).isEqualTo(1)
            assertThat(innerDraws).isEqualTo(iterations + 1)
        }
    }

    /**
     * Finds the x coordinate in the image of the top row of pixels where the color first changes
     * from [left] to [right]. If the change is gradual, the middle of the change is returned.
     * Returns -1 if no edge can be found.
     */
    private fun ImageBitmap.findFirstColorEdge(left: Color, right: Color): Int {
        val pixelMap = toPixelMap()
        var edgeStartX = -1

        for (x in 0 until pixelMap.width) {
            val pixel = pixelMap[x, 0]
            if (pixel == left) {
                edgeStartX = x
            } else if (pixel == right) {
                return if (edgeStartX >= 0) {
                    println("OMG found edge: $edgeStartX - $x")
                    ((edgeStartX.toFloat() + x.toFloat()) / 2f).roundToInt()
                } else {
                    // Never found the start of the edge.
                    -1
                }
            }
        }
        return -1
    }
}