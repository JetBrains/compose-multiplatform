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

package androidx.compose.runtime

import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.monotonicFrameClock
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class MonotonicFrameClockTest {
    @ExperimentalComposeApi
    @Test
    fun monotonicFrameClockThrowsWhenAbsent() {
        assertFailsWith<IllegalStateException> {
            runBlocking {
                coroutineContext.monotonicFrameClock
            }
        }
    }

    @ExperimentalComposeApi
    @Test
    fun monotonicFrameClockReturnsContextClock() {
        val clock = object : MonotonicFrameClock {
            override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
                error("not implemented")
            }
        }

        val result = runBlocking(clock) {
            coroutineContext.monotonicFrameClock
        }

        assertSame(clock, result)
    }

    @Test
    fun withFrameNanosThrowsWithNoClock() {
        assertFailsWith<IllegalStateException> {
            runBlocking {
                withFrameNanos {
                    throw RuntimeException("withFrameNanos block should not be called")
                }
            }
        }
    }

    @Test
    fun withFrameNanosCallsPresentClock() {
        val clock = object : MonotonicFrameClock {
            var callCount = 0
            override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
                callCount++
                return onFrame(0)
            }
        }
        val expected = Any()
        val result = runBlocking(clock) {
            withFrameNanos { expected }
        }
        assertSame(expected, result, "expected value not returned from withFrameNanos")
        assertEquals(1, clock.callCount, "withFrameNanos did not use supplied clock")
    }
}
