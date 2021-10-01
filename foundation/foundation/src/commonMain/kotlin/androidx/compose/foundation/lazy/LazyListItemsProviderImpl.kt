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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.node.Ref
import kotlinx.coroutines.flow.collect

@Composable
internal fun rememberStateOfItemsProvider(
    state: LazyListState,
    content: LazyListScope.() -> Unit,
    itemScope: Ref<LazyItemScopeImpl>
): State<LazyListItemsProvider> {
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
        derivedStateOf<LazyListItemsProvider> {
            val listScope = LazyListScopeImpl().apply(latestContent.value)
            LazyListItemsProviderImpl(
                itemScope,
                listScope.intervals,
                listScope.headerIndexes,
                nearestItemsRangeState.value
            )
        }
    }
}

internal class LazyListItemsProviderImpl(
    private val itemScope: Ref<LazyItemScopeImpl>,
    private val list: IntervalList<LazyListIntervalContent>,
    override val headerIndexes: List<Int>,
    nearestItemsRange: IntRange
) : LazyListItemsProvider {
    override val itemsCount get() = list.totalSize

    override fun getKey(index: Int): Any {
        val interval = list.intervalForIndex(index)
        val localIntervalIndex = index - interval.startIndex
        val key = interval.content.key?.invoke(localIntervalIndex)
        return key ?: getDefaultLazyKeyFor(index)
    }

    override fun getContent(index: Int): @Composable () -> Unit {
        val interval = list.intervalForIndex(index)
        val localIntervalIndex = index - interval.startIndex
        return interval.content.content.invoke(itemScope.value!!, localIntervalIndex)
    }

    override val keyToIndexMap: Map<Any, Int> = generateKeyToIndexMap(nearestItemsRange, list)
}

/**
 * This should create an object meeting following requirements:
 * 1) objects created for the same index are equals and never equals for different indexes
 * 2) this class is saveable via a default SaveableStateRegistry on the platform
 * 3) this objects can't be equals to any object which could be provided by a user as a custom key
 */
internal expect fun getDefaultLazyKeyFor(index: Int): Any

/**
 * Traverses the interval [list] in order to create a mapping from the key to the index for all
 * the indexes in the passed [range].
 * The returned map will not contain the values for intervals with no key mapping provided.
 */
private fun generateKeyToIndexMap(
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
