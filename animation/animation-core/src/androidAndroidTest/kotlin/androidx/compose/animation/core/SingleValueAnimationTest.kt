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

package androidx.compose.animation.core

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
@OptIn(ExperimentalTestApi::class)
class SingleValueAnimationTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun animate1DTest() {
        fun <T> myTween(): TweenSpec<T> =
            TweenSpec(
                easing = FastOutSlowInEasing,
                durationMillis = 100
            )

        var enabled by mutableStateOf(false)
        var expected by mutableStateOf(250f)
        rule.setContent {
            Box {
                val animationValue by animateDpAsState(
                    if (enabled) 50.dp else 250.dp, myTween()
                )
                // TODO: Properly test this with a deterministic clock when the test framework is
                // ready
                if (enabled) {
                    LaunchedEffect(Unit) {
                        val startTime = withFrameNanos { it }
                        var frameTime = startTime
                        do {
                            val playTime = (frameTime - startTime) / 1_000_000L
                            val fraction = FastOutSlowInEasing.transform(playTime / 100f)
                            expected = lerp(250f, 50f, fraction)
                            frameTime = withFrameNanos { it }
                        } while (frameTime - startTime <= 100_000_000L)
                        // Animation is finished at this point
                        expected = 50f
                    }
                    assertEquals(expected.dp, animationValue)
                } else {
                    assertEquals(250.dp, animationValue)
                }
            }
        }
        assertEquals(250f, expected)
        rule.runOnIdle { enabled = true }
        rule.waitForIdle()
        assertEquals(50f, expected)
    }

    @Test
    fun animate1DOnCoroutineTest() {
        var enabled by mutableStateOf(false)
        var expected by mutableStateOf(250f)
        rule.setContent {
            Box {
                // Animate from 250f to 50f when enable flips to true
                val animationValue by animateFloatAsState(
                    if (enabled) 50f else 250f, tween(200, easing = FastOutLinearInEasing)
                )
                // TODO: Properly test this with a deterministic clock when the test framework is
                // ready
                if (enabled) {
                    LaunchedEffect(Unit) {
                        assertEquals(250f, animationValue)
                        val startTime = withFrameNanos { it }
                        var frameTime = startTime
                        do {
                            val playTime = (frameTime - startTime) / 1_000_000L
                            val fraction = FastOutLinearInEasing.transform(playTime / 200f)
                            expected = lerp(250f, 50f, fraction)
                            frameTime = withFrameNanos { it }
                        } while (frameTime - startTime <= 200_000_000L)
                        expected = 50f
                    }
                }
                assertEquals(expected, animationValue)
            }
        }
        rule.runOnIdle { enabled = true }
        rule.waitForIdle()
        // Animation is finished at this point
        assertEquals(50f, expected)
    }

    @Test
    fun animate2DTest() {

        val startVal = AnimationVector(120f, 56f)
        val endVal = AnimationVector(0f, 77f)
        var expected by mutableStateOf(startVal)

        fun <V> tween(): TweenSpec<V> =
            TweenSpec(
                easing = LinearEasing,
                durationMillis = 100
            )

        var enabled by mutableStateOf(false)
        rule.setContent {
            Box {
                val sizeValue by animateSizeAsState(
                    if (enabled)
                        Size.VectorConverter.convertFromVector(endVal)
                    else
                        Size.VectorConverter.convertFromVector(startVal),
                    tween()
                )

                val pxPositionValue by animateOffsetAsState(
                    if (enabled)
                        Offset.VectorConverter.convertFromVector(endVal)
                    else
                        Offset.VectorConverter.convertFromVector(startVal),
                    tween()
                )

                if (enabled) {
                    LaunchedEffect(Unit) {
                        val startTime = withFrameNanos { it }
                        var frameTime = startTime
                        do {
                            val playTime = (frameTime - startTime) / 1_000_000L
                            expected = AnimationVector(
                                lerp(startVal.v1, endVal.v1, playTime / 100f),
                                lerp(startVal.v2, endVal.v2, playTime / 100f)
                            )

                            frameTime = withFrameNanos { it }
                        } while (frameTime - startTime <= 100_000_000L)
                        expected = endVal
                    }
                }

                assertEquals(Size.VectorConverter.convertFromVector(expected), sizeValue)
                assertEquals(
                    Offset.VectorConverter.convertFromVector(expected),
                    pxPositionValue
                )
            }
        }

        rule.runOnIdle { enabled = true }
        rule.waitForIdle()
        assertEquals(endVal, expected)
    }

    @Test
    fun animate4DRectTest() {
        val startVal = AnimationVector(30f, -76f, 280f, 35f)
        val endVal = AnimationVector(-42f, 89f, 77f, 100f)

        fun <V> tween(): TweenSpec<V> =
            TweenSpec(
                easing = LinearOutSlowInEasing,
                durationMillis = 100
            )

        var enabled by mutableStateOf(false)
        var expected by mutableStateOf(startVal)
        rule.setContent {
            Box {
                val pxBoundsValue by animateRectAsState(
                    if (enabled)
                        Rect.VectorConverter.convertFromVector(endVal)
                    else
                        Rect.VectorConverter.convertFromVector(startVal),
                    tween()
                )

                if (enabled) {
                    LaunchedEffect(Unit) {
                        val startTime = withFrameNanos { it }
                        var frameTime = startTime
                        do {
                            val playTime = (frameTime - startTime) / 1_000_000L

                            val fraction = LinearOutSlowInEasing.transform(playTime / 100f)
                            expected = AnimationVector(
                                lerp(startVal.v1, endVal.v1, fraction),
                                lerp(startVal.v2, endVal.v2, fraction),
                                lerp(startVal.v3, endVal.v3, fraction),
                                lerp(startVal.v4, endVal.v4, fraction)
                            )

                            frameTime = withFrameNanos { it }
                        } while (frameTime - startTime <= 100_000_000L)
                        expected = endVal
                    }
                }

                // Check this every frame
                assertEquals(
                    Rect.VectorConverter.convertFromVector(expected),
                    pxBoundsValue
                )
            }
        }

        rule.runOnIdle { enabled = true }
        rule.waitForIdle()
        assertEquals(endVal, expected)
    }

    @Test
    fun animateColorTest() {
        var enabled by mutableStateOf(false)
        var expected by mutableStateOf(Color.Black)
        rule.setContent {
            Box {
                val value by animateColorAsState(
                    if (enabled) Color.Cyan else Color.Black,
                    TweenSpec(
                        durationMillis = 100,
                        easing = FastOutLinearInEasing
                    )
                )
                if (enabled) {
                    LaunchedEffect(Unit) {
                        val startTime = withFrameNanos { it }
                        var frameTime = startTime
                        do {
                            val playTime = (frameTime - startTime) / 1_000_000L
                            val fraction = FastOutLinearInEasing.transform(playTime / 100f)
                            expected = lerp(Color.Black, Color.Cyan, fraction)
                            frameTime = withFrameNanos { it }
                        } while (frameTime - startTime <= 100_000_000L)
                        expected = Color.Cyan
                    }
                }
                // Check every frame
                assertEquals(expected.red, value.red, 1 / 255f)
                assertEquals(expected.green, value.green, 1 / 255f)
                assertEquals(expected.blue, value.blue, 1 / 255f)
                assertEquals(expected.alpha, value.alpha, 1 / 255f)
            }
        }

        rule.runOnIdle { enabled = true }
        rule.waitForIdle()
        assertEquals(Color.Cyan, expected)
    }

    @Test
    fun frameByFrameInterruptionTest() {
        var enabled by mutableStateOf(false)
        var currentValue by mutableStateOf(Offset(-300f, -300f))
        rule.setContent {
            Box {
                var destination: Offset by remember { mutableStateOf(Offset(600f, 600f)) }
                val offsetValue = animateOffsetAsState(
                    if (enabled)
                        destination
                    else
                        Offset(0f, 0f)
                )
                if (enabled) {
                    LaunchedEffect(enabled) {
                        var startTime = -1L
                        while (true) {
                            val current = withFrameMillis {
                                if (startTime < 0) startTime = it
                                // Fuzzy test by fine adjusting the target on every frame, and
                                // verify there's a reasonable amount of test. This is to make sure
                                // the animation does not stay "frozen" when there's continuous
                                // target changes.
                                if (destination.x >= 600) {
                                    destination = Offset(599f, 599f)
                                } else {
                                    destination = Offset(601f, 601f)
                                }
                                it
                            }
                            currentValue = offsetValue.value
                            if (current - startTime > 1000) {
                                break
                            }
                        }
                    }
                }
            }
        }
        rule.runOnIdle {
            enabled = true
            assertEquals(Offset(-300f, -300f), currentValue)
        }
        rule.waitUntil(1300) {
            currentValue.x > 300f && currentValue.y > 300f
        }
    }

    @Test
    fun visibilityThresholdTest() {

        val specForFloat = FloatSpringSpec(visibilityThreshold = 0.01f)
        val specForOffset = FloatSpringSpec(visibilityThreshold = 0.5f)

        var expectedFloat by mutableStateOf(0f)
        var expectedOffset by mutableStateOf(Offset(0f, 0f))
        var enabled by mutableStateOf(false)
        rule.setContent {
            Box {
                val offsetValue by animateOffsetAsState(
                    if (enabled)
                        Offset(100f, 100f)
                    else
                        Offset(0f, 0f)
                )

                val floatValue by animateFloatAsState(if (enabled) 100f else 0f)

                val durationForFloat = specForFloat.getDurationNanos(0f, 100f, 0f)
                val durationForOffset = specForOffset.getDurationNanos(0f, 100f, 0f)

                if (enabled) {
                    LaunchedEffect(Unit) {
                        val startTime = withFrameNanos { it }
                        var frameTime = startTime
                        do {
                            val playTime = frameTime - startTime
                            expectedFloat =
                                specForFloat.getValueFromNanos(playTime, 0f, 100f, 0f)

                            if (playTime < durationForOffset) {
                                val offset =
                                    specForOffset.getValueFromNanos(playTime, 0f, 100f, 0f)
                                expectedOffset = Offset(offset, offset)
                            } else {
                                expectedOffset = Offset(100f, 100f)
                            }

                            frameTime = withFrameNanos { it }
                        } while (frameTime - startTime <= durationForFloat)
                        expectedFloat = 100f
                    }
                }

                assertEquals(expectedOffset, offsetValue)
                assertEquals(expectedFloat, floatValue)
            }
        }

        rule.runOnIdle { enabled = true }
        rule.waitForIdle()
    }

    @Test
    fun updateAnimationSpecTest() {
        var duration by mutableStateOf(100)
        var firstRun by mutableStateOf(true)
        fun <T> myTween(): TweenSpec<T> =
            TweenSpec(
                easing = FastOutSlowInEasing,
                durationMillis = duration
            )

        var enabled by mutableStateOf(false)
        var expected by mutableStateOf(250f)
        rule.setContent {
            Box {
                val animationValue by animateDpAsState(
                    if (enabled) 50.dp else 250.dp, myTween()
                )
                assertEquals(expected.dp, animationValue)
                if (!firstRun) {
                    LaunchedEffect(enabled) {
                        if (enabled) {
                            assertEquals(100, duration)
                        } else {
                            assertEquals(200, duration)
                        }
                        val startTime = withFrameNanos { it }
                        var frameTime = startTime
                        do {
                            val playTime = (frameTime - startTime) / 1_000_000L
                            val fraction = FastOutSlowInEasing.transform(
                                playTime / duration.toFloat()
                            )
                            expected =
                                if (enabled) {
                                    lerp(250f, 50f, fraction)
                                } else {
                                    lerp(50f, 250f, fraction)
                                }
                            frameTime = withFrameNanos { it }
                        } while (frameTime - startTime <= duration * 1_000_000L)
                        expected = if (enabled) 50f else 250f
                    }
                }
            }
        }
        rule.runOnIdle {
            enabled = true
            firstRun = false
        }
        rule.waitForIdle()
        // Animation is finished at this point
        assertEquals(50f, expected)

        rule.runOnIdle {
            enabled = false
            duration = 200
        }
        rule.waitForIdle()
        // Animation is finished at this point
        assertEquals(250f, expected)
    }
}
