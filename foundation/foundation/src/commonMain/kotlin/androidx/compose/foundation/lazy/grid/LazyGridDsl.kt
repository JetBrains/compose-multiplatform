/*
 * Copyright 2022 The Android Open Source Project
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
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * A lazy vertical grid layout. It composes only visible rows of the grid.
 *
 * Sample:
 * @sample androidx.compose.foundation.samples.LazyVerticalGridSample
 *
 * Sample with custom item spans:
 * @sample androidx.compose.foundation.samples.LazyVerticalGridSpanSample
 *
 * @param columns describes the count and the size of the grid's columns,
 * see [GridCells] doc for more information
 * @param modifier the modifier to apply to this layout
 * @param state the state object to be used to control or observe the list's state
 * @param contentPadding specify a padding around the whole content
 * @param reverseLayout reverse the direction of scrolling and layout. When `true`, items will be
 * laid out in the reverse order  and [LazyGridState.firstVisibleItemIndex] == 0 means
 * that grid is scrolled to the bottom. Note that [reverseLayout] does not change the behavior of
 * [verticalArrangement],
 * e.g. with [Arrangement.Top] (top) 123### (bottom) becomes (top) 321### (bottom).
 * @param verticalArrangement The vertical arrangement of the layout's children
 * @param horizontalArrangement The horizontal arrangement of the layout's children
 * @param flingBehavior logic describing fling behavior
 * @param userScrollEnabled whether the scrolling via the user gestures or accessibility actions
 * is allowed. You can still scroll programmatically using the state even when it is disabled.
 * @param content the [LazyGridScope] which describes the content
 */
@Composable
fun LazyVerticalGrid(
    columns: GridCells,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    content: LazyGridScope.() -> Unit
) {
    val slotSizesSums = rememberColumnWidthSums(columns, horizontalArrangement, contentPadding)
    LazyGrid(
        slotSizesSums = slotSizesSums,
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        isVertical = true,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        content = content
    )
}

/**
 * A lazy horizontal grid layout. It composes only visible columns of the grid.
 *
 * Sample:
 * @sample androidx.compose.foundation.samples.LazyHorizontalGridSample
 *
 * Sample with custom item spans:
 * @sample androidx.compose.foundation.samples.LazyHorizontalGridSpanSample
 *
 * @param rows a class describing how cells form rows, see [GridCells] doc for more information
 * @param modifier the modifier to apply to this layout
 * @param state the state object to be used to control or observe the list's state
 * @param contentPadding specify a padding around the whole content
 * @param reverseLayout reverse the direction of scrolling and layout. When `true`, items are
 * laid out in the reverse order and [LazyGridState.firstVisibleItemIndex] == 0 means
 * that grid is scrolled to the end. Note that [reverseLayout] does not change the behavior of
 * [horizontalArrangement], e.g. with [Arrangement.Start] [123###] becomes [321###].
 * @param verticalArrangement The vertical arrangement of the layout's children
 * @param horizontalArrangement The horizontal arrangement of the layout's children
 * @param flingBehavior logic describing fling behavior
 * @param userScrollEnabled whether the scrolling via the user gestures or accessibility actions
 * is allowed. You can still scroll programmatically using the state even when it is disabled.
 * @param content the [LazyGridScope] which describes the content
 */
@Composable
fun LazyHorizontalGrid(
    rows: GridCells,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    horizontalArrangement: Arrangement.Horizontal =
        if (!reverseLayout) Arrangement.Start else Arrangement.End,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    content: LazyGridScope.() -> Unit
) {
    val slotSizesSums = rememberRowHeightSums(rows, verticalArrangement, contentPadding)
    LazyGrid(
        slotSizesSums = slotSizesSums,
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        isVertical = false,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        content = content
    )
}

/** Returns prefix sums of column widths. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun rememberColumnWidthSums(
    columns: GridCells,
    horizontalArrangement: Arrangement.Horizontal,
    contentPadding: PaddingValues
) = remember<Density.(Constraints) -> List<Int>>(
    columns,
    horizontalArrangement,
    contentPadding,
) {
    { constraints ->
        require(constraints.maxWidth != Constraints.Infinity) {
            "LazyVerticalGrid's width should be bound by parent."
        }
        val horizontalPadding = contentPadding.calculateStartPadding(LayoutDirection.Ltr) +
            contentPadding.calculateEndPadding(LayoutDirection.Ltr)
        val gridWidth = constraints.maxWidth - horizontalPadding.roundToPx()
        with(columns) {
            calculateCrossAxisCellSizes(
                gridWidth,
                horizontalArrangement.spacing.roundToPx()
            ).toMutableList().apply {
                for (i in 1 until size) {
                    this[i] += this[i - 1]
                }
            }
        }
    }
}

/** Returns prefix sums of row heights. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun rememberRowHeightSums(
    rows: GridCells,
    verticalArrangement: Arrangement.Vertical,
    contentPadding: PaddingValues
) = remember<Density.(Constraints) -> List<Int>>(
    rows,
    verticalArrangement,
    contentPadding,
) {
    { constraints ->
        require(constraints.maxHeight != Constraints.Infinity) {
            "LazyHorizontalGrid's height should be bound by parent."
        }
        val verticalPadding = contentPadding.calculateTopPadding() +
            contentPadding.calculateBottomPadding()
        val gridHeight = constraints.maxHeight - verticalPadding.roundToPx()
        with(rows) {
            calculateCrossAxisCellSizes(
                gridHeight,
                verticalArrangement.spacing.roundToPx()
            ).toMutableList().apply {
                for (i in 1 until size) {
                    this[i] += this[i - 1]
                }
            }
        }
    }
}

/**
 * This class describes the count and the sizes of columns in vertical grids,
 * or rows in horizontal grids.
 */
@Stable
interface GridCells {
    /**
     * Calculates the number of cells and their cross axis size based on
     * [availableSize] and [spacing].
     *
     * For example, in vertical grids, [spacing] is passed from the grid's [Arrangement.Horizontal].
     * The [Arrangement.Horizontal] will also be used to arrange items in a row if the grid is wider
     * than the calculated sum of columns.
     *
     * Note that the calculated cross axis sizes will be considered in an RTL-aware manner --
     * if the grid is vertical and the layout direction is RTL, the first width in the returned
     * list will correspond to the rightmost column.
     *
     * @param availableSize available size on cross axis, e.g. width of [LazyVerticalGrid].
     * @param spacing cross axis spacing, e.g. horizontal spacing for [LazyVerticalGrid].
     * The spacing is passed from the corresponding [Arrangement] param of the lazy grid.
     */
    fun Density.calculateCrossAxisCellSizes(availableSize: Int, spacing: Int): List<Int>

    /**
     * Defines a grid with fixed number of rows or columns.
     *
     * For example, for the vertical [LazyVerticalGrid] Fixed(3) would mean that
     * there are 3 columns 1/3 of the parent width.
     */
    class Fixed(private val count: Int) : GridCells {
        init {
            require(count > 0)
        }

        override fun Density.calculateCrossAxisCellSizes(
            availableSize: Int,
            spacing: Int
        ): List<Int> {
            return calculateCellsCrossAxisSizeImpl(availableSize, count, spacing)
        }

        override fun hashCode(): Int {
            return -count // Different sign from Adaptive.
        }

        override fun equals(other: Any?): Boolean {
            return other is Fixed && count == other.count
        }
    }

    /**
     * Defines a grid with as many rows or columns as possible on the condition that
     * every cell has at least [minSize] space and all extra space distributed evenly.
     *
     * For example, for the vertical [LazyVerticalGrid] Adaptive(20.dp) would mean that
     * there will be as many columns as possible and every column will be at least 20.dp
     * and all the columns will have equal width. If the screen is 88.dp wide then
     * there will be 4 columns 22.dp each.
     */
    class Adaptive(private val minSize: Dp) : GridCells {
        init {
            require(minSize > 0.dp)
        }

        override fun Density.calculateCrossAxisCellSizes(
            availableSize: Int,
            spacing: Int
        ): List<Int> {
            val count = maxOf((availableSize + spacing) / (minSize.roundToPx() + spacing), 1)
            return calculateCellsCrossAxisSizeImpl(availableSize, count, spacing)
        }

        override fun hashCode(): Int {
            return minSize.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            return other is Adaptive && minSize == other.minSize
        }
    }
}

private fun calculateCellsCrossAxisSizeImpl(
    gridSize: Int,
    slotCount: Int,
    spacing: Int
): List<Int> {
    val gridSizeWithoutSpacing = gridSize - spacing * (slotCount - 1)
    val slotSize = gridSizeWithoutSpacing / slotCount
    val remainingPixels = gridSizeWithoutSpacing % slotCount
    return List(slotCount) {
        slotSize + if (it < remainingPixels) 1 else 0
    }
}

/**
 * Receiver scope which is used by [LazyVerticalGrid].
 */
@LazyGridScopeMarker
sealed interface LazyGridScope {
    /**
     * Adds a single item to the scope.
     *
     * @param key a stable and unique key representing the item. Using the same key
     * for multiple items in the grid is not allowed. Type of the key should be saveable
     * via Bundle on Android. If null is passed the position in the grid will represent the key.
     * When you specify the key the scroll position will be maintained based on the key, which
     * means if you add/remove items before the current visible item the item with the given key
     * will be kept as the first visible one.
     * @param span the span of the item. Default is 1x1. It is good practice to leave it `null`
     * when this matches the intended behavior, as providing a custom implementation impacts
     * performance
     * @param contentType the type of the content of this item. The item compositions of the same
     * type could be reused more efficiently. Note that null is a valid type and items of such
     * type will be considered compatible.
     * @param content the content of the item
     */
    fun item(
        key: Any? = null,
        span: (LazyGridItemSpanScope.() -> GridItemSpan)? = null,
        contentType: Any? = null,
        content: @Composable LazyGridItemScope.() -> Unit
    )

    /**
     * Adds a [count] of items.
     *
     * @param count the items count
     * @param key a factory of stable and unique keys representing the item. Using the same key
     * for multiple items in the grid is not allowed. Type of the key should be saveable
     * via Bundle on Android. If null is passed the position in the grid will represent the key.
     * When you specify the key the scroll position will be maintained based on the key, which
     * means if you add/remove items before the current visible item the item with the given key
     * will be kept as the first visible one.
     * @param span define custom spans for the items. Default is 1x1. It is good practice to
     * leave it `null` when this matches the intended behavior, as providing a custom
     * implementation impacts performance
     * @param contentType a factory of the content types for the item. The item compositions of
     * the same type could be reused more efficiently. Note that null is a valid type and items
     * of such type will be considered compatible.
     * @param itemContent the content displayed by a single item
     */
    fun items(
        count: Int,
        key: ((index: Int) -> Any)? = null,
        span: (LazyGridItemSpanScope.(index: Int) -> GridItemSpan)? = null,
        contentType: (index: Int) -> Any? = { null },
        itemContent: @Composable LazyGridItemScope.(index: Int) -> Unit
    )
}

/**
 * Adds a list of items.
 *
 * @param items the data list
 * @param key a factory of stable and unique keys representing the item. Using the same key
 * for multiple items in the grid is not allowed. Type of the key should be saveable
 * via Bundle on Android. If null is passed the position in the grid will represent the key.
 * When you specify the key the scroll position will be maintained based on the key, which
 * means if you add/remove items before the current visible item the item with the given key
 * will be kept as the first visible one.
 * @param span define custom spans for the items. Default is 1x1. It is good practice to
 * leave it `null` when this matches the intended behavior, as providing a custom implementation
 * impacts performance
 * @param contentType a factory of the content types for the item. The item compositions of
 * the same type could be reused more efficiently. Note that null is a valid type and items of such
 * type will be considered compatible.
 * @param itemContent the content displayed by a single item
 */
inline fun <T> LazyGridScope.items(
    items: List<T>,
    noinline key: ((item: T) -> Any)? = null,
    noinline span: (LazyGridItemSpanScope.(item: T) -> GridItemSpan)? = null,
    noinline contentType: (item: T) -> Any? = { null },
    crossinline itemContent: @Composable LazyGridItemScope.(item: T) -> Unit
) = items(
    count = items.size,
    key = if (key != null) { index: Int -> key(items[index]) } else null,
    span = if (span != null) { { span(items[it]) } } else null,
    contentType = { index: Int -> contentType(items[index]) }
) {
    itemContent(items[it])
}

/**
 * Adds a list of items where the content of an item is aware of its index.
 *
 * @param items the data list
 * @param key a factory of stable and unique keys representing the item. Using the same key
 * for multiple items in the grid is not allowed. Type of the key should be saveable
 * via Bundle on Android. If null is passed the position in the grid will represent the key.
 * When you specify the key the scroll position will be maintained based on the key, which
 * means if you add/remove items before the current visible item the item with the given key
 * will be kept as the first visible one.
 * @param span define custom spans for the items. Default is 1x1. It is good practice to leave
 * it `null` when this matches the intended behavior, as providing a custom implementation
 * impacts performance
 * @param contentType a factory of the content types for the item. The item compositions of
 * the same type could be reused more efficiently. Note that null is a valid type and items of such
 * type will be considered compatible.
 * @param itemContent the content displayed by a single item
 */
inline fun <T> LazyGridScope.itemsIndexed(
    items: List<T>,
    noinline key: ((index: Int, item: T) -> Any)? = null,
    noinline span: (LazyGridItemSpanScope.(index: Int, item: T) -> GridItemSpan)? = null,
    crossinline contentType: (index: Int, item: T) -> Any? = { _, _ -> null },
    crossinline itemContent: @Composable LazyGridItemScope.(index: Int, item: T) -> Unit
) = items(
    count = items.size,
    key = if (key != null) { index: Int -> key(index, items[index]) } else null,
    span = if (span != null) { { span(it, items[it]) } } else null,
    contentType = { index -> contentType(index, items[index]) }
) {
    itemContent(it, items[it])
}

/**
 * Adds an array of items.
 *
 * @param items the data array
 * @param key a factory of stable and unique keys representing the item. Using the same key
 * for multiple items in the grid is not allowed. Type of the key should be saveable
 * via Bundle on Android. If null is passed the position in the grid will represent the key.
 * When you specify the key the scroll position will be maintained based on the key, which
 * means if you add/remove items before the current visible item the item with the given key
 * will be kept as the first visible one.
 * @param span define custom spans for the items. Default is 1x1. It is good practice to leave
 * it `null` when this matches the intended behavior, as providing a custom implementation
 * impacts performance
 * @param contentType a factory of the content types for the item. The item compositions of
 * the same type could be reused more efficiently. Note that null is a valid type and items of such
 * type will be considered compatible.
 * @param itemContent the content displayed by a single item
 */
inline fun <T> LazyGridScope.items(
    items: Array<T>,
    noinline key: ((item: T) -> Any)? = null,
    noinline span: (LazyGridItemSpanScope.(item: T) -> GridItemSpan)? = null,
    noinline contentType: (item: T) -> Any? = { null },
    crossinline itemContent: @Composable LazyGridItemScope.(item: T) -> Unit
) = items(
    count = items.size,
    key = if (key != null) { index: Int -> key(items[index]) } else null,
    span = if (span != null) { { span(items[it]) } } else null,
    contentType = { index: Int -> contentType(items[index]) }
) {
    itemContent(items[it])
}

/**
 * Adds an array of items where the content of an item is aware of its index.
 *
 * @param items the data array
 * @param key a factory of stable and unique keys representing the item. Using the same key
 * for multiple items in the grid is not allowed. Type of the key should be saveable
 * via Bundle on Android. If null is passed the position in the grid will represent the key.
 * When you specify the key the scroll position will be maintained based on the key, which
 * means if you add/remove items before the current visible item the item with the given key
 * will be kept as the first visible one.
 * @param span define custom spans for the items. Default is 1x1. It is good practice to leave
 * it `null` when this matches the intended behavior, as providing a custom implementation
 * impacts performance
 * @param contentType a factory of the content types for the item. The item compositions of
 * the same type could be reused more efficiently. Note that null is a valid type and items of such
 * type will be considered compatible.
 * @param itemContent the content displayed by a single item
 */
inline fun <T> LazyGridScope.itemsIndexed(
    items: Array<T>,
    noinline key: ((index: Int, item: T) -> Any)? = null,
    noinline span: (LazyGridItemSpanScope.(index: Int, item: T) -> GridItemSpan)? = null,
    crossinline contentType: (index: Int, item: T) -> Any? = { _, _ -> null },
    crossinline itemContent: @Composable LazyGridItemScope.(index: Int, item: T) -> Unit
) = items(
    count = items.size,
    key = if (key != null) { index: Int -> key(index, items[index]) } else null,
    span = if (span != null) { { span(it, items[it]) } } else null,
    contentType = { index -> contentType(index, items[index]) }
) {
    itemContent(it, items[it])
}
