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
import androidx.compose.foundation.lazy.layout.IntervalList
import androidx.compose.foundation.lazy.layout.calculateNearestItemsRange
import androidx.compose.foundation.lazy.layout.getDefaultLazyLayoutKey
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.structuralEqualityPolicy

@ExperimentalFoundationApi
@Composable
internal fun rememberItemProvider(
    state: LazyListState,
    content: LazyListScope.() -> Unit
): LazyListItemProvider {
    val latestContent = rememberUpdatedState(content)

    val nearestItemsRangeState = remember(state) {
        derivedStateOf(structuralEqualityPolicy()) {
            calculateNearestItemsRange(
                slidingWindowSize = NearestItemsSlidingWindowSize,
                extraItemCount = NearestItemsExtraItemCount,
                firstVisibleItem = state.firstVisibleItemIndex
            )
        }
    }

    return remember(nearestItemsRangeState) {
        LazyListItemProviderImpl(
            derivedStateOf {
                val listScope = LazyListScopeImpl().apply(latestContent.value)
                LazyListItemsSnapshot(
                    listScope.intervals,
                    listScope.headerIndexes,
                    nearestItemsRangeState.value
                )
            }
        )
    }
}

@ExperimentalFoundationApi
internal class LazyListItemsSnapshot(
    private val intervals: IntervalList<LazyListIntervalContent>,
    val headerIndexes: List<Int>,
    nearestItemsRange: IntRange
) {
    val itemsCount get() = intervals.size

    fun getKey(index: Int): Any {
        val interval = intervals[index]
        val localIntervalIndex = index - interval.startIndex
        val key = interval.value.key?.invoke(localIntervalIndex)
        return key ?: getDefaultLazyLayoutKey(index)
    }

    @Composable
    fun Item(scope: LazyItemScope, index: Int) {
        val interval = intervals[index]
        val localIntervalIndex = index - interval.startIndex
        interval.value.item.invoke(scope, localIntervalIndex)
    }

    val keyToIndexMap: Map<Any, Int> = generateKeyToIndexMap(nearestItemsRange, intervals)

    fun getContentType(index: Int): Any? {
        val interval = intervals[index]
        val localIntervalIndex = index - interval.startIndex
        return interval.value.type.invoke(localIntervalIndex)
    }
}

@ExperimentalFoundationApi
internal class LazyListItemProviderImpl(
    private val itemsSnapshot: State<LazyListItemsSnapshot>
) : LazyListItemProvider {

    override val itemScope = LazyItemScopeImpl()

    override val headerIndexes: List<Int> get() = itemsSnapshot.value.headerIndexes

     override val itemCount get() = itemsSnapshot.value.itemsCount

    override fun getKey(index: Int) = itemsSnapshot.value.getKey(index)

    @Composable
    override fun Item(index: Int) {
        itemsSnapshot.value.Item(itemScope, index)
    }

    override val keyToIndexMap: Map<Any, Int> get() = itemsSnapshot.value.keyToIndexMap

    override fun getContentType(index: Int) = itemsSnapshot.value.getContentType(index)
}

/**
 * Traverses the interval [list] in order to create a mapping from the key to the index for all
 * the indexes in the passed [range].
 * The returned map will not contain the values for intervals with no key mapping provided.
 */
@ExperimentalFoundationApi
internal fun generateKeyToIndexMap(
    range: IntRange,
    list: IntervalList<LazyListIntervalContent>
): Map<Any, Int> {
    val first = range.first
    check(first >= 0)
    val last = minOf(range.last, list.size - 1)
    return if (last < first) {
        emptyMap()
    } else {
        hashMapOf<Any, Int>().also { map ->
            list.forEach(
                fromIndex = first,
                toIndex = last,
            ) {
                if (it.value.key != null) {
                    val keyFactory = requireNotNull(it.value.key)
                    val start = maxOf(first, it.startIndex)
                    val end = minOf(last, it.startIndex + it.size - 1)
                    for (i in start..end) {
                        map[keyFactory(i - it.startIndex)] = i
                    }
                }
            }
        }
    }
}

/**
 * We use the idea of sliding window as an optimization, so user can scroll up to this number of
 * items until we have to regenerate the key to index map.
 */
private const val NearestItemsSlidingWindowSize = 30

/**
 * The minimum amount of items near the current first visible item we want to have mapping for.
 */
private const val NearestItemsExtraItemCount = 100
