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

package androidx.compose.ui.test

import androidx.compose.runtime.withFrameNanos
import androidx.test.filters.SmallTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

@ExperimentalCoroutinesApi
@SmallTest
class TestMonotonicFrameClockTest {
    @Test
    fun testMonotonicFrameClockRunsFrame() = runTest(UnconfinedTestDispatcher()) {
        val frameDelayNanos = 16_000_000L
        withContext(TestMonotonicFrameClock(this, frameDelayNanos)) {
            val startTime = currentTime
            val expectedFrameTime = startTime + frameDelayNanos / 1_000_000
            val counter = TestCounter()
            launch {
                withFrameNanos {
                    counter.expect(2, "in frame callback 1")
                    assertEquals("frame time 1", expectedFrameTime, currentTime)
                }
                counter.expect(4, "after resuming frame callback 1")
            }
            launch {
                withFrameNanos {
                    counter.expect(3, "in frame callback 2")
                    assertEquals("frame time 2", expectedFrameTime, currentTime)
                }
                counter.expect(5, "after resuming frame callback 2")
            }
            counter.expect(1)
            advanceUntilIdle()
            counter.expect(6, "final result")
        }
    }
}

private class TestCounter {
    private var count = 0

    fun expect(checkpoint: Int, message: String = "(no message)") {
        val expected = count + 1
        if (checkpoint != expected) {
            fail("out of order event $checkpoint, expected $expected, $message")
        }
        count = expected
    }
}
