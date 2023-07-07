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

package androidx.compose.animation.graphics.vector

import androidx.compose.animation.core.InternalAnimationApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.VectorProperty
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class AnimatorTest {

    @get:Rule
    val rule = createComposeRule()

    private val tolerance = 0.01f

    private fun objectAnimator(
        propertyName: String,
        duration: Int,
        keyframes: List<Keyframe<Float>>,
        startDelay: Int = 0,
        repeatCount: Int = 0,
        repeatMode: RepeatMode = RepeatMode.Restart,
    ): ObjectAnimator {
        return ObjectAnimator(
            duration,
            startDelay,
            repeatCount,
            repeatMode,
            listOf(PropertyValuesHolderFloat(propertyName, keyframes))
        )
    }

    private fun sequentialAnimatorSet(
        propertyName: String,
        startValue: Float,
        durations: List<Int>,
        toValues: List<Float>,
        startDelay: Int = 0,
        repeatCount: Int = 0,
        repeatMode: RepeatMode = RepeatMode.Restart,
    ): AnimatorSet {
        if (durations.size != toValues.size) {
            throw RuntimeException()
        }
        val size = durations.size
        return AnimatorSet(
            (0 until size).map { i ->
                objectAnimator(
                    propertyName,
                    durations[i],
                    listOf(
                        Keyframe(0f, if (i == 0) startValue else toValues[i - 1], LinearEasing),
                        Keyframe(1f, toValues[i], LinearEasing),
                    ),
                    startDelay,
                    repeatCount,
                    repeatMode
                )
            },
            Ordering.Sequentially
        )
    }

    @Test
    fun simpleFloatProperty() {
        verifyAnimatorIsLinear(
            objectAnimator(
                "translateX",
                1000,
                listOf(
                    Keyframe(0f, 0f, LinearEasing),
                    Keyframe(1f, 1000f, LinearEasing)
                )
            )
        )
    }

    @Test
    fun keyframes() {
        verifyAnimatorIsLinear(
            objectAnimator(
                "translateX",
                1000,
                listOf(
                    Keyframe(0f, 0f, LinearEasing),
                    Keyframe(0.2f, 200f, LinearEasing),
                    Keyframe(0.5f, 500f, LinearEasing),
                    Keyframe(0.7f, 700f, LinearEasing),
                    Keyframe(1f, 1000f, LinearEasing)
                )
            )
        )
    }

    @Test
    fun sequentialAnimatorSet() {
        verifyAnimatorIsLinear(
            sequentialAnimatorSet(
                "translateX",
                0f,
                listOf(200, 300, 200, 300),
                listOf(200f, 500f, 700f, 1000f)
            )
        )
    }

    @Test
    fun offsetAnimators() {
        verifyAnimatorIsLinear(
            AnimatorSet(
                animators = listOf(
                    objectAnimator(
                        propertyName = "translateX",
                        duration = 500,
                        keyframes = listOf(
                            Keyframe(0f, 0f, LinearEasing),
                            Keyframe(1f, 500f, LinearEasing),
                        )
                    ),
                    objectAnimator(
                        propertyName = "translateX",
                        duration = 500,
                        startDelay = 500,
                        keyframes = listOf(
                            Keyframe(0f, 500f, LinearEasing),
                            Keyframe(1f, 1000f, LinearEasing),
                        )
                    )
                ),
                ordering = Ordering.Together
            )
        )
    }

    @OptIn(InternalAnimationApi::class)
    private fun verifyAnimatorIsLinear(a: Animator) {
        val isAtEnd = mutableStateOf(false)
        val config = StateVectorConfig()
        rule.setContent {
            val transition = updateTransition(isAtEnd.value, label = "translateX")
            a.Configure(transition, config, 1000)
            if (transition.isRunning) {
                assertThat(config.getOrDefault(VectorProperty.TranslateX, -1f))
                    .isWithin(tolerance)
                    .of(
                        if (transition.targetState) {
                            transition.playTimeNanos / 1000f / 1000f
                        } else {
                            1000f - transition.playTimeNanos / 1000f / 1000f
                        }
                    )
            }
        }
        assertThat(config.getOrDefault(VectorProperty.TranslateX, -1f))
            .isWithin(tolerance)
            .of(0f)
        // Start to end
        rule.runOnIdle { isAtEnd.value = true }
        rule.waitForIdle()
        assertThat(config.getOrDefault(VectorProperty.TranslateX, -1f))
            .isWithin(tolerance)
            .of(1000f)
        // End to start
        rule.runOnIdle { isAtEnd.value = false }
        rule.waitForIdle()
        assertThat(config.getOrDefault(VectorProperty.TranslateX, -1f))
            .isWithin(tolerance)
            .of(0f)
    }

    @OptIn(InternalAnimationApi::class)
    @Test
    fun pathData() {
        val a = ObjectAnimator(
            duration = 1000,
            startDelay = 0,
            repeatCount = 0,
            repeatMode = RepeatMode.Restart,
            listOf(
                PropertyValuesHolderPath(
                    propertyName = "pathData",
                    listOf(
                        Keyframe(
                            fraction = 0f,
                            value = addPathNodes("M 0 0 L 1000 0 L 1000 1000 L 0 1000 Z"),
                            interpolator = LinearEasing
                        ),
                        Keyframe(
                            fraction = 1f,
                            value = addPathNodes("M 1000 0 L 1000 0 L 1000 1000 L 0 1000 Z"),
                            interpolator = LinearEasing
                        )
                    )
                )
            )
        )
        val isAtEnd = mutableStateOf(false)
        val config = StateVectorConfig()
        rule.setContent {
            val transition = updateTransition(isAtEnd.value, label = "pathData")
            a.Configure(transition, config, 1000)
            if (transition.isRunning) {
                val timeMillis = transition.playTimeNanos / 1000f / 1000f
                val pathData = config.getOrDefault(VectorProperty.PathData, emptyList())
                assertThat((pathData[0] as PathNode.MoveTo).x)
                    .isWithin(tolerance)
                    .of(if (transition.targetState) timeMillis else 1000f - timeMillis)
            }
        }
        assertThat(config.getOrDefault(VectorProperty.PathData, emptyList()))
            .isEqualTo(addPathNodes("M 0 0 L 1000 0 L 1000 1000 L 0 1000 Z"))
        // Start to end
        rule.runOnIdle { isAtEnd.value = true }
        rule.waitForIdle()
        assertThat(config.getOrDefault(VectorProperty.PathData, emptyList()))
            .isEqualTo(addPathNodes("M 1000 0 L 1000 0 L 1000 1000 L 0 1000 Z"))
        // End to start
        rule.runOnIdle { isAtEnd.value = false }
        rule.waitForIdle()
        assertThat(config.getOrDefault(VectorProperty.PathData, emptyList()))
            .isEqualTo(addPathNodes("M 0 0 L 1000 0 L 1000 1000 L 0 1000 Z"))
    }

    @OptIn(InternalAnimationApi::class)
    @Test
    fun pathData_repeat() {
        val a = ObjectAnimator(
            duration = 1000,
            startDelay = 0,
            repeatCount = 2,
            repeatMode = RepeatMode.Restart,
            listOf(
                PropertyValuesHolderPath(
                    propertyName = "pathData",
                    listOf(
                        Keyframe(
                            fraction = 0f,
                            value = addPathNodes("M 0 0 L 1000 0 L 1000 1000 L 0 1000 Z"),
                            interpolator = LinearEasing
                        ),
                        Keyframe(
                            fraction = 1f,
                            value = addPathNodes("M 1000 0 L 1000 0 L 1000 1000 L 0 1000 Z"),
                            interpolator = LinearEasing
                        )
                    )
                )
            )
        )
        assertThat(a.totalDuration).isEqualTo(3000)
        val isAtEnd = mutableStateOf(false)
        val config = StateVectorConfig()
        var hasRun = false
        rule.setContent {
            val transition = updateTransition(isAtEnd.value, label = "pathData")
            a.Configure(transition, config, 3000)
            if (transition.isRunning) {
                val timeMillis = transition.playTimeNanos / 1000f / 1000f
                if (timeMillis > 1000f) {
                    hasRun = true
                }
                val value = timeMillis % 1000f
                val pathData = config.getOrDefault(VectorProperty.PathData, emptyList())
                if (value != 0f && value != 1000f) {
                    assertThat((pathData[0] as PathNode.MoveTo).x)
                        .isWithin(tolerance)
                        .of(if (transition.targetState) value else 1000f - value)
                }
            }
        }
        assertThat(config.getOrDefault(VectorProperty.PathData, emptyList()))
            .isEqualTo(addPathNodes("M 0 0 L 1000 0 L 1000 1000 L 0 1000 Z"))
        // Start to end
        rule.runOnIdle { isAtEnd.value = true }
        rule.waitForIdle()
        assertThat(config.getOrDefault(VectorProperty.PathData, emptyList()))
            .isEqualTo(addPathNodes("M 1000 0 L 1000 0 L 1000 1000 L 0 1000 Z"))
        assertThat(hasRun).isTrue()
        // End to start
        rule.runOnIdle { isAtEnd.value = false }
        rule.waitForIdle()
        assertThat(config.getOrDefault(VectorProperty.PathData, emptyList()))
            .isEqualTo(addPathNodes("M 0 0 L 1000 0 L 1000 1000 L 0 1000 Z"))
    }

    @Test
    fun startDelay() {
        val a = objectAnimator(
            propertyName = "trimPathEnd",
            duration = 400,
            keyframes = listOf(
                Keyframe(fraction = 0f, value = 0f, interpolator = LinearEasing),
                Keyframe(fraction = 1f, value = 1f, interpolator = LinearEasing),
            ),
            startDelay = 100
        )
        val isAtEnd = mutableStateOf(false)
        val config = StateVectorConfig()
        rule.setContent {
            val transition = updateTransition(isAtEnd.value, label = "startDelay")
            val control = transition.animateFloat(
                label = "control",
                transitionSpec = {
                    if (targetState) {
                        tween(durationMillis = 400, delayMillis = 100, easing = LinearEasing)
                    } else {
                        tween(durationMillis = 400, easing = LinearEasing)
                    }
                }
            ) { if (it) 1f else 0f }
            a.Configure(transition, config, 500)
            if (transition.isRunning) {
                val trimPathEnd = config.getOrDefault(VectorProperty.TrimPathEnd, -1f)
                assertThat(trimPathEnd).isWithin(tolerance).of(control.value)
            }
        }
        assertThat(config.getOrDefault(VectorProperty.TrimPathEnd, -1f)).isEqualTo(0f)
        rule.runOnIdle { isAtEnd.value = true }
        rule.waitForIdle()
        assertThat(config.getOrDefault(VectorProperty.TrimPathEnd, -1f)).isEqualTo(1f)
        rule.runOnIdle { isAtEnd.value = false }
        rule.waitForIdle()
        assertThat(config.getOrDefault(VectorProperty.TrimPathEnd, -1f)).isEqualTo(0f)
    }

    @Test
    fun repeat_restart() {
        val a = objectAnimator(
            propertyName = "translateX",
            duration = 500,
            keyframes = listOf(
                Keyframe(fraction = 0f, value = 0f, interpolator = LinearEasing),
                Keyframe(fraction = 1f, value = 500f, interpolator = LinearEasing),
            ),
            repeatCount = 3
        )
        assertThat(a.totalDuration).isEqualTo(2000)
        val isAtEnd = mutableStateOf(false)
        val config = StateVectorConfig()
        rule.setContent {
            val transition = updateTransition(isAtEnd.value, label = "translateX")
            val control = transition.animateFloat(
                label = "control",
                transitionSpec = {
                    repeatable(iterations = 4, tween(durationMillis = 500, easing = LinearEasing))
                }
            ) {
                if (it) 500f else 0f
            }
            a.Configure(transition, config, overallDuration = a.totalDuration)
            if (transition.isRunning) {
                val translateX = config.getOrDefault(VectorProperty.TranslateX, -1f)
                assertThat(translateX).isWithin(tolerance).of(control.value)
            }
        }
        assertThat(config.getOrDefault(VectorProperty.TranslateX, -1f)).isEqualTo(0f)
        rule.runOnIdle { isAtEnd.value = true }
        rule.waitForIdle()
        assertThat(config.getOrDefault(VectorProperty.TranslateX, -1f)).isEqualTo(500f)
    }

    @Test
    fun repeat_reverse() {
        val a = objectAnimator(
            propertyName = "translateX",
            duration = 500,
            keyframes = listOf(
                Keyframe(fraction = 0f, value = 0f, interpolator = LinearEasing),
                Keyframe(fraction = 1f, value = 500f, interpolator = LinearEasing),
            ),
            repeatCount = 3,
            repeatMode = RepeatMode.Reverse
        )
        assertThat(a.totalDuration).isEqualTo(2000)
        val isAtEnd = mutableStateOf(false)
        val config = StateVectorConfig()
        rule.setContent {
            val transition = updateTransition(isAtEnd.value, label = "translateX")
            val control = transition.animateFloat(
                label = "control",
                transitionSpec = {
                    repeatable(
                        iterations = 4,
                        animation = tween(durationMillis = 500, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                }
            ) {
                if (it) 500f else 0f
            }
            a.Configure(transition, config, overallDuration = a.totalDuration)
            if (transition.isRunning) {
                val translateX = config.getOrDefault(VectorProperty.TranslateX, -1f)
                assertThat(translateX).isWithin(tolerance).of(control.value)
            }
        }
        assertThat(config.getOrDefault(VectorProperty.TranslateX, -1f)).isEqualTo(0f)
        rule.runOnIdle { isAtEnd.value = true }
        rule.waitForIdle()
        assertThat(config.getOrDefault(VectorProperty.TranslateX, -1f)).isEqualTo(500f)
    }

    @Test
    fun repeat_infinite() {
        val a = objectAnimator(
            propertyName = "translateX",
            duration = 500,
            keyframes = listOf(
                Keyframe(fraction = 0f, value = 0f, interpolator = LinearEasing),
                Keyframe(fraction = 1f, value = 500f, interpolator = LinearEasing),
            ),
            repeatCount = RepeatCountInfinite,
            repeatMode = RepeatMode.Reverse
        )
        assertThat(a.totalDuration).isEqualTo(Int.MAX_VALUE)
        val isAtEnd = mutableStateOf(false)
        val config = StateVectorConfig()
        rule.setContent {
            val transition = updateTransition(isAtEnd.value, label = "translateX")
            val control = transition.animateFloat(
                label = "control",
                transitionSpec = {
                    repeatable(
                        iterations = Int.MAX_VALUE,
                        animation = tween(durationMillis = 500, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                }
            ) {
                if (it) 500f else 0f
            }
            a.Configure(transition, config, overallDuration = a.totalDuration)
            if (transition.isRunning) {
                val translateX = config.getOrDefault(VectorProperty.TranslateX, -1f)
                assertThat(translateX).isWithin(tolerance).of(control.value)
            }
        }
        assertThat(config.getOrDefault(VectorProperty.TranslateX, -1f)).isEqualTo(0f)
        rule.runOnIdle { isAtEnd.value = true }
        // Run for 300 frames.
        repeat(300) {
            rule.mainClock.advanceTimeBy(16)
        }
    }
}
