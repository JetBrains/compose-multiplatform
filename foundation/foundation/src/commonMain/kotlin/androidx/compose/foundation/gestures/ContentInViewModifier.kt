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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.onFocusedBoundsChanged
import androidx.compose.foundation.relocation.BringIntoViewResponder
import androidx.compose.foundation.relocation.bringIntoViewResponder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.OnPlacedModifier
import androidx.compose.ui.layout.OnRemeasuredModifier
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch

/**
 * Handles any logic related to bringing or keeping content in view, including
 * [BringIntoViewResponder] and ensuring the focused child stays in view when the scrollable area
 * is shrunk.
 */
@OptIn(ExperimentalFoundationApi::class)
internal class ContentInViewModifier(
    private val scope: CoroutineScope,
    private val orientation: Orientation,
    private val scrollableState: ScrollableState,
    private val reverseDirection: Boolean
) : BringIntoViewResponder, OnRemeasuredModifier, OnPlacedModifier {
    private var focusedChild: LayoutCoordinates? = null
    private var coordinates: LayoutCoordinates? = null
    private var oldSize: IntSize? = null

    // These properties are used to detect the case where the viewport size is animated shrinking
    // while the scroll animation used to keep the focused child in view is still running.
    private var focusedChildBeingAnimated: LayoutCoordinates? = null
    private var focusTargetBounds: Rect? by mutableStateOf(null)
    private var focusAnimationJob: Job? = null

    val modifier: Modifier = this
        .onFocusedBoundsChanged { focusedChild = it }
        .bringIntoViewResponder(this)

    override fun onRemeasured(size: IntSize) {
        val coordinates = coordinates
        val oldSize = oldSize
        // We only care when this node becomes smaller than it previously was, so don't care about
        // the initial measurement.
        if (oldSize != null && oldSize != size && coordinates?.isAttached == true) {
            onSizeChanged(coordinates, oldSize)
        }
        this.oldSize = size
    }

    override fun onPlaced(coordinates: LayoutCoordinates) {
        this.coordinates = coordinates
    }

    override fun calculateRectForParent(localRect: Rect): Rect {
        val oldSize = checkNotNull(oldSize) {
            "Expected BringIntoViewRequester to not be used before parents are placed."
        }
        // oldSize will only be null before the initial measurement.
        return computeDestination(localRect, oldSize)
    }

    override suspend fun bringChildIntoView(localRect: Rect) {
        performBringIntoView(
            source = localRect,
            destination = calculateRectForParent(localRect)
        )
    }

    /**
     * Handles when the size of the scroll viewport changes by making sure any focused child is kept
     * appropriately visible when the viewport shrinks and would otherwise hide it.
     *
     * One common instance of this is when a text field in a scrollable near the bottom is focused
     * while the soft keyboard is hidden, causing the keyboard to show, and cover the field.
     * See b/192043120 and related bugs.
     *
     * To future debuggers of this method, it might be helpful to add a draw modifier to the chain
     * above to draw the focus target bounds:
     * ```
     * .drawWithContent {
     *   drawContent()
     *   focusTargetBounds?.let {
     *     drawRect(
     *       Color.Red,
     *       topLeft = it.topLeft,
     *       size = it.size,
     *       style = Stroke(1.dp.toPx())
     *     )
     *   }
     * }
     * ```
     */
    private fun onSizeChanged(coordinates: LayoutCoordinates, oldSize: IntSize) {
        val containerShrunk = if (orientation == Orientation.Horizontal) {
            coordinates.size.width < oldSize.width
        } else {
            coordinates.size.height < oldSize.height
        }
        // If the container is growing, then if the focused child is only partially visible it will
        // soon be _more_ visible, so don't scroll.
        if (!containerShrunk) return

        val focusedChild = focusedChild ?: return
        val focusedBounds = coordinates.localBoundingBoxOf(focusedChild, clipBounds = false)

        // In order to check if we need to scroll to bring the focused child into view, it's not
        // enough to consider where the child actually is right now. If the viewport was recently
        // shrunk, we may have already started a scroll animation to bring it into view. In that
        // case, we need to compare with the target of the animation, not the current position. If
        // we don't do that, then in some cases when the viewport size is being animated (e.g. when
        // the keyboard insets are being animated on API 30+) we might stop trying to keep the
        // focused child in view before the viewport animation is finished, and the scroll animation
        // will stop short and leave the focused child out of the viewport. See b/230756508.
        val eventualFocusedBounds = if (focusedChild === focusedChildBeingAnimated) {
            // A previous call to this method started an animation that is still running, so compare
            // with the target of that animation.
            checkNotNull(focusTargetBounds)
        } else {
            focusedBounds
        }

        val myOldBounds = Rect(Offset.Zero, oldSize.toSize())
        if (!myOldBounds.overlaps(eventualFocusedBounds)) {
            // The focused child was not visible before the resize, so we don't need to keep
            // it visible.
            return
        }

        val targetBounds = computeDestination(eventualFocusedBounds, coordinates.size)
        if (targetBounds == eventualFocusedBounds) {
            // The focused child is already fully visible (not clipped or hidden) after the resize,
            // or will be after it finishes animating, so we don't need to do anything.
            return
        }

        // If execution has gotten to this point, it means the focused child was at least partially
        // visible before the resize, and it is either partially clipped or completely hidden after
        // the resize, so we need to adjust scroll to keep it in view.
        focusedChildBeingAnimated = focusedChild
        focusTargetBounds = targetBounds
        scope.launch(NonCancellable) {
            val job = launch {
                // Animate the scroll offset to keep the focused child in view. This is a suspending
                // call that will suspend until the animation is finished, and only return if it
                // completes. If any other scroll operations are performed after the animation starts,
                // e.g. the viewport shrinks again or the user manually scrolls, this animation will
                // be cancelled and this function will throw a CancellationException.
                performBringIntoView(source = focusedBounds, destination = targetBounds)
            }
            focusAnimationJob = job

            // If the scroll was interrupted by another viewport shrink that happens while the
            // animation is running, we don't want to clear these fields since the later call to
            // this onSizeChanged method will have updated the fields with its own values.
            // If the animation completed, or was cancelled for any other reason, we need to clear
            // them so the next viewport shrink doesn't think there's already a scroll animation in
            // progress.
            // Doing this wrong has a few implications:
            // 1. If the fields are nulled out when another onSizeChange call happens, it will not
            //    use the current animation target and viewport animations will lose track of the
            //    focusable.
            // 2. If the fields are not nulled out in other cases, the next viewport animation will
            //    not keep the focusable in view if the focus hasn't changed.
            try {
                job.join()
            } finally {
                if (focusAnimationJob === job) {
                    focusedChildBeingAnimated = null
                    focusTargetBounds = null
                    focusAnimationJob = null
                }
            }
        }
    }

    /**
     * Compute the destination given the source rectangle and current bounds.
     *
     * @param childBounds The bounding box of the item that sent the request to be brought into view.
     * @return the destination rectangle.
     */
    private fun computeDestination(childBounds: Rect, containerSize: IntSize): Rect {
        val size = containerSize.toSize()
        return when (orientation) {
            Orientation.Vertical ->
                childBounds.translate(
                    translateX = 0f,
                    translateY = -relocationDistance(
                        childBounds.top,
                        childBounds.bottom,
                        size.height
                    )
                )
            Orientation.Horizontal ->
                childBounds.translate(
                    translateX = -relocationDistance(
                        childBounds.left,
                        childBounds.right,
                        size.width
                    ),
                    translateY = 0f
                )
        }
    }

    /**
     * Using the source and destination bounds, perform an animated scroll.
     */
    private suspend fun performBringIntoView(source: Rect, destination: Rect) {
        val offset = when (orientation) {
            Orientation.Vertical -> destination.top - source.top
            Orientation.Horizontal -> destination.left - source.left
        }
        val scrollDelta = if (reverseDirection) -offset else offset

        // Note that this results in weird behavior if called before the previous
        // performBringIntoView finishes due to b/220119990.
        scrollableState.animateScrollBy(scrollDelta)
    }

    /**
     * Calculate the offset needed to bring one of the edges into view. The leadingEdge is the side
     * closest to the origin (For the x-axis this is 'left', for the y-axis this is 'top').
     * The trailing edge is the other side (For the x-axis this is 'right', for the y-axis this is
     * 'bottom').
     */
    private fun relocationDistance(leadingEdge: Float, trailingEdge: Float, parentSize: Float) =
        when {
            // If the item is already visible, no need to scroll.
            leadingEdge >= 0 && trailingEdge <= parentSize -> 0f

            // If the item is visible but larger than the parent, we don't scroll.
            leadingEdge < 0 && trailingEdge > parentSize -> 0f

            // Find the minimum scroll needed to make one of the edges coincide with the parent's
            // edge.
            abs(leadingEdge) < abs(trailingEdge - parentSize) -> leadingEdge
            else -> trailingEdge - parentSize
        }
}