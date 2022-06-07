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

package androidx.compose.runtime.dispatch

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class BroadcastFrameClockTest {
    @Test
    fun sendAndReceiveFrames() = runTest(UnconfinedTestDispatcher()) {
        val clock = androidx.compose.runtime.BroadcastFrameClock()

        val frameAwaiter = async { clock.withFrameNanos { it } }

        clock.sendFrame(1)
        assertEquals(1, frameAwaiter.await(), "awaiter frame time")
    }

    private suspend fun assertAwaiterCancelled(
        name: String,
        awaiter: Deferred<*>
    ) {
        assertTrue(
            runCatching { awaiter.await() }.exceptionOrNull() is CancellationException,
            "$name threw CancellationException"
        )
    }

    @Test
    fun cancelClock() = runTest(UnconfinedTestDispatcher()) {
        val clock = androidx.compose.runtime.BroadcastFrameClock()
        val frameAwaiter = async { clock.withFrameNanos { it } }

        clock.cancel()

        assertAwaiterCancelled("awaiter", frameAwaiter)

        assertTrue(
            runCatching { clock.withFrameNanos { it } }.exceptionOrNull() is CancellationException,
            "late awaiter threw CancellationException"
        )
    }

    @Test
    fun failClockWhenNewAwaitersNotified() = runTest(UnconfinedTestDispatcher()) {
        val clock = androidx.compose.runtime.BroadcastFrameClock {
            throw CancellationException("failed frame clock")
        }

        val failingAwaiter = async { clock.withFrameNanos { it } }
        assertAwaiterCancelled("failingAwaiter", failingAwaiter)

        val lateAwaiter = async { clock.withFrameNanos { it } }
        assertAwaiterCancelled("lateAwaiter", lateAwaiter)
    }
}