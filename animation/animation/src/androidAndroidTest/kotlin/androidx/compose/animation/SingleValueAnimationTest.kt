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

package androidx.compose.animation

import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FloatSpringSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.dispatch.withFrameNanos
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.test.ExperimentalTesting
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Bounds
import androidx.compose.ui.unit.Position
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
@OptIn(ExperimentalTesting::class)
class SingleValueAnimationTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun animate1DTest() {
        val startVal = 20f
        val endVal = 250f

        var dpValue = startVal.dp

        fun <T> tween(): TweenSpec<T> =
            TweenSpec(
                easing = FastOutSlowInEasing,
                durationMillis = 100
            )

        val content: @Composable (Boolean) -> Unit = { enabled ->
            dpValue = animate(
                if (enabled) endVal.dp else startVal.dp,
                tween()
            )
        }

        val verify: () -> Unit = {
            for (i in 0..100 step 50) {
                val value = lerp(
                    startVal, endVal,
                    FastOutSlowInEasing.invoke(i / 100f)
                )
                assertEquals(value.dp, dpValue)
                rule.clockTestRule.advanceClock(50)
                rule.waitForIdle()
            }
        }

        animateTest(content, verify)
    }

    @Test
    fun animate1DOnCoroutineTest() {
        var enabled by mutableStateOf(false)
        rule.setContent {
            Box {
                var animationValue by remember { mutableStateOf(250f) }
                // Animate from 250f to 50f when enable flips to true
                animationValue = animate(
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
                            val fraction = FastOutLinearInEasing.invoke(playTime / 200f)
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

        var vectorValue = startVal
        var positionValue = Position.VectorConverter.convertFromVector(startVal)
        var sizeValue = Size.VectorConverter.convertFromVector(startVal)
        var pxPositionValue = Offset.VectorConverter.convertFromVector(startVal)

        fun <V> tween(): TweenSpec<V> =
            TweenSpec(
                easing = LinearEasing,
                durationMillis = 100
            )

        val content: @Composable (Boolean) -> Unit = { enabled ->
            vectorValue = animate(
                if (enabled) endVal else startVal,
                tween()
            )

            positionValue = animate(
                if (enabled)
                    Position.VectorConverter.convertFromVector(endVal)
                else
                    Position.VectorConverter.convertFromVector(startVal),
                tween()
            )

            sizeValue = animate(
                if (enabled)
                    Size.VectorConverter.convertFromVector(endVal)
                else
                    Size.VectorConverter.convertFromVector(startVal),
                tween()
            )

            pxPositionValue = animate(
                if (enabled)
                    Offset.VectorConverter.convertFromVector(endVal)
                else
                    Offset.VectorConverter.convertFromVector(startVal),
                tween()
            )
        }

        val verify: () -> Unit = {
            for (i in 0..100 step 50) {
                val expect = AnimationVector(
                    lerp(startVal.v1, endVal.v1, i / 100f),
                    lerp(startVal.v2, endVal.v2, i / 100f)
                )

                assertEquals(expect, vectorValue)
                assertEquals(Size.VectorConverter.convertFromVector(expect), sizeValue)
                assertEquals(Position.VectorConverter.convertFromVector(expect), positionValue)
                assertEquals(Offset.VectorConverter.convertFromVector(expect), pxPositionValue)
                rule.clockTestRule.advanceClock(50)
                rule.waitForIdle()
            }
        }

        animateTest(content, verify)
    }

    @Test
    fun animate4DRectTest() {
        val startVal = AnimationVector(30f, -76f, 280f, 35f)
        val endVal = AnimationVector(-42f, 89f, 77f, 100f)

        var vectorValue = startVal
        var boundsValue = Bounds.VectorConverter.convertFromVector(startVal)
        var pxBoundsValue = Rect.VectorConverter.convertFromVector(startVal)

        fun <V> tween(): TweenSpec<V> =
            TweenSpec(
                easing = LinearOutSlowInEasing,
                durationMillis = 100
            )

        val content: @Composable (Boolean) -> Unit = { enabled ->
            vectorValue = animate(
                if (enabled) endVal else startVal,
                tween()
            )

            boundsValue = animate(
                if (enabled)
                    Bounds.VectorConverter.convertFromVector(endVal)
                else
                    Bounds.VectorConverter.convertFromVector(startVal),
                tween()
            )

            pxBoundsValue = animate(
                if (enabled)
                    Rect.VectorConverter.convertFromVector(endVal)
                else
                    Rect.VectorConverter.convertFromVector(startVal),
                tween()
            )
        }

        val verify: () -> Unit = {
            for (i in 0..100 step 50) {
                val fraction = LinearOutSlowInEasing.invoke(i / 100f)
                val expect = AnimationVector(
                    lerp(startVal.v1, endVal.v1, fraction),
                    lerp(startVal.v2, endVal.v2, fraction),
                    lerp(startVal.v3, endVal.v3, fraction),
                    lerp(startVal.v4, endVal.v4, fraction)
                )

                assertEquals(expect, vectorValue)
                assertEquals(Bounds.VectorConverter.convertFromVector(expect), boundsValue)
                assertEquals(Rect.VectorConverter.convertFromVector(expect), pxBoundsValue)
                rule.clockTestRule.advanceClock(50)
                rule.waitForIdle()
            }
        }

        animateTest(content, verify)
    }

    @Suppress("DEPRECATION")
    @Test
    fun animate4DTest() {
        val startVal = AnimationVector(30f, -76f, 280f, 35f)
        val endVal = AnimationVector(-42f, 89f, 77f, 100f)

        var vectorValue = startVal
        var boundsValue = Bounds.VectorConverter.convertFromVector(startVal)

        fun <V> tween(): TweenSpec<V> =
            TweenSpec(
                easing = LinearOutSlowInEasing,
                durationMillis = 100
            )

        val content: @Composable (Boolean) -> Unit = { enabled ->
            vectorValue = animate(
                if (enabled) endVal else startVal,
                tween()
            )

            boundsValue = animate(
                if (enabled)
                    Bounds.VectorConverter.convertFromVector(endVal)
                else
                    Bounds.VectorConverter.convertFromVector(startVal),
                tween()
            )
        }

        val verify: () -> Unit = {
            for (i in 0..100 step 50) {
                val fraction = LinearOutSlowInEasing.invoke(i / 100f)
                val expect = AnimationVector(
                    lerp(startVal.v1, endVal.v1, fraction),
                    lerp(startVal.v2, endVal.v2, fraction),
                    lerp(startVal.v3, endVal.v3, fraction),
                    lerp(startVal.v4, endVal.v4, fraction)
                )

                assertEquals(expect, vectorValue)
                assertEquals(Bounds.VectorConverter.convertFromVector(expect), boundsValue)
                rule.clockTestRule.advanceClock(50)
                rule.waitForIdle()
            }
        }

        animateTest(content, verify)
    }

    @Test
    fun animateColorTest() {
        var value = Color.Black
        val content: @Composable (Boolean) -> Unit = { enabled ->
            value = animate(
                if (enabled) Color.Cyan else Color.Black,
                TweenSpec(
                    durationMillis = 100,
                    easing = FastOutLinearInEasing
                )
            )
        }

        val verify: () -> Unit = {

            for (i in 0..100 step 50) {
                val fraction = FastOutLinearInEasing.invoke(i / 100f)
                val expected = lerp(Color.Black, Color.Cyan, fraction)
                assertEquals(expected, value)
                rule.clockTestRule.advanceClock(50)
                rule.waitForIdle()
            }
        }

        animateTest(content, verify)
    }

    @Test
    fun visibilityThresholdTest() {

        var vectorValue = AnimationVector(0f)
        var offsetValue = Offset(0f, 0f)
        var boundsValue = Bounds(0.dp, 0.dp, 0.dp, 0.dp)

        val specForFloat = FloatSpringSpec(visibilityThreshold = 0.01f)
        val specForVector = FloatSpringSpec(visibilityThreshold = 0.5f)
        val specForOffset = FloatSpringSpec(visibilityThreshold = 0.5f)
        val specForBounds = FloatSpringSpec(visibilityThreshold = 0.1f)

        val content: @Composable (Boolean) -> Unit = { enabled ->
            vectorValue = animate(
                if (enabled) AnimationVector(100f) else AnimationVector(0f),
                visibilityThreshold = AnimationVector(0.5f)
            )

            offsetValue = animate(
                if (enabled)
                    Offset(100f, 100f)
                else
                    Offset(0f, 0f)
            )

            boundsValue = animate(
                if (enabled)
                    Bounds(100.dp, 100.dp, 100.dp, 100.dp)
                else
                    Bounds(0.dp, 0.dp, 0.dp, 0.dp)
            )
        }

        val durationForFloat = specForFloat.getDurationMillis(0f, 100f, 0f)
        val durationForVector = specForVector.getDurationMillis(0f, 100f, 0f)
        val durationForOffset = specForOffset.getDurationMillis(0f, 100f, 0f)
        val durationForBounds = specForBounds.getDurationMillis(0f, 100f, 0f)
        val verify: () -> Unit = {
            for (i in 0..durationForFloat step 50) {

                if (i < durationForVector) {
                    val expectVector = specForVector.getValue(i, 0f, 100f, 0f)
                    assertEquals(AnimationVector(expectVector), vectorValue)
                } else {
                    assertEquals(AnimationVector(100f), vectorValue)
                }

                if (i < durationForOffset) {
                    val expectOffset = specForOffset.getValue(i, 0f, 100f, 0f)
                    assertEquals(Offset(expectOffset, expectOffset), offsetValue)
                } else {
                    assertEquals(Offset(100f, 100f), offsetValue)
                }

                if (i < durationForBounds) {
                    val expectBounds = specForBounds.getValue(i, 0f, 100f, 0f)
                    assertEquals(
                        Bounds(expectBounds.dp, expectBounds.dp, expectBounds.dp, expectBounds.dp),
                        boundsValue
                    )
                } else {
                    assertEquals(Bounds(100.dp, 100.dp, 100.dp, 100.dp), boundsValue)
                }

                rule.clockTestRule.advanceClock(50)
                rule.waitForIdle()
            }
        }

        animateTest(content, verify)
    }

    private fun animateTest(content: @Composable (Boolean) -> Unit, verify: () -> Unit) {

        rule.clockTestRule.pauseClock()
        var enabled by mutableStateOf(false)
        rule.setContent {
            Box {
                content(enabled)
            }
        }
        rule.runOnIdle { enabled = true }
        rule.waitForIdle()

        verify()
    }
}
