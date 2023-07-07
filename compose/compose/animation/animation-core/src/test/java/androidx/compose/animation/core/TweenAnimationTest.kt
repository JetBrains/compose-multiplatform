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
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TweenAnimationTest {

    @Test
    fun delayCorrectness() {
        val testDelay = 100L
        val testDuration = 200
        val start = AnimationVector1D(0f)
        val end = AnimationVector1D(1000f)

        val animation = VectorizedTweenSpec<AnimationVector1D>(
            delayMillis = 100,
            durationMillis = testDuration,
            easing = LinearEasing
        )

        fun atPlaytime(playTime: Long) =
            animation.getValueFromMillis(playTime, start, end, AnimationVector1D(0f)).value

        assertThat(atPlaytime(0L)).isZero()
        assertThat(atPlaytime(testDelay / 2)).isZero()
        assertThat(atPlaytime(testDelay)).isZero()
        assertThat(atPlaytime(testDelay + 1)).isNonZero()
    }

    @Test
    fun easingIsApplied() {
        val totalDuration = 300
        val accelerateEasing: Easing = Easing { fraction -> fraction * 2f }
        val animation = VectorizedTweenSpec<AnimationVector1D>(
            durationMillis = totalDuration,
            easing = accelerateEasing
        )

        val fraction = 0.3f
        val value = animation.at((totalDuration * fraction).toLong())
        val expectedValue = accelerateEasing.transform(fraction)
        assertThat(value).isEqualTo(expectedValue)
    }

    @Test
    fun endValueCalculatedForPlaytimeOverDuration() {
        val testDuration = 200

        val animation = VectorizedTweenSpec<AnimationVector1D>(
            durationMillis = testDuration
        )

        assertThat(animation.at(testDuration + 10L)).isEqualTo(1f)
    }
}
