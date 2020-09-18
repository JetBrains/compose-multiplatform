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
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.lang.Float.min

@RunWith(JUnit4::class)
class TransitionAnimationTest {
    @Test
    fun testDefaultTransition() {
        val clock = ManualAnimationClock(0)
        val anim = TransitionAnimation(def1, clock)
        anim.toState(AnimState.B)
        val physicsAnim = FloatSpringSpec()
        for (playTime in 0L until physicsAnim.getDurationMillis(0f, 1f, 0f) step 20L) {
            clock.clockTimeMillis = playTime
            assertEquals(
                anim[prop1],
                physicsAnim.getValue(playTime, 0f, 1f, 0f),
                epsilon
            )
        }
        for (playTime in 0L until physicsAnim.getDurationMillis(100f, -100f, 0f) step 20L) {
            clock.clockTimeMillis = playTime
            assertEquals(
                anim[prop2],
                physicsAnim.getValue(playTime, 100f, -100f, 0f),
                epsilon
            )
        }
    }

    @Test
    fun testInitialState() {
        val clock = ManualAnimationClock(0)
        val anim = TransitionAnimation(def1, clock, AnimState.C)
        assertEquals(anim[prop1], 1000f)
        assertEquals(anim[prop2], -250f)
    }

    @Test
    fun testStateChangedListener() {
        val clock = ManualAnimationClock(0)
        val anim = TransitionAnimation(def1, clock, AnimState.C)
        var lastState: AnimState? = null
        anim.onStateChangeFinished = {
            lastState = it
        }
        anim.toState(AnimState.A)
        // Increment the clock by some large amount to guarantee the finish of the animation
        clock.clockTimeMillis += 100000
        assertEquals(AnimState.A, lastState)

        anim.toState(AnimState.B)
        // Increment the clock by some large amount to guarantee the finish of the animation
        clock.clockTimeMillis += 100000
        assertEquals(AnimState.B, lastState)
    }

    @Test
    @OptIn(InternalAnimationApi::class)
    fun testNonMonotonicTransition() {
        var finished = false
        val clock = ManualAnimationClock(0)
        val anim = TransitionAnimation(def2, clock, AnimState.A)
        anim.monotonic = false
        anim.onStateChangeFinished = {
            finished = true
        }
        anim.toState(AnimState.B)

        listOf(0, 200, 499, 500, 600, 300, 100, 0, 505).forEach { timeStamp ->
            clock.clockTimeMillis = timeStamp.toLong()
            assertEquals(min(timeStamp.toFloat(), 200f), anim[prop1])
            assertEquals(min(timeStamp.toFloat(), 500f), anim[prop2])
        }
        assertFalse(finished)

        // Changing monotonic to true should result in another frame being pumped with the last
        // time stamp.
        anim.monotonic = true
        assertTrue(finished)
    }

    @Test
    @OptIn(InternalAnimationApi::class)
    fun testSnapToState() {
        val clock = ManualAnimationClock(0)
        val anim = TransitionAnimation(def2, clock, AnimState.A)

        // Animate A -> B
        anim.toState(AnimState.B)
        clock.clockTimeMillis = 100
        assertEquals(100f, anim[prop1])
        assertEquals(100f, anim[prop2])

        // Snap to end state
        anim.snapToState(AnimState.B)
        assertEquals(200f, anim[prop1])
        assertEquals(500f, anim[prop2])
        assertFalse(anim.isRunning)

        clock.clockTimeMillis = 0
        // Animate B->A
        anim.toState(AnimState.A)
        clock.clockTimeMillis = 250
        assertEquals(0f, anim[prop1])
        assertEquals(250f, anim[prop2])

        assertTrue(anim.isRunning)
        // Snap to start state
        anim.snapToState(AnimState.B)
        assertEquals(200f, anim[prop1])
        assertEquals(500f, anim[prop2])
        assertFalse(anim.isRunning)
    }
}

private enum class AnimState {
    A, B, C
}

private val prop1 = FloatPropKey()
private val prop2 = FloatPropKey()

private val def1 = transitionDefinition<AnimState> {
    state(AnimState.A) {
        this[prop1] = 0f
        this[prop2] = 100f
    }

    state(AnimState.B) {
        this[prop1] = 1f
        this[prop2] = -100f
    }

    state(AnimState.C) {
        this[prop1] = 1000f
        this[prop2] = -250f
    }
}

private val def2 = transitionDefinition<AnimState> {
    state(AnimState.A) {
        this[prop1] = 0f
        this[prop2] = 0f
    }
    state(AnimState.B) {
        this[prop1] = 200f
        this[prop2] = 500f
    }

    transition {
        prop1 using TweenSpec(durationMillis = 200, easing = LinearEasing)
        prop2 using TweenSpec(durationMillis = 500, easing = LinearEasing)
    }
}