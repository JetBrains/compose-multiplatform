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

package androidx.compose.foundation.lazy.grid

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.fastFilter
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.layout.LazyLayoutPinnedItemList
import androidx.compose.foundation.lazy.layout.findIndexByKey
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastSumBy
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign

/**
 * Measures and calculates the positions for the currently visible items. The result is produced
 * as a [LazyGridMeasureResult] which contains all the calculations.
 */
@OptIn(ExperimentalFoundationApi::class)
internal fun measureLazyGrid(
    itemsCount: Int,
    itemProvider: LazyGridItemProvider,
    measuredLineProvider: LazyMeasuredLineProvider,
    measuredItemProvider: LazyMeasuredItemProvider,
    mainAxisAvailableSize: Int,
    beforeContentPadding: Int,
    afterContentPadding: Int,
    spaceBetweenLines: Int,
    firstVisibleLineIndex: LineIndex,
    firstVisibleLineScrollOffset: Int,
    scrollToBeConsumed: Float,
    constraints: Constraints,
    isVertical: Boolean,
    verticalArrangement: Arrangement.Vertical?,
    horizontalArrangement: Arrangement.Horizontal?,
    reverseLayout: Boolean,
    density: Density,
    placementAnimator: LazyGridItemPlacementAnimator,
    spanLayoutProvider: LazyGridSpanLayoutProvider,
    pinnedItems: LazyLayoutPinnedItemList,
    layout: (Int, Int, Placeable.PlacementScope.() -> Unit) -> MeasureResult
): LazyGridMeasureResult {
    require(beforeContentPadding >= 0)
    require(afterContentPadding >= 0)
    if (itemsCount <= 0) {
        // empty data set. reset the current scroll and report zero size
        return LazyGridMeasureResult(
            firstVisibleLine = null,
            firstVisibleLineScrollOffset = 0,
            canScrollForward = false,
            consumedScroll = 0f,
            measureResult = layout(constraints.minWidth, constraints.minHeight) {},
            visibleItemsInfo = emptyList(),
            viewportStartOffset = -beforeContentPadding,
            viewportEndOffset = mainAxisAvailableSize + afterContentPadding,
            totalItemsCount = 0,
            reverseLayout = reverseLayout,
            orientation = if (isVertical) Orientation.Vertical else Orientation.Horizontal,
            afterContentPadding = afterContentPadding,
            mainAxisItemSpacing = spaceBetweenLines
        )
    } else {
        var currentFirstLineIndex = firstVisibleLineIndex
        var currentFirstLineScrollOffset = firstVisibleLineScrollOffset

        // represents the real amount of scroll we applied as a result of this measure pass.
        var scrollDelta = scrollToBeConsumed.roundToInt()

        // applying the whole requested scroll offset. we will figure out if we can't consume
        // all of it later
        currentFirstLineScrollOffset -= scrollDelta

        // if the current scroll offset is less than minimally possible
        if (currentFirstLineIndex == LineIndex(0) && currentFirstLineScrollOffset < 0) {
            scrollDelta += currentFirstLineScrollOffset
            currentFirstLineScrollOffset = 0
        }

        // this will contain all the MeasuredItems representing the visible lines
        val visibleLines = mutableListOf<LazyGridMeasuredLine>()

        // define min and max offsets
        val minOffset = -beforeContentPadding + if (spaceBetweenLines < 0) spaceBetweenLines else 0
        val maxOffset = mainAxisAvailableSize

        // include the start padding so we compose items in the padding area and neutralise item
        // spacing (if the spacing is negative this will make sure the previous item is composed)
        // before starting scrolling forward we will remove it back
        currentFirstLineScrollOffset += minOffset

        // we had scrolled backward or we compose items in the start padding area, which means
        // items before current firstLineScrollOffset should be visible. compose them and update
        // firstLineScrollOffset
        while (currentFirstLineScrollOffset < 0 && currentFirstLineIndex > LineIndex(0)) {
            val previous = LineIndex(currentFirstLineIndex.value - 1)
            val measuredLine = measuredLineProvider.getAndMeasure(previous)
            visibleLines.add(0, measuredLine)
            currentFirstLineScrollOffset += measuredLine.mainAxisSizeWithSpacings
            currentFirstLineIndex = previous
        }

        // if we were scrolled backward, but there were not enough items before. this means
        // not the whole scroll was consumed
        if (currentFirstLineScrollOffset < minOffset) {
            scrollDelta += currentFirstLineScrollOffset
            currentFirstLineScrollOffset = minOffset
        }

        // neutralize previously added padding as we stopped filling the before content padding
        currentFirstLineScrollOffset -= minOffset

        var index = currentFirstLineIndex
        val maxMainAxis = (maxOffset + afterContentPadding).coerceAtLeast(0)
        var currentMainAxisOffset = -currentFirstLineScrollOffset

        // first we need to skip lines we already composed while composing backward
        visibleLines.fastForEach {
            index++
            currentMainAxisOffset += it.mainAxisSizeWithSpacings
        }

        // then composing visible lines forward until we fill the whole viewport.
        // we want to have at least one line in visibleItems even if in fact all the items are
        // offscreen, this can happen if the content padding is larger than the available size.
        while (index.value < itemsCount &&
            (currentMainAxisOffset < maxMainAxis ||
                currentMainAxisOffset <= 0 || // filling beforeContentPadding area
                visibleLines.isEmpty())
        ) {
            val measuredLine = measuredLineProvider.getAndMeasure(index)
            if (measuredLine.isEmpty()) {
                break
            }

            currentMainAxisOffset += measuredLine.mainAxisSizeWithSpacings
            if (currentMainAxisOffset <= minOffset &&
                measuredLine.items.last().index.value != itemsCount - 1) {
                // this line is offscreen and will not be placed. advance firstVisibleLineIndex
                currentFirstLineIndex = index + 1
                currentFirstLineScrollOffset -= measuredLine.mainAxisSizeWithSpacings
            } else {
                visibleLines.add(measuredLine)
            }
            index++
        }

        // we didn't fill the whole viewport with lines starting from firstVisibleLineIndex.
        // lets try to scroll back if we have enough lines before firstVisibleLineIndex.
        if (currentMainAxisOffset < maxOffset) {
            val toScrollBack = maxOffset - currentMainAxisOffset
            currentFirstLineScrollOffset -= toScrollBack
            currentMainAxisOffset += toScrollBack
            while (currentFirstLineScrollOffset < beforeContentPadding &&
                currentFirstLineIndex > LineIndex(0)
            ) {
                val previousIndex = LineIndex(currentFirstLineIndex.value - 1)
                val measuredLine = measuredLineProvider.getAndMeasure(previousIndex)
                visibleLines.add(0, measuredLine)
                currentFirstLineScrollOffset += measuredLine.mainAxisSizeWithSpacings
                currentFirstLineIndex = previousIndex
            }
            scrollDelta += toScrollBack
            if (currentFirstLineScrollOffset < 0) {
                scrollDelta += currentFirstLineScrollOffset
                currentMainAxisOffset += currentFirstLineScrollOffset
                currentFirstLineScrollOffset = 0
            }
        }

        // report the amount of pixels we consumed. scrollDelta can be smaller than
        // scrollToBeConsumed if there were not enough lines to fill the offered space or it
        // can be larger if lines were resized, or if, for example, we were previously
        // displaying the line 15, but now we have only 10 lines in total in the data set.
        val consumedScroll = if (scrollToBeConsumed.roundToInt().sign == scrollDelta.sign &&
            abs(scrollToBeConsumed.roundToInt()) >= abs(scrollDelta)
        ) {
            scrollDelta.toFloat()
        } else {
            scrollToBeConsumed
        }

        // the initial offset for lines from visibleLines list
        require(currentFirstLineScrollOffset >= 0)
        val visibleLinesScrollOffset = -currentFirstLineScrollOffset
        var firstLine = visibleLines.first()

        val firstItemIndex = firstLine.items.firstOrNull()?.index?.value ?: 0
        val lastItemIndex = visibleLines.lastOrNull()?.items?.lastOrNull()?.index?.value ?: 0
        val extraItemsBefore = calculateExtraItems(
            pinnedItems,
            measuredItemProvider,
            itemProvider,
            itemConstraints = { measuredLineProvider.itemConstraints(it) },
            filter = { it in 0 until firstItemIndex }
        )

        val extraItemsAfter = calculateExtraItems(
            pinnedItems,
            measuredItemProvider,
            itemProvider,
            itemConstraints = { measuredLineProvider.itemConstraints(it) },
            filter = { it in (lastItemIndex + 1) until itemsCount }
        )

        // even if we compose lines to fill before content padding we should ignore lines fully
        // located there for the state's scroll position calculation (first line + first offset)
        if (beforeContentPadding > 0 || spaceBetweenLines < 0) {
            for (i in visibleLines.indices) {
                val size = visibleLines[i].mainAxisSizeWithSpacings
                if (currentFirstLineScrollOffset != 0 && size <= currentFirstLineScrollOffset &&
                    i != visibleLines.lastIndex) {
                    currentFirstLineScrollOffset -= size
                    firstLine = visibleLines[i + 1]
                } else {
                    break
                }
            }
        }

        val layoutWidth = if (isVertical) {
            constraints.maxWidth
        } else {
            constraints.constrainWidth(currentMainAxisOffset)
        }
        val layoutHeight = if (isVertical) {
            constraints.constrainHeight(currentMainAxisOffset)
        } else {
            constraints.maxHeight
        }

        val positionedItems = calculateItemsOffsets(
            lines = visibleLines,
            itemsBefore = extraItemsBefore,
            itemsAfter = extraItemsAfter,
            layoutWidth = layoutWidth,
            layoutHeight = layoutHeight,
            finalMainAxisOffset = currentMainAxisOffset,
            maxOffset = maxOffset,
            firstLineScrollOffset = visibleLinesScrollOffset,
            isVertical = isVertical,
            verticalArrangement = verticalArrangement,
            horizontalArrangement = horizontalArrangement,
            reverseLayout = reverseLayout,
            density = density
        )

        placementAnimator.onMeasured(
            consumedScroll = consumedScroll.toInt(),
            layoutWidth = layoutWidth,
            layoutHeight = layoutHeight,
            positionedItems = positionedItems,
            itemProvider = measuredItemProvider,
            spanLayoutProvider = spanLayoutProvider
        )

        return LazyGridMeasureResult(
            firstVisibleLine = firstLine,
            firstVisibleLineScrollOffset = currentFirstLineScrollOffset,
            canScrollForward =
                lastItemIndex != itemsCount - 1 || currentMainAxisOffset > maxOffset,
            consumedScroll = consumedScroll,
            measureResult = layout(layoutWidth, layoutHeight) {
                positionedItems.fastForEach { it.place(this) }
            },
            viewportStartOffset = -beforeContentPadding,
            viewportEndOffset = mainAxisAvailableSize + afterContentPadding,
            visibleItemsInfo = if (extraItemsBefore.isEmpty() && extraItemsAfter.isEmpty()) {
                positionedItems
            } else {
                positionedItems.fastFilter {
                    it.index in firstItemIndex..lastItemIndex
                }
            },
            totalItemsCount = itemsCount,
            reverseLayout = reverseLayout,
            orientation = if (isVertical) Orientation.Vertical else Orientation.Horizontal,
            afterContentPadding = afterContentPadding,
            mainAxisItemSpacing = spaceBetweenLines
        )
    }
}

@ExperimentalFoundationApi
private inline fun calculateExtraItems(
    pinnedItems: LazyLayoutPinnedItemList,
    measuredItemProvider: LazyMeasuredItemProvider,
    itemProvider: LazyGridItemProvider,
    itemConstraints: (ItemIndex) -> Constraints,
    filter: (Int) -> Boolean
): List<LazyGridMeasuredItem> {
    var items: MutableList<LazyGridMeasuredItem>? = null

    pinnedItems.fastForEach { item ->
        val index = itemProvider.findIndexByKey(item.key, item.index)
        if (filter(index)) {
            val itemIndex = ItemIndex(index)
            val constraints = itemConstraints(itemIndex)
            val measuredItem = measuredItemProvider.getAndMeasure(
                itemIndex,
                constraints = constraints
            )
            if (items == null) {
                items = mutableListOf()
            }
            items?.add(measuredItem)
        }
    }

    return items ?: emptyList()
}

/**
 * Calculates [LazyGridMeasuredLine]s offsets.
 */
private fun calculateItemsOffsets(
    lines: List<LazyGridMeasuredLine>,
    itemsBefore: List<LazyGridMeasuredItem>,
    itemsAfter: List<LazyGridMeasuredItem>,
    layoutWidth: Int,
    layoutHeight: Int,
    finalMainAxisOffset: Int,
    maxOffset: Int,
    firstLineScrollOffset: Int,
    isVertical: Boolean,
    verticalArrangement: Arrangement.Vertical?,
    horizontalArrangement: Arrangement.Horizontal?,
    reverseLayout: Boolean,
    density: Density,
): MutableList<LazyGridPositionedItem> {
    val mainAxisLayoutSize = if (isVertical) layoutHeight else layoutWidth
    val hasSpareSpace = finalMainAxisOffset < min(mainAxisLayoutSize, maxOffset)
    if (hasSpareSpace) {
        check(firstLineScrollOffset == 0)
    }

    val positionedItems = ArrayList<LazyGridPositionedItem>(lines.fastSumBy { it.items.size })

    if (hasSpareSpace) {
        require(itemsBefore.isEmpty() && itemsAfter.isEmpty())
        val linesCount = lines.size
        fun Int.reverseAware() =
            if (!reverseLayout) this else linesCount - this - 1

        val sizes = IntArray(linesCount) { index ->
            lines[index.reverseAware()].mainAxisSize
        }
        val offsets = IntArray(linesCount) { 0 }
        if (isVertical) {
            with(requireNotNull(verticalArrangement)) {
                density.arrange(mainAxisLayoutSize, sizes, offsets)
            }
        } else {
            with(requireNotNull(horizontalArrangement)) {
                // Enforces Ltr layout direction as it is mirrored with placeRelative later.
                density.arrange(mainAxisLayoutSize, sizes, LayoutDirection.Ltr, offsets)
            }
        }

        val reverseAwareOffsetIndices =
            if (reverseLayout) offsets.indices.reversed() else offsets.indices

        for (index in reverseAwareOffsetIndices) {
            val absoluteOffset = offsets[index]
            // when reverseLayout == true, offsets are stored in the reversed order to items
            val line = lines[index.reverseAware()]
            val relativeOffset = if (reverseLayout) {
                // inverse offset to align with scroll direction for positioning
                mainAxisLayoutSize - absoluteOffset - line.mainAxisSize
            } else {
                absoluteOffset
            }
            positionedItems.addAll(
                line.position(relativeOffset, layoutWidth, layoutHeight)
            )
        }
    } else {
        var currentMainAxis = firstLineScrollOffset

        itemsBefore.fastForEach {
            currentMainAxis -= it.mainAxisSizeWithSpacings
            positionedItems.add(it.positionExtraItem(currentMainAxis, layoutWidth, layoutHeight))
        }

        currentMainAxis = firstLineScrollOffset
        lines.fastForEach {
            positionedItems.addAll(it.position(currentMainAxis, layoutWidth, layoutHeight))
            currentMainAxis += it.mainAxisSizeWithSpacings
        }

        itemsAfter.fastForEach {
            positionedItems.add(it.positionExtraItem(currentMainAxis, layoutWidth, layoutHeight))
            currentMainAxis += it.mainAxisSizeWithSpacings
        }
    }
    return positionedItems
}

private fun LazyGridMeasuredItem.positionExtraItem(
    mainAxisOffset: Int,
    layoutWidth: Int,
    layoutHeight: Int
): LazyGridPositionedItem =
    position(
        mainAxisOffset = mainAxisOffset,
        crossAxisOffset = 0,
        layoutWidth = layoutWidth,
        layoutHeight = layoutHeight,
        row = 0,
        column = 0
    )
