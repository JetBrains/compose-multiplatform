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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.InternalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Receiver scope which is used by [LazyColumn] and [LazyRow].
 */
interface LazyListScope {
    /**
     * Adds a list of items and their content to the scope.
     *
     * @param items the data list
     * @param itemContent the content displayed by a single item
     */
    fun <T> items(
        items: List<T>,
        itemContent: @Composable LazyItemScope.(item: T) -> Unit
    )

    /**
     * Adds a single item to the scope.
     *
     * @param content the content of the item
     */
    fun item(content: @Composable LazyItemScope.() -> Unit)

    /**
     * Adds a list of items to the scope where the content of an item is aware of its index.
     *
     * @param items the data list
     * @param itemContent the content displayed by a single item
     */
    fun <T> itemsIndexed(
        items: List<T>,
        itemContent: @Composable LazyItemScope.(index: Int, item: T) -> Unit
    )
}

internal class IntervalHolder(
    val startIndex: Int,
    val content: LazyItemScope.(Int) -> (@Composable () -> Unit)
)

internal class LazyListScopeImpl : LazyListScope {
    private val intervals = mutableListOf<IntervalHolder>()
    var totalSize = 0

    fun contentFor(index: Int, scope: LazyItemScope): @Composable () -> Unit {
        val intervalIndex = findIndexOfHighestValueLesserThan(intervals, index)

        val interval = intervals[intervalIndex]
        val localIntervalIndex = index - interval.startIndex

        return interval.content(scope, localIntervalIndex)
    }

    override fun <T> items(
        items: List<T>,
        itemContent: @Composable LazyItemScope.(item: T) -> Unit
    ) {
        // There aren't any items to display
        if (items.isEmpty()) { return }

        val interval = IntervalHolder(
            startIndex = totalSize,
            content = { index ->
                val item = items[index]

                { itemContent(item) }
            }
        )

        totalSize += items.size

        intervals.add(interval)
    }

    override fun item(content: @Composable LazyItemScope.() -> Unit) {
        val interval = IntervalHolder(
            startIndex = totalSize,
            content = { { content() } }
        )

        totalSize += 1

        intervals.add(interval)
    }

    override fun <T> itemsIndexed(
        items: List<T>,
        itemContent: @Composable LazyItemScope.(index: Int, item: T) -> Unit
    ) {
        // There aren't any items to display
        if (items.isEmpty()) { return }

        val interval = IntervalHolder(
            startIndex = totalSize,
            content = { index ->
                val item = items[index]

                { itemContent(index, item) }
            }
        )

        totalSize += items.size

        intervals.add(interval)
    }

    /**
     * Finds the index of the [list] which contains the highest value of [IntervalHolder.startIndex]
     * that is less than or equal to the given [value].
     */
    private fun findIndexOfHighestValueLesserThan(list: List<IntervalHolder>, value: Int): Int {
        var left = 0
        var right = list.lastIndex

        while (left < right) {
            val middle = (left + right) / 2

            val middleValue = list[middle].startIndex
            if (middleValue == value) {
                return middle
            }

            if (middleValue < value) {
                left = middle + 1

                // Verify that the left will not be bigger than our value
                if (value < list[left].startIndex) {
                    return middle
                }
            } else {
                right = middle - 1
            }
        }

        return left
    }
}

/**
 * The horizontally scrolling list that only composes and lays out the currently visible items.
 * The [content] block defines a DSL which allows you to emit items of different types. For
 * example you can use [LazyListScope.item] to add a single item and [LazyListScope.items] to add
 * a list of items.
 *
 * @sample androidx.compose.foundation.samples.LazyRowSample
 *
 * @param modifier the modifier to apply to this layout
 * @param state the state object to be used to control or observe the list's state
 * @param contentPadding a padding around the whole content. This will add padding for the
 * content after it has been clipped, which is not possible via [modifier] param. You can use it
 * to add a padding before the first item or after the last one. If you want to add a spacing
 * between each item use [horizontalArrangement].
 * @param reverseLayout reverse the direction of scrolling and layout, when `true` items will be
 * composed from the end to the start and [LazyListState.firstVisibleItemIndex] == 0 will mean
 * the first item is located at the end.
 * @param horizontalArrangement The horizontal arrangement of the layout's children. This allows
 * to add a spacing between items and specify the arrangement of the items when we have not enough
 * of them to fill the whole minimum size.
 * @param verticalAlignment the vertical alignment applied to the items
 * @param content a block which describes the content. Inside this block you can use methods like
 * [LazyListScope.item] to add a single item or [LazyListScope.items] to add a list of items.
 */
@OptIn(InternalLayoutApi::class)
@Composable
fun LazyRow(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    horizontalArrangement: Arrangement.Horizontal =
        if (!reverseLayout) Arrangement.Start else Arrangement.End,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: LazyListScope.() -> Unit
) {
    val scope = LazyListScopeImpl()
    scope.apply(content)

    LazyList(
        itemsCount = scope.totalSize,
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        verticalAlignment = verticalAlignment,
        horizontalArrangement = horizontalArrangement,
        isVertical = false,
        reverseLayout = reverseLayout
    ) { index ->
        scope.contentFor(index, this)
    }
}

/**
 * The vertically scrolling list that only composes and lays out the currently visible items.
 * The [content] block defines a DSL which allows you to emit items of different types. For
 * example you can use [LazyListScope.item] to add a single item and [LazyListScope.items] to add
 * a list of items.
 *
 * @sample androidx.compose.foundation.samples.LazyColumnSample
 *
 * @param modifier the modifier to apply to this layout.
 * @param state the state object to be used to control or observe the list's state.
 * @param contentPadding a padding around the whole content. This will add padding for the.
 * content after it has been clipped, which is not possible via [modifier] param. You can use it
 * to add a padding before the first item or after the last one. If you want to add a spacing
 * between each item use [verticalArrangement].
 * @param reverseLayout reverse the direction of scrolling and layout, when `true` items will be
 * composed from the bottom to the top and [LazyListState.firstVisibleItemIndex] == 0 will mean
 * we scrolled to the bottom.
 * @param verticalArrangement The vertical arrangement of the layout's children. This allows
 * to add a spacing between items and specify the arrangement of the items when we have not enough
 * of them to fill the whole minimum size.
 * @param horizontalAlignment the horizontal alignment applied to the items.
 * @param content a block which describes the content. Inside this block you can use methods like
 * [LazyListScope.item] to add a single item or [LazyListScope.items] to add a list of items.
 */
@OptIn(InternalLayoutApi::class)
@Composable
fun LazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: LazyListScope.() -> Unit
) {
    val scope = LazyListScopeImpl()
    scope.apply(content)

    LazyList(
        itemsCount = scope.totalSize,
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = verticalArrangement,
        isVertical = true,
        reverseLayout = reverseLayout
    ) { index ->
        scope.contentFor(index, this)
    }
}
