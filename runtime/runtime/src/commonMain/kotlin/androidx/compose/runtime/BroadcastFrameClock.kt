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

package androidx.compose.runtime

import androidx.compose.runtime.snapshots.fastForEach
import androidx.compose.util.createSynchronizedObject
import androidx.compose.util.synchronized
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.Continuation
import kotlin.coroutines.resumeWithException

/**
 * A simple frame clock.
 *
 * This implementation is intended for low-contention environments involving
 * low total numbers of threads in a pool on the order of ~number of CPU cores available for UI
 * recomposition work, while avoiding additional allocation where possible.
 *
 * [onNewAwaiters] will be invoked whenever the number of awaiters has changed from 0 to 1.
 * If [onNewAwaiters] **fails** by throwing an exception it will permanently fail this
 * [BroadcastFrameClock]; all current and future awaiters will resume with the thrown exception.
 */
class BroadcastFrameClock(
    private val onNewAwaiters: (() -> Unit)? = null
) : MonotonicFrameClock {

    private class FrameAwaiter<R>(val onFrame: (Long) -> R, val continuation: Continuation<R>) {
        fun resume(timeNanos: Long) {
            continuation.resumeWith(runCatching { onFrame(timeNanos) })
        }
    }

    private val lock = createSynchronizedObject()
    private var failureCause: Throwable? = null
    private var awaiters = mutableListOf<FrameAwaiter<*>>()
    private var spareList = mutableListOf<FrameAwaiter<*>>()

    /**
     * `true` if there are any callers of [withFrameNanos] awaiting to run for a pending frame.
     */
    val hasAwaiters: Boolean get() = synchronized(lock) { awaiters.isNotEmpty() }

    /**
     * Send a frame for time [timeNanos] to all current callers of [withFrameNanos].
     * The `onFrame` callback for each caller is invoked synchronously during the call to
     * [sendFrame].
     */
    fun sendFrame(timeNanos: Long) {
        synchronized(lock) {
            // Rotate the lists so that if a resumed continuation on an immediate dispatcher
            // bound to the thread calling sendFrame immediately awaits again we don't disrupt
            // iteration of resuming the rest.
            val toResume = awaiters
            awaiters = spareList
            spareList = toResume

            for (i in 0 until toResume.size) {
                toResume[i].resume(timeNanos)
            }
            toResume.clear()
        }
    }

    override suspend fun <R> withFrameNanos(
        onFrame: (Long) -> R
    ): R = suspendCancellableCoroutine { co ->
        lateinit var awaiter: FrameAwaiter<R>
        val hasNewAwaiters = synchronized(lock) {
            val cause = failureCause
            if (cause != null) {
                co.resumeWithException(cause)
                return@suspendCancellableCoroutine
            }
            awaiter = FrameAwaiter(onFrame, co)
            val hadAwaiters = awaiters.isNotEmpty()
            awaiters.add(awaiter)
            !hadAwaiters
        }

        co.invokeOnCancellation {
            synchronized(lock) {
                awaiters.remove(awaiter)
            }
        }

        // Wake up anything that was waiting for someone to schedule a frame
        if (hasNewAwaiters && onNewAwaiters != null) {
            try {
                // BUG: Kotlin 1.4.21 plugin doesn't smart cast for a direct onNewAwaiters() here
                onNewAwaiters.invoke()
            } catch (t: Throwable) {
                // If onNewAwaiters fails, we permanently fail the BroadcastFrameClock.
                fail(t)
            }
        }
    }

    private fun fail(cause: Throwable) {
        synchronized(lock) {
            if (failureCause != null) return
            failureCause = cause
            awaiters.fastForEach { awaiter ->
                awaiter.continuation.resumeWithException(cause)
            }
            awaiters.clear()
        }
    }

    /**
     * Permanently cancel this [BroadcastFrameClock] and cancel all current and future
     * awaiters with [cancellationException].
     */
    fun cancel(
        cancellationException: CancellationException = CancellationException("clock cancelled")
    ) {
        fail(cancellationException)
    }
}
