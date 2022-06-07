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
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
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
    measuredLineProvider: LazyMeasuredLineProvider,
    measuredItemProvider: LazyMeasuredItemProvider,
    mainAxisAvailableSize: Int,
    slotsPerLine: Int,
    beforeContentPadding: Int,
    afterContentPadding: Int,
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
            afterContentPadding = afterContentPadding
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
        val visibleLines = mutableListOf<LazyMeasuredLine>()

        // include the start padding so we compose items in the padding area. before starting
        // scrolling forward we would remove it back
        currentFirstLineScrollOffset -= beforeContentPadding

        // define min and max offsets (min offset currently includes beforeContentPadding)
        val minOffset = -beforeContentPadding
        val maxOffset = mainAxisAvailableSize

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
        // if we were scrolled backward, but there were not enough lines before. this means
        // not the whole scroll was consumed
        if (currentFirstLineScrollOffset < minOffset) {
            scrollDelta += currentFirstLineScrollOffset
            currentFirstLineScrollOffset = minOffset
        }

        // neutralize previously added start padding as we stopped filling the before content padding
        currentFirstLineScrollOffset += beforeContentPadding

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
        while (currentMainAxisOffset <= maxMainAxis || visibleLines.isEmpty()) {
            val measuredLine = measuredLineProvider.getAndMeasure(index)
            if (measuredLine.isEmpty()) {
                --index
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
        val visibleLinesScrollOffset = -currentFirstLineScrollOffset
        var firstLine = visibleLines.first()

        // even if we compose lines to fill before content padding we should ignore lines fully
        // located there for the state's scroll position calculation (first line + first offset)
        if (beforeContentPadding > 0) {
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
            slotsPerLine = slotsPerLine,
            reverseLayout = reverseLayout,
            positionedItems = positionedItems,
            measuredItemProvider = measuredItemProvider
        )

        return LazyGridMeasureResult(
            firstVisibleLine = firstLine,
            firstVisibleLineScrollOffset = currentFirstLineScrollOffset,
            canScrollForward = currentMainAxisOffset > maxOffset,
            consumedScroll = consumedScroll,
            measureResult = layout(layoutWidth, layoutHeight) {
                positionedItems.fastForEach { it.place(this) }
            },
            viewportStartOffset = -beforeContentPadding,
            viewportEndOffset = mainAxisAvailableSize + afterContentPadding,
            visibleItemsInfo = positionedItems,
            totalItemsCount = itemsCount,
            reverseLayout = reverseLayout,
            orientation = if (isVertical) Orientation.Vertical else Orientation.Horizontal,
            afterContentPadding = afterContentPadding
        )
    }
}

/**
 * Calculates [LazyMeasuredLine]s offsets.
 */
@OptIn(ExperimentalFoundationApi::class)
private fun calculateItemsOffsets(
    lines: List<LazyMeasuredLine>,
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
        lines.fastForEach {
            positionedItems.addAll(it.position(currentMainAxis, layoutWidth, layoutHeight))
            currentMainAxis += it.mainAxisSizeWithSpacings
        }
    }
    return positionedItems
}
