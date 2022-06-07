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
import androidx.compose.foundation.lazy.layout.IntervalList
import androidx.compose.foundation.lazy.layout.getDefaultLazyLayoutKey
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot

@ExperimentalFoundationApi
@Composable
internal fun rememberItemProvider(
    state: LazyGridState,
    content: LazyGridScope.() -> Unit,
): LazyGridItemProvider {
    val latestContent = rememberUpdatedState(content)
    // mutableState + LaunchedEffect below are used instead of derivedStateOf to ensure that update
    // of derivedState in return expr will only happen after the state value has been changed.
    val nearestItemsRangeState = remember(state) {
        mutableStateOf(
            Snapshot.withoutReadObservation {
                // State read is observed in composition, causing it to recompose 1 additional time.
                calculateNearestItemsRange(state.firstVisibleItemIndex)
            }
        )
    }
    LaunchedEffect(nearestItemsRangeState) {
        snapshotFlow { calculateNearestItemsRange(state.firstVisibleItemIndex) }
            // MutableState's SnapshotMutationPolicy will make sure the provider is only
            // recreated when the state is updated with a new range.
            .collect { nearestItemsRangeState.value = it }
    }

    return remember(nearestItemsRangeState) {
        LazyGridItemProviderImpl(
            derivedStateOf {
                val listScope = LazyGridScopeImpl().apply(latestContent.value)
                LazyGridItemsSnapshot(
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
    private val intervals: IntervalList<LazyGridIntervalContent>,
    val hasCustomSpans: Boolean,
    nearestItemsRange: IntRange
) {
    val itemsCount get() = intervals.size

    val spanLayoutProvider = LazyGridSpanLayoutProvider(this)

    fun getKey(index: Int): Any {
        val interval = intervals[index]
        val localIntervalIndex = index - interval.startIndex
        val key = interval.value.key?.invoke(localIntervalIndex)
        return key ?: getDefaultLazyLayoutKey(index)
    }

    fun LazyGridItemSpanScope.getSpan(index: Int): GridItemSpan {
        val interval = intervals[index]
        val localIntervalIndex = index - interval.startIndex
        return interval.value.span.invoke(this, localIntervalIndex)
    }

    @Composable
    fun Item(index: Int) {
        val interval = intervals[index]
        val localIntervalIndex = index - interval.startIndex
        interval.value.item.invoke(LazyGridItemScopeImpl, localIntervalIndex)
    }

    val keyToIndexMap: Map<Any, Int> = generateKeyToIndexMap(nearestItemsRange, intervals)

    fun getContentType(index: Int): Any? {
        val interval = intervals[index]
        val localIntervalIndex = index - interval.startIndex
        return interval.value.type.invoke(localIntervalIndex)
    }
}

@ExperimentalFoundationApi
internal class LazyGridItemProviderImpl(
    private val itemsSnapshot: State<LazyGridItemsSnapshot>
) : LazyGridItemProvider {
    override val itemCount get() = itemsSnapshot.value.itemsCount

    override fun getKey(index: Int) = itemsSnapshot.value.getKey(index)

    @Composable
    override fun Item(index: Int) {
        itemsSnapshot.value.Item(index)
    }

    override val keyToIndexMap: Map<Any, Int> get() = itemsSnapshot.value.keyToIndexMap

    override fun getContentType(index: Int) = itemsSnapshot.value.getContentType(index)

    override val spanLayoutProvider: LazyGridSpanLayoutProvider
        get() = itemsSnapshot.value.spanLayoutProvider
}

/**
 * Traverses the interval [list] in order to create a mapping from the key to the index for all
 * the indexes in the passed [range].
 * The returned map will not contain the values for intervals with no key mapping provided.
 */
@ExperimentalFoundationApi
internal fun generateKeyToIndexMap(
    range: IntRange,
    list: IntervalList<LazyGridIntervalContent>
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
