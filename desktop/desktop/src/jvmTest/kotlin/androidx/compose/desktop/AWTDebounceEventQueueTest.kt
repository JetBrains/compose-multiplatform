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

import androidx.compose.desktop.AWTDebounceEventQueueTest.Event.Input
import androidx.compose.desktop.AWTDebounceEventQueueTest.Event.Render
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class AWTDebounceEventQueueTest {
    enum class Event {
        Input, Render
    }

    var events = mutableListOf<Event>()
    var time = 0L

    fun input() {
        events.add(Input)
        time++
    }

    fun TestCoroutineScope.scheduleRender() = launch {
        yield()
        events.add(Render)
    }

    fun TestCoroutineScope.testQueue() = AWTDebounceEventQueue(
        maxNanosToBlockThread = 3,
        nanoTime = { time },
        coroutineContext
    )

    @Test
    fun `queue events`() = runBlockingTest {
        val queue = testQueue()
        queue.post { input() }
        assertEquals(listOf(Input), events)

        queue.post { input() }
        queue.post { input() }
        assertEquals(listOf(Input, Input, Input), events)

        queue.post { input() }
        assertEquals(listOf(Input, Input, Input), events)

        runCurrent()
        assertEquals(listOf(Input, Input, Input, Input), events)

        queue.cancel()
    }

    @Test
    fun `queue events and render simultaneously`() = runBlockingTest {
        val queue = testQueue()

        scheduleRender()
        scheduleRender()

        for (i in 0 until 7) {
            queue.post { input() }
        }

        runCurrent()

        assertEquals(
            listOf(Input, Input, Input, Render, Render, Input, Input, Input, Input),
            events
        )

        queue.cancel()
    }
}