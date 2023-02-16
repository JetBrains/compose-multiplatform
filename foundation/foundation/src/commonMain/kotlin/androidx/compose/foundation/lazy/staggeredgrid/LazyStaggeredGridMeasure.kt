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

package androidx.compose.foundation.lazy.staggeredgrid

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.fastFold
import androidx.compose.foundation.fastMaxOfOrNull
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasureScope
import androidx.compose.foundation.lazy.layout.findIndexByKey
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridLaneInfo.Companion.FullSpan
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridLaneInfo.Companion.Unset
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.packInts
import androidx.compose.ui.util.unpackInt1
import androidx.compose.ui.util.unpackInt2
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign

private const val DebugLoggingEnabled = false

@ExperimentalFoundationApi
private inline fun <T> withDebugLogging(
    scope: LazyLayoutMeasureScope,
    block: LazyLayoutMeasureScope.() -> T
): T {
    val result = if (DebugLoggingEnabled) {
        try {
            println("╭──────{ measure start }───────────")
            with(scope, block)
        } finally {
            println("╰──────{ measure done }────────────")
        }
    } else {
        with(scope, block)
    }
    return result
}

private fun Array<ArrayDeque<LazyStaggeredGridMeasuredItem>>.debugRender(): String =
    if (DebugLoggingEnabled) {
        @Suppress("ListIterator")
        map { items -> items.map { it.index } }.toString()
    } else {
        ""
    }

private inline fun debugLog(message: () -> String) {
    if (DebugLoggingEnabled) {
        println("│ - ${message()}")
    }
}

@ExperimentalFoundationApi
internal fun LazyLayoutMeasureScope.measureStaggeredGrid(
    state: LazyStaggeredGridState,
    itemProvider: LazyStaggeredGridItemProvider,
    resolvedSlotSums: IntArray,
    constraints: Constraints,
    isVertical: Boolean,
    reverseLayout: Boolean,
    contentOffset: IntOffset,
    mainAxisAvailableSize: Int,
    mainAxisSpacing: Int,
    crossAxisSpacing: Int,
    beforeContentPadding: Int,
    afterContentPadding: Int,
): LazyStaggeredGridMeasureResult {
    val context = LazyStaggeredGridMeasureContext(
        state = state,
        itemProvider = itemProvider,
        resolvedSlotSums = resolvedSlotSums,
        constraints = constraints,
        isVertical = isVertical,
        contentOffset = contentOffset,
        mainAxisAvailableSize = mainAxisAvailableSize,
        beforeContentPadding = beforeContentPadding,
        afterContentPadding = afterContentPadding,
        reverseLayout = reverseLayout,
        mainAxisSpacing = mainAxisSpacing,
        crossAxisSpacing = crossAxisSpacing,
        measureScope = this,
    )

    val initialItemIndices: IntArray
    val initialItemOffsets: IntArray

    Snapshot.withoutReadObservation {
        val firstVisibleIndices = state.scrollPosition.indices
        val firstVisibleOffsets = state.scrollPosition.offsets

        initialItemIndices =
            if (firstVisibleIndices.size == resolvedSlotSums.size) {
                firstVisibleIndices
            } else {
                // Grid got resized (or we are in a initial state)
                // Adjust indices accordingly
                context.laneInfo.reset()
                IntArray(resolvedSlotSums.size).apply {
                    // Try to adjust indices in case grid got resized
                    for (lane in indices) {
                        this[lane] = if (
                            lane < firstVisibleIndices.size && firstVisibleIndices[lane] != Unset
                        ) {
                            firstVisibleIndices[lane]
                        } else {
                            if (lane == 0) {
                                0
                            } else {
                                maxInRange(SpanRange(0, lane)) + 1
                            }
                        }
                        // Ensure spans are updated to be in correct range
                        context.laneInfo.setLane(this[lane], lane)
                    }
                }
            }
        initialItemOffsets =
            if (firstVisibleOffsets.size == resolvedSlotSums.size) {
                firstVisibleOffsets
            } else {
                // Grid got resized (or we are in a initial state)
                // Adjust offsets accordingly
                IntArray(resolvedSlotSums.size).apply {
                    // Adjust offsets to match previously set ones
                    for (lane in indices) {
                        this[lane] = if (lane < firstVisibleOffsets.size) {
                            firstVisibleOffsets[lane]
                        } else {
                            if (lane == 0) 0 else this[lane - 1]
                        }
                    }
                }
            }
    }

    return context.measure(
        initialScrollDelta = state.scrollToBeConsumed.roundToInt(),
        initialItemIndices = initialItemIndices,
        initialItemOffsets = initialItemOffsets,
        canRestartMeasure = true,
    )
}

@OptIn(ExperimentalFoundationApi::class)
private class LazyStaggeredGridMeasureContext(
    val state: LazyStaggeredGridState,
    val itemProvider: LazyStaggeredGridItemProvider,
    val resolvedSlotSums: IntArray,
    val constraints: Constraints,
    val isVertical: Boolean,
    val measureScope: LazyLayoutMeasureScope,
    val mainAxisAvailableSize: Int,
    val contentOffset: IntOffset,
    val beforeContentPadding: Int,
    val afterContentPadding: Int,
    val reverseLayout: Boolean,
    val mainAxisSpacing: Int,
    val crossAxisSpacing: Int,
) {
    val measuredItemProvider = LazyStaggeredGridMeasureProvider(
        isVertical = isVertical,
        itemProvider = itemProvider,
        measureScope = measureScope,
        resolvedSlotSums = resolvedSlotSums,
        crossAxisSpacing = crossAxisSpacing
    ) { index, lane, span, key, placeables ->
        LazyStaggeredGridMeasuredItem(
            index = index,
            key = key,
            placeables = placeables,
            isVertical = isVertical,
            spacing = mainAxisSpacing,
            lane = lane,
            span = span,
        )
    }

    val laneInfo = state.laneInfo

    val laneCount = resolvedSlotSums.size

    fun LazyStaggeredGridItemProvider.isFullSpan(itemIndex: Int): Boolean =
        spanProvider.isFullSpan(itemIndex)

    fun LazyStaggeredGridItemProvider.getSpanRange(itemIndex: Int, lane: Int): SpanRange {
        val isFullSpan = spanProvider.isFullSpan(itemIndex)
        val span = if (isFullSpan) laneCount else 1
        val targetLane = if (isFullSpan) 0 else lane
        return SpanRange(targetLane, span)
    }

    inline val SpanRange.isFullSpan: Boolean
        get() = size != 1

    inline val SpanRange.laneInfo: Int
        get() = if (isFullSpan) FullSpan else start
}

@ExperimentalFoundationApi
private fun LazyStaggeredGridMeasureContext.measure(
    initialScrollDelta: Int,
    initialItemIndices: IntArray,
    initialItemOffsets: IntArray,
    canRestartMeasure: Boolean,
): LazyStaggeredGridMeasureResult {
    withDebugLogging(measureScope) {
        val itemCount = itemProvider.itemCount

        if (itemCount <= 0 || laneCount == 0) {
            return LazyStaggeredGridMeasureResult(
                firstVisibleItemIndices = initialItemIndices,
                firstVisibleItemScrollOffsets = initialItemOffsets,
                consumedScroll = 0f,
                measureResult = layout(constraints.minWidth, constraints.minHeight) {},
                canScrollForward = false,
                canScrollBackward = false,
                isVertical = isVertical,
                visibleItemsInfo = emptyList(),
                totalItemsCount = itemCount,
                viewportSize = IntSize(constraints.minWidth, constraints.minHeight),
                viewportStartOffset = -beforeContentPadding,
                viewportEndOffset = mainAxisAvailableSize + afterContentPadding,
                beforeContentPadding = beforeContentPadding,
                afterContentPadding = afterContentPadding,
                mainAxisItemSpacing = mainAxisSpacing
            )
        }

        // represents the real amount of scroll we applied as a result of this measure pass.
        var scrollDelta = initialScrollDelta

        val firstItemIndices = initialItemIndices.copyOf()
        val firstItemOffsets = initialItemOffsets.copyOf()

        // update spans in case item count is lower than before
        ensureIndicesInRange(firstItemIndices, itemCount)

        // applying the whole requested scroll offset. we will figure out if we can't consume
        // all of it later
        firstItemOffsets.offsetBy(-scrollDelta)

        // this will contain all the MeasuredItems representing the visible items
        val measuredItems = Array(laneCount) {
            ArrayDeque<LazyStaggeredGridMeasuredItem>(16)
        }

        // include the start padding so we compose items in the padding area. before starting
        // scrolling forward we would remove it back
        firstItemOffsets.offsetBy(-beforeContentPadding)

        fun hasSpaceBeforeFirst(): Boolean {
            for (lane in firstItemIndices.indices) {
                val itemIndex = firstItemIndices[lane]
                val itemOffset = firstItemOffsets[lane]

                if (itemOffset < maxOf(-mainAxisSpacing, 0) && itemIndex > 0) {
                    return true
                }
            }

            return false
        }

        debugLog {
            "up from indices: ${firstItemIndices.toList()}, offsets: ${firstItemOffsets.toList()}"
        }

        var laneToCheckForGaps = -1

        // we had scrolled backward or we compose items in the start padding area, which means
        // items before current firstItemOffset should be visible. compose them and update
        // firstItemOffsets
        while (hasSpaceBeforeFirst()) {
            // staggered grid always keeps item index increasing top to bottom
            // the first item that should contain something before it must have the largest index
            // among the rest
            val laneIndex = firstItemIndices.indexOfMaxValue()
            val itemIndex = firstItemIndices[laneIndex]

            // other lanes might have smaller offsets than the one chosen above, which indicates
            // incorrect measurement (e.g. item was deleted or it changed size)
            // correct this by offsetting affected lane back to match currently chosen offset
            for (i in firstItemOffsets.indices) {
                if (
                    firstItemIndices[i] != firstItemIndices[laneIndex] &&
                        firstItemOffsets[i] < firstItemOffsets[laneIndex]
                ) {
                    // If offset of the lane is smaller than currently chosen lane,
                    // offset the lane to be where current value of the chosen index is.
                    firstItemOffsets[i] = firstItemOffsets[laneIndex]
                }
            }

            val previousItemIndex = findPreviousItemIndex(itemIndex, laneIndex)
            if (previousItemIndex < 0) {
                laneToCheckForGaps = laneIndex
                break
            }

            val spanRange = itemProvider.getSpanRange(previousItemIndex, laneIndex)
            laneInfo.setLane(previousItemIndex, spanRange.laneInfo)
            val measuredItem = measuredItemProvider.getAndMeasure(
                index = previousItemIndex,
                span = spanRange
            )

            val offset = firstItemOffsets.maxInRange(spanRange)
            val gaps = if (spanRange.isFullSpan) laneInfo.getGaps(previousItemIndex) else null
            spanRange.forEach { lane ->
                firstItemIndices[lane] = previousItemIndex
                val gap = if (gaps == null) 0 else gaps[lane]
                firstItemOffsets[lane] = offset + measuredItem.sizeWithSpacings + gap
            }
        }
        debugLog {
            "up done. measured items: ${measuredItems.debugRender()}"
        }

        fun misalignedStart(referenceLane: Int): Boolean {
            // If we scrolled past the first item in the lane, we have a point of reference
            // to re-align items.

            // Case 1: Each lane has laid out all items, but offsets do no match
            for (lane in firstItemIndices.indices) {
                val misalignedOffsets =
                    findPreviousItemIndex(firstItemIndices[lane], lane) == Unset &&
                        firstItemOffsets[lane] != firstItemOffsets[referenceLane]

                if (misalignedOffsets) {
                    return true
                }
            }
            // Case 2: Some lanes are still missing items, and there's no space left to place them
            for (lane in firstItemIndices.indices) {
                val moreItemsInOtherLanes =
                    findPreviousItemIndex(firstItemIndices[lane], lane) != Unset &&
                        firstItemOffsets[lane] >= firstItemOffsets[referenceLane]

                if (moreItemsInOtherLanes) {
                    return true
                }
            }
            // Case 3: the first item is in the wrong lane (it should always be in
            // the first one)
            val firstItemLane = laneInfo.getLane(0)
            return firstItemLane != 0 && firstItemLane != Unset && firstItemLane != FullSpan
        }

        // define min offset (currently includes beforeContentPadding)
        val minOffset = -beforeContentPadding

        // we scrolled backward, but there were not enough items to fill the start. this means
        // some amount of scroll should be left over
        if (firstItemOffsets[0] < minOffset) {
            scrollDelta += firstItemOffsets[0]
            firstItemOffsets.offsetBy(minOffset - firstItemOffsets[0])
            debugLog {
                "up, correcting scroll delta from ${firstItemOffsets[0]} to $minOffset"
            }
        }

        // neutralize previously added start padding as we stopped filling the before content padding
        firstItemOffsets.offsetBy(beforeContentPadding)

        laneToCheckForGaps =
            if (laneToCheckForGaps == -1) firstItemIndices.indexOf(0) else laneToCheckForGaps

        // re-check if columns are aligned after measure
        if (laneToCheckForGaps != -1) {
            val lane = laneToCheckForGaps
            if (misalignedStart(lane) && canRestartMeasure) {
                laneInfo.reset()
                return measure(
                    initialScrollDelta = scrollDelta,
                    initialItemIndices = IntArray(firstItemIndices.size) { -1 },
                    initialItemOffsets = IntArray(firstItemOffsets.size) {
                        firstItemOffsets[lane]
                    },
                    canRestartMeasure = false
                )
            }
        }

        // start measuring down from first item indices/offsets decided above to ensure correct
        // arrangement.
        // this means we are calling measure second time on items previously measured in this
        // function, but LazyLayout caches them, so no overhead.
        val currentItemIndices = firstItemIndices.copyOf()
        val currentItemOffsets = IntArray(firstItemOffsets.size) {
            -firstItemOffsets[it]
        }

        val maxOffset = (mainAxisAvailableSize + afterContentPadding).coerceAtLeast(0)

        debugLog {
            "down current, indices: ${currentItemIndices.toList()}, " +
                "offsets: ${currentItemOffsets.toList()}"
        }

        // current item should be pointing to the index of previously measured item below,
        // as lane assignments must be decided based on size and offset of all previous items
        // this loop makes sure to measure items that were initially passed to the current item
        // indices with correct item order
        var initialItemsMeasured = 0
        var initialLaneToMeasure = currentItemIndices.indexOfMinValue()
        while (initialLaneToMeasure != -1 && initialItemsMeasured < laneCount) {
            val itemIndex = currentItemIndices[initialLaneToMeasure]
            val laneIndex = initialLaneToMeasure

            initialLaneToMeasure = currentItemIndices.indexOfMinValue(minBound = itemIndex)
            initialItemsMeasured++

            if (itemIndex < 0) continue

            val spanRange = itemProvider.getSpanRange(itemIndex, laneIndex)
            val measuredItem = measuredItemProvider.getAndMeasure(
                itemIndex,
                spanRange
            )

            laneInfo.setLane(itemIndex, spanRange.laneInfo)
            val offset = currentItemOffsets.maxInRange(spanRange) + measuredItem.sizeWithSpacings
            spanRange.forEach { lane ->
                currentItemOffsets[lane] = offset
                currentItemIndices[lane] = itemIndex
                measuredItems[lane].addLast(measuredItem)
            }

            if (currentItemOffsets[spanRange.start] <= minOffset + mainAxisSpacing) {
                measuredItem.isVisible = false
            }

            if (spanRange.isFullSpan) {
                // full span items overwrite other slots if we measure it here, so skip measuring
                // the rest of the slots
                initialItemsMeasured = laneCount
            }
        }

        debugLog {
            "current filled, measured: ${measuredItems.debugRender()}"
        }
        debugLog {
            "down from indices: ${currentItemIndices.toList()}, " +
                "offsets: ${currentItemOffsets.toList()}"
        }

        // then composing visible items forward until we fill the whole viewport.
        // we want to have at least one item in measuredItems even if in fact all the items are
        // offscreen, this can happen if the content padding is larger than the available size.
        while (
            currentItemOffsets.any {
                it < maxOffset ||
                    it <= 0 // filling beforeContentPadding area
            } || measuredItems.all { it.isEmpty() }
        ) {
            val currentLaneIndex = currentItemOffsets.indexOfMinValue()
            val previousItemIndex = currentItemIndices.max()
            val itemIndex = previousItemIndex + 1

            if (itemIndex >= itemCount) {
                break
            }

            val spanRange = itemProvider.getSpanRange(itemIndex, currentLaneIndex)

            laneInfo.setLane(itemIndex, spanRange.laneInfo)
            val measuredItem = measuredItemProvider.getAndMeasure(itemIndex, spanRange)

            val offset = currentItemOffsets.maxInRange(spanRange)
            val gaps = if (spanRange.isFullSpan) {
                laneInfo.getGaps(itemIndex) ?: IntArray(laneCount)
            } else {
                null
            }
            spanRange.forEach { lane ->
                if (gaps != null) {
                    gaps[lane] = offset - currentItemOffsets[lane]
                }
                currentItemIndices[lane] = itemIndex
                currentItemOffsets[lane] = offset + measuredItem.sizeWithSpacings
                measuredItems[lane].addLast(measuredItem)
            }
            laneInfo.setGaps(itemIndex, gaps)

            if (currentItemOffsets[spanRange.start] <= minOffset + mainAxisSpacing) {
                // We scrolled past measuredItem, and it is not visible anymore. We measured it
                // for correct positioning of other items, but there's no need to place it.
                // Mark it as not visible and filter below.
                measuredItem.isVisible = false
            }
        }

        debugLog {
            "down done. measured items: ${measuredItems.debugRender()}"
        }

        // some measured items are offscreen, remove them from the list and adjust indices/offsets
        for (laneIndex in measuredItems.indices) {
            val laneItems = measuredItems[laneIndex]

            while (laneItems.size > 1 && !laneItems.first().isVisible) {
                val item = laneItems.removeFirst()
                val gaps = if (item.span != 1) laneInfo.getGaps(item.index) else null
                firstItemOffsets[laneIndex] -=
                    item.sizeWithSpacings + if (gaps == null) 0 else gaps[laneIndex]
            }

            firstItemIndices[laneIndex] = laneItems.firstOrNull()?.index ?: Unset
        }

        if (currentItemIndices.any { it == itemCount - 1 }) {
            currentItemOffsets.offsetBy(-mainAxisSpacing)
        }

        debugLog {
            "removed invisible items: ${measuredItems.debugRender()}"
        }
        debugLog {
            "back up, indices: ${firstItemIndices.toList()}, " +
                "offsets: ${firstItemOffsets.toList()}"
        }

        // we didn't fill the whole viewport with items starting from firstVisibleItemIndex.
        // lets try to scroll back if we have enough items before firstVisibleItemIndex.
        if (currentItemOffsets.all { it < mainAxisAvailableSize }) {
            val maxOffsetLane = currentItemOffsets.indexOfMaxValue()
            val toScrollBack = mainAxisAvailableSize - currentItemOffsets[maxOffsetLane]
            firstItemOffsets.offsetBy(-toScrollBack)
            currentItemOffsets.offsetBy(toScrollBack)
            while (
                firstItemOffsets.any { it < beforeContentPadding }
            ) {
                val laneIndex = firstItemOffsets.indexOfMinValue()
                val currentIndex =
                    if (firstItemIndices[laneIndex] == -1) {
                        itemCount
                    } else {
                        firstItemIndices[laneIndex]
                    }

                val previousIndex =
                    findPreviousItemIndex(currentIndex, laneIndex)

                if (previousIndex < 0) {
                    if (misalignedStart(laneIndex) && canRestartMeasure) {
                        laneInfo.reset()
                        return measure(
                            initialScrollDelta = scrollDelta,
                            initialItemIndices = IntArray(firstItemIndices.size) { -1 },
                            initialItemOffsets = IntArray(firstItemOffsets.size) {
                                firstItemOffsets[laneIndex]
                            },
                            canRestartMeasure = false
                        )
                    }
                    break
                }

                val spanRange = itemProvider.getSpanRange(previousIndex, laneIndex)
                laneInfo.setLane(previousIndex, spanRange.laneInfo)
                val measuredItem = measuredItemProvider.getAndMeasure(
                    index = previousIndex,
                    spanRange
                )

                val offset = firstItemOffsets.maxInRange(spanRange)
                val gaps = if (spanRange.isFullSpan) laneInfo.getGaps(previousIndex) else null
                spanRange.forEach { lane ->
                    measuredItems[lane].addFirst(measuredItem)
                    firstItemIndices[lane] = previousIndex
                    val gap = if (gaps == null) 0 else gaps[lane]
                    firstItemOffsets[lane] = offset + measuredItem.sizeWithSpacings + gap
                }
            }
            scrollDelta += toScrollBack

            val minOffsetLane = firstItemOffsets.indexOfMinValue()
            if (firstItemOffsets[minOffsetLane] < 0) {
                val offsetValue = firstItemOffsets[minOffsetLane]
                scrollDelta += offsetValue
                currentItemOffsets.offsetBy(offsetValue)
                firstItemOffsets.offsetBy(-offsetValue)
            }
        }

        debugLog {
            "measured: ${measuredItems.debugRender()}"
        }
        debugLog {
            "first indices: ${firstItemIndices.toList()}, offsets: ${firstItemOffsets.toList()}"
        }

        // report the amount of pixels we consumed. scrollDelta can be smaller than
        // scrollToBeConsumed if there were not enough items to fill the offered space or it
        // can be larger if items were resized, or if, for example, we were previously
        // displaying the item 15, but now we have only 10 items in total in the data set.
        val consumedScroll = if (
            state.scrollToBeConsumed.roundToInt().sign == scrollDelta.sign &&
            abs(state.scrollToBeConsumed.roundToInt()) >= abs(scrollDelta)
        ) {
            scrollDelta.toFloat()
        } else {
            state.scrollToBeConsumed
        }

        val itemScrollOffsets = firstItemOffsets.copyOf().transform { -it }

        // even if we compose items to fill before content padding we should ignore items fully
        // located there for the state's scroll position calculation (first item + first offset)
        debugLog { "adjusting for content padding" }
        if (beforeContentPadding > 0) {
            for (laneIndex in measuredItems.indices) {
                val laneItems = measuredItems[laneIndex]
                for (i in laneItems.indices) {
                    val item = laneItems[i]
                    val gaps = laneInfo.getGaps(item.index)
                    val size = item.sizeWithSpacings + if (gaps == null) 0 else gaps[laneIndex]
                    if (
                        i != laneItems.lastIndex &&
                        firstItemOffsets[laneIndex] != 0 &&
                        firstItemOffsets[laneIndex] >= size
                    ) {

                        firstItemOffsets[laneIndex] -= size
                        firstItemIndices[laneIndex] = laneItems[i + 1].index
                    } else {
                        break
                    }
                }
            }
        }

        debugLog {
            "final first indices: ${firstItemIndices.toList()}, " +
                "offsets: ${firstItemOffsets.toList()}"
        }

        // end measure

        // start placement

        val layoutWidth = if (isVertical) {
            constraints.maxWidth
        } else {
            constraints.constrainWidth(currentItemOffsets.max())
        }
        val layoutHeight = if (isVertical) {
            constraints.constrainHeight(currentItemOffsets.max())
        } else {
            constraints.maxHeight
        }

        val mainAxisLayoutSize =
            min(if (isVertical) layoutHeight else layoutWidth, mainAxisAvailableSize)

        var extraItemOffset = itemScrollOffsets[0]
        val extraItemsBefore = calculateExtraItems(
            position = {
                extraItemOffset -= it.sizeWithSpacings
                it.position(0, extraItemOffset, 0, mainAxisLayoutSize)
            },
            filter = { itemIndex ->
                val lane = laneInfo.getLane(itemIndex)
                when (lane) {
                    Unset, FullSpan -> {
                        firstItemIndices.all { it > itemIndex }
                    }
                    else -> {
                        firstItemIndices[lane] > itemIndex
                    }
                }
            }
        )

        val positionedItems = calculatePositionedItems(
            measuredItems,
            itemScrollOffsets,
            mainAxisLayoutSize,
        )

        extraItemOffset = itemScrollOffsets[0]
        val extraItemsAfter = calculateExtraItems(
            position = {
                val positionedItem = it.position(0, extraItemOffset, 0, mainAxisLayoutSize)
                extraItemOffset += it.sizeWithSpacings
                positionedItem
            },
            filter = { itemIndex ->
                if (itemIndex >= itemCount) {
                    return@calculateExtraItems false
                }
                val lane = laneInfo.getLane(itemIndex)
                when (lane) {
                    Unset, FullSpan -> {
                        currentItemIndices.all { it < itemIndex }
                    }
                    else -> {
                        currentItemIndices[lane] < itemIndex
                    }
                }
            }
        )

        debugLog {
            "positioned: $positionedItems"
        }

        // end placement

        // only scroll backward if the first item is not on screen or fully visible
        val canScrollBackward = !(firstItemIndices[0] == 0 && firstItemOffsets[0] <= 0)
        // only scroll forward if the last item is not on screen or fully visible
        val canScrollForward = currentItemOffsets.any { it > mainAxisAvailableSize } ||
            currentItemIndices.all { it < itemCount - 1 }

        @Suppress("UNCHECKED_CAST")
        return LazyStaggeredGridMeasureResult(
            firstVisibleItemIndices = firstItemIndices,
            firstVisibleItemScrollOffsets = firstItemOffsets,
            consumedScroll = consumedScroll,
            measureResult = layout(layoutWidth, layoutHeight) {
                extraItemsBefore.fastForEach { item ->
                    item.place(scope = this, context = this@measure)
                }

                positionedItems.fastForEach { item ->
                    item.place(scope = this, context = this@measure)
                }

                extraItemsAfter.fastForEach { item ->
                    item.place(scope = this, context = this@measure)
                }
            },
            canScrollForward = canScrollForward,
            canScrollBackward = canScrollBackward,
            isVertical = isVertical,
            visibleItemsInfo = positionedItems,
            totalItemsCount = itemCount,
            viewportSize = IntSize(layoutWidth, layoutHeight),
            viewportStartOffset = minOffset,
            viewportEndOffset = maxOffset,
            beforeContentPadding = beforeContentPadding,
            afterContentPadding = afterContentPadding,
            mainAxisItemSpacing = mainAxisSpacing
        )
    }
}

private fun LazyStaggeredGridMeasureContext.calculatePositionedItems(
    measuredItems: Array<ArrayDeque<LazyStaggeredGridMeasuredItem>>,
    itemScrollOffsets: IntArray,
    mainAxisLayoutSize: Int,
): List<LazyStaggeredGridPositionedItem> {
    val positionedItems = ArrayList<LazyStaggeredGridPositionedItem>(
        measuredItems.sumOf { it.size }
    )
    while (measuredItems.any { it.isNotEmpty() }) {
        // find the next item to position
        val laneIndex = measuredItems.indexOfMinBy {
            it.firstOrNull()?.index ?: Int.MAX_VALUE
        }
        val item = measuredItems[laneIndex].removeFirst()

        if (item.lane != laneIndex) {
            continue
        }

        val spanRange = SpanRange(item.lane, item.span)
        val mainAxisOffset = itemScrollOffsets.maxInRange(spanRange)
        val crossAxisOffset =
            if (laneIndex == 0) {
                0
            } else {
                resolvedSlotSums[laneIndex - 1] + crossAxisSpacing * laneIndex
            }

        if (item.placeables.isEmpty()) {
            // nothing to place, ignore spacings
            continue
        }

        positionedItems +=
            item.position(laneIndex, mainAxisOffset, crossAxisOffset, mainAxisLayoutSize)
        spanRange.forEach { lane ->
            itemScrollOffsets[lane] = mainAxisOffset + item.sizeWithSpacings
        }
    }
    return positionedItems
}

@ExperimentalFoundationApi
private inline fun LazyStaggeredGridMeasureContext.calculateExtraItems(
    position: (LazyStaggeredGridMeasuredItem) -> LazyStaggeredGridPositionedItem,
    filter: (itemIndex: Int) -> Boolean
): List<LazyStaggeredGridPositionedItem> {
    var result: MutableList<LazyStaggeredGridPositionedItem>? = null

    val pinnedItems = state.pinnedItems
    pinnedItems.fastForEach { item ->
        val index = itemProvider.findIndexByKey(item.key, item.index)

        if (filter(index)) {
            val spanRange = itemProvider.getSpanRange(index, 0)
            if (result == null) {
                result = mutableListOf()
            }
            val measuredItem = measuredItemProvider.getAndMeasure(index, spanRange)
            result?.add(position(measuredItem))
        }
    }

    return result ?: emptyList()
}

@JvmInline
private value class SpanRange private constructor(val packedValue: Long) {
    constructor(lane: Int, span: Int) : this(packInts(lane, lane + span))

    inline val start get(): Int = unpackInt1(packedValue)
    inline val end get(): Int = unpackInt2(packedValue)
    inline val size get(): Int = end - start
}

private inline fun SpanRange.forEach(block: (Int) -> Unit) {
    for (i in start until end) {
        block(i)
    }
}

private fun IntArray.offsetBy(delta: Int) {
    for (i in indices) {
        this[i] = this[i] + delta
    }
}

private fun IntArray.maxInRange(indexRange: SpanRange): Int {
    var max = Int.MIN_VALUE
    indexRange.forEach {
        max = maxOf(max, this[it])
    }
    return max
}

internal fun IntArray.indexOfMinValue(minBound: Int = Int.MIN_VALUE): Int {
    var result = -1
    var min = Int.MAX_VALUE
    for (i in indices) {
        if (this[i] in (minBound + 1) until min) {
            min = this[i]
            result = i
        }
    }

    return result
}

private inline fun <T> Array<T>.indexOfMinBy(block: (T) -> Int): Int {
    var result = -1
    var min = Int.MAX_VALUE
    for (i in indices) {
        val value = block(this[i])
        if (min > value) {
            min = value
            result = i
        }
    }

    return result
}

private fun IntArray.indexOfMaxValue(): Int {
    var result = -1
    var max = Int.MIN_VALUE
    for (i in indices) {
        if (max < this[i]) {
            max = this[i]
            result = i
        }
    }

    return result
}

private inline fun IntArray.transform(block: (Int) -> Int): IntArray {
    for (i in indices) {
        this[i] = block(this[i])
    }
    return this
}

private fun LazyStaggeredGridMeasureContext.ensureIndicesInRange(
    indices: IntArray,
    itemCount: Int
) {
    // reverse traverse to make sure last items are recorded to the latter lanes
    for (i in indices.indices.reversed()) {
        while (indices[i] >= itemCount || !laneInfo.assignedToLane(indices[i], i)) {
            indices[i] = findPreviousItemIndex(indices[i], i)
        }
        if (indices[i] >= 0) {
            // reserve item for span
            if (!itemProvider.isFullSpan(indices[i])) {
                laneInfo.setLane(indices[i], i)
            }
        }
    }
}

private fun LazyStaggeredGridMeasureContext.findPreviousItemIndex(item: Int, lane: Int): Int =
    laneInfo.findPreviousItemIndex(item, lane)

@OptIn(ExperimentalFoundationApi::class)
private class LazyStaggeredGridMeasureProvider(
    private val isVertical: Boolean,
    private val itemProvider: LazyLayoutItemProvider,
    private val measureScope: LazyLayoutMeasureScope,
    private val resolvedSlotSums: IntArray,
    private val crossAxisSpacing: Int,
    private val measuredItemFactory: MeasuredItemFactory,
) {
    private fun childConstraints(slot: Int, span: Int): Constraints {
        val previousSum = if (slot == 0) 0 else resolvedSlotSums[slot - 1]
        val crossAxisSize =
            resolvedSlotSums[slot + span - 1] - previousSum + crossAxisSpacing * (span - 1)
        return if (isVertical) {
            Constraints.fixedWidth(crossAxisSize)
        } else {
            Constraints.fixedHeight(crossAxisSize)
        }
    }

    fun getAndMeasure(index: Int, span: SpanRange): LazyStaggeredGridMeasuredItem {
        val key = itemProvider.getKey(index)
        val placeables = measureScope.measure(index, childConstraints(span.start, span.size))
        return measuredItemFactory.createItem(index, span.start, span.size, key, placeables)
    }
}

// This interface allows to avoid autoboxing on index param
private fun interface MeasuredItemFactory {
    fun createItem(
        index: Int,
        lane: Int,
        span: Int,
        key: Any,
        placeables: List<Placeable>
    ): LazyStaggeredGridMeasuredItem
}

private class LazyStaggeredGridMeasuredItem(
    val index: Int,
    val key: Any,
    val placeables: List<Placeable>,
    val isVertical: Boolean,
    val spacing: Int,
    val lane: Int,
    val span: Int,
) {
    var isVisible = true

    val mainAxisSize: Int = placeables.fastFold(0) { size, placeable ->
        size + if (isVertical) placeable.height else placeable.width
    }

    val sizeWithSpacings: Int = (mainAxisSize + spacing).coerceAtLeast(0)

    val crossAxisSize: Int = placeables.fastMaxOfOrNull {
        if (isVertical) it.width else it.height
    } ?: 0

    fun position(
        lane: Int,
        mainAxis: Int,
        crossAxis: Int,
        mainAxisLayoutSize: Int,
    ): LazyStaggeredGridPositionedItem =
        LazyStaggeredGridPositionedItem(
            offset = if (isVertical) {
                IntOffset(crossAxis, mainAxis)
            } else {
                IntOffset(mainAxis, crossAxis)
            },
            lane = lane,
            index = index,
            key = key,
            size = if (isVertical) {
                IntSize(crossAxisSize, sizeWithSpacings)
            } else {
                IntSize(sizeWithSpacings, crossAxisSize)
            },
            placeables = placeables,
            isVertical = isVertical,
            mainAxisLayoutSize = mainAxisLayoutSize,
        )
}

@OptIn(ExperimentalFoundationApi::class)
private class LazyStaggeredGridPositionedItem(
    override val offset: IntOffset,
    override val index: Int,
    override val lane: Int,
    override val key: Any,
    override val size: IntSize,
    private val placeables: List<Placeable>,
    private val isVertical: Boolean,
    private val mainAxisLayoutSize: Int,
) : LazyStaggeredGridItemInfo {
    fun place(
        scope: Placeable.PlacementScope,
        context: LazyStaggeredGridMeasureContext
    ) = with(context) {
        with(scope) {
            placeables.fastForEach { placeable ->
                val reverseLayoutAwareOffset = if (reverseLayout) {
                    offset.copy { mainAxisOffset ->
                        mainAxisLayoutSize - mainAxisOffset - placeable.mainAxisSize
                    }
                } else {
                    offset
                }

                placeable.placeRelativeWithLayer(reverseLayoutAwareOffset + contentOffset)
            }
        }
    }

    private inline val Placeable.mainAxisSize get() = if (isVertical) height else width
    private inline fun IntOffset.copy(mainAxisMap: (Int) -> Int): IntOffset =
        IntOffset(if (isVertical) x else mainAxisMap(x), if (isVertical) mainAxisMap(y) else y)

    override fun toString(): String =
        if (DebugLoggingEnabled) {
            "{$index at $offset}"
        } else {
            super.toString()
        }
}
