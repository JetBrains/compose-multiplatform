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

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

const val epsilon = 0.00001f

@RunWith(JUnit4::class)
class DecayAnimationTest {

    @Test
    fun testExponentialDecay() {
        val anim = FloatExponentialDecaySpec(absVelocityThreshold = 2.0f)
        val startValue = 200f
        val startVelocity = -800f

        val animWrapper = anim.createAnimation(startValue, startVelocity)
        // Obtain finish value by passing in an absurdly large playtime.
        val finishValue = animWrapper.getValueFromNanos(Int.MAX_VALUE.toLong())
        val finishTimeNanos = animWrapper.durationMillis * MillisToNanos

        for (playTimeMillis in 0L..4000L step 200L) {
            val playTimeNanos = playTimeMillis * MillisToNanos
            val value = anim.getValueFromNanos(playTimeNanos, startValue, startVelocity)
            val velocity = anim.getVelocityFromNanos(playTimeNanos, startValue, startVelocity)
            val finished = playTimeNanos >= finishTimeNanos
            assertTrue(finished == animWrapper.isFinishedFromNanos(playTimeNanos))

            if (!finished) {
                // Before the animation finishes, absolute velocity is above the threshold
                assertTrue(Math.abs(velocity) >= 2.0f)
                assertEquals(value, animWrapper.getValueFromNanos(playTimeNanos), epsilon)
                assertEquals(
                    velocity,
                    animWrapper.getVelocityVectorFromNanos(playTimeNanos).value,
                    epsilon
                )
                assertTrue(playTimeNanos < finishTimeNanos)
            } else {
                // When the animation is finished, expect absolute velocity < threshold
                assertTrue(Math.abs(velocity) < 2.0f)

                // Once the animation is finished, the value should not change any more
                assertEquals(finishValue, animWrapper.getValueFromNanos(playTimeNanos), epsilon)

                assertTrue(playTimeNanos >= finishTimeNanos)
            }
        }
    }

    /**
     * This test verifies that the velocity threshold is stopping the animation at the right value
     * when velocity reaches that threshold.
     */
    @Test
    fun testDecayThreshold() {
        // TODO: Use parameterized tests
        val threshold = 500f
        val anim1 = FloatExponentialDecaySpec(absVelocityThreshold = threshold)
        val anim2 = FloatExponentialDecaySpec(absVelocityThreshold = 0f)

        val startValue = 2000f
        val startVelocity = 800f
        val fullAnim = FloatExponentialDecaySpec(absVelocityThreshold = 0f).createAnimation(
            startValue,
            startVelocity
        )

        val finishValue = fullAnim.getValueFromNanos(Int.MAX_VALUE.toLong())

        val finishValue1 = anim1.createAnimation(startValue, startVelocity)
            .getValueFromNanos(Int.MAX_VALUE.toLong())

        val finishVelocity1 = anim1.createAnimation(startValue, startVelocity)
            .getVelocityVectorFromNanos(Int.MAX_VALUE.toLong()).value

        // Verify that the finish velocity is at the threshold
        assertEquals(threshold, finishVelocity1, epsilon)

        // Feed in the finish value from anim1 to anim2
        val finishValue2 = anim2.createAnimation(finishValue1, finishVelocity1)
            .getValueFromNanos(Int.MAX_VALUE.toLong())

        assertEquals(finishValue, finishValue2, 2f)
    }
}
