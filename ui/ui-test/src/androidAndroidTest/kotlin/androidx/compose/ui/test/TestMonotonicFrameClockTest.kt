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
import androidx.compose.ui.test.util.TestCounter
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
@SmallTest
class TestMonotonicFrameClockTest {

    // These delay tests aren't just testing that the test dispatchers correctly skip delays
    // (which is code not under our control and thus not worth testing), but rather that the
    // TestMonotonicFrameClock machinery doesn't break the delay mechanism and end up using wall
    // clock time for delays.
    @Test
    fun delaysAreSkipped_unconfinedDispatcher() = runTest(UnconfinedTestDispatcher()) {
        val delayMillis = 999_999_999L
        withTestClockContext {
            val virtualStartTimeMillis = currentTime
            val realStartTimeNanos = System.nanoTime()
            delay(delayMillis)
            val realDurationMillis = (System.nanoTime() - realStartTimeNanos) / 1_000_000
            val virtualDurationMillis = currentTime - virtualStartTimeMillis

            assertThat(virtualDurationMillis).isEqualTo(delayMillis)
            // This could theoretically fail if the test host is running _really_ slow, but 500ms
            // should be more than enough.
            assertThat(realDurationMillis).isLessThan(500)
        }
    }

    @Test
    fun delaysAreSkipped_standardDispatcher() = runTest(StandardTestDispatcher()) {
        val delayMillis = 999_999_999L
        withTestClockContext {
            val virtualStartTimeMillis = currentTime
            val realStartTimeNanos = System.nanoTime()
            delay(delayMillis)
            val realDurationMillis = (System.nanoTime() - realStartTimeNanos) / 1_000_000
            val virtualDurationMillis = currentTime - virtualStartTimeMillis

            assertThat(virtualDurationMillis).isEqualTo(delayMillis)
            // This could theoretically fail if the test host is running _really_ slow, but 500ms
            // should be more than enough.
            assertThat(realDurationMillis).isLessThan(500)
        }
    }

    @Test
    fun testMonotonicFrameClockRunsFrame_unconfinedDispatcher() =
        runTest(UnconfinedTestDispatcher()) {
            withTestClockContext {
                val startTime = currentTime
                val expectedFrameTime = startTime + FrameDelayNanos / 1_000_000
                val counter = TestCounter()
                launch {
                    counter.expect(1, "in coroutine 1")
                    withFrameNanos {
                        counter.expect(4, "in frame callback 1")
                        assertEquals("frame time 1", expectedFrameTime, currentTime)
                    }
                    counter.expect(6, "after resuming frame callback 1")
                }
                launch {
                    counter.expect(2, "in coroutine 2")
                    withFrameNanos {
                        counter.expect(5, "in frame callback 2")
                        assertEquals("frame time 2", expectedFrameTime, currentTime)
                    }
                    counter.expect(7, "after resuming frame callback 2")
                }
                counter.expect(3)
                advanceUntilIdleWorkaround()
                counter.expect(8, "final result")
            }
        }

    @Test
    fun testMonotonicFrameClockRunsFrame_standardDispatcher() =
        runTest(StandardTestDispatcher()) {
            withTestClockContext {
                val startTime = currentTime
                val expectedFrameTime = startTime + FrameDelayNanos / 1_000_000
                val counter = TestCounter()
                launch {
                    counter.expect(2, "in coroutine 1")
                    withFrameNanos {
                        counter.expect(4, "in frame callback 1")
                        assertEquals("frame time 1", expectedFrameTime, currentTime)
                    }
                    counter.expect(6, "after resuming frame callback 1")
                }
                launch {
                    counter.expect(3, "in coroutine 2")
                    withFrameNanos {
                        counter.expect(5, "in frame callback 2")
                        assertEquals("frame time 2", expectedFrameTime, currentTime)
                    }
                    counter.expect(7, "after resuming frame callback 2")
                }
                counter.expect(1)
                advanceUntilIdleWorkaround()
                counter.expect(8, "final result")
            }
        }

    @Test
    fun testMonotonicFrameClockDispatcherDefersResumesInsideFrame_unconfinedDispatcher() =
        runTest(UnconfinedTestDispatcher()) {
            withTestClockContext {
                val counter = TestCounter()
                var continuation: Continuation<Unit>? = null

                launch {
                    counter.expect(1, "in external coroutine")
                    suspendCancellableCoroutine { continuation = it }
                    counter.expect(9, "after resuming external coroutine")
                }

                launch {
                    counter.expect(2)
                    withFrameNanos {
                        counter.expect(5, "in frame callback 1")
                        // Coroutines launched inside withFrameNanos shouldn't be dispatched until
                        // after all frame callbacks are complete.
                        launch {
                            counter.expect(6, "in \"effect\" coroutine")
                        }
                        counter.expect(7, "after launching \"effect\" coroutine")
                    }
                    counter.expect(11, "after resuming frame callback 1")
                }

                launch {
                    counter.expect(3)
                    withFrameNanos {
                        counter.expect(8, "in frame callback 2")
                        // Coroutines that were already suspended and are resumed inside
                        // withFrameNanos shouldn't be dispatched until after all frame callbacks
                        // are complete either.
                        continuation!!.resume(Unit)
                        counter.expect(10, "after resuming external continuation")
                    }
                    counter.expect(12, "after resuming frame callback 2")
                }

                counter.expect(4)
                advanceUntilIdleWorkaround()
                counter.expect(13, "final result")
            }
        }

    @Test
    fun testMonotonicFrameClockDispatcherDefersResumesInsideFrame_standardDispatcher() =
        runTest(StandardTestDispatcher()) {
            withTestClockContext {
                val counter = TestCounter()
                var continuation: Continuation<Unit>? = null

                launch {
                    counter.expect(2, "in external coroutine")
                    suspendCancellableCoroutine { continuation = it }
                    counter.expect(10, "after resuming external coroutine")
                }

                launch {
                    counter.expect(3)
                    withFrameNanos {
                        counter.expect(5, "in frame callback 1")
                        // Coroutines launched inside withFrameNanos shouldn't be dispatched until
                        // after all frame callbacks are complete.
                        launch {
                            counter.expect(9, "in \"effect\" coroutine")
                        }
                        counter.expect(6, "after launching \"effect\" coroutine")
                    }
                    counter.expect(11, "after resuming frame callback 1")
                }

                launch {
                    counter.expect(4)
                    withFrameNanos {
                        counter.expect(7, "in frame callback 2")
                        // Coroutines that were already suspended and are resumed inside
                        // withFrameNanos shouldn't be dispatched until after all frame callbacks
                        // are complete either.
                        continuation!!.resume(Unit)
                        counter.expect(8, "after resuming external continuation")
                    }
                    counter.expect(12, "after resuming frame callback 2")
                }

                counter.expect(1)
                advanceUntilIdleWorkaround()
                counter.expect(13, "final result")
            }
        }

    @Test
    fun withFrameNanosThrows_unconfinedDispatcher() = runTest(UnconfinedTestDispatcher()) {
        val message = "the frame threw an error"
        var error: RuntimeException? = null
        var firstCoroutineContinued = false
        var secondFrameCallbackRan = false
        var secondCoroutineContinued = false

        withTestClockContext {
            launch {
                try {
                    withFrameNanos {
                        throw RuntimeException(message)
                    }
                } catch (e: RuntimeException) {
                    error = e
                }
                firstCoroutineContinued = true
            }

            launch {
                withFrameNanos {
                    // If one frame callback throws, other frame callbacks should still run.
                    secondFrameCallbackRan = true
                }
                secondCoroutineContinued = true
            }

            advanceUntilIdleWorkaround()
            assertThat(error).hasMessageThat().isEqualTo(message)
            assertThat(firstCoroutineContinued).isTrue()
            assertThat(secondFrameCallbackRan).isTrue()
            assertThat(secondCoroutineContinued).isTrue()
        }
    }

    @Test
    fun withFrameNanosThrows_standardDispatcher() = runTest(StandardTestDispatcher()) {
        val message = "the frame threw an error"
        var error: RuntimeException? = null
        var firstCoroutineContinued = false
        var secondFrameCallbackRan = false
        var secondCoroutineContinued = false

        withTestClockContext {
            launch {
                try {
                    withFrameNanos {
                        throw RuntimeException(message)
                    }
                } catch (e: RuntimeException) {
                    error = e
                }
                firstCoroutineContinued = true
            }

            launch {
                withFrameNanos {
                    // If one frame callback throws, other frame callbacks should still run.
                    secondFrameCallbackRan = true
                }
                secondCoroutineContinued = true
            }

            advanceUntilIdleWorkaround()
            assertThat(error).hasMessageThat().isEqualTo(message)
            assertThat(firstCoroutineContinued).isTrue()
            assertThat(secondFrameCallbackRan).isTrue()
            assertThat(secondCoroutineContinued).isTrue()
        }
    }

    private suspend fun TestScope.withTestClockContext(block: suspend CoroutineScope.() -> Unit) {
        withContext(TestMonotonicFrameClock(this, FrameDelayNanos), block)
    }

    /** Workaround for https://github.com/Kotlin/kotlinx.coroutines/issues/3493. */
    private suspend fun TestScope.advanceUntilIdleWorkaround() {
        // Yielding is required to dispatch launches.
        // Advancing is required to execute withFrameNanos callbacks.
        // There need to be enough repetitions to execute all of each of those in the tests.
        repeat(10) {
            yield()
            advanceUntilIdle()
        }
    }

    private companion object {
        const val FrameDelayNanos = 16_000_000L
    }
}