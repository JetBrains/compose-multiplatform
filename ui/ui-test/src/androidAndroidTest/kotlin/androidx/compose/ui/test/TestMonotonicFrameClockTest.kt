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
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTestApi::class)
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
            var startTime = -1L
            fun expectedFrameTimeNanos() = startTime + FrameDelayNanos
            fun expectedFrameTimeMillis() = expectedFrameTimeNanos() / 1_000_000
            val counter = TestCounter()

            withTestClockContext(
                onPerformTraversals = { frameTime ->
                    counter.expect(6, "perform traversals")
                    assertEquals("current time traversals", expectedFrameTimeMillis(), currentTime)
                    assertEquals("frame time traversals", expectedFrameTimeNanos(), frameTime)
                }
            ) {
                startTime = currentTime
                launch {
                    counter.expect(1, "in coroutine 1")
                    withFrameNanos { frameTime ->
                        counter.expect(4, "in frame callback 1")
                        assertEquals("current time 1", expectedFrameTimeMillis(), currentTime)
                        assertEquals("frame time 1", expectedFrameTimeNanos(), frameTime)
                    }
                    counter.expect(7, "after resuming frame callback 1")
                }
                launch {
                    counter.expect(2, "in coroutine 2")
                    withFrameNanos { frameTime ->
                        counter.expect(5, "in frame callback 2")
                        assertEquals("current time 2", expectedFrameTimeMillis(), currentTime)
                        assertEquals("frame time 2", expectedFrameTimeNanos(), frameTime)
                    }
                    counter.expect(8, "after resuming frame callback 2")
                }
                counter.expect(3)
                advanceUntilIdleWorkaround()
                counter.expect(9, "final result")
            }
        }

    @Test
    fun testMonotonicFrameClockRunsFrame_standardDispatcher() =
        runTest(StandardTestDispatcher()) {
            var startTime = -1L
            fun expectedFrameTimeNanos() = startTime + FrameDelayNanos
            fun expectedFrameTimeMillis() = expectedFrameTimeNanos() / 1_000_000
            val counter = TestCounter()

            withTestClockContext(
                onPerformTraversals = { frameTime ->
                    counter.expect(6, "perform traversals")
                    assertEquals("current time traversals", expectedFrameTimeMillis(), currentTime)
                    assertEquals("frame time traversals", expectedFrameTimeNanos(), frameTime)
                }
            ) {
                startTime = currentTime
                launch {
                    counter.expect(2, "in coroutine 1")
                    withFrameNanos { frameTime ->
                        counter.expect(4, "in frame callback 1")
                        assertEquals("current time 1", expectedFrameTimeMillis(), currentTime)
                        assertEquals("frame time 1", expectedFrameTimeNanos(), frameTime)
                    }
                    counter.expect(7, "after resuming frame callback 1")
                }
                launch {
                    counter.expect(3, "in coroutine 2")
                    withFrameNanos { frameTime ->
                        counter.expect(5, "in frame callback 2")
                        assertEquals("current time 2", expectedFrameTimeMillis(), currentTime)
                        assertEquals("frame time 2", expectedFrameTimeNanos(), frameTime)
                    }
                    counter.expect(8, "after resuming frame callback 2")
                }
                counter.expect(1)
                advanceUntilIdleWorkaround()
                counter.expect(9, "final result")
            }
        }

    @Test
    fun testMonotonicFrameClockDispatcherDefersResumesInsideFrame_unconfinedDispatcher() =
        runTest(UnconfinedTestDispatcher()) {
            val counter = TestCounter()

            withTestClockContext(
                onPerformTraversals = {
                    counter.expect(9, "perform traversals")
                }
            ) {
                var continuation: Continuation<Unit>? = null

                launch {
                    counter.expect(1, "in external coroutine")
                    suspendCancellableCoroutine { continuation = it }
                    counter.expect(12, "after resuming external coroutine")
                }

                launch {
                    counter.expect(2)
                    withFrameNanos {
                        counter.expect(5, "in frame callback 1")
                        // Coroutines launched inside withFrameNanos shouldn't be dispatched until
                        // after all frame callbacks are complete.
                        launch {
                            counter.expect(10, "in \"effect\" coroutine")
                        }
                        counter.expect(6, "after launching \"effect\" coroutine")
                    }
                    counter.expect(11, "after resuming frame callback 1")
                }

                launch {
                    counter.expect(3)
                    withFrameNanos {
                        counter.expect(7, "in frame callback 2")
                        // Coroutines that were already suspended and are resumed inside
                        // withFrameNanos shouldn't be dispatched until after all frame callbacks
                        // are complete either.
                        continuation!!.resume(Unit)
                        counter.expect(8, "after resuming external continuation")
                    }
                    counter.expect(13, "after resuming frame callback 2")
                }

                counter.expect(4)
                advanceUntilIdleWorkaround()
                counter.expect(14, "final result")
            }
        }

    @Test
    fun testMonotonicFrameClockDispatcherDefersResumesInsideFrame_standardDispatcher() =
        runTest(StandardTestDispatcher()) {
            val counter = TestCounter()

            withTestClockContext(
                onPerformTraversals = {
                    counter.expect(9, "perform traversals")
                }
            ) {
                var continuation: Continuation<Unit>? = null

                launch {
                    counter.expect(2, "in external coroutine")
                    suspendCancellableCoroutine { continuation = it }
                    counter.expect(12, "after resuming external coroutine")
                }

                launch {
                    counter.expect(3)
                    withFrameNanos {
                        counter.expect(5, "in frame callback 1")
                        // Coroutines launched inside withFrameNanos shouldn't be dispatched until
                        // after all frame callbacks are complete.
                        launch {
                            counter.expect(10, "in \"effect\" coroutine")
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
                    counter.expect(13, "after resuming frame callback 2")
                }

                counter.expect(1)
                advanceUntilIdleWorkaround()
                counter.expect(14, "final result")
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

    @Test
    fun performTraversalsThrows_cancelsClockScope_unconfinedDispatcher() {
        test_performTraversalsThrows_cancelsClockScope(UnconfinedTestDispatcher())
    }

    @Test
    fun performTraversalsThrows_cancelsClockScope_standardDispatcher() {
        test_performTraversalsThrows_cancelsClockScope(StandardTestDispatcher())
    }

    @Test
    fun performTraversalsThrows_andFrameThrows_cancelsClockScope_unconfinedDispatcher() {
        val traversalFailure = RuntimeException("traversal failure")
        val frameFailure = RuntimeException("frame failure")

        val testFailure = assertFailsWith<RuntimeException> {
            runTest(UnconfinedTestDispatcher()) {
                // Need to override the exception handler installed by runTest so it won't fail the
                // test unnecessarily.
                val clock = TestMonotonicFrameClock(
                    coroutineScope = this,
                    onPerformTraversals = { throw traversalFailure }
                )

                withTestClockContext(clock) {
                    launch {
                        withFrameNanos { throw frameFailure }
                    }
                }
            }
        }

        assertThat(testFailure).isSameInstanceAs(frameFailure)
        assertThat(testFailure.suppressedExceptions).contains(traversalFailure)
    }

    @Test
    fun performTraversalsThrows_andFrameThrows_cancelsClockScope_standardDispatcher() {
        val traversalFailure = RuntimeException("traversal failure")
        val frameFailure = RuntimeException("frame failure")

        val testFailure = assertFailsWith<RuntimeException> {
            runTest(StandardTestDispatcher()) {
                // Need to override the exception handler installed by runTest so it won't fail the
                // test unnecessarily.
                val clock = TestMonotonicFrameClock(
                    coroutineScope = this,
                    onPerformTraversals = { throw traversalFailure }
                )

                withTestClockContext(clock) {
                    launch {
                        withFrameNanos { throw frameFailure }
                    }
                }
            }
        }

        assertThat(testFailure).isSameInstanceAs(traversalFailure)
        assertThat(testFailure.suppressedExceptions).contains(frameFailure)
    }

    @Test
    fun performTraversalsThrows_resumesFrameCoroutines_unconfinedDispatcher() {
        test_performTraversalsThrows_resumesFrameCoroutines(UnconfinedTestDispatcher())
    }

    @Test
    fun performTraversalsThrows_resumesFrameCoroutines_standardDispatcher() {
        test_performTraversalsThrows_resumesFrameCoroutines(StandardTestDispatcher())
    }

    @Test
    fun performTraversalsThrows_reportedOnFrameExceptions_unconfinedDispatcher() {
        var frame1Resumed = false
        var internalError1: Throwable? = null
        var internalError2: Throwable? = null
        // Don't set the parent, this job will get cancelled.
        val clockJob = Job()
        val traversalFailure = RuntimeException("traversal failed")
        val frameFailure1 = RuntimeException("frame 1 callback failed")
        val frameFailure2 = RuntimeException("frame 2 callback failed")

        runTest(UnconfinedTestDispatcher()) {
            // Need to override the exception handler installed by runTest so it won't fail the
            // test unnecessarily.
            val clockScope = this + clockJob + CoroutineExceptionHandler { _, _ ->
                // Ignore
            }
            val clock = TestMonotonicFrameClock(
                coroutineScope = clockScope,
                onPerformTraversals = { throw traversalFailure }
            )

            withTestClockContext(clock) {
                launch {
                    withFrameNanos {}
                    frame1Resumed = true
                }
                launch {
                    internalError1 = assertFailsWith<RuntimeException> {
                        withFrameNanos { throw frameFailure1 }
                    }
                }
                launch {
                    internalError2 = assertFailsWith<RuntimeException> {
                        withFrameNanos { throw frameFailure2 }
                    }
                }
            }
        }

        // Siblings should still resume successfully.
        assertThat(frame1Resumed).isTrue()

        // But failed coroutines should include both exceptions.
        assertThat(internalError1).isSameInstanceAs(frameFailure1)
        assertThat(internalError1!!.suppressedExceptions).contains(traversalFailure)
        assertThat(internalError2).isSameInstanceAs(frameFailure2)
        assertThat(internalError2!!.suppressedExceptions).contains(traversalFailure)
    }

    @Test
    fun performTraversalsThrows_reportedOnFrameExceptions_standardDispatcher() {
        var frame1Resumed = false
        var internalError1: Throwable? = null
        var internalError2: Throwable? = null
        // Don't set the parent, this job will get cancelled.
        val clockJob = Job()
        val traversalFailure = RuntimeException("traversal failed")
        val frameFailure1 = RuntimeException("frame 1 callback failed")
        val frameFailure2 = RuntimeException("frame 2 callback failed")

        runTest(StandardTestDispatcher()) {
            // Need to override the exception handler installed by runTest so it won't fail the
            // test unnecessarily.
            val clockScope = this + clockJob + CoroutineExceptionHandler { _, _ ->
                // Ignore
            }
            val clock = TestMonotonicFrameClock(
                coroutineScope = clockScope,
                onPerformTraversals = { throw traversalFailure }
            )

            withTestClockContext(clock) {
                launch {
                    withFrameNanos {}
                    frame1Resumed = true
                }
                launch {
                    internalError1 = assertFailsWith<RuntimeException> {
                        withFrameNanos { throw frameFailure1 }
                    }
                }
                launch {
                    internalError2 = assertFailsWith<RuntimeException> {
                        withFrameNanos { throw frameFailure2 }
                    }
                }
            }
        }

        // Siblings should still resume successfully.
        assertThat(frame1Resumed).isTrue()

        // Contrary to the unconfined dispatcher case, exceptions here won't have been dispatched
        // until after the frame finishes, so the test clock won't have added the suppressed
        // exceptions. However, in that case, they won't have a chance to fail the test before the
        // test clock exception anyway, so it's fine.
        assertThat(internalError1).isSameInstanceAs(frameFailure1)
        assertThat(internalError2).isSameInstanceAs(frameFailure2)
    }

    private fun test_performTraversalsThrows_cancelsClockScope(dispatcher: TestDispatcher) {
        val traversalFailure = RuntimeException("traversal failure")

        val testFailure = assertFailsWith<RuntimeException> {
            runTest(dispatcher) {
                // Need to override the exception handler installed by runTest so it won't fail the
                // test unnecessarily.
                val clock = TestMonotonicFrameClock(
                    coroutineScope = this,
                    onPerformTraversals = { throw traversalFailure }
                )

                withTestClockContext(clock) {
                    launch {
                        withFrameNanos {}
                    }
                }
            }
        }

        assertThat(testFailure).isSameInstanceAs(traversalFailure)
    }

    private fun test_performTraversalsThrows_resumesFrameCoroutines(dispatcher: TestDispatcher) {
        var frame1Resumed = false
        var frame2Resumed = false
        // Don't set the parent, this job will get cancelled.
        val clockJob = Job()

        runTest(dispatcher) {
            // Need to override the exception handler installed by runTest so it won't fail the
            // test unnecessarily.
            val clockScope = this + clockJob + CoroutineExceptionHandler { _, _ ->
                // Ignore
            }
            val clock = TestMonotonicFrameClock(
                coroutineScope = clockScope,
                onPerformTraversals = { throw RuntimeException("traversal failed") }
            )

            // Run these with a separate job so they don't get cancelled by their parent.
            withTestClockContext(clock) {
                launch {
                    withFrameNanos {}
                    frame1Resumed = true
                }
                launch {
                    withFrameNanos {}
                    frame2Resumed = true
                }
            }
        }

        assertThat(frame1Resumed).isTrue()
        assertThat(frame2Resumed).isTrue()
    }

    private suspend fun CoroutineScope.withTestClockContext(
        onPerformTraversals: (Long) -> Unit = {},
        block: suspend CoroutineScope.() -> Unit
    ) {
        val testClock = TestMonotonicFrameClock(this, FrameDelayNanos, onPerformTraversals)
        withTestClockContext(testClock, block)
    }

    @OptIn(ExperimentalTestApi::class)
    private suspend fun withTestClockContext(
        testClock: TestMonotonicFrameClock,
        block: suspend CoroutineScope.() -> Unit
    ) {
        withContext(testClock + testClock.continuationInterceptor, block)
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