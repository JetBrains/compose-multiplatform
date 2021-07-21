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
import androidx.compose.animation.core.updateTransition
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.vector.VectorProperty
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
        verifyAnimator(
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
        verifyAnimator(
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
        verifyAnimator(
            sequentialAnimatorSet(
                "translateX",
                0f,
                listOf(200, 300, 200, 300),
                listOf(200f, 500f, 700f, 1000f)
            )
        )
    }

    @OptIn(InternalAnimationApi::class, ExperimentalComposeUiApi::class)
    private fun verifyAnimator(a: Animator) {
        val isAtEnd = mutableStateOf(false)
        val config = StateVectorConfig()
        rule.setContent {
            val transition = updateTransition(isAtEnd.value, label = "translateX")
            a.Configure(transition, config, 1000, 0)
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
}
