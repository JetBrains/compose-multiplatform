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

import androidx.compose.runtime.MonotonicFrameClock
import kotlin.coroutines.ContinuationInterceptor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.TestCoroutineScheduler

private const val DefaultFrameDelay = 16_000_000L

/**
 * A [MonotonicFrameClock] with a time source controlled by a `kotlinx-coroutines-test`
 * [TestCoroutineScheduler]. This frame clock may be used to consistently drive time under
 * controlled tests.
 *
 * Calls to [withFrameNanos] will schedule an upcoming frame [frameDelayNanos] nanoseconds in the
 * future by launching into [coroutineScope] if such a frame has not yet been scheduled. The
 * current frame time for [withFrameNanos] is provided by [delayController]. It is strongly
 * suggested that [coroutineScope] contain the test dispatcher controlled by [delayController].
 *
 * @param coroutineScope The [CoroutineScope] used to simulate the main thread and schedule frames
 * on. It must contain a [TestCoroutineScheduler].
 * @param frameDelayNanos The number of nanoseconds to [delay] between executing frames.
 * @param onPerformTraversals Called with the frame time of the frame that was just executed,
 * after running all `withFrameNanos` callbacks, but before resuming their callers' continuations.
 * Any continuations resumed while running frame callbacks or [onPerformTraversals] will not be
 * dispatched until after [onPerformTraversals] finishes. If [onPerformTraversals] throws, all
 * `withFrameNanos` callers will be cancelled.
 */
// This is intentionally not OptIn, because we want to communicate to consumers that by using this
// API, they're also transitively getting all the experimental risk of using the experimental API
// in the kotlinx testing library. DO NOT MAKE OPT-IN!
@ExperimentalCoroutinesApi
@ExperimentalTestApi
class TestMonotonicFrameClock(
    private val coroutineScope: CoroutineScope,
    @get:Suppress("MethodNameUnits") // Nanos for high-precision animation clocks
    val frameDelayNanos: Long = DefaultFrameDelay,
    private val onPerformTraversals: (Long) -> Unit = {}
) : MonotonicFrameClock {
    private val delayController =
        requireNotNull(coroutineScope.coroutineContext[TestCoroutineScheduler]) {
            "coroutineScope should have TestCoroutineScheduler"
        }
    private val parentInterceptor = coroutineScope.coroutineContext[ContinuationInterceptor]
    private val lock = Any()
    private var awaiters = mutableListOf<(Long) -> Unit>()
    private var spareAwaiters = mutableListOf<(Long) -> Unit>()
    private var scheduledFrameDispatch = false
    private val frameDeferringInterceptor = FrameDeferringContinuationInterceptor(parentInterceptor)

    /**
     * Returns whether there are any awaiters on this clock.
     */
    val hasAwaiters: Boolean
        get() = frameDeferringInterceptor.hasTrampolinedTasks || synchronized(lock) {
            awaiters.isNotEmpty()
        }

    /**
     * A [CoroutineDispatcher] that will defer continuation resumptions requested within
     * [withFrameNanos] calls to until after the frame callbacks have finished running. Resumptions
     * will then be dispatched before resuming the continuations from the [withFrameNanos] calls
     * themselves.
     */
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalTestApi
    @ExperimentalTestApi
    val continuationInterceptor: ContinuationInterceptor get() = frameDeferringInterceptor

    /**
     * Schedules [onFrame] to be ran on the next "fake" frame, and schedules the task to actually
     * perform that frame if it hasn't already been scheduled.
     *
     * Instead of waiting for a vsync message to perform the next frame, it simply calls the
     * coroutine [delay] function for the test frame time [frameDelayMillis] (which the underlying
     * test coroutine scheduler will actually complete immediately without waiting), and then run
     * all scheduled tasks.
     */
    override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R =
        suspendCancellableCoroutine { co ->
            synchronized(lock) {
                awaiters.add { frameTime ->
                    co.resumeWith(runCatching { onFrame(frameTime) })
                }
                if (!scheduledFrameDispatch) {
                    scheduledFrameDispatch = true
                    coroutineScope.launch {
                        delay(frameDelayMillis)
                        performFrame()
                    }
                }
            }
        }

    /**
     * Executes all scheduled frame callbacks, and then dispatches any continuations that were
     * resumed by the callbacks and deferred by [continuationInterceptor].
     *
     * This method performs a subset of the responsibilities of `Choreographer.doFrame` on
     * Android, which is usually responsible for executing animation frames and coroutines, and also
     * "performing traversals", which practically just means doing the layout pass on the view tree.
     * Since this method replaces `doFrame`, it also needs to trigger the compose layout pass
     * (see b/222093277).
     *
     * Typically, the only task that will have been enqueued will be the `Recomposer`'s
     * `runRecomposeAndApplyChanges`' call to [withFrameNanos] – any app coroutines waiting for the
     * next frame will actually be dispatched by `runRecomposeAndApplyChanges`'
     * `BroadcastFrameClock`, not this method.
     */
    private fun performFrame() {
        frameDeferringInterceptor.runWithoutResumingCoroutines {
            // This is set after acquiring the lock in case the virtual time was advanced while
            // waiting for it.
            val frameTime: Long
            val toRun = synchronized(lock) {
                check(scheduledFrameDispatch)

                frameTime = delayController.currentTime * 1_000_000
                scheduledFrameDispatch = false
                awaiters.also {
                    awaiters = spareAwaiters
                    spareAwaiters = it
                }
            }

            // Because runningFrameCallbacks is still true, all these resumptions will be queued to
            // toRunTrampolined.
            toRun.forEach { it(frameTime) }
            toRun.clear()

            onPerformTraversals(frameTime)
        }
    }
}

/**
 * The frame delay time for the [TestMonotonicFrameClock] in milliseconds.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
@get:ExperimentalTestApi // Required to annotate Java-facing APIs
@ExperimentalTestApi // Required by kotlinc to use frameDelayNanos
val TestMonotonicFrameClock.frameDelayMillis: Long
    get() = frameDelayNanos / 1_000_000
