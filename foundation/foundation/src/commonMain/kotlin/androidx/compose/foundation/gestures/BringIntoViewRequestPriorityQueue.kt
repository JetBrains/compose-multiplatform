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

package androidx.compose.foundation.gestures

import androidx.compose.foundation.gestures.ContentInViewModifier.Request
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.ui.geometry.Rect
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException

/**
 * Ongoing requests from [ContentInViewModifier.bringChildIntoView], with the invariant that it is
 * always sorted by overlapping order: each item's bounds completely overlaps the next item.
 *
 * Requests are enqueued by calling [enqueue], which inserts the request at the correct position
 * and cancels and removes any requests that it interrupts. When a request is enqueued, its
 * continuation has a completion handler set that will remove the request from the queue when
 * it's cancelled.
 *
 * One a request has been enqueued, it cannot be removed without completing the continuation.
 * This helps prevent leaking requests. Requests are removed in two ways:
 *  1. By an [enqueue] call for a request that doesn't overlap them, or
 *  2. By calling [cancelAndRemoveAll], which does exactly what it says.
 */
@OptIn(ExperimentalContracts::class)
internal class BringIntoViewRequestPriorityQueue {
    private val requests = mutableVectorOf<Request>()

    val size: Int get() = requests.size

    fun isEmpty(): Boolean = requests.isEmpty()

    /**
     * Adds [request] to the queue, enforcing the invariants of that list:
     *  - It will be inserted in the correct position to preserve sorted order.
     *  - Any requests not contains by or containing this request will be evicted.
     *
     * After this function is called, [request] will always be either resumed or cancelled
     * before it's removed from the queue, so the caller no longer needs to worry about
     * completing it.
     *
     *  @return True if the request was enqueued, false if it was not, e.g. because the rect
     *  function returned null.
     */
    fun enqueue(request: Request): Boolean {
        val requestBounds = request.currentBounds() ?: run {
            request.continuation.resume(Unit)
            return false
        }

        // If the request is cancelled for any reason, remove it from the queue.
        request.continuation.invokeOnCancellation {
            requests.remove(request)
        }

        for (i in requests.indices.reversed()) {
            val r = requests[i]
            val rBounds = r.currentBounds() ?: continue
            val intersection = requestBounds.intersect(rBounds)
            if (intersection == requestBounds) {
                // The current item fully contains the new request, so insert it after.
                requests.add(i + 1, request)
                return true
            } else if (intersection != rBounds) {
                // The new request and the current item do not fully overlap, so cancel the
                // current item and all requests after it, remove them, then continue the
                // search to the next-largest request.
                val cause = CancellationException(
                    "bringIntoView call interrupted by a newer, non-overlapping call"
                )
                for (j in requests.size - 1..i) {
                    // This mutates the list while iterating, but since we're iterating
                    // backwards in both cases, it's fine.
                    // Cancelling the continuation will remove the request from the queue.
                    requests[i].continuation.cancel(cause)
                }
            }
            // Otherwise the new request fully contains the current item, so keep searching up
            // the queue.
        }

        // No existing request contained the new one. Either the new requests contains all
        // existing requests and it should be the new head of the queue, or all other requests
        // were removed.
        requests.add(0, request)
        return true
    }

    inline fun forEachFromSmallest(block: (bounds: Rect?) -> Unit) {
        contract { callsInPlace(block) }
        requests.forEachReversed { block(it.currentBounds()) }
    }

    fun resumeAndRemoveAll() {
        for (i in requests.indices) {
            requests[i].continuation.resume(Unit)
        }
        requests.clear()
    }

    inline fun resumeAndRemoveWhile(block: (bounds: Rect?) -> Boolean) {
        contract { callsInPlace(block) }
        while (requests.isNotEmpty()) {
            if (block(requests.last().currentBounds())) {
                requests.removeAt(requests.lastIndex).continuation.resume(Unit)
            } else {
                return
            }
        }
    }

    fun cancelAndRemoveAll(cause: Throwable?) {
        // The continuation completion handler will remove the request from the queue when it's
        // cancelled, so we need to make a copy of the list before iterating to avoid concurrent
        // mutation.
        requests.map { it.continuation }.forEach {
            it.cancel(cause)
        }
        check(requests.isEmpty())
    }
}