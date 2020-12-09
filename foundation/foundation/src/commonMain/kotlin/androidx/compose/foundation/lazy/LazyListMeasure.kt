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

package androidx.compose.foundation.lazy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.InternalLayoutApi
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.util.fastForEach
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

/**
 * Measures and calculates the positions for the currently visible items. The result is produced
 * as a [LazyListMeasureResult] which contains all the calculations.
 */
internal fun measureLazyList(
    itemsCount: Int,
    itemProvider: LazyMeasuredItemProvider,
    mainAxisMaxSize: Int,
    startContentPadding: Int,
    endContentPadding: Int,
    firstVisibleItemIndex: DataIndex,
    firstVisibleItemScrollOffset: Int,
    scrollToBeConsumed: Float
): LazyListMeasureResult {
    require(startContentPadding >= 0)
    require(endContentPadding >= 0)
    if (itemsCount <= 0) {
        // empty data set. reset the current scroll and report zero size
        return LazyListMeasureResult(
            mainAxisSize = 0,
            crossAxisSize = 0,
            items = emptyList(),
            itemsScrollOffset = 0,
            firstVisibleItemIndex = DataIndex(0),
            firstVisibleItemScrollOffset = 0,
            canScrollForward = false,
            consumedScroll = 0f
        )
    } else {
        var currentFirstItemIndex = firstVisibleItemIndex
        var currentFirstItemScrollOffset = firstVisibleItemScrollOffset
        if (currentFirstItemIndex.value >= itemsCount) {
            // the data set has been updated and now we have less items that we were
            // scrolled to before
            currentFirstItemIndex = DataIndex(itemsCount - 1)
            currentFirstItemScrollOffset = 0
        }

        // represents the real amount of scroll we applied as a result of this measure pass.
        var scrollDelta = scrollToBeConsumed.roundToInt()

        // applying the whole requested scroll offset. we will figure out if we can't consume
        // all of it later
        currentFirstItemScrollOffset -= scrollDelta

        // if the current scroll offset is less than minimally possible
        if (currentFirstItemIndex == DataIndex(0) && currentFirstItemScrollOffset < 0) {
            scrollDelta += currentFirstItemScrollOffset
            currentFirstItemScrollOffset = 0
        }

        // saving it into the field as we first go backward and after that want to go forward
        // again from the initial position
        val goingForwardInitialIndex = currentFirstItemIndex
        var goingForwardInitialScrollOffset = currentFirstItemScrollOffset

        // this will contain all the MeasuredItems representing the visible items
        val visibleItems = mutableListOf<LazyMeasuredItem>()

        // include the start padding so we compose items in the padding area. in the end we
        // will remove it back from the currentFirstItemScrollOffset calculation
        currentFirstItemScrollOffset -= startContentPadding

        // define min and max offsets (min offset currently includes startPadding)
        val minOffset = -startContentPadding
        val maxOffset = mainAxisMaxSize

        // max of cross axis sizes of all visible items
        var maxCrossAxis = 0

        // we had scrolled backward or we compose items in the start padding area, which means
        // items before current firstItemScrollOffset should be visible. compose them and update
        // firstItemScrollOffset
        while (currentFirstItemScrollOffset < 0 && currentFirstItemIndex > DataIndex(0)) {
            val previous = DataIndex(currentFirstItemIndex.value - 1)
            val measuredItem = itemProvider.getAndMeasure(previous)
            visibleItems.add(0, measuredItem)
            maxCrossAxis = maxOf(maxCrossAxis, measuredItem.crossAxisSize)
            currentFirstItemScrollOffset += measuredItem.mainAxisSize
            currentFirstItemIndex = previous
        }
        // if we were scrolled backward, but there were not enough items before. this means
        // not the whole scroll was consumed
        if (currentFirstItemScrollOffset < minOffset) {
            scrollDelta += currentFirstItemScrollOffset
            goingForwardInitialScrollOffset += currentFirstItemScrollOffset
            currentFirstItemScrollOffset = minOffset
        }

        // remembers the composed MeasuredItem which we are not currently placing as they are out
        // of screen. it is possible we will need to place them if the remaining items will
        // not fill the whole viewport and we will need to scroll back
        var notUsedButComposedItems: MutableList<LazyMeasuredItem>? = null

        // composing visible items starting from goingForwardInitialIndex until we fill the
        // whole viewport
        var index = goingForwardInitialIndex
        val maxMainAxis = maxOffset + endContentPadding
        var mainAxisUsed = -goingForwardInitialScrollOffset
        while (mainAxisUsed <= maxMainAxis && index.value < itemsCount) {
            val measuredItem = itemProvider.getAndMeasure(index)
            mainAxisUsed += measuredItem.mainAxisSize

            if (mainAxisUsed < minOffset) {
                // this item is offscreen and will not be placed. advance firstVisibleItemIndex
                currentFirstItemIndex = index + 1
                currentFirstItemScrollOffset -= measuredItem.mainAxisSize
                // but remember the corresponding placeables in case we will be forced to
                // scroll back as there were not enough items to fill the viewport
                if (notUsedButComposedItems == null) {
                    notUsedButComposedItems = mutableListOf()
                }
                notUsedButComposedItems.add(measuredItem)
            } else {
                maxCrossAxis = maxOf(maxCrossAxis, measuredItem.crossAxisSize)
                visibleItems.add(measuredItem)
            }

            index++
        }

        // we didn't fill the whole viewport with items starting from firstVisibleItemIndex.
        // lets try to scroll back if we have enough items before firstVisibleItemIndex.
        if (mainAxisUsed < maxOffset) {
            val toScrollBack = maxOffset - mainAxisUsed
            currentFirstItemScrollOffset -= toScrollBack
            mainAxisUsed += toScrollBack
            while (currentFirstItemScrollOffset < 0 && currentFirstItemIndex > DataIndex(0)) {
                val previous = DataIndex(currentFirstItemIndex.value - 1)
                val alreadyComposedIndex = notUsedButComposedItems?.lastIndex ?: -1
                val measuredItem = if (alreadyComposedIndex >= 0) {
                    notUsedButComposedItems!!.removeAt(alreadyComposedIndex)
                } else {
                    itemProvider.getAndMeasure(previous)
                }
                visibleItems.add(0, measuredItem)
                maxCrossAxis = maxOf(maxCrossAxis, measuredItem.crossAxisSize)
                currentFirstItemScrollOffset += measuredItem.mainAxisSize
                currentFirstItemIndex = previous
            }
            scrollDelta += toScrollBack
            if (currentFirstItemScrollOffset < minOffset) {
                scrollDelta += currentFirstItemScrollOffset
                mainAxisUsed += currentFirstItemScrollOffset
                currentFirstItemScrollOffset = minOffset
            }
        }

        // report the amount of pixels we consumed. scrollDelta can be smaller than
        // scrollToBeConsumed if there were not enough items to fill the offered space or it
        // can be larger if items were resized, or if, for example, we were previously
        // displaying the item 15, but now we have only 10 items in total in the data set.
        val consumedScroll = if (scrollToBeConsumed.roundToInt().sign == scrollDelta.sign &&
            abs(scrollToBeConsumed.roundToInt()) >= abs(scrollDelta)
        ) {
            scrollDelta.toFloat()
        } else {
            scrollToBeConsumed
        }

        // the initial offset for items from visibleItems list
        val firstItemOffset = -(currentFirstItemScrollOffset + startContentPadding)

        // compensate the content padding we initially added in currentFirstItemScrollOffset.
        // if the item is fully located in the start padding area we  need to use the next
        // item as a value for currentFirstItemIndex
        if (startContentPadding > 0) {
            currentFirstItemScrollOffset += startContentPadding
            var startPaddingItems = 0
            while (startPaddingItems < visibleItems.lastIndex) {
                val size = visibleItems[startPaddingItems].mainAxisSize
                if (size <= currentFirstItemScrollOffset) {
                    startPaddingItems++
                    currentFirstItemScrollOffset -= size
                    currentFirstItemIndex++
                } else {
                    break
                }
            }
        }

        return LazyListMeasureResult(
            mainAxisSize = mainAxisUsed + startContentPadding,
            crossAxisSize = maxCrossAxis,
            items = visibleItems,
            itemsScrollOffset = firstItemOffset,
            firstVisibleItemIndex = currentFirstItemIndex,
            firstVisibleItemScrollOffset = currentFirstItemScrollOffset,
            canScrollForward = mainAxisUsed > maxOffset,
            consumedScroll = consumedScroll
        )
    }
}

/**
 * Lays out [LazyMeasuredItem]s based on the [LazyListMeasureResult] and the passed arrangement.
 */
@OptIn(InternalLayoutApi::class)
internal fun MeasureScope.layoutLazyList(
    constraints: Constraints,
    isVertical: Boolean,
    verticalArrangement: Arrangement.Vertical?,
    horizontalArrangement: Arrangement.Horizontal?,
    measureResult: LazyListMeasureResult,
    reverseLayout: Boolean
): MeasureResult {
    val layoutWidth = constraints.constrainWidth(
        if (isVertical) measureResult.crossAxisSize else measureResult.mainAxisSize
    )
    val layoutHeight = constraints.constrainHeight(
        if (isVertical) measureResult.mainAxisSize else measureResult.crossAxisSize
    )
    val mainAxisLayoutSize = if (isVertical) layoutHeight else layoutWidth
    val hasSpareSpace = measureResult.mainAxisSize < mainAxisLayoutSize
    if (hasSpareSpace) {
        check(measureResult.itemsScrollOffset == 0)
    }
    val density = this

    return layout(layoutWidth, layoutHeight) {
        var currentMainAxis = measureResult.itemsScrollOffset
        if (hasSpareSpace) {
            val items = if (reverseLayout) measureResult.items.reversed() else measureResult.items
            val sizes = IntArray(items.size) { index ->
                items[index].let { it.mainAxisSize - it.extraMainAxisSize }
            }
            val positions = IntArray(items.size) { 0 }
            if (isVertical) {
                requireNotNull(verticalArrangement)
                    .arrange(mainAxisLayoutSize, sizes, density, positions)
            } else {
                requireNotNull(horizontalArrangement)
                    .arrange(mainAxisLayoutSize, sizes, layoutDirection, density, positions)
            }
            positions.forEachIndexed { index, position ->
                items[index].place(this, layoutWidth, layoutHeight, position, reverseLayout)
            }
        } else {
            measureResult.items.fastForEach {
                val offset = if (reverseLayout) {
                    mainAxisLayoutSize - currentMainAxis - (it.mainAxisSize - it.extraMainAxisSize)
                } else {
                    currentMainAxis
                }
                it.place(this, layoutWidth, layoutHeight, offset, reverseLayout)
                currentMainAxis += it.mainAxisSize
            }
        }
    }
}
