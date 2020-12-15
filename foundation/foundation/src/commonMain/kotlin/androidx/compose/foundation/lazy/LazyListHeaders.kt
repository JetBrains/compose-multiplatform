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

import androidx.compose.ui.layout.Placeable

/**
 * @param itemProvider the provider so we can compose a header if it wasn't composed already
 * @param headerIndexes list of indexes of headers. Must be sorted.
 * @param measureResult the result of the measuring.
 */
internal class LazyListHeaders(
    private val itemProvider: LazyMeasuredItemProvider,
    headerIndexes: List<Int>,
    measureResult: LazyListMeasureResult,
    private val startContentPadding: Int
) {
    private val currentHeaderListPosition: Int
    private val nextHeaderListPosition: Int

    private val notUsedButComposedItems: MutableList<LazyMeasuredItem>?

    private var currentHeaderItem: LazyMeasuredItem? = null
    private var currentHeaderOffset: Int = Int.MIN_VALUE
    private var nextHeaderOffset: Int = Int.MIN_VALUE
    private var nextHeaderSize: Int = Int.MIN_VALUE

    init {
        var currentHeaderListPosition = -1
        var nextHeaderListPosition = -1
        // we use visibleItemsInfo and not firstVisibleItemIndex as visibleItemsInfo list also
        // contains all the items which are visible in the start content padding area
        val firstVisible = measureResult.visibleItemsInfo.first().index
        // find the header which can be displayed
        for (index in headerIndexes.indices) {
            if (headerIndexes[index] <= firstVisible) {
                currentHeaderListPosition = headerIndexes[index]
                nextHeaderListPosition = headerIndexes.getOrElse(index + 1) { -1 }
            } else {
                break
            }
        }
        this.currentHeaderListPosition = currentHeaderListPosition
        this.nextHeaderListPosition = nextHeaderListPosition

        notUsedButComposedItems = measureResult.notUsedButComposedItems
    }

    fun onBeforeItemsPlacing() {
        currentHeaderItem = null
        currentHeaderOffset = Int.MIN_VALUE
        nextHeaderOffset = Int.MIN_VALUE
    }

    fun place(
        item: LazyMeasuredItem,
        scope: Placeable.PlacementScope,
        layoutWidth: Int,
        layoutHeight: Int,
        offset: Int,
        reverseOrder: Boolean
    ) {
        if (item.index == currentHeaderListPosition) {
            currentHeaderItem = item
            currentHeaderOffset = offset
        } else {
            item.place(scope, layoutWidth, layoutHeight, offset, reverseOrder)
            if (item.index == nextHeaderListPosition) {
                nextHeaderOffset = offset
                nextHeaderSize = item.size
            }
        }
    }

    fun onAfterItemsPlacing(
        scope: Placeable.PlacementScope,
        mainAxisLayoutSize: Int,
        layoutWidth: Int,
        layoutHeight: Int,
        reverseOrder: Boolean
    ) {
        if (currentHeaderListPosition == -1) {
            // we have no headers needing special handling
            return
        }

        val headerItem = currentHeaderItem
            ?: notUsedButComposedItems?.find { it.index == currentHeaderListPosition }
            ?: itemProvider.getAndMeasure(DataIndex(currentHeaderListPosition))

        var headerOffset = if (!reverseOrder) {
            if (currentHeaderOffset != Int.MIN_VALUE) {
                maxOf(-startContentPadding, currentHeaderOffset)
            } else {
                -startContentPadding
            }
        } else {
            if (currentHeaderOffset != Int.MIN_VALUE) {
                minOf(
                    mainAxisLayoutSize + startContentPadding - headerItem.size,
                    currentHeaderOffset
                )
            } else {
                mainAxisLayoutSize + startContentPadding - headerItem.size
            }
        }
        // if we have a next header overlapping with the current header, the next one will be
        // pushing the current one away from the viewport.
        if (nextHeaderOffset != Int.MIN_VALUE) {
            if (!reverseOrder) {
                headerOffset = minOf(headerOffset, nextHeaderOffset - headerItem.size)
            } else {
                headerOffset = maxOf(headerOffset, nextHeaderOffset + headerItem.size)
            }
        }

        headerItem.place(scope, layoutWidth, layoutHeight, headerOffset, reverseOrder)
    }
}
