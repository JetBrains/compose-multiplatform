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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.WithConstraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp

/**
 * The DSL implementation of a lazy grid layout. It composes only visible rows of the grid.
 * This API is not stable, please consider using stable components like [LazyColumn] and [Row]
 * to achieve the same result.
 *
 * @param cells a class describing how cells form columns, see [GridCells] doc for more information
 * @param modifier the modifier to apply to this layout
 * @param contentPadding specify a padding around the whole content
 * @param content the [LazyListScope] which describes the content
 */
@ExperimentalFoundationApi
@Composable
fun LazyVerticalGrid(
    cells: GridCells,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: LazyGridScope.() -> Unit
) {
    val scope = LazyGridScopeImpl()
    scope.apply(content)

    when (cells) {
        is GridCells.Fixed ->
            FixedLazyGrid(
                nColumns = cells.count,
                modifier = modifier,
                state = state,
                contentPadding = contentPadding,
                scope = scope
            )
        is GridCells.Adaptive ->
            WithConstraints(
                modifier = modifier
            ) {
                val nColumns = maxOf((maxWidth / cells.minSize).toInt(), 1)
                FixedLazyGrid(
                    nColumns = nColumns,
                    state = state,
                    contentPadding = contentPadding,
                    scope = scope
                )
            }
    }
}

/**
 * This class describes how cells form columns in vertical grids or rows in horizontal grids.
 */
sealed class GridCells {
    /**
     * Combines cells with fixed number rows or columns.
     *
     * For example, for the vertical [LazyVerticalGrid] Fixed(3) would mean that there are 3 columns 1/3
     * of the parent wide.
     */
    class Fixed(val count: Int) : GridCells()

    /**
     * Combines cells with adaptive number of rows or columns. It will try to position as many rows
     * or columns as possible on the condition that every cell has at least [minSize] space and
     * all extra space distributed evenly.
     *
     * For example, for the vertical [LazyVerticalGrid] Adaptive(20.dp) would mean that there will be as
     * many columns as possible and every column will be at least 20.dp and all the columns will
     * have equal width. If the screen is 88.dp wide then there will be 4 columns 22.dp each.
     */
    class Adaptive(val minSize: Dp) : GridCells()
}

/**
 * Receiver scope which is used by [LazyVerticalGrid].
 */
interface LazyGridScope {
    /**
     * Adds a single item to the scope.
     *
     * @param content the content of the item
     */
    fun item(content: @Composable LazyItemScope.() -> Unit)

    /**
     * Adds a [count] of items.
     *
     * @param count the items count
     * @param itemContent the content displayed by a single item
     */
    fun items(count: Int, itemContent: @Composable LazyItemScope.(index: Int) -> Unit)
}

/**
 * Adds a list of items.
 *
 * @param items the data list
 * @param itemContent the content displayed by a single item
 */
inline fun <T> LazyGridScope.items(
    items: List<T>,
    crossinline itemContent: @Composable LazyItemScope.(item: T) -> Unit
) = items(items.size) {
    itemContent(items[it])
}

/**
 * Adds a list of items where the content of an item is aware of its index.
 *
 * @param items the data list
 * @param itemContent the content displayed by a single item
 */
inline fun <T> LazyGridScope.itemsIndexed(
    items: List<T>,
    crossinline itemContent: @Composable LazyItemScope.(index: Int, item: T) -> Unit
) = items(items.size) {
    itemContent(it, items[it])
}

/**
 * Adds an array of items.
 *
 * @param items the data array
 * @param itemContent the content displayed by a single item
 */
inline fun <T> LazyGridScope.items(
    items: Array<T>,
    crossinline itemContent: @Composable LazyItemScope.(item: T) -> Unit
) = items(items.size) {
    itemContent(items[it])
}

/**
 * Adds an array of items where the content of an item is aware of its index.
 *
 * @param items the data array
 * @param itemContent the content displayed by a single item
 */
inline fun <T> LazyGridScope.itemsIndexed(
    items: Array<T>,
    crossinline itemContent: @Composable LazyItemScope.(index: Int, item: T) -> Unit
) = items(items.size) {
    itemContent(it, items[it])
}

@Composable
private fun FixedLazyGrid(
    nColumns: Int,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    scope: LazyGridScopeImpl
) {
    val rows = (scope.totalSize + nColumns - 1) / nColumns
    LazyList(
        itemsCount = rows,
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        isVertical = true,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top,
        reverseLayout = false
    ) { rowIndex ->
        @Composable {
            Row {
                for (columnIndex in 0 until nColumns) {
                    val itemIndex = rowIndex * nColumns + columnIndex
                    if (itemIndex < scope.totalSize) {
                        GridCellBox(
                            modifier = Modifier.weight(1f, fill = true)
                        ) {
                            scope.contentFor(itemIndex, this@LazyList).invoke()
                        }
                    } else {
                        Spacer(Modifier.weight(1f, fill = true))
                    }
                }
            }
        }
    }
}

/**
 * TODO: Remove when the Box component supports fixed constraints.
 */
@Composable
private fun GridCellBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content, modifier) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }
        val size = placeables.fold(IntSize.Zero) { size, item ->
            IntSize(maxOf(size.width, item.width), maxOf(size.height, item.height))
        }
        layout(constraints.constrainWidth(size.width), constraints.constrainHeight(size.height)) {
            placeables.forEach {
                it.place(0, 0)
            }
        }
    }
}

internal class LazyGridScopeImpl : LazyGridScope {
    private val intervals = IntervalList<LazyItemScope.(Int) -> (@Composable () -> Unit)>()

    val totalSize get() = intervals.totalSize

    fun contentFor(index: Int, scope: LazyItemScope): @Composable () -> Unit {
        val interval = intervals.intervalForIndex(index)
        val localIntervalIndex = index - interval.startIndex

        return interval.content(scope, localIntervalIndex)
    }

    override fun item(content: @Composable LazyItemScope.() -> Unit) {
        intervals.add(1) { @Composable { content() } }
    }

    override fun items(count: Int, itemContent: @Composable LazyItemScope.(index: Int) -> Unit) {
        intervals.add(count) {
            @Composable { itemContent(it) }
        }
    }
}
