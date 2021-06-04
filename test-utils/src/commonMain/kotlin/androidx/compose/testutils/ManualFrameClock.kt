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

package androidx.compose.testutils

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.MonotonicFrameClock

/**
 * A [MonotonicFrameClock] built on a [BroadcastFrameClock] that keeps track of the current time.
 */
// TODO(b/163462047): Consider propagating the onNewAwaiters callback from BroadcastFrameClock
class ManualFrameClock
constructor(
    initialTime: Long = 0L,
) : MonotonicFrameClock {

    private val broadcastClock = BroadcastFrameClock()

    /**
     * The current time of this [frame clock][MonotonicFrameClock], in nanoseconds.
     */
    var currentTime: Long = initialTime
        private set

    /**
     * Whether or not there are currently routines awaiting a frame from this clock.
     *
     * Note that immediately after [advanceClock], coroutines that have received a frame time
     * might not have had their continuation run yet. This can lead to [hasAwaiters] returning
     * false, even though those coroutines may request another frame immediately when they are
     * continued. To work around this caveat, make sure that those coroutines have run before
     * calling [hasAwaiters]. For example, `withContext(AndroidUiDispatcher.Main) {}` will finish
     * when all work currently scheduled on the AndroidUiDispatcher is done.
     */
    val hasAwaiters: Boolean get() = broadcastClock.hasAwaiters

    /**
     * Advances the clock by the given number of [nanoseconds][nanos]. One frame will be sent,
     * with a frame time that is [nanos] nanoseconds after the last frame time (or after the
     * initial time, if no frames have been sent yet).
     */
    fun advanceClock(nanos: Long) {
        require(nanos > 0) {
            "Cannot advance the clock by $nanos ns, only values greater than 0 are allowed"
        }
        // Make sure multiple invocations of advanceClock can't run concurrently
        @Suppress("DEPRECATION_ERROR")
        synchronized(broadcastClock) {
            currentTime += nanos
            broadcastClock.sendFrame(currentTime)
        }
    }

    override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
        return broadcastClock.withFrameNanos(onFrame)
    }
}

/**
 * Advances the clock by the given number of [milliseconds][millis]. See
 * [ManualFrameClock.advanceClock] for more information.
 */
fun ManualFrameClock.advanceClockMillis(millis: Long) = advanceClock(millis * 1_000_000)
