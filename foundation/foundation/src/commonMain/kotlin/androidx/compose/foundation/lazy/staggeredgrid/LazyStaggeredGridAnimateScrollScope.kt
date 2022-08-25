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

package androidx.compose.foundation.lazy.staggeredgrid

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.lazy.layout.LazyAnimateScrollScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.fastSumBy

@ExperimentalFoundationApi
internal class LazyStaggeredGridAnimateScrollScope(
    private val state: LazyStaggeredGridState
) : LazyAnimateScrollScope {
    override val density: Density get() = state.density

    override val firstVisibleItemIndex: Int get() = state.firstVisibleItemIndex

    override val firstVisibleItemScrollOffset: Int get() = state.firstVisibleItemScrollOffset

    override val lastVisibleItemIndex: Int
        get() = state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

    override val itemCount: Int get() = state.layoutInfo.totalItemsCount

    override fun getTargetItemOffset(index: Int): Int? =
        state.layoutInfo.findVisibleItem(index)?.offset?.let {
            if (state.isVertical) it.y else it.x
        }

    override fun ScrollScope.snapToItem(index: Int, scrollOffset: Int) {
        with(state) {
            snapToItemInternal(index, scrollOffset)
        }
    }

    override fun expectedDistanceTo(index: Int, targetScrollOffset: Int): Float {
        val visibleItems = state.layoutInfo.visibleItemsInfo
        val itemSizeSum = visibleItems
            .fastSumBy { if (state.isVertical) it.size.height else it.size.width }
        val averageMainAxisItemSize = itemSizeSum / (visibleItems.size * state.laneCount)

        val indexesDiff = index - firstVisibleItemIndex
        return (averageMainAxisItemSize * indexesDiff).toFloat() +
            targetScrollOffset - firstVisibleItemScrollOffset
    }

    override val numOfItemsForTeleport: Int get() = 100 * state.laneCount

    override suspend fun scroll(block: suspend ScrollScope.() -> Unit) {
        state.scroll(block = block)
    }
}