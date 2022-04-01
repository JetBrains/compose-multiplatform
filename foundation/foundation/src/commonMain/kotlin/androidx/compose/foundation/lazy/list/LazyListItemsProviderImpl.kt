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

package androidx.compose.foundation.lazy.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.getDefaultLazyKeyFor
import androidx.compose.foundation.lazy.layout.IntervalHolder
import androidx.compose.foundation.lazy.layout.IntervalList
import androidx.compose.foundation.lazy.layout.intervalForIndex
import androidx.compose.foundation.lazy.layout.intervalIndexForItemIndex
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.node.Ref

@ExperimentalFoundationApi
@Composable
internal fun rememberItemsProvider(
    state: LazyListState,
    content: LazyListScope.() -> Unit,
    itemScope: Ref<LazyItemScopeImpl>
): LazyListItemsProvider {
    val latestContent = rememberUpdatedState(content)
    val nearestItemsRangeState = remember(state) {
        mutableStateOf(
            calculateNearestItemsRange(state.firstVisibleItemIndexNonObservable.value)
        )
    }
    LaunchedEffect(nearestItemsRangeState) {
        snapshotFlow { calculateNearestItemsRange(state.firstVisibleItemIndex) }
            // MutableState's SnapshotMutationPolicy will make sure the provider is only
            // recreated when the state is updated with a new range.
            .collect { nearestItemsRangeState.value = it }
    }
    return remember(nearestItemsRangeState) {
        LazyListItemsProviderImpl(
            derivedStateOf {
                val listScope = LazyListScopeImpl().apply(latestContent.value)
                LazyListItemsSnapshot(
                    itemScope,
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
    private val itemScope: Ref<LazyItemScopeImpl>,
    private val list: IntervalList<LazyListIntervalContent>,
    val headerIndexes: List<Int>,
    nearestItemsRange: IntRange
) {
    /**
     * Caches the last interval we binary searched for. We might not need to recalculate
     * for subsequent queries, as they tend to be localised.
     */
    private var lastInterval: IntervalHolder<LazyListIntervalContent>? = null

    val itemsCount get() = list.totalSize

    private fun getIntervalForIndex(itemIndex: Int) = lastInterval.let {
        if (it != null && itemIndex in it.startIndex until it.startIndex + it.size) {
            it
        } else {
            list.intervalForIndex(itemIndex).also { lastInterval = it }
        }
    }

    fun getKey(index: Int): Any {
        val interval = getIntervalForIndex(index)
        val localIntervalIndex = index - interval.startIndex
        val key = interval.content.key?.invoke(localIntervalIndex)
        return key ?: getDefaultLazyKeyFor(index)
    }

    fun getContent(index: Int): @Composable () -> Unit {
        val interval = getIntervalForIndex(index)
        val localIntervalIndex = index - interval.startIndex
        return interval.content.content.invoke(itemScope.value!!, localIntervalIndex)
    }

    val keyToIndexMap: Map<Any, Int> = generateKeyToIndexMap(nearestItemsRange, list)

    fun getContentType(index: Int): Any? {
        val interval = getIntervalForIndex(index)
        val localIntervalIndex = index - interval.startIndex
        return interval.content.type.invoke(localIntervalIndex)
    }
}

@ExperimentalFoundationApi
internal class LazyListItemsProviderImpl(
    private val itemsSnapshot: State<LazyListItemsSnapshot>
) : LazyListItemsProvider {

    override val headerIndexes: List<Int> get() = itemsSnapshot.value.headerIndexes

    override val itemsCount get() = itemsSnapshot.value.itemsCount

    override fun getKey(index: Int) = itemsSnapshot.value.getKey(index)

    override fun getContent(index: Int) = itemsSnapshot.value.getContent(index)

    override val keyToIndexMap: Map<Any, Int> get() = itemsSnapshot.value.keyToIndexMap

    override fun getContentType(index: Int) = itemsSnapshot.value.getContentType(index)
}

/**
 * Traverses the interval [list] in order to create a mapping from the key to the index for all
 * the indexes in the passed [range].
 * The returned map will not contain the values for intervals with no key mapping provided.
 */
internal fun generateKeyToIndexMap(
    range: IntRange,
    list: IntervalList<LazyListIntervalContent>
): Map<Any, Int> {
    val first = range.first
    check(first >= 0)
    val last = minOf(range.last, list.totalSize - 1)
    return if (last < first) {
        emptyMap()
    } else {
        hashMapOf<Any, Int>().also { map ->
            var intervalIndex = list.intervalIndexForItemIndex(first)
            var itemIndex = first
            while (itemIndex <= last) {
                val interval = list.intervals[intervalIndex]
                val keyFactory = interval.content.key
                if (keyFactory != null) {
                    val localItemIndex = itemIndex - interval.startIndex
                    if (localItemIndex == interval.size) {
                        intervalIndex++
                    } else {
                        map[keyFactory(localItemIndex)] = itemIndex
                        itemIndex++
                    }
                } else {
                    intervalIndex++
                    itemIndex = interval.startIndex + interval.size
                }
            }
        }
    }
}

/**
 * Returns a range of indexes which contains at least [ExtraItemsNearTheSlidingWindow] items near
 * the first visible item. It is optimized to return the same range for small changes in the
 * firstVisibleItem value so we do not regenerate the map on each scroll.
 */
private fun calculateNearestItemsRange(firstVisibleItem: Int): IntRange {
    val slidingWindowStart = VisibleItemsSlidingWindowSize *
        (firstVisibleItem / VisibleItemsSlidingWindowSize)

    val start = maxOf(slidingWindowStart - ExtraItemsNearTheSlidingWindow, 0)
    val end = slidingWindowStart + VisibleItemsSlidingWindowSize + ExtraItemsNearTheSlidingWindow
    return start until end
}

/**
 * We use the idea of sliding window as an optimization, so user can scroll up to this number of
 * items until we have to regenerate the key to index map.
 */
private val VisibleItemsSlidingWindowSize = 30

/**
 * The minimum amount of items near the current first visible item we want to have mapping for.
 */
private val ExtraItemsNearTheSlidingWindow = 100
