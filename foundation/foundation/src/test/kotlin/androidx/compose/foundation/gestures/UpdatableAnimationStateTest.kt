/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.foundation.gestures

import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.ui.MotionDurationScale
import com.google.common.truth.Truth.assertThat
import kotlin.test.fail
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class UpdatableAnimationStateTest {

    private val frameClock = TestFrameClock()
    private val state = UpdatableAnimationState()

    @Test
    fun animateToZero_doesNothing_whenValueIsZero() {
        state.value = 0f

        runBlocking {
            state.animateToZero(
                beforeFrame = { fail() },
                afterFrame = { fail() }
            )

            // Should immediately get to here without suspending.
        }
    }

    @Test
    fun animateToZero_animatesToZero_fromPositiveValue() {
        val deltas = mutableListOf<Float>()
        state.value = 10f

        runBlocking {
            launch(frameClock) {
                state.animateToZero(
                    beforeFrame = { deltas += it },
                    afterFrame = {},
                )
            }
        }

        assertThat(state.value).isEqualTo(0f)
        assertThat(deltas).isNotEmpty()
    }

    @Test
    fun animateToZero_animatesToZero_fromNegativeValue() {
        val deltas = mutableListOf<Float>()
        state.value = -10f

        runBlocking(frameClock) {
            state.animateToZero(
                beforeFrame = { deltas += it },
                afterFrame = {},
            )
        }

        assertThat(state.value).isEqualTo(0f)
        assertThat(deltas).isNotEmpty()
    }

    @Test
    fun animateToZero_handlesZeroAnimationScale() {
        val deltas = mutableListOf<Float>()
        val scale = object : MotionDurationScale {
            override val scaleFactor: Float = 0f
        }
        state.value = 10f

        runBlocking(frameClock + scale) {
            state.animateToZero(
                beforeFrame = { deltas += it },
                afterFrame = {}
            )
        }

        assertThat(state.value).isEqualTo(0f)
        assertThat(deltas).isNotEmpty()
    }

    @Test
    fun animateToZero_handlesDoubleAnimationScale() {
        val deltas = mutableListOf<Float>()
        val scale = object : MotionDurationScale {
            override val scaleFactor: Float = 2f
        }
        state.value = 10f

        runBlocking(frameClock + scale) {
            state.animateToZero(
                beforeFrame = { deltas += it },
                afterFrame = {}
            )
        }

        assertThat(state.value).isEqualTo(0f)
        assertThat(deltas).isNotEmpty()
    }

    @Test
    fun animateToZero_animatesToZero_whenValueIncreasedAfterFrame() {
        state.value = 10f
        val valuesToSet = mutableListOf(20f, 30f, 40f)

        runBlocking(frameClock) {
            state.animateToZero(
                beforeFrame = {},
                afterFrame = {
                    valuesToSet.removeFirstOrNull()?.let { state.value = it }
                },
            )
        }

        assertThat(state.value).isEqualTo(0f)
    }

    @Test
    fun animateToZero_animatesToZero_whenValueDecreasedAfterFrame() {
        state.value = 100f
        val valuesToSet = mutableListOf(50f, 25f, 1f)

        runBlocking(frameClock) {
            state.animateToZero(
                beforeFrame = {},
                afterFrame = {
                    valuesToSet.removeFirstOrNull()?.let { state.value = it }
                },
            )
        }

        assertThat(state.value).isEqualTo(0f)
    }

    private class TestFrameClock : MonotonicFrameClock {
        private var frame = 0L

        override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R =
            onFrame(frame).also {
                frame += 16_000_000L
            }
    }
}