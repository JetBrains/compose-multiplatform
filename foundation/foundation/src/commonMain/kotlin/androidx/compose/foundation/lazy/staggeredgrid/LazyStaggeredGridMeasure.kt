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
import androidx.compose.runtime.collection.MutableVector
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.util.fastForEach
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign

@ExperimentalFoundationApi
internal fun LazyLayoutMeasureScope.measureStaggeredGrid(
    state: LazyStaggeredGridState,
    itemProvider: LazyLayoutItemProvider,
    resolvedSlotSums: IntArray,
    constraints: Constraints,
    isVertical: Boolean,
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
                context.spans.reset()
                IntArray(resolvedSlotSums.size).apply {
                    // Try to adjust indices in case grid got resized
                    for (lane in indices) {
                        this[lane] = if (lane < firstVisibleIndices.size) {
                            firstVisibleIndices[lane]
                        } else {
                            if (lane == 0) {
                                0
                            } else {
                                context.findNextItemIndex(this[lane - 1], lane)
                            }
                        }
                        // Ensure spans are updated to be in correct range
                        context.spans.setSpan(this[lane], lane)
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
    val itemProvider: LazyLayoutItemProvider,
    val resolvedSlotSums: IntArray,
    val constraints: Constraints,
    val isVertical: Boolean,
    val measureScope: LazyLayoutMeasureScope,
    val mainAxisAvailableSize: Int,
    val contentOffset: IntOffset,
    val beforeContentPadding: Int,
    val afterContentPadding: Int,
    val mainAxisSpacing: Int,
    val crossAxisSpacing: Int,
) {
    val measuredItemProvider = LazyStaggeredGridMeasureProvider(
        isVertical,
        itemProvider,
        measureScope,
        resolvedSlotSums
    ) { index, lane, key, placeables ->
        val isLastInLane = spans.findNextItemIndex(index, lane) >= itemProvider.itemCount
        LazyStaggeredGridMeasuredItem(
            index,
            key,
            placeables,
            isVertical,
            contentOffset,
            if (isLastInLane) 0 else mainAxisSpacing
        )
    }

    val spans = state.spans
}

@ExperimentalFoundationApi
private fun LazyStaggeredGridMeasureContext.measure(
    initialScrollDelta: Int,
    initialItemIndices: IntArray,
    initialItemOffsets: IntArray,
    canRestartMeasure: Boolean,
): LazyStaggeredGridMeasureResult {
    with(measureScope) {
        val itemCount = itemProvider.itemCount

        if (itemCount <= 0 || resolvedSlotSums.isEmpty()) {
            return LazyStaggeredGridMeasureResult(
                firstVisibleItemIndices = initialItemIndices,
                firstVisibleItemScrollOffsets = initialItemOffsets,
                consumedScroll = 0f,
                measureResult = layout(constraints.minWidth, constraints.minHeight) {},
                canScrollForward = false,
                canScrollBackward = false,
                visibleItemsInfo = emptyList(),
                totalItemsCount = itemCount,
                viewportSize = IntSize(constraints.minWidth, constraints.minHeight),
                viewportStartOffset = -beforeContentPadding,
                viewportEndOffset = mainAxisAvailableSize + afterContentPadding,
                beforeContentPadding = beforeContentPadding,
                afterContentPadding = afterContentPadding,
                mainAxisItemSpacingInternal = mainAxisSpacing
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
        val measuredItems = Array(resolvedSlotSums.size) {
            ArrayDeque<LazyStaggeredGridMeasuredItem>()
        }

        // include the start padding so we compose items in the padding area. before starting
        // scrolling forward we would remove it back
        firstItemOffsets.offsetBy(-beforeContentPadding)

        fun hasSpaceBeforeFirst(): Boolean {
            for (lane in firstItemIndices.indices) {
                val itemIndex = firstItemIndices[lane]
                val itemOffset = firstItemOffsets[lane]

                if (itemOffset < -mainAxisSpacing && itemIndex > 0) {
                    return true
                }
            }

            return false
        }

        var laneToCheckForGaps = -1

        // we had scrolled backward or we compose items in the start padding area, which means
        // items before current firstItemScrollOffset should be visible. compose them and update
        // firstItemScrollOffset
        while (hasSpaceBeforeFirst()) {
            val laneIndex = firstItemOffsets.indexOfMinValue()
            val previousItemIndex = findPreviousItemIndex(
                item = firstItemIndices[laneIndex],
                lane = laneIndex
            )

            if (previousItemIndex < 0) {
                laneToCheckForGaps = laneIndex
                break
            }

            if (spans.getSpan(previousItemIndex) == LazyStaggeredGridSpans.Unset) {
                spans.setSpan(previousItemIndex, laneIndex)
            }

            val measuredItem = measuredItemProvider.getAndMeasure(
                previousItemIndex,
                laneIndex
            )
            measuredItems[laneIndex].addFirst(measuredItem)

            firstItemIndices[laneIndex] = previousItemIndex
            firstItemOffsets[laneIndex] += measuredItem.sizeWithSpacings
        }

        fun misalignedStart(referenceLane: Int): Boolean {
            // If we scrolled past the first item in the lane, we have a point of reference
            // to re-align items.
            val laneRange = firstItemIndices.indices

            // Case 1: Each lane has laid out all items, but offsets do no match
            val misalignedOffsets = laneRange.any { lane ->
                findPreviousItemIndex(firstItemIndices[lane], lane) == -1 &&
                    firstItemOffsets[lane] != firstItemOffsets[referenceLane]
            }
            // Case 2: Some lanes are still missing items, and there's no space left to place them
            val moreItemsInOtherLanes = laneRange.any { lane ->
                findPreviousItemIndex(firstItemIndices[lane], lane) != -1 &&
                    firstItemOffsets[lane] >= firstItemOffsets[referenceLane]
            }
            // Case 3: the first item is in the wrong lane (it should always be in
            // the first one)
            val firstItemInWrongLane = spans.getSpan(0) != 0
            // If items are not aligned, reset all measurement data we gathered before and
            // proceed with initial measure
            return misalignedOffsets || moreItemsInOtherLanes || firstItemInWrongLane
        }

        // define min offset (currently includes beforeContentPadding)
        val minOffset = -beforeContentPadding

        // we scrolled backward, but there were not enough items to fill the start. this means
        // some amount of scroll should be left over
        if (firstItemOffsets[0] < minOffset) {
            scrollDelta += firstItemOffsets[0]
            firstItemOffsets.offsetBy(minOffset - firstItemOffsets[0])
        }

        // neutralize previously added start padding as we stopped filling the before content padding
        firstItemOffsets.offsetBy(beforeContentPadding)

        laneToCheckForGaps =
            if (laneToCheckForGaps == -1) firstItemIndices.indexOf(0) else laneToCheckForGaps

        // re-check if columns are aligned after measure
        if (laneToCheckForGaps != -1) {
            val lane = laneToCheckForGaps
            if (misalignedStart(lane) && canRestartMeasure) {
                spans.reset()
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

        val currentItemIndices = initialItemIndices.copyOf().apply {
            // ensure indices match item count, in case it decreased
            ensureIndicesInRange(this, itemCount)
        }
        val currentItemOffsets = IntArray(initialItemOffsets.size) {
            -(initialItemOffsets[it] - scrollDelta)
        }

        val maxOffset = (mainAxisAvailableSize + afterContentPadding).coerceAtLeast(0)

        // compose first visible items we received from state
        currentItemIndices.forEachIndexed { laneIndex, itemIndex ->
            if (itemIndex < 0) return@forEachIndexed

            val measuredItem = measuredItemProvider.getAndMeasure(itemIndex, laneIndex)
            currentItemOffsets[laneIndex] += measuredItem.sizeWithSpacings
            measuredItems[laneIndex].addLast(measuredItem)

            spans.setSpan(itemIndex, laneIndex)
        }

        // then composing visible items forward until we fill the whole viewport.
        // we want to have at least one item in visibleItems even if in fact all the items are
        // offscreen, this can happen if the content padding is larger than the available size.
        while (
            currentItemOffsets.any { it <= maxOffset } || measuredItems.all { it.isEmpty() }
        ) {
            val currentLaneIndex = currentItemOffsets.indexOfMinValue()
            val nextItemIndex =
                findNextItemIndex(currentItemIndices[currentLaneIndex], currentLaneIndex)

            if (nextItemIndex >= itemCount) {
                // if any items changed its size, the spans may not behave correctly
                // there are no more items in this lane, but there could be more in others
                // recheck if we can add more items and reset spans accordingly
                var missedItemIndex = Int.MAX_VALUE
                currentItemIndices.forEachIndexed { laneIndex, i ->
                    if (laneIndex == currentLaneIndex) return@forEachIndexed
                    var itemIndex = findNextItemIndex(i, laneIndex)
                    while (itemIndex < itemCount) {
                        missedItemIndex = minOf(itemIndex, missedItemIndex)
                        spans.setSpan(itemIndex, LazyStaggeredGridSpans.Unset)
                        itemIndex = findNextItemIndex(itemIndex, laneIndex)
                    }
                }
                // there's at least one missed item which may fit current lane
                if (missedItemIndex != Int.MAX_VALUE && canRestartMeasure) {
                    // reset current lane to the missed item index and restart measure
                    initialItemIndices[currentLaneIndex] =
                        min(initialItemIndices[currentLaneIndex], missedItemIndex)
                    return measure(
                        initialScrollDelta = initialScrollDelta,
                        initialItemIndices = initialItemIndices,
                        initialItemOffsets = initialItemOffsets,
                        canRestartMeasure = false
                    )
                } else {
                    break
                }
            }

            if (firstItemIndices[currentLaneIndex] == -1) {
                firstItemIndices[currentLaneIndex] = nextItemIndex
            }
            spans.setSpan(nextItemIndex, currentLaneIndex)

            val measuredItem =
                measuredItemProvider.getAndMeasure(nextItemIndex, currentLaneIndex)
            currentItemOffsets[currentLaneIndex] += measuredItem.sizeWithSpacings
            measuredItems[currentLaneIndex].addLast(measuredItem)
            currentItemIndices[currentLaneIndex] = nextItemIndex
        }

        // some measured items are offscreen, remove them from the list and adjust indices/offsets
        for (laneIndex in measuredItems.indices) {
            val laneItems = measuredItems[laneIndex]
            var offset = currentItemOffsets[laneIndex]
            var inBoundsIndex = 0
            for (i in laneItems.lastIndex downTo 0) {
                val item = laneItems[i]
                offset -= item.sizeWithSpacings
                inBoundsIndex = i
                if (offset <= minOffset + mainAxisSpacing) {
                    break
                }
            }

            // the rest of the items are offscreen, update firstIndex/Offset for lane and remove
            // items from measured list
            for (i in 0 until inBoundsIndex) {
                val item = laneItems.removeFirst()
                firstItemOffsets[laneIndex] -= item.sizeWithSpacings
            }
            if (laneItems.isNotEmpty()) {
                firstItemIndices[laneIndex] = laneItems.first().index
            }
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
                        spans.reset()
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

                spans.setSpan(previousIndex, laneIndex)

                val measuredItem = measuredItemProvider.getAndMeasure(
                    previousIndex,
                    laneIndex
                )
                measuredItems[laneIndex].addFirst(measuredItem)
                firstItemOffsets[laneIndex] += measuredItem.sizeWithSpacings
                firstItemIndices[laneIndex] = previousIndex
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
        if (beforeContentPadding > 0) {
            for (laneIndex in measuredItems.indices) {
                val laneItems = measuredItems[laneIndex]
                for (i in laneItems.indices) {
                    val size = laneItems[i].sizeWithSpacings
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

        // end measure

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

        // Placement
        val positionedItems = MutableVector<LazyStaggeredGridPositionedItem>(
            capacity = measuredItems.sumOf { it.size }
        )
        while (measuredItems.any { it.isNotEmpty() }) {
            // find the next item to position
            val laneIndex = measuredItems.indexOfMinBy {
                it.firstOrNull()?.index ?: Int.MAX_VALUE
            }
            val item = measuredItems[laneIndex].removeFirst()

            // todo(b/182882362): arrangement support
            val mainAxisOffset = itemScrollOffsets[laneIndex]
            val crossAxisOffset =
                if (laneIndex == 0) {
                    0
                } else {
                    resolvedSlotSums[laneIndex - 1] + crossAxisSpacing * laneIndex
                }

            positionedItems += item.position(laneIndex, mainAxisOffset, crossAxisOffset)
            itemScrollOffsets[laneIndex] += item.sizeWithSpacings
        }

        // todo: reverse layout support

        // End placement

        // only scroll backward if the first item is not on screen or fully visible
        val canScrollBackward = !(firstItemIndices[0] == 0 && firstItemOffsets[0] <= 0)
        // only scroll forward if the last item is not on screen or fully visible
        val canScrollForward = currentItemOffsets.any { it > mainAxisAvailableSize }

        @Suppress("UNCHECKED_CAST")
        return LazyStaggeredGridMeasureResult(
            firstVisibleItemIndices = firstItemIndices,
            firstVisibleItemScrollOffsets = firstItemOffsets,
            consumedScroll = consumedScroll,
            measureResult = layout(layoutWidth, layoutHeight) {
                positionedItems.forEach { item ->
                    item.place(this)
                }
            },
            canScrollForward = canScrollForward,
            canScrollBackward = canScrollBackward,
            visibleItemsInfo = positionedItems.asMutableList(),
            totalItemsCount = itemCount,
            viewportSize = IntSize(layoutWidth, layoutHeight),
            viewportStartOffset = minOffset,
            viewportEndOffset = maxOffset,
            beforeContentPadding = beforeContentPadding,
            afterContentPadding = afterContentPadding,
            mainAxisItemSpacingInternal = mainAxisSpacing
        )
    }
}

private fun IntArray.offsetBy(delta: Int) {
    for (i in indices) {
        this[i] = this[i] + delta
    }
}

internal fun IntArray.indexOfMinValue(): Int {
    var result = -1
    var min = Int.MAX_VALUE
    for (i in indices) {
        if (min > this[i]) {
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
        while (indices[i] >= itemCount) {
            indices[i] = findPreviousItemIndex(indices[i], i)
        }
        if (indices[i] != -1) {
            // reserve item for span
            spans.setSpan(indices[i], i)
        }
    }
}

private fun LazyStaggeredGridMeasureContext.findPreviousItemIndex(item: Int, lane: Int): Int =
    spans.findPreviousItemIndex(item, lane)

private fun LazyStaggeredGridMeasureContext.findNextItemIndex(item: Int, lane: Int): Int =
    spans.findNextItemIndex(item, lane)

@OptIn(ExperimentalFoundationApi::class)
private class LazyStaggeredGridMeasureProvider(
    private val isVertical: Boolean,
    private val itemProvider: LazyLayoutItemProvider,
    private val measureScope: LazyLayoutMeasureScope,
    private val resolvedSlotSums: IntArray,
    private val measuredItemFactory: MeasuredItemFactory
) {
    private fun childConstraints(slot: Int): Constraints {
        val previousSum = if (slot == 0) 0 else resolvedSlotSums[slot - 1]
        val crossAxisSize = resolvedSlotSums[slot] - previousSum
        return if (isVertical) {
            Constraints.fixedWidth(crossAxisSize)
        } else {
            Constraints.fixedHeight(crossAxisSize)
        }
    }

    fun getAndMeasure(index: Int, lane: Int): LazyStaggeredGridMeasuredItem {
        val key = itemProvider.getKey(index)
        val placeables = measureScope.measure(index, childConstraints(lane))
        return measuredItemFactory.createItem(index, lane, key, placeables)
    }
}

// This interface allows to avoid autoboxing on index param
private fun interface MeasuredItemFactory {
    fun createItem(
        index: Int,
        lane: Int,
        key: Any,
        placeables: List<Placeable>
    ): LazyStaggeredGridMeasuredItem
}

private class LazyStaggeredGridMeasuredItem(
    val index: Int,
    val key: Any,
    val placeables: List<Placeable>,
    val isVertical: Boolean,
    val contentOffset: IntOffset,
    val spacing: Int
) {
    val mainAxisSize: Int = placeables.fastFold(0) { size, placeable ->
        size + if (isVertical) placeable.height else placeable.width
    }

    val sizeWithSpacings: Int = (mainAxisSize + spacing).coerceAtLeast(0)

    val crossAxisSize: Int = placeables.fastMaxOfOrNull {
        if (isVertical) it.width else it.height
    }!!

    fun position(
        lane: Int,
        mainAxis: Int,
        crossAxis: Int,
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
            size = IntSize(sizeWithSpacings, crossAxisSize),
            placeables = placeables,
            contentOffset = contentOffset,
            isVertical = isVertical
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
    private val contentOffset: IntOffset,
    private val isVertical: Boolean
) : LazyStaggeredGridItemInfo {
    fun place(scope: Placeable.PlacementScope) = with(scope) {
        placeables.fastForEach { placeable ->
            if (isVertical) {
                placeable.placeWithLayer(offset + contentOffset)
            } else {
                placeable.placeRelativeWithLayer(offset + contentOffset)
            }
        }
    }
}
