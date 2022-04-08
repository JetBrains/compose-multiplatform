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

import androidx.compose.ui.util.fastForEachIndexed

/**
 * This method finds the sticky header in composedItems list or composes the header item if needed.
 *
 * @param composedVisibleItems list of items already composed and expected to be visible. if the
 * header wasn't in this list but is needed the header will be added as the first item in this list.
 * @param itemProvider the provider so we can compose a header if it wasn't composed already
 * @param headerIndexes list of indexes of headers. Must be sorted.
 * @param beforeContentPadding the padding before the first item in the list
 */
internal fun findOrComposeLazyListHeader(
    composedVisibleItems: MutableList<LazyListPositionedItem>,
    itemProvider: LazyMeasuredItemProvider,
    headerIndexes: List<Int>,
    beforeContentPadding: Int,
    layoutWidth: Int,
    layoutHeight: Int,
): LazyListPositionedItem? {
    var currentHeaderOffset: Int = Int.MIN_VALUE
    var nextHeaderOffset: Int = Int.MIN_VALUE

    var currentHeaderListPosition = -1
    var nextHeaderListPosition = -1
    // we use visibleItemsInfo and not firstVisibleItemIndex as visibleItemsInfo list also
    // contains all the items which are visible in the start content padding area
    val firstVisible = composedVisibleItems.first().index
    // find the header which can be displayed
    for (index in headerIndexes.indices) {
        if (headerIndexes[index] <= firstVisible) {
            currentHeaderListPosition = headerIndexes[index]
            nextHeaderListPosition = headerIndexes.getOrElse(index + 1) { -1 }
        } else {
            break
        }
    }

    var indexInComposedVisibleItems = -1
    composedVisibleItems.fastForEachIndexed { index, item ->
        if (item.index == currentHeaderListPosition) {
            indexInComposedVisibleItems = index
            currentHeaderOffset = item.offset
        } else {
            if (item.index == nextHeaderListPosition) {
                nextHeaderOffset = item.offset
            }
        }
    }

    if (currentHeaderListPosition == -1) {
        // we have no headers needing special handling
        return null
    }

    val measuredHeaderItem = itemProvider.getAndMeasure(DataIndex(currentHeaderListPosition))

    var headerOffset = if (currentHeaderOffset != Int.MIN_VALUE) {
        maxOf(-beforeContentPadding, currentHeaderOffset)
    } else {
        -beforeContentPadding
    }
    // if we have a next header overlapping with the current header, the next one will be
    // pushing the current one away from the viewport.
    if (nextHeaderOffset != Int.MIN_VALUE) {
        headerOffset = minOf(headerOffset, nextHeaderOffset - measuredHeaderItem.size)
    }

    return measuredHeaderItem.position(headerOffset, layoutWidth, layoutHeight).also {
        if (indexInComposedVisibleItems != -1) {
            composedVisibleItems[indexInComposedVisibleItems] = it
        } else {
            composedVisibleItems.add(0, it)
        }
    }
}
