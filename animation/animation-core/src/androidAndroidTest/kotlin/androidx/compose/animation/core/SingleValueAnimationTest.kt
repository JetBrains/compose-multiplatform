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

import androidx.compose.animation.animateAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.dispatch.withFrameNanos
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Bounds
import androidx.compose.ui.unit.DpOffset
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
        rule.setContent {
            Box {
                val animationValue by animateAsState(
                    if (enabled) 50.dp else 250.dp, myTween()
                )
                // TODO: Properly test this with a deterministic clock when the test framework is
                // ready
                if (enabled) {
                    LaunchedEffect(Unit) {
                        assertEquals(250.dp, animationValue)
                        val startTime = withFrameNanos { it }
                        var frameTime = startTime
                        do {
                            val playTime = (frameTime - startTime) / 1_000_000L
                            val fraction = FastOutSlowInEasing.transform(playTime / 100f)
                            val expected = lerp(250f, 50f, fraction)
                            assertEquals(expected.dp, animationValue)
                            frameTime = withFrameNanos { it }
                        } while (frameTime - startTime <= 100_000_000L)
                        // Animation is finished at this point
                        assertEquals(50.dp, animationValue)
                    }
                }
            }
        }
        rule.runOnIdle { enabled = true }
        rule.waitForIdle()
    }

    @Test
    fun animate1DOnCoroutineTest() {
        var enabled by mutableStateOf(false)
        rule.setContent {
            Box {
                // Animate from 250f to 50f when enable flips to true
                val animationValue by animateAsState(
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
                            val expected = lerp(250f, 50f, fraction)
                            assertEquals(expected, animationValue)
                            frameTime = withFrameNanos { it }
                        } while (frameTime - startTime <= 200_000_000L)
                        // Animation is finished at this point
                        assertEquals(50f, animationValue)
                    }
                }
            }
        }
        rule.runOnIdle { enabled = true }
        rule.waitForIdle()
    }

    @Test
    fun animate2DTest() {

        val startVal = AnimationVector(120f, 56f)
        val endVal = AnimationVector(0f, 77f)

        fun <V> tween(): TweenSpec<V> =
            TweenSpec(
                easing = LinearEasing,
                durationMillis = 100
            )

        var enabled by mutableStateOf(false)
        rule.setContent {
            Box {
                val vectorValue by animateAsState(
                    if (enabled) endVal else startVal,
                    tween()
                )

                val positionValue by animateAsState(
                    if (enabled)
                        DpOffset.VectorConverter.convertFromVector(endVal)
                    else
                        DpOffset.VectorConverter.convertFromVector(startVal),
                    tween()
                )

                val sizeValue by animateAsState(
                    if (enabled)
                        Size.VectorConverter.convertFromVector(endVal)
                    else
                        Size.VectorConverter.convertFromVector(startVal),
                    tween()
                )

                val pxPositionValue by animateAsState(
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
                            val expect = AnimationVector(
                                lerp(startVal.v1, endVal.v1, playTime / 100f),
                                lerp(startVal.v2, endVal.v2, playTime / 100f)
                            )

                            assertEquals(expect, vectorValue)
                            assertEquals(Size.VectorConverter.convertFromVector(expect), sizeValue)
                            assertEquals(
                                DpOffset.VectorConverter.convertFromVector(expect),
                                positionValue
                            )
                            assertEquals(
                                Offset.VectorConverter.convertFromVector(expect),
                                pxPositionValue
                            )
                            frameTime = withFrameNanos { it }
                        } while (frameTime - startTime <= 100_000_000L)
                    }
                }
            }
        }

        rule.runOnIdle { enabled = true }
        rule.waitForIdle()
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
        rule.setContent {
            Box {
                val vectorValue by animateAsState(
                    if (enabled) endVal else startVal,
                    tween()
                )

                val boundsValue by animateAsState(
                    if (enabled)
                        Bounds.VectorConverter.convertFromVector(endVal)
                    else
                        Bounds.VectorConverter.convertFromVector(startVal),
                    tween()
                )

                val pxBoundsValue by animateAsState(
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
                            val expect = AnimationVector(
                                lerp(startVal.v1, endVal.v1, fraction),
                                lerp(startVal.v2, endVal.v2, fraction),
                                lerp(startVal.v3, endVal.v3, fraction),
                                lerp(startVal.v4, endVal.v4, fraction)
                            )

                            assertEquals(expect, vectorValue)
                            assertEquals(
                                Bounds.VectorConverter.convertFromVector(expect),
                                boundsValue
                            )
                            assertEquals(
                                Rect.VectorConverter.convertFromVector(expect),
                                pxBoundsValue
                            )
                            frameTime = withFrameNanos { it }
                        } while (frameTime - startTime <= 100_000_000L)
                    }
                }
            }
        }

        rule.runOnIdle { enabled = true }
        rule.waitForIdle()
    }

    @Test
    fun animate4DTest() {
        val startVal = AnimationVector(30f, -76f, 280f, 35f)
        val endVal = AnimationVector(-42f, 89f, 77f, 100f)

        fun <V> tween(): TweenSpec<V> =
            TweenSpec(
                easing = LinearOutSlowInEasing,
                durationMillis = 100
            )

        var enabled by mutableStateOf(false)
        rule.setContent {
            Box {
                val vectorValue by animateAsState(
                    if (enabled) endVal else startVal,
                    tween()
                )

                val boundsValue by animateAsState(
                    if (enabled)
                        Bounds.VectorConverter.convertFromVector(endVal)
                    else
                        Bounds.VectorConverter.convertFromVector(startVal),
                    tween()
                )
                if (enabled) {
                    LaunchedEffect(Unit) {
                        val startTime = withFrameNanos { it }
                        var frameTime = startTime
                        do {
                            val playTime = (frameTime - startTime) / 1_000_000L
                            val fraction = LinearOutSlowInEasing.transform(playTime / 100f)
                            val expect = AnimationVector(
                                lerp(startVal.v1, endVal.v1, fraction),
                                lerp(startVal.v2, endVal.v2, fraction),
                                lerp(startVal.v3, endVal.v3, fraction),
                                lerp(startVal.v4, endVal.v4, fraction)
                            )

                            assertEquals(expect, vectorValue)
                            assertEquals(
                                Bounds.VectorConverter.convertFromVector(expect),
                                boundsValue
                            )
                            frameTime = withFrameNanos { it }
                        } while (frameTime - startTime <= 100_000_000L)
                    }
                }
            }
        }

        rule.runOnIdle { enabled = true }
        rule.waitForIdle()
    }

    @Test
    fun animateColorTest() {
        var enabled by mutableStateOf(false)
        rule.setContent {
            Box {
                val value by animateAsState(
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
                            val expected = lerp(Color.Black, Color.Cyan, fraction)
                            assertEquals(expected, value)
                            frameTime = withFrameNanos { it }
                        } while (frameTime - startTime <= 100_000_000L)
                    }
                }
            }
        }

        rule.runOnIdle { enabled = true }
        rule.waitForIdle()
    }

    @Test
    fun visibilityThresholdTest() {

        val specForFloat = FloatSpringSpec(visibilityThreshold = 0.01f)
        val specForVector = FloatSpringSpec(visibilityThreshold = 0.5f)
        val specForOffset = FloatSpringSpec(visibilityThreshold = 0.5f)
        val specForBounds = FloatSpringSpec(visibilityThreshold = 0.1f)

        var enabled by mutableStateOf(false)
        rule.setContent {
            Box {
                val vectorValue by animateAsState(
                    if (enabled) AnimationVector(100f) else AnimationVector(0f),
                    visibilityThreshold = AnimationVector(0.5f)
                )

                val offsetValue by animateAsState(
                    if (enabled)
                        Offset(100f, 100f)
                    else
                        Offset(0f, 0f)
                )

                val boundsValue by animateAsState(
                    if (enabled)
                        Bounds(100.dp, 100.dp, 100.dp, 100.dp)
                    else
                        Bounds(0.dp, 0.dp, 0.dp, 0.dp)
                )

                val floatValue by animateAsState(if (enabled) 100f else 0f)

                val durationForFloat = specForFloat.getDurationMillis(0f, 100f, 0f)
                val durationForVector = specForVector.getDurationMillis(0f, 100f, 0f)
                val durationForOffset = specForOffset.getDurationMillis(0f, 100f, 0f)
                val durationForBounds = specForBounds.getDurationMillis(0f, 100f, 0f)

                if (enabled) {
                    LaunchedEffect(Unit) {
                        val startTime = withFrameNanos { it }
                        var frameTime = startTime
                        do {
                            val playTime = (frameTime - startTime) / 1_000_000L
                            val expectFloat = specForFloat.getValue(playTime, 0f, 100f, 0f)
                            assertEquals("play time: $playTime", expectFloat, floatValue)

                            if (playTime < durationForVector) {
                                val expectVector = specForVector.getValue(playTime, 0f, 100f, 0f)
                                assertEquals(AnimationVector(expectVector), vectorValue)
                            } else {
                                assertEquals(AnimationVector(100f), vectorValue)
                            }

                            if (playTime < durationForOffset) {
                                val expectOffset = specForOffset.getValue(playTime, 0f, 100f, 0f)
                                assertEquals(Offset(expectOffset, expectOffset), offsetValue)
                            } else {
                                assertEquals(Offset(100f, 100f), offsetValue)
                            }

                            if (playTime < durationForBounds) {
                                val expectBounds = specForBounds.getValue(playTime, 0f, 100f, 0f)
                                assertEquals(
                                    Bounds(
                                        expectBounds.dp,
                                        expectBounds.dp,
                                        expectBounds.dp,
                                        expectBounds.dp
                                    ),
                                    boundsValue
                                )
                            } else {
                                assertEquals(
                                    Bounds(100.dp, 100.dp, 100.dp, 100.dp),
                                    boundsValue
                                )
                            }

                            frameTime = withFrameNanos { it }
                        } while (frameTime - startTime <= durationForFloat)
                    }
                }
            }
        }

        rule.runOnIdle { enabled = true }
        rule.waitForIdle()
    }
}
