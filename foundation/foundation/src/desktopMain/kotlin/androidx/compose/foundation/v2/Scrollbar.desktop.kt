/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.v2

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.runBlocking


/**
 * Defines how to scroll the scrollable component and how to display a scrollbar for it.
 *
 * The values of this interface are typically in pixels, but do not have to be.
 * It's possible to create an adapter with any scroll range of `Double` values.
 */
interface ScrollbarAdapter {

    // We use `Double` values here in order to allow scrolling both very large (think LazyList with millions of items)
    // and very small (think something whose natural coordinates are less than 1) content.

    /**
     * Scroll offset of the content inside the scrollable component.
     *
     * For example, a value of `100` could mean the content is scrolled by 100 pixels from the start.
     */
    val scrollOffset: Double

    /**
     * The size of the scrollable content, on the scrollable axis.
     */
    val contentSize: Double

    /**
     * The size of the viewport, on the scrollable axis.
     */
    val viewportSize: Double

    /**
     * Instantly jump to [scrollOffset].
     *
     * @param scrollOffset target offset to jump to, value will be coerced to the valid
     * scroll range.
     */
    suspend fun scrollTo(scrollOffset: Double)

}

/**
 * The maximum scroll offset of the scrollable content.
 */
val ScrollbarAdapter.maxScrollOffset: Double
    get() = (contentSize - viewportSize).coerceAtLeast(0.0)

internal class ScrollableScrollbarAdapter(
    private val scrollState: ScrollState
) : ScrollbarAdapter {

    override val scrollOffset: Double get() = scrollState.value.toDouble()

    override suspend fun scrollTo(scrollOffset: Double) {
        scrollState.scrollTo(scrollOffset.roundToInt())
    }

    override val contentSize: Double
        // This isn't strictly correct, as the actual content can be smaller
        // than the viewport when scrollState.maxValue is 0, but the scrollbar
        // doesn't really care as long as contentSize <= viewportSize; it's
        // just not showing itself
        get() = scrollState.maxValue + viewportSize

    override val viewportSize: Double
        get() = scrollState.viewportSize.toDouble()

}

internal class LazyScrollbarAdapter(
    private val scrollState: LazyListState
) : ScrollbarAdapter {

    override val scrollOffset: Double
        get() {
            val firstVisibleItem = scrollState.layoutInfo.visibleItemsInfo.firstOrNull()
            return if (firstVisibleItem == null)
                0.0
            else
                firstVisibleItem.index * averageItemSize - firstVisibleItem.offset
        }

    override val viewportSize: Double
        get() = with(scrollState.layoutInfo){
            if (orientation == Orientation.Vertical)
                viewportSize.height
            else
                viewportSize.width
        }.toDouble()

    override val contentSize: Double
        get() {
            return averageItemSize * itemCount +
                scrollState.layoutInfo.beforeContentPadding +
                scrollState.layoutInfo.afterContentPadding
        }

    override suspend fun scrollTo(scrollOffset: Double) {
        val distance = scrollOffset - this@LazyScrollbarAdapter.scrollOffset

        // if we scroll less than viewport we need to use scrollBy function to avoid
        // undesirable scroll jumps (when an item size is different)
        //
        // if we scroll more than viewport we should immediately jump to this position
        // without recreating all items between the current and the new position
        if (abs(distance) <= viewportSize) {
            scrollState.scrollBy(distance.toFloat())
        } else {
            snapTo(scrollOffset)
        }
    }

    private suspend fun snapTo(scrollOffset: Double) {
        val scrollOffsetCoerced = scrollOffset.coerceIn(0.0, maxScrollOffset)

        val index = (scrollOffsetCoerced / averageItemSize)
            .toInt()
            .coerceAtLeast(0)
            .coerceAtMost(itemCount - 1)

        val offset = (scrollOffsetCoerced - index * averageItemSize)
            .toInt()
            .coerceAtLeast(0)

        scrollState.scrollToItem(index = index, scrollOffset = offset)
    }

    private val itemCount get() = scrollState.layoutInfo.totalItemsCount

    private val averageItemSize by derivedStateOf {
        scrollState
            .layoutInfo
            .visibleItemsInfo
            .asSequence()
            .map { it.size }
            .average()
    }

}

internal class SliderAdapter(
    private val adapter: ScrollbarAdapter,
    private val trackSize: Int,
    private val minHeight: Float,
    private val reverseLayout: Boolean,
    private val isVertical: Boolean,
) {

    private val contentSize get() = adapter.contentSize
    private val visiblePart: Double
        get() {
            val contentSize = contentSize
            return if (contentSize == 0.0)
                1.0
            else
                (adapter.viewportSize / contentSize).coerceAtMost(1.0)
        }

    val thumbSize
        get() = (trackSize * visiblePart).coerceAtLeast(minHeight.toDouble())

    private val scrollScale: Double
        get() {
            val extraScrollbarSpace = trackSize - thumbSize
            val extraContentSpace = adapter.maxScrollOffset  // == contentSize - viewportSize
            return if (extraContentSpace == 0.0) 1.0 else extraScrollbarSpace / extraContentSpace
        }

    private var rawPosition: Double
        get() = scrollScale * adapter.scrollOffset
        set(value) {
            runBlocking {
                adapter.scrollTo(value / scrollScale)
            }
        }

    var position: Double
        get() = if (reverseLayout) trackSize - thumbSize - rawPosition else rawPosition
        set(value) {
            rawPosition = if (reverseLayout) {
                trackSize - thumbSize - value
            } else {
                value
            }
        }

    val bounds get() = position..position + thumbSize

    // Stores the unrestricted position during a dragging gesture
    private var positionDuringDrag = 0.0

    /** Called when the thumb dragging starts */
    fun onDragStarted() {
        positionDuringDrag = position
    }

    /** Called on every movement while dragging the thumb */
    fun onDragDelta(offset: Offset) {
        val dragDelta = if (isVertical) offset.y else offset.x
        val maxScrollPosition = adapter.maxScrollOffset * scrollScale
        val sliderDelta =
            (positionDuringDrag + dragDelta).coerceIn(0.0, maxScrollPosition) -
                positionDuringDrag.coerceIn(0.0, maxScrollPosition)
        position += sliderDelta  // Have to add to position for smooth content scroll if the items are of different size
        positionDuringDrag += dragDelta
    }

}
