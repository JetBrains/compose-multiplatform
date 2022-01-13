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
import androidx.compose.foundation.lazy.GridItemSpan
import androidx.compose.foundation.lazy.layout.LazyLayoutPlaceable
import androidx.compose.foundation.lazy.layout.LazyLayoutPlaceablesProvider
import androidx.compose.ui.unit.Constraints
import kotlin.math.max

/**
 * Abstracts away the subcomposition from the measuring logic.
 */
@OptIn(ExperimentalFoundationApi::class)
internal class LazyMeasuredLineProvider(
    constraints: Constraints,
    private val isVertical: Boolean,
    slotsPerLine: Int,
    crossAxisSpacing: Int,
    private val itemsProvider: LazyGridItemsProvider,
    private val spanLayoutProvider: LazyGridSpanLayoutProvider,
    private val placeablesProvider: LazyLayoutPlaceablesProvider,
    private val measuredLineFactory: MeasuredLineFactory
) {
    private val crossAxisSize = if (isVertical) constraints.maxWidth else constraints.maxHeight
    private val availableCrossAxis = max(crossAxisSize - (slotsPerLine - 1) * crossAxisSpacing, 0)
    private val slotSize = availableCrossAxis / slotsPerLine
    private val slotsWithRemainder = availableCrossAxis % slotsPerLine

    // the constraints we will measure child with. the main axis is not restricted
    internal val childConstraints: (startSlot: Int, span: Int) -> Constraints = { startSlot, span ->
        val crossAxisSize = slotSize * span + crossAxisSpacing * (span - 1) +
            (slotsWithRemainder - startSlot).coerceIn(0, span)
        if (isVertical) {
            Constraints.fixedWidth(crossAxisSize)
        } else {
            Constraints.fixedHeight(crossAxisSize)
        }
    }

    /**
     * Used to subcompose items of lazy lists. Composed placeables will be measured with the
     * correct constraints and wrapped into [LazyMeasuredLine].
     */
    fun getAndMeasure(lineIndex: LineIndex): LazyMeasuredLine {
        val lineConfiguration = spanLayoutProvider.getLineConfiguration(lineIndex.value)
        val lineItemsCount = lineConfiguration.spans.size
        val keys = Array(lineItemsCount) {
            itemsProvider.getKey(lineConfiguration.firstItemIndex + it)
        }
        var startSlot = 0
        val crossAxisSizes = Array(lineItemsCount) { 0 }
        val placeables = Array<Array<LazyLayoutPlaceable>>(lineItemsCount) { emptyArray() }
        for (i in 0 until lineItemsCount) {
            val span = lineConfiguration.spans[i].currentLineSpan
            val constraints = childConstraints(startSlot, span)
            crossAxisSizes[i] = if (isVertical) constraints.minWidth else constraints.minHeight
            placeables[i] = placeablesProvider.getAndMeasure(
                lineConfiguration.firstItemIndex + i,
                constraints
            )
            startSlot += span
        }
        return measuredLineFactory.createLine(
            lineIndex,
            ItemIndex(lineConfiguration.firstItemIndex),
            lineConfiguration.spans,
            keys,
            crossAxisSizes,
            placeables
        )
    }

    /**
     * Contains the mapping between the key and the index. It could contain not all the items of
     * the list as an optimization.
     **/
    val keyToIndexMap: Map<Any, Int> get() = itemsProvider.keyToIndexMap
}

// This interface allows to avoid autoboxing on index param
@OptIn(ExperimentalFoundationApi::class)
internal fun interface MeasuredLineFactory {
    fun createLine(
        index: LineIndex,
        firstItemIndex: ItemIndex,
        spans: List<GridItemSpan>,
        keys: Array<Any>,
        crossAxisSizes: Array<Int>,
        placeables: Array<Array<LazyLayoutPlaceable>>
    ): LazyMeasuredLine
}
