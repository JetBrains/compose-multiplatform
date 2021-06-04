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

import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach

/**
 * This method finds the sticky header in composedItems list or composes the header item if needed.
 *
 * @param composedVisibleItems list of items already composed and expected to be visible. if the
 * header wasn't in this list but is needed the header will be added as the first item in this list.
 * @param notUsedButComposedItems list of items already composed, but not going to be visible as
 * their position is not within the viewport. in some conditions the header could be in this list.
 * @param itemProvider the provider so we can compose a header if it wasn't composed already
 * @param headerIndexes list of indexes of headers. Must be sorted.
 * @param startContentPadding the padding before the first item in the list
 */
internal fun findOrComposeLazyListHeader(
    composedVisibleItems: MutableList<LazyMeasuredItem>,
    notUsedButComposedItems: List<LazyMeasuredItem>?,
    itemProvider: LazyMeasuredItemProvider,
    headerIndexes: List<Int>,
    startContentPadding: Int
): LazyMeasuredItem? {
    var alreadyVisibleHeaderItem: LazyMeasuredItem? = null
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

    composedVisibleItems.fastForEach { item ->
        if (item.index == currentHeaderListPosition) {
            alreadyVisibleHeaderItem = item
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

    val headerItem = alreadyVisibleHeaderItem
        ?: notUsedButComposedItems?.fastFirstOrNull { it.index == currentHeaderListPosition }
            ?.also {
                composedVisibleItems.add(0, it)
            }
        ?: itemProvider.getAndMeasure(DataIndex(currentHeaderListPosition)).also {
            composedVisibleItems.add(0, it)
        }

    var headerOffset = if (currentHeaderOffset != Int.MIN_VALUE) {
        maxOf(-startContentPadding, currentHeaderOffset)
    } else {
        -startContentPadding
    }
    // if we have a next header overlapping with the current header, the next one will be
    // pushing the current one away from the viewport.
    if (nextHeaderOffset != Int.MIN_VALUE) {
        headerOffset = minOf(headerOffset, nextHeaderOffset - headerItem.size)
    }

    headerItem.offset = headerOffset
    return headerItem
}
