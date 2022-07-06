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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.google.common.truth.Truth.assertThat
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BringIntoViewRequestPriorityQueueTest {

    private val queue = BringIntoViewRequestPriorityQueue()
    private val scope = CoroutineScope(Dispatchers.Unconfined)

    private val largeRequest = Request(
        currentBounds = { Rect(Offset.Zero, Size(10f, 10f)) },
        continuation = createContinuation()
    )
    private val mediumRequest = Request(
        currentBounds = { Rect(Offset.Zero, Size(5f, 5f)) },
        continuation = createContinuation()
    )
    private val otherMediumRequest = Request(
        currentBounds = { Rect(Offset(5f, 5f), Size(5f, 5f)) },
        continuation = createContinuation()
    )
    private val smallRequest = Request(
        currentBounds = { Rect(Offset.Zero, Size(1f, 1f)) },
        continuation = createContinuation()
    )

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun request_toString() {
        val request = Request(
            currentBounds = { Rect.Zero },
            continuation = createContinuation()
        )

        assertThat(request.toString()).contains("currentBounds()=Rect.fromLTRB(0.0, 0.0, 0.0, 0.0)")
    }

    @Test
    fun request_toString_withCoroutineName() {
        val request = Request(
            currentBounds = { Rect.Zero },
            continuation = createContinuation(CoroutineName("sam"))
        )

        assertThat(request.toString()).contains("[sam]")
    }

    @Test
    fun empty() {
        assertThat(queue.size).isEqualTo(0)
        assertThat(queue.isEmpty()).isTrue()
    }

    @Test
    fun enqueueSingleRequest_nullBounds() {
        val request = Request(
            currentBounds = { null },
            continuation = createContinuation()
        )
        queue.enqueue(request)

        assertThat(queue.size).isEqualTo(0)
        assertThat(queue.isEmpty()).isTrue()
        assertThat(request.continuation.isCompleted).isTrue()
        assertThat(request.continuation.isCancelled).isFalse()
    }

    @Test
    fun enqueueSingleRequest() {
        queue.enqueue(smallRequest)

        assertThat(queue.size).isEqualTo(1)
        assertThat(queue.isEmpty()).isFalse()
        assertThat(queue.toList()).isEqualTo(listOf(smallRequest.currentBounds()))
        assertThat(smallRequest.continuation.isActive).isTrue()
    }

    @Test
    fun enqueueOverlappingRequests_largeToSmall_isSorted() {
        queue.enqueue(largeRequest)
        queue.enqueue(mediumRequest)
        queue.enqueue(smallRequest)

        val expectedRequests = listOf(largeRequest, mediumRequest, smallRequest)
        assertThat(queue.toList()).isEqualTo(expectedRequests.map { it.currentBounds() })
        expectedRequests.forEach {
            assertThat(it.continuation.isActive).isTrue()
        }
    }

    @Test
    fun enqueueOverlappingRequests_smallToLarge_isSorted() {
        queue.enqueue(smallRequest)
        queue.enqueue(mediumRequest)
        queue.enqueue(largeRequest)

        val expectedRequests = listOf(largeRequest, mediumRequest, smallRequest)
        assertThat(queue.toList()).isEqualTo(expectedRequests.map { it.currentBounds() })
        expectedRequests.forEach {
            assertThat(it.continuation.isActive).isTrue()
        }
    }

    @Test
    fun enqueueOverlappingRequests_outOfOrder_isSorted() {
        queue.enqueue(mediumRequest)
        queue.enqueue(smallRequest)
        queue.enqueue(largeRequest)

        val expectedRequests = listOf(largeRequest, mediumRequest, smallRequest)
        assertThat(queue.toList()).isEqualTo(expectedRequests.map { it.currentBounds() })
        expectedRequests.forEach {
            assertThat(it.continuation.isActive).isTrue()
        }
    }

    @Test
    fun enqueueNonOverlappingRequest() {
        queue.enqueue(smallRequest)
        queue.enqueue(mediumRequest)
        queue.enqueue(largeRequest)

        queue.enqueue(otherMediumRequest)

        // Containing request should be preserved.
        val expectedRequests = listOf(largeRequest, otherMediumRequest)
        assertThat(queue.toList()).isEqualTo(expectedRequests.map { it.currentBounds() })
        expectedRequests.forEach {
            assertThat(it.continuation.isActive).isTrue()
        }
        // Non-overlapping requests should be cancelled.
        listOf(mediumRequest, smallRequest).forEach {
            assertThat(it.continuation.isCancelled).isTrue()
        }
    }

    @Test
    fun enqueuedRequest_isRemoved_whenCancelled() {
        val request = Request(
            currentBounds = { Rect.Zero },
            continuation = createContinuation()
        )
        queue.enqueue(request)

        request.continuation.cancel()

        assertThat(queue.isEmpty()).isTrue()
    }

    @Test
    fun removeAll_cancelsAllContinuations() {
        queue.enqueue(smallRequest)
        queue.enqueue(mediumRequest)
        queue.enqueue(largeRequest)
        val cause = RuntimeException("Oops")

        queue.cancelAndRemoveAll(cause)

        assertThat(queue.isEmpty()).isTrue()
        listOf(smallRequest, mediumRequest, largeRequest).forEach {
            assertThat(it.continuation.isCancelled).isTrue()
        }
    }

    @Test
    fun resumeAndRemoveAll() {
        queue.enqueue(smallRequest)
        queue.enqueue(mediumRequest)
        queue.enqueue(largeRequest)

        queue.resumeAndRemoveWhile { true }

        assertThat(queue.isEmpty()).isTrue()
        listOf(smallRequest, mediumRequest, largeRequest).forEach {
            assertThat(it.continuation.isCompleted).isTrue()
            assertThat(it.continuation.isCancelled).isFalse()
        }
    }

    @Test
    fun resumeAndRemoveWhile_whenAlwaysFalse() {
        queue.enqueue(smallRequest)
        queue.enqueue(mediumRequest)
        queue.enqueue(largeRequest)

        queue.resumeAndRemoveWhile { false }

        assertThat(queue.size).isEqualTo(3)
        listOf(smallRequest, mediumRequest, largeRequest).forEach {
            assertThat(it.continuation.isActive).isTrue()
        }
    }

    @Test
    fun resumeAndRemoveWhile_whenAlwaysTrue() {
        queue.enqueue(smallRequest)
        queue.enqueue(mediumRequest)
        queue.enqueue(largeRequest)

        queue.resumeAndRemoveWhile { true }

        assertThat(queue.isEmpty()).isTrue()
        listOf(smallRequest, mediumRequest, largeRequest).forEach {
            assertThat(it.continuation.isCompleted).isTrue()
            assertThat(it.continuation.isCancelled).isFalse()
        }
    }

    @Test
    fun resumeAndRemoveWhile_removeSingleItem() {
        queue.enqueue(smallRequest)
        queue.enqueue(mediumRequest)
        queue.enqueue(largeRequest)

        queue.resumeAndRemoveWhile {
            // The small request will be the last request in the queue, so the first one passed
            // to this method.
            it == smallRequest.currentBounds()
        }

        val expectedRequests = listOf(largeRequest, mediumRequest)
        assertThat(queue.toList()).isEqualTo(expectedRequests.map { it.currentBounds() })
        assertThat(smallRequest.continuation.isCompleted).isTrue()
        assertThat(smallRequest.continuation.isCancelled).isFalse()
        expectedRequests.forEach {
            assertThat(it.continuation.isActive).isTrue()
        }
    }

    private fun createContinuation(
        context: CoroutineContext = EmptyCoroutineContext
    ): CancellableContinuation<Unit> {
        lateinit var continuation: CancellableContinuation<Unit>
        scope.launch(
            context = context,
            start = CoroutineStart.UNDISPATCHED
        ) {
            suspendCancellableCoroutine {
                continuation = it
            }
        }
        return continuation
    }

    private fun BringIntoViewRequestPriorityQueue.toList() = buildList {
        forEachFromSmallest {
            add(it)
        }
    }.asReversed()
}