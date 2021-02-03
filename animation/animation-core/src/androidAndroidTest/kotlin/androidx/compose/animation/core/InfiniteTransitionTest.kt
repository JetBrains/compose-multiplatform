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

package androidx.compose.animation.core

import androidx.compose.animation.VectorConverter
import androidx.compose.animation.animateColor
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class InfiniteTransitionTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun transitionTest() {
        // Manually advance the clock to prevent the infinite transition from being cancelled
        rule.mainClock.autoAdvance = false

        val colorAnim = TargetBasedAnimation(
            tween(1000),
            Color.VectorConverter(Color.Red.colorSpace),
            Color.Red,
            Color.Green
        )

        // Animate from 0f to 0f for 1000ms
        val keyframes = keyframes<Float> {
            durationMillis = 1000
            0f at 0
            200f at 400
            1000f at 1000
        }

        val keyframesAnim = TargetBasedAnimation(
            keyframes,
            Float.VectorConverter,
            0f,
            0f
        )

        val runAnimation = mutableStateOf(true)
        rule.setContent {
            val transition = rememberInfiniteTransition()
            if (runAnimation.value) {
                val animFloat = transition.animateFloat(
                    0f,
                    0f,
                    infiniteRepeatable(
                        keyframes,
                        repeatMode = RepeatMode.Reverse
                    )
                )

                val animColor = transition.animateColor(
                    Color.Red,
                    Color.Green,
                    infiniteRepeatable(
                        tween(1000)
                    )
                )

                LaunchedEffect(Unit) {
                    val startTime = withFrameNanos { it }
                    var playTime = 0L
                    while (playTime < 2100L) {
                        playTime = withFrameNanos { it } - startTime
                        var iterationTime = playTime % (2000 * MillisToNanos)
                        if (iterationTime > 1000 * MillisToNanos) {
                            iterationTime = 2000L * MillisToNanos - iterationTime
                        }
                        val expectedFloat = keyframesAnim.getValueFromNanos(iterationTime)
                        val expectedColor = colorAnim.getValueFromNanos(
                            playTime % (1000 * MillisToNanos)
                        )
                        assertEquals(expectedFloat, animFloat.value, 0.01f)
                        assertEquals(expectedColor, animColor.value)
                    }
                    runAnimation.value = false
                }
            }
        }
        // Manually advance the clock
        while (runAnimation.value) {
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
        }
        assertFalse(runAnimation.value)
    }
}
