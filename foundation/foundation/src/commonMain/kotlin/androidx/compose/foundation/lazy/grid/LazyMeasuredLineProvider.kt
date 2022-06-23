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
import androidx.compose.ui.unit.Constraints

/**
 * Abstracts away subcomposition and span calculation from the measuring logic of entire lines.
 */
@OptIn(ExperimentalFoundationApi::class)
internal class LazyMeasuredLineProvider(
    private val isVertical: Boolean,
    private val slotSizesSums: List<Int>,
    private val crossAxisSpacing: Int,
    private val gridItemsCount: Int,
    private val spaceBetweenLines: Int,
    private val measuredItemProvider: LazyMeasuredItemProvider,
    private val spanLayoutProvider: LazyGridSpanLayoutProvider,
    private val measuredLineFactory: MeasuredLineFactory
) {
    // The constraints for cross axis size. The main axis is not restricted.
    internal fun childConstraints(startSlot: Int, span: Int): Constraints {
        val lastSlotSum = slotSizesSums[startSlot + span - 1]
        val prevSlotSum = if (startSlot == 0) 0 else slotSizesSums[startSlot - 1]
        val slotsSize = lastSlotSum - prevSlotSum
        val crossAxisSize = (slotsSize + crossAxisSpacing * (span - 1)).coerceAtLeast(0)
        return if (isVertical) {
            Constraints.fixedWidth(crossAxisSize)
        } else {
            Constraints.fixedHeight(crossAxisSize)
        }
    }

    /**
     * Used to subcompose items on lines of lazy grids. Composed placeables will be measured
     * with the correct constraints and wrapped into [LazyMeasuredLine].
     */
    fun getAndMeasure(lineIndex: LineIndex): LazyMeasuredLine {
        val lineConfiguration = spanLayoutProvider.getLineConfiguration(lineIndex.value)
        val lineItemsCount = lineConfiguration.spans.size

        // we add space between lines as an extra spacing for all lines apart from the last one
        // so the lazy grid measuring logic will take it into account.
        val mainAxisSpacing = if (lineItemsCount == 0 ||
            lineConfiguration.firstItemIndex + lineItemsCount == gridItemsCount) {
            0
        } else {
            spaceBetweenLines
        }

        var startSlot = 0
        val items = Array(lineItemsCount) {
            val span = lineConfiguration.spans[it].currentLineSpan
            val constraints = childConstraints(startSlot, span)
            measuredItemProvider.getAndMeasure(
                ItemIndex(lineConfiguration.firstItemIndex + it),
                mainAxisSpacing,
                constraints
            ).also { startSlot += span }
        }
        return measuredLineFactory.createLine(
            lineIndex,
            items,
            lineConfiguration.spans,
            mainAxisSpacing
        )
    }

    /**
     * Contains the mapping between the key and the index. It could contain not all the items of
     * the list as an optimization.
     **/
    val keyToIndexMap: Map<Any, Int> get() = measuredItemProvider.keyToIndexMap
}

// This interface allows to avoid autoboxing on index param
@OptIn(ExperimentalFoundationApi::class)
internal fun interface MeasuredLineFactory {
    fun createLine(
        index: LineIndex,
        items: Array<LazyMeasuredItem>,
        spans: List<GridItemSpan>,
        mainAxisSpacing: Int
    ): LazyMeasuredLine
}
