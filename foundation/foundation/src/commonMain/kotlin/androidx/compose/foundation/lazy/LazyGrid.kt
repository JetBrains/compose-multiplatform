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
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.grid.LazyGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The DSL implementation of a lazy grid layout. It composes only visible rows of the grid.
 * This API is not stable, please consider using stable components like [LazyColumn] and [Row]
 * to achieve the same result.
 *
 * @param cells a class describing how cells form columns, see [GridCells] doc for more information
 * @param modifier the modifier to apply to this layout
 * @param state the state object to be used to control or observe the list's state
 * @param contentPadding specify a padding around the whole content
 * @param verticalArrangement The vertical arrangement of the layout's children
 * @param horizontalArrangement The horizontal arrangement of the layout's children
 * @param flingBehavior logic describing fling behavior
 * @param userScrollEnabled whether the scrolling via the user gestures or accessibility actions
 * is allowed. You can still scroll programmatically using the state even when it is disabled.
 * @param content the [LazyListScope] which describes the content
 */
@ExperimentalFoundationApi
@Composable
fun LazyVerticalGrid(
    cells: GridCells,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    content: LazyGridScope.() -> Unit
) {
    val slotsPerLine = remember<Density.(Constraints) -> Int>(cells) {
        { constraints ->
            if (cells is GridCells.Fixed) {
                cells.count
            } else {
                require(cells is GridCells.Adaptive)
                maxOf((constraints.maxWidth.toDp() / cells.minSize).toInt(), 1)
            }
        }
    }
    LazyGrid(
        slotsPerLine = slotsPerLine,
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        content = content
    )
}

/**
 * This class describes how cells form columns in vertical grids or rows in horizontal grids.
 */
@ExperimentalFoundationApi
@Stable
sealed class GridCells {
    /**
     * Combines cells with fixed number rows or columns.
     *
     * For example, for the vertical [LazyVerticalGrid] Fixed(3) would mean that there are 3 columns 1/3
     * of the parent wide.
     */
    @ExperimentalFoundationApi
    @Stable
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
    @ExperimentalFoundationApi
    @Stable
    class Adaptive(val minSize: Dp) : GridCells()

    override fun hashCode() = if (this is Fixed) {
        31 + count
    } else {
        require(this is Adaptive)
        62 + minSize.hashCode()
    }

    override fun equals(other: Any?) =
        (this is Fixed && other is Fixed && this.count == other.count) ||
            (this is Adaptive && other is Adaptive && this.minSize == other.minSize)
}

/**
 * Receiver scope which is used by [LazyVerticalGrid].
 */
@ExperimentalFoundationApi
interface LazyGridScope {
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
        content: @Composable () -> Unit
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
     * the same type could be reused more efficiently. Note that null is a valid type and items of such
     * type will be considered compatible.
     * @param itemContent the content displayed by a single item
     */
    fun items(
        count: Int,
        key: ((index: Int) -> Any)? = null,
        span: (LazyGridItemSpanScope.(index: Int) -> GridItemSpan)? = null,
        contentType: (index: Int) -> Any? = { null },
        itemContent: @Composable (index: Int) -> Unit
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
@ExperimentalFoundationApi
inline fun <T> LazyGridScope.items(
    items: List<T>,
    noinline key: ((item: T) -> Any)? = null,
    noinline span: (LazyGridItemSpanScope.(item: T) -> GridItemSpan)? = null,
    noinline contentType: (item: T) -> Any? = { null },
    crossinline itemContent: @Composable (item: T) -> Unit
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
@ExperimentalFoundationApi
inline fun <T> LazyGridScope.itemsIndexed(
    items: List<T>,
    noinline key: ((index: Int, item: T) -> Any)? = null,
    noinline span: (LazyGridItemSpanScope.(index: Int, item: T) -> GridItemSpan)? = null,
    crossinline contentType: (index: Int, item: T) -> Any? = { _, _ -> null },
    crossinline itemContent: @Composable (index: Int, item: T) -> Unit
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
@ExperimentalFoundationApi
inline fun <T> LazyGridScope.items(
    items: Array<T>,
    noinline key: ((item: T) -> Any)? = null,
    noinline span: (LazyGridItemSpanScope.(item: T) -> GridItemSpan)? = null,
    noinline contentType: (item: T) -> Any? = { null },
    crossinline itemContent: @Composable (item: T) -> Unit
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
@ExperimentalFoundationApi
inline fun <T> LazyGridScope.itemsIndexed(
    items: Array<T>,
    noinline key: ((index: Int, item: T) -> Any)? = null,
    noinline span: (LazyGridItemSpanScope.(index: Int, item: T) -> GridItemSpan)? = null,
    crossinline contentType: (index: Int, item: T) -> Any? = { _, _ -> null },
    crossinline itemContent: @Composable (index: Int, item: T) -> Unit
) = items(
    count = items.size,
    key = if (key != null) { index: Int -> key(index, items[index]) } else null,
    span = if (span != null) { { span(it, items[it]) } } else null,
    contentType = { index -> contentType(index, items[index]) }
) {
    itemContent(it, items[it])
}
