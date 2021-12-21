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

package androidx.compose.runtime

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import androidx.compose.util.createSynchronizedObject
import androidx.compose.util.synchronized

/**
 * A boolean open or closed latch for awaiting a single repeating event, like pending
 * recompositions. Closing while already closed or opening while already open is a no-op;
 * only one event producer should be responsible for opening or closing the latch.
 *
 * This implementation is intended for low-contention environments involving
 * low total numbers of threads in a pool on the order of ~number of CPU cores available for UI
 * recomposition work, while avoiding additional allocation where possible.
 */
internal class Latch {

    private val lock = createSynchronizedObject()
    private var awaiters = mutableListOf<Continuation<Unit>>()
    private var spareList = mutableListOf<Continuation<Unit>>()

    private var _isOpen = true
    val isOpen get() = synchronized(lock) { _isOpen }

    inline fun <R> withClosed(block: () -> R): R {
        closeLatch()
        return try {
            block()
        } finally {
            openLatch()
        }
    }

    fun closeLatch() {
        synchronized(lock) {
            _isOpen = false
        }
    }

    fun openLatch() {
        synchronized(lock) {
            if (isOpen) return

            // Rotate the lists so that if a resumed continuation on an immediate dispatcher
            // bound to the thread calling openLatch immediately awaits again we don't disrupt
            // iteration of resuming the rest. This is also why we set isClosed before resuming.
            val toResume = awaiters
            awaiters = spareList
            spareList = toResume
            _isOpen = true

            for (i in 0 until toResume.size) {
                toResume[i].resume(Unit)
            }
            toResume.clear()
        }
    }

    suspend fun await() {
        if (isOpen) return

        suspendCancellableCoroutine<Unit> { co ->
            synchronized(lock) {
                awaiters.add(co)
            }

            co.invokeOnCancellation {
                synchronized(lock) {
                    awaiters.remove(co)
                }
            }
        }
    }
}
