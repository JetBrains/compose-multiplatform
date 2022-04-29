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

package androidx.compose.foundation.relocation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.modifier.ModifierLocalProvider
import androidx.compose.ui.modifier.ProvidableModifierLocal
import androidx.compose.ui.platform.debugInspectorInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

/**
 * A parent that can respond to [bringChildIntoView] requests from its children, and scroll so that
 * the item is visible on screen. To apply a responder to an element, pass it to the
 * [bringIntoViewResponder] modifier.
 *
 * When a component calls [BringIntoViewRequester.bringIntoView], the
 * [BringIntoView ModifierLocal][ModifierLocalBringIntoViewParent] is read to gain access to the
 * [BringIntoViewResponder], which is responsible for, in order:
 *
 * 1. Calculating a rectangle that its parent responder should bring into view by returning it from
 *    [calculateRectForParent].
 * 2. Performing any scroll or other layout adjustments needed to ensure the requested rectangle is
 *    brought into view in [bringChildIntoView].
 *
 * Here is a sample defining a custom [BringIntoViewResponder]:
 * @sample androidx.compose.foundation.samples.BringIntoViewResponderSample
 *
 * Here is a sample where a composable is brought into view:
 * @sample androidx.compose.foundation.samples.BringIntoViewSample
 *
 * Here is a sample where a part of a composable is brought into view:
 * @sample androidx.compose.foundation.samples.BringPartOfComposableIntoViewSample
 *
 * @see BringIntoViewRequester
 */
@ExperimentalFoundationApi
interface BringIntoViewResponder {

    /**
     * Return the rectangle in this node that should be brought into view by this node's parent,
     * in coordinates relative to this node. If this node needs to adjust itself to bring
     * [localRect] into view, the returned rectangle should be the destination rectangle that
     * [localRect] will eventually occupy once this node's content is adjusted.
     *
     * @param localRect The rectangle that should be brought into view, relative to this node. This
     * will be the same rectangle passed to [bringChildIntoView].
     * @return The rectangle in this node that should be brought into view itself, relative to this
     * node. If this node needs to scroll to bring [localRect] into view, the returned rectangle
     * should be the destination rectangle that [localRect] will eventually occupy, once the
     * scrolling animation is finished.
     */
    @ExperimentalFoundationApi
    fun calculateRectForParent(localRect: Rect): Rect

    /**
     * Bring this specified rectangle into bounds by making this scrollable parent scroll
     * appropriately.
     *
     * This method should ensure that only one call is being handled at a time. If you use Compose's
     * `Animatable` you get this for free, since it will cancel the previous animation when a new
     * one is started while preserving velocity.
     *
     * @param localRect The rectangle that should be brought into view, relative to this node. This
     * is the same rectangle that will have been passed to [calculateRectForParent].
     */
    @ExperimentalFoundationApi
    suspend fun bringChildIntoView(localRect: Rect)
}

/**
 * A parent that can respond to [BringIntoViewRequester] requests from its children, and scroll so
 * that the item is visible on screen. See [BringIntoViewResponder] for more details about how
 * this mechanism works.
 *
 * @sample androidx.compose.foundation.samples.BringIntoViewResponderSample
 * @sample androidx.compose.foundation.samples.BringIntoViewSample
 *
 * @see BringIntoViewRequester
 */
@ExperimentalFoundationApi
fun Modifier.bringIntoViewResponder(
    responder: BringIntoViewResponder
): Modifier = composed(debugInspectorInfo {
    name = "bringIntoViewResponder"
    properties["responder"] = responder
}) {
    val defaultParent = rememberDefaultBringIntoViewParent()
    val modifier = remember(defaultParent) {
        BringIntoViewResponderModifier(defaultParent)
    }
    modifier.responder = responder
    return@composed modifier
}

/**
 * A modifier that holds state and modifier implementations for [bringIntoViewResponder]. It has
 * access to the next [BringIntoViewParent] via [BringIntoViewChildModifier] and additionally
 * provides itself as the [BringIntoViewParent] for subsequent modifiers. This class is responsible
 * for recursively propagating requests up the responder chain.
 */
@OptIn(ExperimentalFoundationApi::class)
private class BringIntoViewResponderModifier(
    defaultParent: BringIntoViewParent
) : BringIntoViewChildModifier(defaultParent),
    ModifierLocalProvider<BringIntoViewParent?>,
    BringIntoViewParent {

    lateinit var responder: BringIntoViewResponder

    override val key: ProvidableModifierLocal<BringIntoViewParent?>
        get() = ModifierLocalBringIntoViewParent
    override val value: BringIntoViewParent
        get() = this

    /**
     * Stores the rectangle and coroutine [Job] of the newest request to be handled by
     * [bringChildIntoView].
     *
     * This property is not guarded by a lock since requests should only be made on the main thread.
     *
     * ## Request queuing
     *
     * When multiple requests are received concurrently, i.e. a new request comes in before the
     * previous one finished, they are put in a queue. The queue is implicit – this property only
     * stores a reference to the tail of the queue. The head of the queue is the oldest request that
     * hasn't finished executing yet. Each subsequent request in the queue waits for the previous
     * request to finish by [joining][Job.join] on its [Job]. When the oldest request finishes (or
     * is cancelled), it is "removed" from the queue by simply returning from [bringChildIntoView].
     * This completes its job, which resumes the next request, and makes that the new "head". When
     * the last request finishes it "clears" the queue by setting this property to null.
     *
     * ## Interruption
     *
     * There is one case not covered above: If a request comes in for a rectangle that is
     * not [fully-overlapped][completelyOverlaps] by the previous request, the new request will
     * "interrupt" all previous requests and indirectly remove them from the queue. The reason only
     * overlapping requests are queued is because if rectangle A contains rectangle B, then when A
     * is fully on screen, then B is also fully on screen. By satisfying A first, both A and B can
     * be satisfied in a single call to the [BringIntoViewResponder]. If B were to interrupt A, A
     * might never be fully brought into view (see b/216790855).
     *
     * Interruption works by immediately dispatching the new request to the
     * [BringIntoViewResponder]. Note that this class does not actually cancel the previously-
     * running request's [Job] – the responder is responsible for cancelling any in-progress request
     * and immediately start handling the new one. In particular, if the responder is using
     * Compose's animation, the animation will handle cancellation itself and preserve velocity.
     * When the previously-running request is cancelled, its job will complete and the next request
     * in the queue will try to run. However, it will see that another request was already
     * dispatched, and return early, completing its job as well. Etc etc until all requests that
     * were received before the interrupting one have returned from their [bringChildIntoView]
     * calls.
     *
     * The way a request determines if it's been interrupted is by comparing the previous request
     * to the value of [newestDispatchedRequest]. [newestDispatchedRequest] is only set just before
     * the [BringIntoViewResponder] is called.
     */
    private var newestReceivedRequest: Pair<Rect, Job>? = null

    /**
     * The last queued request to have been sent to the [BringIntoViewResponder], or null if no
     * requests are in progress.
     */
    private var newestDispatchedRequest: Pair<Rect, Job>? = null

    /**
     * Responds to a child's request by first converting [rect] into this node's [LayoutCoordinates]
     * and then, concurrently, calling the [responder] and the [parent] to handle the request.
     */
    override suspend fun bringChildIntoView(rect: Rect, childCoordinates: LayoutCoordinates) {
        coroutineScope {
            val layoutCoordinates = layoutCoordinates ?: return@coroutineScope
            if (!childCoordinates.isAttached) return@coroutineScope
            val localRect = layoutCoordinates.localRectOf(childCoordinates, rect)

            // Immediately make this request the tail of the queue, before suspending, so that
            // any requests that come in while suspended will join on this one.
            // Note that this job is not the caller's job, since coroutineScope introduces a child
            // job.
            // For more information about how requests are queued, see the kdoc on newestRequest.
            val requestJob = coroutineContext.job
            val thisRequest = Pair(localRect, requestJob)
            val previousRequest = newestReceivedRequest
            newestReceivedRequest = thisRequest

            try {
                // In the simplest case there are no ongoing requests, so just dispatch.
                if (previousRequest == null ||
                    // If there's an ongoing request but it won't satisfy this request, then
                    // interrupt it.
                    !previousRequest.first.completelyOverlaps(localRect)
                ) {
                    dispatchRequest(thisRequest, layoutCoordinates)
                    return@coroutineScope
                }

                // The previous request completely overlaps this one, so wait for it to finish since
                // it will probably satisfy this request.
                // Note that even if the previous request fails or is cancelled, join will return
                // normally. It could be cancelled either because a newer job interrupted it, or
                // because the caller/sender of that request was cancelled.
                previousRequest.second.join()

                // If a newer request interrupted this one while we were waiting, then it will have
                // already dispatched so we should consider this request cancelled and not dispatch.
                if (newestDispatchedRequest === previousRequest) {
                    // Any requests queued up previously to us have finished, and nothing new came
                    // in while we were waiting.
                    dispatchRequest(thisRequest, layoutCoordinates)
                }
            } finally {
                // Only the last job in the queue should clear the dispatched request, since if
                // there's another job waiting to start it needs to know what the last dispatched
                // request was.
                if (newestDispatchedRequest === newestReceivedRequest) {
                    newestDispatchedRequest = null
                }
                // Only the last job in the queue should clear the queue.
                if (newestReceivedRequest === thisRequest) {
                    newestReceivedRequest = null
                }
            }
        }
    }

    /**
     * Marks [request] as the [newestDispatchedRequest] and then dispatches it to both the
     * [responder] and the [parent].
     */
    private suspend fun dispatchRequest(
        request: Pair<Rect, Job>,
        layoutCoordinates: LayoutCoordinates
    ) {
        newestDispatchedRequest = request
        val localRect = request.first
        val parentRect = responder.calculateRectForParent(localRect)

        coroutineScope {
            // For the item to be visible, if needs to be in the viewport of all its
            // ancestors.
            // Note: For now we run both of these concurrently, but in the future we could
            // make this configurable. (The child relocation could be executed before the
            // parent, or parent before the child).
            launch {
                // Bring the requested Child into this parent's view.
                responder.bringChildIntoView(localRect)
            }

            // TODO I think this needs to be in launch, since if the parent is cancelled (this
            //  throws a CE) due to animation interruption, the child should continue animating.
            parent.bringChildIntoView(parentRect, layoutCoordinates)
        }
        // Don't try to null out newestDispatchedRequest here, bringChildIntoView will take care of
        // that.
    }
}

/**
 * Translates [rect], specified in [sourceCoordinates], into this [LayoutCoordinates].
 */
private fun LayoutCoordinates.localRectOf(
    sourceCoordinates: LayoutCoordinates,
    rect: Rect
): Rect {
    // Translate the supplied layout coordinates into the coordinate system of this parent.
    val localRect = localBoundingBoxOf(sourceCoordinates, clipBounds = false)

    // Translate the rect to this parent's local coordinates.
    return rect.translate(localRect.topLeft)
}

/**
 * Returns true if [other] is fully contained inside this [Rect], using inclusive bound checks.
 */
private fun Rect.completelyOverlaps(other: Rect): Boolean {
    return left <= other.left &&
        top <= other.top &&
        right >= other.right &&
        bottom >= other.bottom
}