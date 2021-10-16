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

package kotlinx.coroutines.test

import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi // Since 1.2.1, tentatively till 1.3.0
public interface DelayController {
    /**
     * Returns the current virtual clock-time as it is known to this Dispatcher.
     *
     * @return The virtual clock-time
     */
    @ExperimentalCoroutinesApi // Since 1.2.1, tentatively till 1.3.0
    public val currentTime: Long

    /**
     * Moves the Dispatcher's virtual clock forward by a specified amount of time.
     *
     * The amount the clock is progressed may be larger than the requested `delayTimeMillis` if the code under test uses
     * blocking coroutines.
     *
     * The virtual clock time will advance once for each delay resumed until the next delay exceeds the requested
     * `delayTimeMills`. In the following test, the virtual time will progress by 2_000 then 1 to resume three different
     * calls to delay.
     *
     * ```
     * @Test
     * fun advanceTimeTest() = kotlinx.coroutines.test.runBlockingTest {
     *     foo()
     *     advanceTimeBy(2_000)  // advanceTimeBy(2_000) will progress through the first two delays
     *     // virtual time is 2_000, next resume is at 2_001
     *     advanceTimeBy(2)      // progress through the last delay of 501 (note 500ms were already advanced)
     *     // virtual time is 2_0002
     * }
     *
     * fun CoroutineScope.foo() {
     *     launch {
     *         delay(1_000)    // advanceTimeBy(2_000) will progress through this delay (resume @ virtual time 1_000)
     *         // virtual time is 1_000
     *         delay(500)      // advanceTimeBy(2_000) will progress through this delay (resume @ virtual time 1_500)
     *         // virtual time is 1_500
     *         delay(501)      // advanceTimeBy(2_000) will not progress through this delay (resume @ virtual time 2_001)
     *         // virtual time is 2_001
     *     }
     * }
     * ```
     *
     * @param delayTimeMillis The amount of time to move the CoroutineContext's clock forward.
     * @return The amount of delay-time that this Dispatcher's clock has been forwarded.
     */
    @ExperimentalCoroutinesApi // Since 1.2.1, tentatively till 1.3.0
    public fun advanceTimeBy(delayTimeMillis: Long): Long

    /**
     * Immediately execute all pending tasks and advance the virtual clock-time to the last delay.
     *
     * If new tasks are scheduled due to advancing virtual time, they will be executed before `advanceUntilIdle`
     * returns.
     *
     * @return the amount of delay-time that this Dispatcher's clock has been forwarded in milliseconds.
     */
    @ExperimentalCoroutinesApi // Since 1.2.1, tentatively till 1.3.0
    public fun advanceUntilIdle(): Long

    /**
     * Run any tasks that are pending at or before the current virtual clock-time.
     *
     * Calling this function will never advance the clock.
     */
    @ExperimentalCoroutinesApi // Since 1.2.1, tentatively till 1.3.0
    public fun runCurrent()

    /**
     * Call after test code completes to ensure that the dispatcher is properly cleaned up.
     *
     * @throws UncompletedCoroutinesError if any pending tasks are active, however it will not throw for suspended
     * coroutines.
     */
    @ExperimentalCoroutinesApi // Since 1.2.1, tentatively till 1.3.0
    // @Throws(UncompletedCoroutinesError::class)
    public fun cleanupTestCoroutines()

    /**
     * Run a block of code in a paused dispatcher.
     *
     * By pausing the dispatcher any new coroutines will not execute immediately. After block executes, the dispatcher
     * will resume auto-advancing.
     *
     * This is useful when testing functions that start a coroutine. By pausing the dispatcher assertions or
     * setup may be done between the time the coroutine is created and started.
     */
    @ExperimentalCoroutinesApi // Since 1.2.1, tentatively till 1.3.0
    public suspend fun pauseDispatcher(block: suspend () -> Unit)

    /**
     * Pause the dispatcher.
     *
     * When paused, the dispatcher will not execute any coroutines automatically, and you must call [runCurrent] or
     * [advanceTimeBy], or [advanceUntilIdle] to execute coroutines.
     */
    @ExperimentalCoroutinesApi // Since 1.2.1, tentatively till 1.3.0
    public fun pauseDispatcher()

    /**
     * Resume the dispatcher from a paused state.
     *
     * Resumed dispatchers will automatically progress through all coroutines scheduled at the current time. To advance
     * time and execute coroutines scheduled in the future use, one of [advanceTimeBy],
     * or [advanceUntilIdle].
     */
    @ExperimentalCoroutinesApi // Since 1.2.1, tentatively till 1.3.0
    public fun resumeDispatcher()
}

/**
 * Thrown when a test has completed and there are tasks that are not completed or cancelled.
 */
// todo: maybe convert into non-public class in 1.3.0 (need use-cases for a public exception type)
@ExperimentalCoroutinesApi // Since 1.2.1, tentatively till 1.3.0
public class UncompletedCoroutinesError(message: String, cause: Throwable? = null): Error(message, cause)