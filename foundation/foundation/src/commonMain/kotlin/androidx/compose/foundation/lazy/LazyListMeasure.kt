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
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
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
    scrollToBeConsumed: Float,
    constraints: Constraints,
    isVertical: Boolean,
    headerIndexes: List<Int>,
    verticalArrangement: Arrangement.Vertical?,
    horizontalArrangement: Arrangement.Horizontal?,
    reverseLayout: Boolean,
    density: Density,
    layoutDirection: LayoutDirection
): LazyListMeasureResult {
    require(startContentPadding >= 0)
    require(endContentPadding >= 0)
    if (itemsCount <= 0) {
        // empty data set. reset the current scroll and report zero size
        return LazyListMeasureResult(
            firstVisibleItem = null,
            firstVisibleItemScrollOffset = 0,
            canScrollForward = false,
            consumedScroll = 0f,
            layoutWidth = constraints.minWidth,
            layoutHeight = constraints.minHeight,
            placementBlock = {},
            visibleItemsInfo = emptyList(),
            viewportStartOffset = -startContentPadding,
            viewportEndOffset = endContentPadding,
            totalItemsCount = 0,
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

        // this will contain all the MeasuredItems representing the visible items
        val visibleItems = mutableListOf<LazyMeasuredItem>()

        // include the start padding so we compose items in the padding area. before starting
        // scrolling forward we would remove it back
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
            currentFirstItemScrollOffset += measuredItem.sizeWithSpacings
            currentFirstItemIndex = previous
        }
        // if we were scrolled backward, but there were not enough items before. this means
        // not the whole scroll was consumed
        if (currentFirstItemScrollOffset < minOffset) {
            scrollDelta += currentFirstItemScrollOffset
            currentFirstItemScrollOffset = minOffset
        }

        // neutralize previously added start padding as we stopped filling the start padding area
        currentFirstItemScrollOffset += startContentPadding

        // remembers the composed MeasuredItem which we are not currently placing as they are out
        // of screen. it is possible we will need to place them if the remaining items will
        // not fill the whole viewport and we will need to scroll back
        var notUsedButComposedItems: MutableList<LazyMeasuredItem>? = null

        var index = currentFirstItemIndex
        val maxMainAxis = maxOffset + endContentPadding
        var mainAxisUsed = -currentFirstItemScrollOffset

        // first we need to skip items we already composed while composing backward
        visibleItems.fastForEach {
            index++
            mainAxisUsed += it.sizeWithSpacings
        }

        // then composing visible items forward until we fill the whole viewport
        while (mainAxisUsed <= maxMainAxis && index.value < itemsCount) {
            val measuredItem = itemProvider.getAndMeasure(index)
            mainAxisUsed += measuredItem.sizeWithSpacings

            if (mainAxisUsed < minOffset) {
                // this item is offscreen and will not be placed. advance firstVisibleItemIndex
                currentFirstItemIndex = index + 1
                currentFirstItemScrollOffset -= measuredItem.sizeWithSpacings
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
                currentFirstItemScrollOffset += measuredItem.sizeWithSpacings
                currentFirstItemIndex = previous
            }
            scrollDelta += toScrollBack
            if (currentFirstItemScrollOffset < 0) {
                scrollDelta += currentFirstItemScrollOffset
                mainAxisUsed += currentFirstItemScrollOffset
                currentFirstItemScrollOffset = 0
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
        val visibleItemsScrollOffset = -currentFirstItemScrollOffset
        var firstItem = visibleItems.firstOrNull()

        // even if we compose items to fill the start padding area we should ignore items fully
        // located there for the state's scroll position calculation (first item + first offset)
        if (startContentPadding > 0) {
            for (i in visibleItems.indices) {
                val size = visibleItems[i].sizeWithSpacings
                if (size <= currentFirstItemScrollOffset && i != visibleItems.lastIndex) {
                    currentFirstItemScrollOffset -= size
                    firstItem = visibleItems[i + 1]
                } else {
                    break
                }
            }
        }

        val layoutWidth =
            constraints.constrainWidth(if (isVertical) maxCrossAxis else mainAxisUsed)
        val layoutHeight =
            constraints.constrainHeight(if (isVertical) mainAxisUsed else maxCrossAxis)

        calculateItemsOffsets(
            items = visibleItems,
            mainAxisLayoutSize = if (isVertical) layoutHeight else layoutWidth,
            usedMainAxisSize = mainAxisUsed,
            itemsScrollOffset = visibleItemsScrollOffset,
            isVertical = isVertical,
            verticalArrangement = verticalArrangement,
            horizontalArrangement = horizontalArrangement,
            reverseLayout = reverseLayout,
            density = density,
            layoutDirection = layoutDirection
        )

        val headerItem = if (headerIndexes.isNotEmpty()) {
            findOrComposeLazyListHeader(
                composedVisibleItems = visibleItems,
                notUsedButComposedItems = notUsedButComposedItems,
                itemProvider = itemProvider,
                headerIndexes = headerIndexes,
                startContentPadding = startContentPadding
            )
        } else {
            null
        }

        val maximumVisibleOffset = minOf(mainAxisUsed, mainAxisMaxSize) + endContentPadding

        return LazyListMeasureResult(
            firstVisibleItem = firstItem,
            firstVisibleItemScrollOffset = currentFirstItemScrollOffset,
            canScrollForward = mainAxisUsed > maxOffset,
            consumedScroll = consumedScroll,
            layoutWidth = layoutWidth,
            layoutHeight = layoutHeight,
            placementBlock = {
                visibleItems.fastForEach {
                    if (it !== headerItem) {
                        it.place(this, layoutWidth, layoutHeight)
                    }
                }
                // the header item should be placed (drawn) after all other items
                headerItem?.place(this, layoutWidth, layoutHeight)
            },
            viewportStartOffset = -startContentPadding,
            viewportEndOffset = maximumVisibleOffset,
            visibleItemsInfo = visibleItems,
            totalItemsCount = itemsCount,
        )
    }
}

/**
 * Calculates [LazyMeasuredItem]s offsets.
 */
private fun calculateItemsOffsets(
    items: List<LazyMeasuredItem>,
    mainAxisLayoutSize: Int,
    usedMainAxisSize: Int,
    itemsScrollOffset: Int,
    isVertical: Boolean,
    verticalArrangement: Arrangement.Vertical?,
    horizontalArrangement: Arrangement.Horizontal?,
    reverseLayout: Boolean,
    density: Density,
    layoutDirection: LayoutDirection
) {
    val hasSpareSpace = usedMainAxisSize < mainAxisLayoutSize
    if (hasSpareSpace) {
        check(itemsScrollOffset == 0)
    }

    if (hasSpareSpace) {
        val itemsCount = items.size
        val sizes = IntArray(itemsCount) { index ->
            val reverseLayoutAwareIndex = if (!reverseLayout) index else itemsCount - index - 1
            items[reverseLayoutAwareIndex].size
        }
        val offsets = IntArray(itemsCount) { 0 }
        if (isVertical) {
            with(requireNotNull(verticalArrangement)) {
                density.arrange(mainAxisLayoutSize, sizes, offsets)
            }
        } else {
            with(requireNotNull(horizontalArrangement)) {
                density.arrange(mainAxisLayoutSize, sizes, layoutDirection, offsets)
            }
        }
        offsets.forEachIndexed { index, absoluteOffset ->
            val reverseLayoutAwareIndex = if (!reverseLayout) index else itemsCount - index - 1
            val item = items[reverseLayoutAwareIndex]
            val relativeOffset = if (reverseLayout) {
                mainAxisLayoutSize - absoluteOffset - item.size
            } else {
                absoluteOffset
            }
            item.offset = relativeOffset
        }
    } else {
        var currentMainAxis = itemsScrollOffset
        items.fastForEach {
            it.offset = currentMainAxis
            currentMainAxis += it.sizeWithSpacings
        }
    }
}
