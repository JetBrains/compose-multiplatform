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
import androidx.compose.foundation.lazy.layout.IntervalHolder
import androidx.compose.foundation.lazy.layout.IntervalList
import androidx.compose.foundation.lazy.layout.getDefaultLazyLayoutKey
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

@ExperimentalFoundationApi
@Composable
internal fun rememberItemsProvider(
    state: LazyGridState,
    content: LazyGridScope.() -> Unit,
    itemScope: LazyGridItemScope
): LazyGridItemsProvider {
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
        LazyGridItemsProviderImpl(
            derivedStateOf {
                val listScope = LazyGridScopeImpl().apply(latestContent.value)
                LazyGridItemsSnapshot(
                    itemScope,
                    listScope.intervals,
                    listScope.hasCustomSpans,
                    nearestItemsRangeState.value
                )
            }
        )
    }
}

@ExperimentalFoundationApi
internal class LazyGridItemsSnapshot(
    private val itemScope: LazyGridItemScope,
    private val intervals: IntervalList<LazyGridIntervalContent>,
    val hasCustomSpans: Boolean,
    nearestItemsRange: IntRange
) {
    /**
     * Caches the last interval we binary searched for. We might not need to recalculate
     * for subsequent queries, as they tend to be localised.
     */
    private var lastInterval: IntervalHolder<LazyGridIntervalContent>? = null

    val itemsCount get() = intervals.totalSize

    fun getKey(index: Int): Any {
        val interval = getIntervalForIndex(index)
        val localIntervalIndex = index - interval.startIndex
        val key = interval.content.key?.invoke(localIntervalIndex)
        return key ?: getDefaultLazyLayoutKey(index)
    }

    fun LazyGridItemSpanScope.getSpan(index: Int): GridItemSpan {
        val interval = getIntervalForIndex(index)
        val localIntervalIndex = index - interval.startIndex
        return interval.content.span.invoke(this, localIntervalIndex)
    }

    fun getContent(index: Int): @Composable () -> Unit {
        val interval = getIntervalForIndex(index)
        val localIntervalIndex = index - interval.startIndex
        return interval.content.content.invoke(itemScope, localIntervalIndex)
    }

    val keyToIndexMap: Map<Any, Int> = generateKeyToIndexMap(nearestItemsRange, intervals)

    fun getContentType(index: Int): Any? {
        val interval = getIntervalForIndex(index)
        val localIntervalIndex = index - interval.startIndex
        return interval.content.type.invoke(localIntervalIndex)
    }

    private fun getIntervalForIndex(itemIndex: Int) = lastInterval.let {
        if (it != null && itemIndex in it.startIndex until it.startIndex + it.size) {
            it
        } else {
            intervals.intervalForIndex(itemIndex).also { lastInterval = it }
        }
    }
}

@ExperimentalFoundationApi
internal class LazyGridItemsProviderImpl(
    private val itemsSnapshot: State<LazyGridItemsSnapshot>
) : LazyGridItemsProvider {

    override val itemsCount get() = itemsSnapshot.value.itemsCount

    override fun getKey(index: Int) = itemsSnapshot.value.getKey(index)

    override fun LazyGridItemSpanScope.getSpan(index: Int): GridItemSpan =
        with(itemsSnapshot.value) { getSpan(index) }

    override val hasCustomSpans: Boolean get() = itemsSnapshot.value.hasCustomSpans

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
    list: IntervalList<LazyGridIntervalContent>
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
private val VisibleItemsSlidingWindowSize = 90

/**
 * The minimum amount of items near the current first visible item we want to have mapping for.
 */
private val ExtraItemsNearTheSlidingWindow = 200
