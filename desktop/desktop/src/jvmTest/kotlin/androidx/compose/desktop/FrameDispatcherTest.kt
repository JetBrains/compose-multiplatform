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

package androidx.compose.desktop

import androidx.compose.ui.platform.FrameDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class FrameDispatcherTest {
    var frameIndex = 0
    var frameTime = 0L

    var frameDuration = 0L

    suspend fun onFrame(nanoTime: Long) {
        frameIndex++
        frameTime = nanoTime / 1_000_000
        delay(frameDuration)
    }

    fun TestCoroutineScope.testFrameDispatcher() = FrameDispatcher(
        ::onFrame,
        framesPerSecond = { 100f }, // one frame is 10 milliseconds
        nanoTime = { currentTime * 1_000_000 },
        coroutineContext
    )

    @Test
    fun `don't schedule`() = runBlockingTest {
        val timer = testFrameDispatcher()

        runCurrent()
        assertEquals(0, currentTime)
        assertEquals(0, frameIndex)
        assertEquals(0, frameTime)

        advanceTimeBy(10_000)
        assertEquals(0, frameIndex)
        assertEquals(0, frameTime)

        timer.cancel()
    }

    @Test
    fun `schedule one time`() = runBlockingTest {
        val timer = testFrameDispatcher()

        advanceTimeBy(1234)
        timer.scheduleFrame()
        assertEquals(1, frameIndex)
        assertEquals(1234, frameTime)

        advanceTimeBy(10_000)
        assertEquals(1, frameIndex)
        assertEquals(1234, frameTime)

        timer.cancel()
    }

    @Test
    fun `schedule multiple times`() = runBlockingTest {
        val timer = testFrameDispatcher()

        advanceTimeBy(10_000)
        timer.scheduleFrame()

        timer.scheduleFrame()
        timer.scheduleFrame()
        assertEquals(1, frameIndex)
        assertEquals(10_000, frameTime)

        advanceTimeBy(9)
        assertEquals(1, frameIndex)
        assertEquals(10_000, frameTime)

        advanceTimeBy(1)
        assertEquals(2, frameIndex)
        assertEquals(10_010, frameTime)

        advanceTimeBy(10_000)
        assertEquals(2, frameIndex)
        assertEquals(10_010, frameTime)

        timer.cancel()
    }

    @Test
    fun `schedule after short delay`() = runBlockingTest {
        val timer = testFrameDispatcher()

        advanceTimeBy(10_000)
        timer.scheduleFrame()

        advanceTimeBy(5)
        timer.scheduleFrame()
        assertEquals(1, frameIndex)
        assertEquals(10_000, frameTime)

        advanceTimeBy(4)
        timer.scheduleFrame()
        assertEquals(1, frameIndex)
        assertEquals(10_000, frameTime)

        advanceTimeBy(1)
        timer.scheduleFrame()
        assertEquals(2, frameIndex)
        assertEquals(10_010, frameTime)

        timer.cancel()
    }

    @Test
    fun `schedule after long delay`() = runBlockingTest {
        val timer = testFrameDispatcher()

        advanceTimeBy(10_000)
        timer.scheduleFrame()

        advanceTimeBy(10_000)
        timer.scheduleFrame()
        assertEquals(2, frameIndex)
        assertEquals(20_000, frameTime)

        timer.cancel()
    }

    @Test
    fun `schedule after short frame`() = runBlockingTest {
        val timer = testFrameDispatcher()
        frameDuration = 7

        advanceTimeBy(10_000)
        timer.scheduleFrame()
        assertEquals(1, frameIndex)
        assertEquals(10_000, frameTime)

        timer.scheduleFrame()
        assertEquals(1, frameIndex)
        assertEquals(10_000, frameTime)

        advanceTimeBy(9)
        timer.scheduleFrame()
        assertEquals(1, frameIndex)
        assertEquals(10_000, frameTime)

        advanceTimeBy(1)
        assertEquals(2, frameIndex)
        assertEquals(10_010, frameTime)

        advanceTimeBy(10_000)
        assertEquals(2, frameIndex)
        assertEquals(10_010, frameTime)

        timer.cancel()
    }

    @Test
    fun `schedule after long frame`() = runBlockingTest {
        val timer = testFrameDispatcher()
        frameDuration = 13

        advanceTimeBy(10_000)
        timer.scheduleFrame()
        assertEquals(1, frameIndex)
        assertEquals(10_000, frameTime)

        timer.scheduleFrame()
        assertEquals(1, frameIndex)
        assertEquals(10_000, frameTime)

        advanceTimeBy(12)
        timer.scheduleFrame()
        assertEquals(1, frameIndex)
        assertEquals(10_000, frameTime)

        advanceTimeBy(1)
        assertEquals(2, frameIndex)
        assertEquals(10_013, frameTime)

        advanceTimeBy(10_000)
        assertEquals(2, frameIndex)
        assertEquals(10_013, frameTime)

        timer.cancel()
    }

    @Test
    fun cancel() = runBlockingTest {
        val timer = testFrameDispatcher()

        advanceTimeBy(10_000)
        timer.scheduleFrame()

        timer.scheduleFrame()
        timer.scheduleFrame()
        timer.cancel()
        advanceTimeBy(10_000)
        assertEquals(1, frameIndex)
        assertEquals(10_000, frameTime)
    }
}