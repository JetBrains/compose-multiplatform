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
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AnimatedValueTest {
    private lateinit var clock: ManualAnimationClock

    @Before
    fun init() {
        clock = ManualAnimationClock(0L)
    }

    @Test
    fun testAnimation() {
        val animatedFloat = AnimatedFloat(0f, clock)
        animatedFloat.animateTo(1f)
        clock.clockTimeMillis += 100000L
        assertEquals(1f, animatedFloat.value, epsilon)
    }

    @Test
    fun testInitialValue() {
        val animatedFloat = AnimatedFloat(1.2f, clock)
        animatedFloat.animateTo(100f)
        assertEquals(1.2f, animatedFloat.value, epsilon)
    }

    @Test
    fun testTargetValue() {
        val animatedFloat = AnimatedFloat(0f, clock)
        animatedFloat.animateTo(2f)
        animatedFloat.animateTo(1.5f)
        assertEquals(1.5f, animatedFloat.targetValue, epsilon)
    }

    /**
     * Test setBounds(Float, Float), min, max, and end reason.
     */
    @Test
    fun testSetBounds() {
        val animatedFloat = AnimatedFloat(0f, clock)
        assertEquals(Float.NEGATIVE_INFINITY, animatedFloat.min)
        assertEquals(Float.POSITIVE_INFINITY, animatedFloat.max)
        animatedFloat.setBounds(-4f, 4f)
        assertEquals(-4f, animatedFloat.min)
        assertEquals(4f, animatedFloat.max)
        var reason = AnimationEndReason.Interrupted

        // Animate beyond upper bound
        animatedFloat.animateTo(5f) { endReason: AnimationEndReason, _: Float ->
            reason = endReason
        }
        clock.clockTimeMillis += 10000L
        assertEquals(4f, animatedFloat.value)
        assertEquals(AnimationEndReason.BoundReached, reason)

        // Now animate beyond lower bound
        reason = AnimationEndReason.Interrupted
        animatedFloat.animateTo(-500f) { endReason: AnimationEndReason, _: Float ->
            reason = endReason
        }
        clock.clockTimeMillis += 10000L
        assertEquals(-4f, animatedFloat.value)
        assertEquals(reason, AnimationEndReason.BoundReached)

        // Change bounds, and check that value is clamped
        val expected = 5f
        animatedFloat.setBounds(expected, expected + 10f)
        assertEquals(expected, animatedFloat.value)

        // Start an animation and set bounds
        reason = AnimationEndReason.TargetReached
        animatedFloat.animateTo(expected + 10) { endReason: AnimationEndReason, _: Float ->
            reason = endReason
        }
        clock.clockTimeMillis += 5L
        assertTrue(animatedFloat.value > expected)
        animatedFloat.setBounds(-expected, expected)
        assertEquals(expected, animatedFloat.value)
        assertEquals(AnimationEndReason.BoundReached, reason)
    }

    @Test
    fun testProgressionUp() {
        val animatedFloat = AnimatedFloat(0f, clock)

        animatedFloat.animateTo(1f)

        clock.clockTimeMillis = 0L
        val valueAtTimeZero = animatedFloat.value

        clock.clockTimeMillis = 1L
        val valueAtTimeOne = animatedFloat.value

        assertTrue(valueAtTimeOne > valueAtTimeZero)
    }

    @Test
    fun testProgressionDown() {
        val animatedFloat = AnimatedFloat(0f, clock)

        animatedFloat.animateTo(-1f)

        clock.clockTimeMillis = 0L
        val valueAtTimeZero = animatedFloat.value

        clock.clockTimeMillis = 1L
        val valueAtTimeOne = animatedFloat.value

        assertTrue(valueAtTimeOne < valueAtTimeZero)
    }

    @Test
    fun testEndCallback() {
        val animatedFloat = AnimatedFloat(0f, clock)

        var animationEndReason: AnimationEndReason? = null
        var animationEndValue: Float? = null

        animatedFloat.animateTo(1f) { reason, value ->
            animationEndReason = reason
            animationEndValue = value
        }

        clock.clockTimeMillis += 100000L

        assertNotNull(animationEndValue)
        assertNotNull(animationEndReason)

        assertEquals(1f, animationEndValue!!, epsilon)
        assertEquals(AnimationEndReason.TargetReached, animationEndReason)
    }

    @Test
    fun testSubscription() {
        val recordedFrameTimes = mutableListOf<Long>()
        val observer = object : AnimationClockObserver {
            override fun onAnimationFrame(frameTimeMillis: Long) {
                recordedFrameTimes.add(frameTimeMillis)
            }
        }

        clock.clockTimeMillis = 0L
        clock.clockTimeMillis = 1L
        clock.unsubscribe(observer)
        clock.subscribe(observer)
        // observer should record 1L
        clock.clockTimeMillis = 2L
        // observer should record 2L
        clock.unsubscribe(observer)
        clock.clockTimeMillis = 3L
        clock.clockTimeMillis = 4L
        clock.subscribe(observer)
        // observer should record 4L
        clock.clockTimeMillis = 5L
        // observer should record 5L
        clock.clockTimeMillis = 6L
        // observer should record 6L
        clock.unsubscribe(observer)
        clock.clockTimeMillis = 7L

        val expectedRecording = listOf(1L, 2L, 4L, 5L, 6L)
        assertEquals(expectedRecording, recordedFrameTimes)
    }
}