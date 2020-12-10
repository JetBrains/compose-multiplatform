/*
 * Copyright 2019 The Android Open Source Project
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

import com.google.common.truth.Truth.assertThat
import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RepeatableAnimationTest {

    private val Animation = TweenSpec<AnimationVector1D>(durationMillis = Duration)

    private val DelayedAnimation = VectorizedTweenSpec<AnimationVector1D>(
        delayMillis = DelayDuration,
        durationMillis = Duration
    )

    @Test
    fun twoRepeatsValuesCalculation() {
        val repeat = VectorizedRepeatableSpec(
            iterations = 2,
            animation = VectorizedTweenSpec<AnimationVector1D>(durationMillis = Duration)
        )

        val animationWrapper = TargetBasedAnimation(
            repeat,
            0f,
            0f,
            Float.VectorConverter
        )

        assertThat(repeat.at(0)).isEqualTo(0f)
        assertThat(repeat.at(Duration - 1)).isGreaterThan(0.9f)
        assertThat(repeat.at(Duration + 1)).isLessThan(0.1f)
        assertThat(repeat.at(Duration * 2 - 1)).isGreaterThan(0.9f)
        assertThat(repeat.at(Duration * 2)).isEqualTo(1f)
        assertThat(animationWrapper.isFinished(Duration * 2L - 1L)).isFalse()
        assertThat(animationWrapper.isFinished(Duration * 2L)).isTrue()
    }

    @Test
    fun testRepeatedAnimationDuration() {
        val iters = 5
        val repeat = VectorizedRepeatableSpec<AnimationVector1D>(
            iterations = iters,
            animation = DelayedAnimation
        )

        val duration = repeat.getDurationMillis(
            AnimationVector1D(0f),
            AnimationVector1D(0f),
            AnimationVector1D(0f)
        )

        assertEquals((DelayDuration + Duration) * iters.toLong(), duration)
    }

    @Test
    fun testRepeatModeReverse() {
        val repeat = repeatable(
            iterations = 9,
            animation = TweenSpec<Float>(
                durationMillis = 100, easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        )

        val repeatAnim = TargetBasedAnimation(
            repeat,
            0f,
            100f,
            Float.VectorConverter
        )

        for (playtime in 0..100L) {
            assertEquals(playtime.toFloat(), repeatAnim.getValue(playtime), 0.01f)
        }
        for (playtime in 100..200L) {
            assertEquals(200f - playtime.toFloat(), repeatAnim.getValue(playtime), 0.01f)
        }
        assertEquals(100f, repeatAnim.getValue(100))
        assertEquals(99f, repeatAnim.getValue(101))
        assertEquals(0f, repeatAnim.getValue(200))
        assertEquals(100f, repeatAnim.getValue(300))
        assertEquals(80f, repeatAnim.getValue(880))
        assertEquals(100f, repeatAnim.getValue(900))
        assertEquals(100f, repeatAnim.getValue(901))
    }

    @Test
    fun testInfiniteRepeat() {
        val repeat = infiniteRepeatable(
            animation = TweenSpec<Float>(
                durationMillis = 100, easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        )

        assertEquals(
            Int.MAX_VALUE.toLong() * 100,
            repeat.vectorize(Float.VectorConverter).getDurationMillis(
                AnimationVector(0f),
                AnimationVector(100f),
                AnimationVector(0f)
            )
        )
    }

    private companion object {
        private val DelayDuration = 13
        private val Duration = 50
    }
}