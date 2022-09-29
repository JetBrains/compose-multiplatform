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

package androidx.compose.foundation.lazy.layout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State

/**
 * Provides all the needed info about the items which could be later composed and displayed as
 * children or [LazyLayout].
 */
@Stable
@ExperimentalFoundationApi
interface LazyLayoutItemProvider {

    /**
     * The total number of items in the lazy layout (visible or not).
     */
    val itemCount: Int

    /**
     * The item for the given [index].
     */
    @Composable
    fun Item(index: Int)

    /**
     * Returns the content type for the item on this index. It is used to improve the item
     * compositions reusing efficiency. Note that null is a valid type and items of such
     * type will be considered compatible.
     */
    fun getContentType(index: Int): Any? = null

    /**
     * Returns the key for the item on this index.
     *
     * @see getDefaultLazyLayoutKey which you can use if the user didn't provide a key.
     */
    fun getKey(index: Int): Any = getDefaultLazyLayoutKey(index)

    /**
     * Contains the mapping between the key and the index. It could contain not all the items of
     * the list as an optimization or be empty if user didn't provide a custom key-index mapping.
     */
    val keyToIndexMap: Map<Any, Int> get() = emptyMap()
}

/**
 * This creates an object meeting following requirements:
 * 1) Objects created for the same index are equals and never equals for different indexes.
 * 2) This class is saveable via a default SaveableStateRegistry on the platform.
 * 3) This objects can't be equals to any object which could be provided by a user as a custom key.
 */
@ExperimentalFoundationApi
@Suppress("MissingNullability")
expect fun getDefaultLazyLayoutKey(index: Int): Any

/**
 * Common content holder to back interval-based `item` DSL of lazy layouts.
 */
@ExperimentalFoundationApi
interface LazyLayoutIntervalContent {
    /**
     * Returns item key based on a local index for the current interval.
     */
    val key: ((index: Int) -> Any)? get() = null

    /**
     * Returns item type based on a local index for the current interval.
     */
    val type: ((index: Int) -> Any?) get() = { null }
}

/**
 * Default implementation of [LazyLayoutItemProvider] shared by lazy layout implementations.
 *
 * @param intervals [IntervalList] of [LazyLayoutIntervalContent] defined by lazy list DSL
 * @param nearestItemsRange range of indices considered near current viewport
 * @param itemContent composable content based on index inside provided interval
 */
@ExperimentalFoundationApi
fun <T : LazyLayoutIntervalContent> LazyLayoutItemProvider(
    intervals: IntervalList<T>,
    nearestItemsRange: IntRange,
    itemContent: @Composable (interval: T, index: Int) -> Unit,
): LazyLayoutItemProvider =
    DefaultLazyLayoutItemsProvider(itemContent, intervals, nearestItemsRange)

@ExperimentalFoundationApi
private class DefaultLazyLayoutItemsProvider<IntervalContent : LazyLayoutIntervalContent>(
    val itemContentProvider: @Composable IntervalContent.(index: Int) -> Unit,
    val intervals: IntervalList<IntervalContent>,
    nearestItemsRange: IntRange
) : LazyLayoutItemProvider {
    override val itemCount get() = intervals.size

    override val keyToIndexMap: Map<Any, Int> = generateKeyToIndexMap(nearestItemsRange, intervals)

    @Composable
    override fun Item(index: Int) {
        withLocalIntervalIndex(index) { localIndex, content ->
            content.itemContentProvider(localIndex)
        }
    }

    override fun getKey(index: Int): Any =
        withLocalIntervalIndex(index) { localIndex, content ->
            content.key?.invoke(localIndex) ?: getDefaultLazyLayoutKey(index)
        }

    override fun getContentType(index: Int): Any? =
        withLocalIntervalIndex(index) { localIndex, content ->
            content.type.invoke(localIndex)
        }

    private inline fun <T> withLocalIntervalIndex(
        index: Int,
        block: (localIndex: Int, content: IntervalContent) -> T
    ): T {
        val interval = intervals[index]
        val localIntervalIndex = index - interval.startIndex
        return block(localIntervalIndex, interval.value)
    }

    /**
     * Traverses the interval [list] in order to create a mapping from the key to the index for all
     * the indexes in the passed [range].
     * The returned map will not contain the values for intervals with no key mapping provided.
     */
    @ExperimentalFoundationApi
    private fun generateKeyToIndexMap(
        range: IntRange,
        list: IntervalList<LazyLayoutIntervalContent>
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
}

/**
 * Delegating version of [LazyLayoutItemProvider], abstracting internal [State] access.
 * This way, passing [LazyLayoutItemProvider] will not trigger recomposition unless
 * its methods are called within composable functions.
 *
 * @param delegate [State] to delegate [LazyLayoutItemProvider] functionality to.
 */
@ExperimentalFoundationApi
fun DelegatingLazyLayoutItemProvider(
    delegate: State<LazyLayoutItemProvider>
): LazyLayoutItemProvider =
    DefaultDelegatingLazyLayoutItemProvider(delegate)

@ExperimentalFoundationApi
private class DefaultDelegatingLazyLayoutItemProvider(
    private val delegate: State<LazyLayoutItemProvider>
) : LazyLayoutItemProvider {
    override val itemCount: Int get() = delegate.value.itemCount

    @Composable
    override fun Item(index: Int) {
        delegate.value.Item(index)
    }

    override val keyToIndexMap: Map<Any, Int> get() = delegate.value.keyToIndexMap

    override fun getKey(index: Int): Any = delegate.value.getKey(index)

    override fun getContentType(index: Int): Any? = delegate.value.getContentType(index)
}

/**
 * Finds a position of the item with the given key in the lists. This logic allows us to
 * detect when there were items added or removed before our current first item.
 */
@ExperimentalFoundationApi
internal fun LazyLayoutItemProvider.findIndexByKey(
    key: Any?,
    lastKnownIndex: Int,
): Int {
    if (key == null) {
        // there were no real item during the previous measure
        return lastKnownIndex
    }
    if (lastKnownIndex < itemCount &&
        key == getKey(lastKnownIndex)
    ) {
        // this item is still at the same index
        return lastKnownIndex
    }
    val newIndex = keyToIndexMap[key]
    if (newIndex != null) {
        return newIndex
    }
    // fallback to the previous index if we don't know the new index of the item
    return lastKnownIndex
}