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

import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.google.common.truth.Truth.assertThat
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlin.math.abs
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AnimatableTest {
    @Test
    fun animateDecayTest() {
        runBlocking {
            val from = 9f
            val initialVelocity = 20f
            val decaySpec = FloatExponentialDecaySpec()
            val anim = DecayAnimation(
                decaySpec,
                initialValue = from,
                initialVelocity = initialVelocity
            )
            val clock = SuspendAnimationTest.TestFrameClock()
            val interval = 50
            withContext(clock) {
                // Put in a bunch of frames 50 milliseconds apart
                for (frameTimeMillis in 0..5000 step interval) {
                    clock.frame(frameTimeMillis * 1_000_000L)
                }
                var playTimeMillis = 0L
                val animatable = Animatable(9f)
                val result = animatable.animateDecay(20f, animationSpec = exponentialDecay()) {
                    assertTrue(isRunning)
                    assertEquals(anim.targetValue, targetValue)
                    assertEquals(anim.getValueFromMillis(playTimeMillis), value, 0.001f)
                    assertEquals(
                        anim.getVelocityFromMillis(playTimeMillis),
                        velocity,
                        0.001f
                    )
                    playTimeMillis += interval
                    assertEquals(value, animatable.value, 0.0001f)
                    assertEquals(velocity, animatable.velocity, 0.0001f)
                }
                // After animation
                assertEquals(anim.targetValue, animatable.value)
                assertEquals(false, animatable.isRunning)
                assertEquals(0f, animatable.velocity)
                assertEquals(AnimationEndReason.Finished, result.endReason)
                assertTrue(abs(result.endState.velocity) <= decaySpec.absVelocityThreshold)
            }
        }
    }

    @Test
    fun animateToTest() {
        runBlocking {
            val anim = TargetBasedAnimation(
                spring(dampingRatio = Spring.DampingRatioMediumBouncy), Float.VectorConverter,
                initialValue = 0f, targetValue = 1f
            )
            val clock = SuspendAnimationTest.TestFrameClock()
            val interval = 50
            val animatable = Animatable(0f)
            withContext(clock) {
                // Put in a bunch of frames 50 milliseconds apart
                for (frameTimeMillis in 0..5000 step interval) {
                    clock.frame(frameTimeMillis * 1_000_000L)
                }
                var playTimeMillis = 0L
                val result = animatable.animateTo(
                    1f,
                    spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                ) {
                    assertTrue(isRunning)
                    assertEquals(1f, targetValue)
                    assertEquals(anim.getValueFromMillis(playTimeMillis), value, 0.001f)
                    assertEquals(anim.getVelocityFromMillis(playTimeMillis), velocity, 0.001f)
                    playTimeMillis += interval
                }
                // After animation
                assertEquals(anim.targetValue, animatable.value)
                assertEquals(0f, animatable.velocity)
                assertEquals(false, animatable.isRunning)
                assertEquals(AnimationEndReason.Finished, result.endReason)
            }
        }
    }

    @Test
    fun animateToGenericTypeTest() =
        runBlocking<Unit> {
            val from = Offset(666f, 321f)
            val to = Offset(919f, 864f)
            val offsetToVector: TwoWayConverter<Offset, AnimationVector2D> =
                TwoWayConverter(
                    convertToVector = { AnimationVector2D(it.x, it.y) },
                    convertFromVector = { Offset(it.v1, it.v2) }
                )
            val anim = TargetBasedAnimation(
                tween(500), offsetToVector,
                initialValue = from, targetValue = to
            )
            val clock = SuspendAnimationTest.TestFrameClock()
            val interval = 50
            val animatable = Animatable(
                initialValue = from,
                typeConverter = offsetToVector
            )
            coroutineScope {
                withContext(clock) {
                    launch {
                        // Put in a bunch of frames 50 milliseconds apart
                        for (frameTimeMillis in 0..1000 step interval) {
                            clock.frame(frameTimeMillis * 1_000_000L)
                            delay(5)
                        }
                    }
                    launch {
                        // The first frame should start at 100ms
                        var playTimeMillis = 0L
                        animatable.animateTo(
                            to,
                            animationSpec = tween(500)
                        ) {
                            assertTrue("PlayTime Millis: $playTimeMillis", isRunning)
                            assertEquals(to, targetValue)
                            val expectedValue = anim.getValueFromMillis(playTimeMillis)
                            assertEquals(
                                "PlayTime Millis: $playTimeMillis",
                                expectedValue.x,
                                value.x,
                                0.001f
                            )
                            assertEquals(
                                "PlayTime Millis: $playTimeMillis",
                                expectedValue.y,
                                value.y,
                                0.001f
                            )
                            playTimeMillis += interval

                            if (playTimeMillis == 300L) {
                                // Prematurely cancel the animation and check corresponding states
                                this@withContext.launch {
                                    stop()
                                    assertFalse(isRunning)
                                    assertEquals(playTimeMillis, 300L)
                                    assertEquals(to, animatable.targetValue)
                                    assertEquals(AnimationVector(0f, 0f), animatable.velocityVector)
                                }
                            }
                        }
                    }
                }
            }
        }

    @Test
    fun animateToWithInterruption() {
        runBlocking {
            val anim1 = TargetBasedAnimation(
                tween(200, easing = LinearEasing),
                Float.VectorConverter,
                0f,
                200f
            )
            val clock = MyTestFrameClock()
            val interval = 50
            coroutineScope {
                withContext(clock) {
                    val animatable = Animatable(0f)
                    var playTimeMillis by mutableStateOf(0L)

                    suspend fun createInterruption() {
                        val anim2 = TargetBasedAnimation(
                            spring(),
                            Float.VectorConverter,
                            animatable.value,
                            300f,
                            animatable.velocity
                        )
                        assertEquals(100L, playTimeMillis)
                        var firstFrame = true
                        val result2 = animatable.animateTo(300f, spring()) {
                            // First frame will arrive with a timestamp of the time of interruption,
                            // which is 100ms. The subsequent frames will be consistent with what's
                            // tracked in `playTimeMillis`.
                            val playTime = if (firstFrame) 100L else playTimeMillis
                            assertTrue(isRunning)
                            assertEquals(300f, targetValue)
                            assertEquals(
                                anim2.getValueFromMillis((playTime - 100)),
                                value
                            )
                            assertEquals(
                                anim2.getVelocityFromMillis((playTime - 100)),
                                velocity
                            )
                            if (!firstFrame) {
                                playTimeMillis += interval
                                clock.trySendFrame(playTimeMillis * 1_000_000L)
                            } else {
                                firstFrame = false
                            }
                        }
                        assertFalse(animatable.isRunning)
                        assertEquals(AnimationEndReason.Finished, result2.endReason)
                        assertEquals(300f, animatable.targetValue)
                        assertEquals(300f, animatable.value)
                        assertEquals(0f, animatable.velocity)
                    }

                    clock.trySendFrame(0)
                    launch {
                        try {
                            animatable.animateTo(
                                200f,
                                animationSpec = tween(200, easing = LinearEasing)
                            ) {
                                assertTrue(isRunning)
                                assertEquals(targetValue, 200f)
                                assertEquals(anim1.getValueFromMillis(playTimeMillis), value)
                                assertEquals(
                                    anim1.getVelocityFromMillis(playTimeMillis),
                                    velocity
                                )

                                assertTrue(playTimeMillis <= 100)
                                if (playTimeMillis == 100L) {
                                    this@withContext.launch {
                                        // No more new frame until the ongoing animation is canceled.
                                        createInterruption()
                                    }
                                } else {
                                    playTimeMillis += interval
                                    clock.trySendFrame(playTimeMillis * 1_000_000L)
                                }
                            }
                        } finally {
                            // At this point the previous animation on the Animatable has been
                            // canceled. Pump a frame to get the new animation going.
                            playTimeMillis += interval
                            clock.trySendFrame(playTimeMillis * 1_000_000L)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun testUpdateBounds() {
        val animatable = Animatable(5f)
        // Update bounds when *not* running
        animatable.updateBounds(0f, 4f)
        assertEquals(4f, animatable.value)
        runBlocking {
            val clock = SuspendAnimationTest.TestFrameClock()
            // Put two frames in clock
            clock.frame(0L)
            clock.frame(200 * 1_000_000L)

            withContext(clock) {
                animatable.animateTo(4f, tween(100)) {
                    if (animatable.upperBound == 4f) {
                        // Update bounds while running
                        animatable.updateBounds(-4f, 0f)
                    }
                }
            }
        }
        assertEquals(0f, animatable.value)

        // Snap to value out of bounds
        runBlocking {
            animatable.snapTo(animatable.lowerBound!! - 100f)
        }
        assertEquals(animatable.lowerBound!!, animatable.value)
    }

    @Test
    fun animationResult_toString() {
        val animatable = AnimationResult(
            endReason = AnimationEndReason.Finished,
            endState = AnimationState(42f)
        )
        val string = animatable.toString()
        assertThat(string).contains(AnimationResult::class.java.simpleName)
        assertThat(string).contains("endReason=Finished")
        assertThat(string).contains("endState=")
    }

    @Test
    fun animationState_toString() {
        val state = AnimationState(
            initialValue = 42f,
            initialVelocity = 2f,
            lastFrameTimeNanos = 4000L,
            finishedTimeNanos = 3000L,
            isRunning = true
        )
        val string = state.toString()
        assertThat(string).contains(AnimationState::class.java.simpleName)
        assertThat(string).contains("value=42.0")
        assertThat(string).contains("velocity=2.0")
        assertThat(string).contains("lastFrameTimeNanos=4000")
        assertThat(string).contains("finishedTimeNanos=3000")
        assertThat(string).contains("isRunning=true")
    }

    private class MyTestFrameClock : MonotonicFrameClock {
        // Make the send non-blocking
        private val frameCh = Channel<Long>(Channel.UNLIMITED)

        suspend fun frame(frameTimeNanos: Long) {
            frameCh.send(frameTimeNanos)
        }

        fun trySendFrame(frameTimeNanos: Long) {
            frameCh.trySend(frameTimeNanos)
        }

        override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R =
            onFrame(frameCh.receive())
    }
}
