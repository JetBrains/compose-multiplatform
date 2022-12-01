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

package androidx.compose.ui.platform

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
class FlushCoroutineDispatcherTest {

    @Test
    fun all_tasks_should_run_with_flush() = runTest {
        val dispatcher = FlushCoroutineDispatcher(this)

        val actualNumbers = mutableListOf<Int>()
        launch(dispatcher) {
            yield()
            actualNumbers.add(1)
            yield()
            yield()
            actualNumbers.add(2)
            yield()
            yield()
            yield()
            actualNumbers.add(3)
        }

        while (dispatcher.hasTasks()) {
            dispatcher.flush()
        }

        assertEquals(listOf(1, 2, 3), actualNumbers)
    }

    @Test
    fun tasks_should_run_even_without_flush() = runTest {
        val dispatcher = FlushCoroutineDispatcher(this)

        val actualNumbers = mutableListOf<Int>()
        launch(dispatcher) {
            yield()
            actualNumbers.add(1)
            yield()
            yield()
            actualNumbers.add(2)
            yield()
            yield()
            yield()
            actualNumbers.add(3)
        }

        testScheduler.advanceUntilIdle()

        assertEquals(listOf(1, 2, 3), actualNumbers)
        assertFalse(dispatcher.hasTasks())
    }

    @Test
    fun flushing_in_another_thread() {
        val actualNumbers = mutableListOf<Int>()
        lateinit var dispatcher: FlushCoroutineDispatcher
        val random = Random(123)

        runTest {
            withContext(Dispatchers.Default) {
                dispatcher = FlushCoroutineDispatcher(this)

                val addJob = launch(dispatcher) {
                    repeat(10000) {
                        actualNumbers.add(it)
                        repeat(random.nextInt(5)) {
                            yield()
                        }
                    }
                }

                launch {
                    while (addJob.isActive) {
                        dispatcher.flush()
                        yield()
                    }
                }
            }
        }

        assertEquals((0 until 10000).toList(), actualNumbers)
        assertFalse(dispatcher.hasTasks())
    }
}
