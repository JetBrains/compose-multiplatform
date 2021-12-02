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
import junit.framework.TestCase.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RepeatableAnimationTest {

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
            Float.VectorConverter,
            0f,
            0f
        )

        assertThat(repeat.at(0)).isEqualTo(0f)
        assertThat(repeat.at(Duration - 1)).isGreaterThan(0.9f)
        assertThat(repeat.at(Duration + 1)).isLessThan(0.1f)
        assertThat(repeat.at(Duration * 2 - 1)).isGreaterThan(0.9f)
        assertThat(repeat.at(Duration * 2)).isEqualTo(1f)
        assertThat(animationWrapper.isFinishedFromMillis(Duration * 2L - 1L)).isFalse()
        assertThat(animationWrapper.isFinishedFromMillis(Duration * 2L)).isTrue()
    }

    @Test
    fun testRepeatedAnimationDuration() {
        val iters = 5
        val repeat = VectorizedRepeatableSpec<AnimationVector1D>(
            iterations = iters,
            animation = DelayedAnimation
        )

        val duration = repeat.getDurationNanos(
            AnimationVector1D(0f),
            AnimationVector1D(0f),
            AnimationVector1D(0f)
        )

        assertEquals((DelayDuration + Duration) * iters * MillisToNanos, duration)
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
            Float.VectorConverter,
            0f,
            100f
        )

        for (playtime in 0..100L) {
            assertEquals(playtime.toFloat(), repeatAnim.getValueFromMillis(playtime), 0.01f)
        }
        for (playtime in 100..200L) {
            assertEquals(200f - playtime.toFloat(), repeatAnim.getValueFromMillis(playtime), 0.01f)
        }
        assertEquals(100f, repeatAnim.getValueFromMillis(100))
        assertEquals(99f, repeatAnim.getValueFromMillis(101))
        assertEquals(0f, repeatAnim.getValueFromMillis(200))
        assertEquals(100f, repeatAnim.getValueFromMillis(300))
        assertEquals(80f, repeatAnim.getValueFromMillis(880))
        assertEquals(100f, repeatAnim.getValueFromMillis(900))
        assertEquals(100f, repeatAnim.getValueFromMillis(901))
    }

    @Test
    fun testInfiniteRepeat() {
        val repeatShortAnimation = infiniteRepeatable(
            animation = TweenSpec<Float>(
                durationMillis = 100, easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        )

        val extraLongDurationNanos = 1000000000
        val repeatLongAnimation = infiniteRepeatable(
            animation = TweenSpec<Float>(
                durationMillis = extraLongDurationNanos, easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        )
        val vectorizedInfiniteRepeatingShort = repeatShortAnimation.vectorize(Float.VectorConverter)
        val vectorizedInfiniteRepeatingLong = repeatLongAnimation.vectorize(Float.VectorConverter)

        assertEquals(
            Long.MAX_VALUE,
            vectorizedInfiniteRepeatingShort
                .getDurationNanos(
                    AnimationVector(0f),
                    AnimationVector(100f),
                    AnimationVector(0f)
                )
        )

        assertEquals(
            Long.MAX_VALUE,
            vectorizedInfiniteRepeatingLong
                .getDurationNanos(
                    AnimationVector(0f),
                    AnimationVector(100f),
                    AnimationVector(0f)
                )
        )

        val repeatShort = TargetBasedAnimation(
            repeatShortAnimation,
            Float.VectorConverter,
            0f,
            100f
        )
        val repeatLong = TargetBasedAnimation(
            repeatLongAnimation,
            Float.VectorConverter,
            0f,
            extraLongDurationNanos.toFloat()
        )

        assertEquals(repeatShort.durationNanos, Long.MAX_VALUE)
        assertEquals(repeatLong.durationNanos, Long.MAX_VALUE)
        assertFalse(repeatShort.isFinishedFromNanos(100000000000000000L))
        assertFalse(repeatShort.isFinishedFromNanos(100000000000000000L))

        // Also check on repeating value. Repeat mode: reverse
        assertEquals(31f, repeatShort.getValueFromNanos(31 * MillisToNanos))
        assertEquals(67f, repeatShort.getValueFromNanos(133 * MillisToNanos))

        // Also check on repeating value. Repeat mode: restart
        assertEquals(31f, repeatLong.getValueFromNanos(31 * MillisToNanos), 0.1f)
        assertEquals(
            31f,
            repeatLong.getValueFromNanos((extraLongDurationNanos + 31) * MillisToNanos),
            0.1f
        )
    }

    @Test
    fun testStartOffsetRepeatable() {
        val duration = 600
        val offset = duration / 2

        val repeatable = TargetBasedAnimation(
            repeatable<Float>(5, tween(duration, easing = LinearEasing), RepeatMode.Restart),
            Float.VectorConverter,
            0f,
            1000f
        )
        val delayedRepeatable = TargetBasedAnimation(
            repeatable<Float>(
                5,
                tween(duration, easing = LinearEasing),
                RepeatMode.Restart, StartOffset(offset)
            ),
            Float.VectorConverter,
            0f,
            1000f
        )
        val fastForwardedRepeatable = TargetBasedAnimation(
            repeatable<Float>(
                5,
                tween(duration, easing = LinearEasing),
                RepeatMode.Restart, StartOffset(offset, StartOffsetType.FastForward)
            ),
            Float.VectorConverter,
            0f,
            1000f
        )

        assertEquals(
            repeatable.durationNanos, delayedRepeatable.durationNanos - offset * 1_000_000L
        )
        assertEquals(
            repeatable.durationNanos, fastForwardedRepeatable.durationNanos + offset * 1_000_000L
        )

        for (playtimeMillis in 0..duration * 3 step 17) {
            assertEquals(
                repeatable.getValueFromNanos(playtimeMillis * MillisToNanos),
                delayedRepeatable.getValueFromNanos((playtimeMillis + offset) * MillisToNanos)
            )
        }

        // Check that during the delayed time, the value is unchanged
        for (playTimeMillis in 0..offset step 10) {
            assertEquals(
                repeatable.getValueFromNanos(0),
                delayedRepeatable.getValueFromNanos(playTimeMillis * MillisToNanos)
            )
        }

        for (playtimeMillis in 0..duration * 3 step 17) {
            assertEquals(
                repeatable.getValueFromNanos(playtimeMillis * MillisToNanos),
                fastForwardedRepeatable.getValueFromNanos((playtimeMillis - offset) * MillisToNanos)
            )
        }
    }

    @Test
    fun testStartOffsetInfiniteRepeatable() {
        val duration = 600
        val offset = 31

        val repeatable = TargetBasedAnimation(
            infiniteRepeatable(tween(duration), RepeatMode.Restart),
            Float.VectorConverter,
            0f,
            1000f
        )
        val delayedRepeatable = TargetBasedAnimation(
            infiniteRepeatable(tween(duration), RepeatMode.Restart, StartOffset(offset)),
            Float.VectorConverter,
            0f,
            1000f
        )
        val fastForwardedRepeatable = TargetBasedAnimation(
            infiniteRepeatable(
                tween(duration),
                RepeatMode.Restart, StartOffset(offset, StartOffsetType.FastForward)
            ),
            Float.VectorConverter,
            0f,
            1000f
        )

        // Duration should be infinite for delay or fast forward
        assertEquals(repeatable.durationNanos, delayedRepeatable.durationNanos)
        assertEquals(repeatable.durationNanos, fastForwardedRepeatable.durationNanos)

        for (playtimeMillis in 0..duration * 3 step 17) {
            assertEquals(
                repeatable.getValueFromNanos(playtimeMillis * MillisToNanos),
                delayedRepeatable.getValueFromNanos((playtimeMillis + offset) * MillisToNanos)
            )
        }

        // Check that during the delayed time, the value is unchanged
        for (playTimeMillis in 0..offset step 10) {
            assertEquals(
                repeatable.getValueFromNanos(0),
                delayedRepeatable.getValueFromNanos(playTimeMillis * MillisToNanos)
            )
        }

        for (playtimeMillis in 0..duration * 3 step 17) {
            assertEquals(
                repeatable.getValueFromNanos(playtimeMillis * MillisToNanos),
                fastForwardedRepeatable.getValueFromNanos((playtimeMillis - offset) * MillisToNanos)
            )
        }
    }

    private companion object {
        private val DelayDuration = 13
        private val Duration = 50
    }
}