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

package androidx.compose.runtime.dispatch

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.Continuation

/**
 * A simple frame clock.
 *
 * This implementation is intended for low-contention environments involving
 * low total numbers of threads in a pool on the order of ~number of CPU cores available for UI
 * recomposition work, while avoiding additional allocation where possible.
 *
 * [onNewAwaiters] will be invoked whenever the number of awaiters has changed from 0 to 1.
 */
class BroadcastFrameClock(
    private val onNewAwaiters: (() -> Unit)? = null
) : MonotonicFrameClock {

    private class FrameAwaiter<R>(val onFrame: (Long) -> R, val continuation: Continuation<R>) {
        fun resume(timeNanos: Long) {
            continuation.resumeWith(runCatching { onFrame(timeNanos) })
        }
    }

    private val lock = Any()
    private var awaiters = mutableListOf<FrameAwaiter<*>>()
    private var spareList = mutableListOf<FrameAwaiter<*>>()

    @Suppress("DEPRECATION_ERROR")
    val hasAwaiters: Boolean get() = synchronized(lock) { awaiters.isNotEmpty() }

    @Suppress("UNCHECKED_CAST")
    fun sendFrame(timeNanos: Long) {
        @Suppress("DEPRECATION_ERROR")
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
        val awaiter = FrameAwaiter(onFrame, co)
        @Suppress("DEPRECATION_ERROR")
        val hasNewAwaiters = synchronized(lock) {
            val hadAwaiters = awaiters.isNotEmpty()
            awaiters.add(awaiter)
            !hadAwaiters
        }

        co.invokeOnCancellation {
            @Suppress("DEPRECATION_ERROR")
            synchronized(lock) {
                awaiters.remove(awaiter)
            }
        }

        // Wake up anything that was waiting for someone to schedule a frame
        if (hasNewAwaiters) onNewAwaiters?.invoke()
    }
}
