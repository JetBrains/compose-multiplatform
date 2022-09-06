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
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * Information about layout state of individual item in lazy staggered grid.
 * @see [LazyStaggeredGridLayoutInfo]
 */
@ExperimentalFoundationApi
interface LazyStaggeredGridItemInfo {
    /**
     * Relative offset from the start of the staggered grid.
     */
    val offset: IntOffset

    /**
     * Index of the item.
     */
    val index: Int

    /**
     * Column (for vertical staggered grids) or row (for horizontal staggered grids) that the item
     * is in.
     */
    val lane: Int

    /**
     * Key of the item passed in [LazyStaggeredGridScope.items]
     */
    val key: Any

    /**
     * Item size in pixels. If item contains multiple layouts, the size is calculated as a sum of
     * their sizes.
     */
    val size: IntSize
}

/**
 * Information about layout state of lazy staggered grids.
 * Can be retrieved from [LazyStaggeredGridState.layoutInfo].
 */
// todo(b/182882362): expose more information about layout state
@ExperimentalFoundationApi
interface LazyStaggeredGridLayoutInfo {
    /**
     * The list of [LazyStaggeredGridItemInfo] per each visible item ordered by index.
     */
    val visibleItemsInfo: List<LazyStaggeredGridItemInfo>

    /**
     * The total count of items passed to staggered grid.
     */
    val totalItemsCount: Int
}

@OptIn(ExperimentalFoundationApi::class)
internal fun LazyStaggeredGridLayoutInfo.findVisibleItem(
    itemIndex: Int
): LazyStaggeredGridItemInfo? {
    if (visibleItemsInfo.isEmpty()) {
        return null
    }

    if (itemIndex !in visibleItemsInfo.first().index..visibleItemsInfo.last().index) {
        return null
    }

    val index = visibleItemsInfo.binarySearch { it.index - itemIndex }
    return visibleItemsInfo.getOrNull(index)
}

@OptIn(ExperimentalFoundationApi::class)
internal class LazyStaggeredGridMeasureResult(
    val firstVisibleItemIndices: IntArray,
    val firstVisibleItemScrollOffsets: IntArray,
    val consumedScroll: Float,
    val measureResult: MeasureResult,
    val canScrollForward: Boolean,
    val canScrollBackward: Boolean,
    override val totalItemsCount: Int,
    override val visibleItemsInfo: List<LazyStaggeredGridItemInfo>
) : LazyStaggeredGridLayoutInfo, MeasureResult by measureResult

@OptIn(ExperimentalFoundationApi::class)
internal object EmptyLazyStaggeredGridLayoutInfo : LazyStaggeredGridLayoutInfo {
    override val visibleItemsInfo: List<LazyStaggeredGridItemInfo> = emptyList()
    override val totalItemsCount: Int = 0
}