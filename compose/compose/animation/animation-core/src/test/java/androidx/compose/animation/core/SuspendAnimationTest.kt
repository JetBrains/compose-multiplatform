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
import androidx.compose.ui.geometry.Offset
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SuspendAnimationTest {
    @Test
    fun animateFloatVariantTest() =
        runBlocking {
            val anim = TargetBasedAnimation(
                spring(dampingRatio = Spring.DampingRatioMediumBouncy), Float.VectorConverter,
                initialValue = 0f, targetValue = 1f
            )
            val clock = TestFrameClock()
            val interval = 50
            withContext(clock) {
                // Put in a bunch of frames 50 milliseconds apart
                for (frameTimeMillis in 0..5000 step interval) {
                    clock.frame(frameTimeMillis * 1_000_000L)
                }
                var playTimeMillis = 0L
                animate(
                    0f, 1f, 0f,
                    spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                ) { value, velocity ->
                    assertEquals(anim.getValueFromMillis(playTimeMillis), value, 0.001f)
                    assertEquals(anim.getVelocityFromMillis(playTimeMillis), velocity, 0.001f)
                    playTimeMillis += interval
                }
            }
        }

    @Test
    fun animateGenericsVariantTest() =
        runBlocking {
            val from = Offset(666f, 321f)
            val to = Offset(919f, 864f)
            val offsetToVector: TwoWayConverter<Offset, AnimationVector2D> =
                TwoWayConverter(
                    convertToVector = { AnimationVector2D(it.x, it.y) },
                    convertFromVector = { Offset(it.v1, it.v2) }
                )
            val anim = TargetBasedAnimation(
                tween(500), offsetToVector, initialValue = from, targetValue = to
            )
            val clock = TestFrameClock()
            val interval = 50
            withContext(clock) {
                // Put in a bunch of frames 50 milliseconds apart
                for (frameTimeMillis in 0..500 step interval) {
                    clock.frame(frameTimeMillis * 1_000_000L)
                }
                var playTimeMillis = 0L
                animate(
                    offsetToVector,
                    from, to,
                    animationSpec = tween(500)
                ) { value, _ ->
                    val expectedValue = anim.getValueFromMillis(playTimeMillis)
                    assertEquals(expectedValue.x, value.x, 0.001f)
                    assertEquals(expectedValue.y, value.y, 0.001f)
                    playTimeMillis += interval
                }
            }
        }

    @Test
    fun animateDecayTest() =
        runBlocking {
            val from = 666f
            val velocity = 999f
            val anim = DecayAnimation(
                FloatExponentialDecaySpec(),
                initialValue = from, initialVelocity = velocity
            )
            val clock = TestFrameClock()
            val interval = 50
            withContext(clock) {
                // Put in a bunch of frames 50 milliseconds apart
                for (frameTimeMillis in 0..5000 step interval) {
                    clock.frame(frameTimeMillis * 1_000_000L)
                }
                var playTimeMillis = 0L
                animateDecay(
                    from, velocity,
                    animationSpec = FloatExponentialDecaySpec()
                ) { value, velocity ->
                    assertEquals(anim.getValueFromMillis(playTimeMillis), value, 0.001f)
                    assertEquals(anim.getVelocityFromMillis(playTimeMillis), velocity, 0.001f)
                    playTimeMillis += interval
                }
            }
        }

    @Test
    fun animateToTest() {
        runBlocking {
            val from = Offset(666f, 321f)
            val to = Offset(919f, 864f)
            val offsetToVector: TwoWayConverter<Offset, AnimationVector2D> =
                TwoWayConverter(
                    convertToVector = { AnimationVector2D(it.x, it.y) },
                    convertFromVector = { Offset(it.v1, it.v2) }
                )
            val anim = TargetBasedAnimation(
                tween(500), offsetToVector, initialValue = from, targetValue = to
            )
            val clock = TestFrameClock()
            val interval = 50
            val animationState = AnimationState(
                initialValue = from,
                typeConverter = offsetToVector,
                lastFrameTimeNanos = 0
            )
            withContext(clock) {
                // Put in a bunch of frames 50 milliseconds apart
                for (frameTimeMillis in 100..1000 step interval) {
                    clock.frame(frameTimeMillis * 1_000_000L)
                }
                // The first frame should start at 100ms
                var playTimeMillis = 0L
                animationState.animateTo(
                    to,
                    animationSpec = tween(500),
                    sequentialAnimation = true
                ) {
                    assertTrue(animationState.isRunning)
                    assertTrue(isRunning)
                    val expectedValue = anim.getValueFromMillis(playTimeMillis)
                    assertEquals(expectedValue.x, value.x, 0.001f)
                    assertEquals(expectedValue.y, value.y, 0.001f)
                    if (playTimeMillis == 0L) {
                        // First invocation to block when starting from last frame is always
                        // playtime = 0
                        playTimeMillis = 100L
                    } else {
                        playTimeMillis += interval
                    }

                    if (playTimeMillis == 300L) {
                        // Prematurely cancel the animation and check corresponding states
                        cancelAnimation()
                        assertFalse(animationState.isRunning)
                        assertFalse(isRunning)
                    }
                }

                // Check that no more frames happened after cancel()
                assertEquals(playTimeMillis, 300L)
                assertFalse(animationState.isRunning)
            }
        }
    }

    @Test
    fun animateDecayOnAnimationStateTest() =
        runBlocking {
            val from = 9f
            val initialVelocity = 20f
            val anim = DecayAnimation(
                FloatExponentialDecaySpec(),
                initialValue = from, initialVelocity = initialVelocity
            )
            val clock = TestFrameClock()
            val interval = 50
            withContext(clock) {
                // Put in a bunch of frames 50 milliseconds apart
                for (frameTimeMillis in 0..5000 step interval) {
                    clock.frame(frameTimeMillis * 1_000_000L)
                }
                var playTimeMillis = 0L
                val state = AnimationState(9f, 20f)
                state.animateDecay(
                    FloatExponentialDecaySpec().generateDecayAnimationSpec()
                ) {
                    assertEquals(anim.getValueFromMillis(playTimeMillis), value, 0.001f)
                    assertEquals(anim.getVelocityFromMillis(playTimeMillis), velocity, 0.001f)
                    playTimeMillis += interval
                    assertEquals(value, state.value, 0.0001f)
                    assertEquals(velocity, state.velocity, 0.0001f)
                }
            }
        }

    internal class TestFrameClock : MonotonicFrameClock {
        // Make the send non-blocking
        private val frameCh = Channel<Long>(Channel.UNLIMITED)

        suspend fun frame(frameTimeNanos: Long) {
            frameCh.send(frameTimeNanos)
        }

        override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R =
            onFrame(frameCh.receive())
    }
}
