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

package androidx.compose.foundation.lazy

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.DelegatingLazyLayoutItemProvider
import androidx.compose.foundation.lazy.layout.IntervalList
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.foundation.lazy.layout.LazyLayoutPinnableItem
import androidx.compose.foundation.lazy.layout.rememberLazyNearestItemsRangeState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

@ExperimentalFoundationApi
internal interface LazyListItemProvider : LazyLayoutItemProvider {
    /** The list of indexes of the sticky header items */
    val headerIndexes: List<Int>
    /** The scope used by the item content lambdas */
    val itemScope: LazyItemScopeImpl
}

@ExperimentalFoundationApi
@Composable
internal fun rememberLazyListItemProvider(
    state: LazyListState,
    content: LazyListScope.() -> Unit
): LazyListItemProvider {
    val latestContent = rememberUpdatedState(content)
    val nearestItemsRangeState = rememberLazyNearestItemsRangeState(
        firstVisibleItemIndex = { state.firstVisibleItemIndex },
        slidingWindowSize = { NearestItemsSlidingWindowSize },
        extraItemCount = { NearestItemsExtraItemCount }
    )

    return remember(nearestItemsRangeState, state) {
        val itemScope = LazyItemScopeImpl()
        val itemProviderState = derivedStateOf {
            val listScope = LazyListScopeImpl().apply(latestContent.value)
            LazyListItemProviderImpl(
                listScope.intervals,
                nearestItemsRangeState.value,
                listScope.headerIndexes,
                itemScope,
                state
            )
        }
        object : LazyListItemProvider,
            LazyLayoutItemProvider by DelegatingLazyLayoutItemProvider(itemProviderState) {
            override val headerIndexes: List<Int> get() = itemProviderState.value.headerIndexes
            override val itemScope: LazyItemScopeImpl get() = itemProviderState.value.itemScope
        }
    }
}

@ExperimentalFoundationApi
private class LazyListItemProviderImpl(
    intervals: IntervalList<LazyListIntervalContent>,
    nearestItemsRange: IntRange,
    override val headerIndexes: List<Int>,
    override val itemScope: LazyItemScopeImpl,
    state: LazyListState
) : LazyListItemProvider,
    LazyLayoutItemProvider by LazyLayoutItemProvider(
        intervals = intervals,
        nearestItemsRange = nearestItemsRange,
        itemContent = { interval, index ->
            val localIndex = index - interval.startIndex
            LazyLayoutPinnableItem(
                key = interval.value.key?.invoke(localIndex),
                index = index,
                pinnedItemList = state.pinnedItems
            ) {
                interval.value.item.invoke(itemScope, localIndex)
            }
        }
    )

/**
 * We use the idea of sliding window as an optimization, so user can scroll up to this number of
 * items until we have to regenerate the key to index map.
 */
private const val NearestItemsSlidingWindowSize = 30

/**
 * The minimum amount of items near the current first visible item we want to have mapping for.
 */
private const val NearestItemsExtraItemCount = 100