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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Receiver scope which is used by [LazyColumn] and [LazyRow].
 */
@LazyScopeMarker
interface LazyListScope {
    /**
     * Adds a single item.
     *
     * @param key a stable and unique key representing the item. Using the same key
     * for multiple items in the list is not allowed. Type of the key should be saveable
     * via Bundle on Android. If null is passed the position in the list will represent the key.
     * When you specify the key the scroll position will be maintained based on the key, which
     * means if you add/remove items before the current visible item the item with the given key
     * will be kept as the first visible one.
     * @param content the content of the item
     */
    fun item(key: Any? = null, content: @Composable LazyItemScope.() -> Unit)

    /**
     * Adds a [count] of items.
     *
     * @param count the items count
     * @param key a factory of stable and unique keys representing the item. Using the same key
     * for multiple items in the list is not allowed. Type of the key should be saveable
     * via Bundle on Android. If null is passed the position in the list will represent the key.
     * When you specify the key the scroll position will be maintained based on the key, which
     * means if you add/remove items before the current visible item the item with the given key
     * will be kept as the first visible one.
     * @param itemContent the content displayed by a single item
     */
    fun items(
        count: Int,
        key: ((index: Int) -> Any)? = null,
        itemContent: @Composable LazyItemScope.(index: Int) -> Unit
    )

    /**
     * Adds a sticky header item, which will remain pinned even when scrolling after it.
     * The header will remain pinned until the next header will take its place.
     *
     * @sample androidx.compose.foundation.samples.StickyHeaderSample
     *
     * @param key a stable and unique key representing the item. Using the same key
     * for multiple items in the list is not allowed. Type of the key should be saveable
     * via Bundle on Android. If null is passed the position in the list will represent the key.
     * When you specify the key the scroll position will be maintained based on the key, which
     * means if you add/remove items before the current visible item the item with the given key
     * will be kept as the first visible one.
     * @param content the content of the header
     */
    @ExperimentalFoundationApi
    fun stickyHeader(key: Any? = null, content: @Composable LazyItemScope.() -> Unit)
}

/**
 * Adds a list of items.
 *
 * @param items the data list
 * @param key a factory of stable and unique keys representing the item. Using the same key
 * for multiple items in the list is not allowed. Type of the key should be saveable
 * via Bundle on Android. If null is passed the position in the list will represent the key.
 * When you specify the key the scroll position will be maintained based on the key, which
 * means if you add/remove items before the current visible item the item with the given key
 * will be kept as the first visible one.
 * @param itemContent the content displayed by a single item
 */
inline fun <T> LazyListScope.items(
    items: List<T>,
    noinline key: ((item: T) -> Any)? = null,
    crossinline itemContent: @Composable LazyItemScope.(item: T) -> Unit
) = items(items.size, if (key != null) { index: Int -> key(items[index]) } else null) {
    itemContent(items[it])
}

/**
 * Adds a list of items where the content of an item is aware of its index.
 *
 * @param items the data list
 * @param key a factory of stable and unique keys representing the item. Using the same key
 * for multiple items in the list is not allowed. Type of the key should be saveable
 * via Bundle on Android. If null is passed the position in the list will represent the key.
 * When you specify the key the scroll position will be maintained based on the key, which
 * means if you add/remove items before the current visible item the item with the given key
 * will be kept as the first visible one.
 * @param itemContent the content displayed by a single item
 */
inline fun <T> LazyListScope.itemsIndexed(
    items: List<T>,
    noinline key: ((index: Int, item: T) -> Any)? = null,
    crossinline itemContent: @Composable LazyItemScope.(index: Int, item: T) -> Unit
) = items(items.size, if (key != null) { index: Int -> key(index, items[index]) } else null) {
    itemContent(it, items[it])
}

/**
 * Adds an array of items.
 *
 * @param items the data array
 * @param key a factory of stable and unique keys representing the item. Using the same key
 * for multiple items in the list is not allowed. Type of the key should be saveable
 * via Bundle on Android. If null is passed the position in the list will represent the key.
 * When you specify the key the scroll position will be maintained based on the key, which
 * means if you add/remove items before the current visible item the item with the given key
 * will be kept as the first visible one.
 * @param itemContent the content displayed by a single item
 */
inline fun <T> LazyListScope.items(
    items: Array<T>,
    noinline key: ((item: T) -> Any)? = null,
    crossinline itemContent: @Composable LazyItemScope.(item: T) -> Unit
) = items(items.size, if (key != null) { index: Int -> key(items[index]) } else null) {
    itemContent(items[it])
}

/**
 * Adds an array of items where the content of an item is aware of its index.
 *
 * @param items the data array
 * @param key a factory of stable and unique keys representing the item. Using the same key
 * for multiple items in the list is not allowed. Type of the key should be saveable
 * via Bundle on Android. If null is passed the position in the list will represent the key.
 * When you specify the key the scroll position will be maintained based on the key, which
 * means if you add/remove items before the current visible item the item with the given key
 * will be kept as the first visible one.
 * @param itemContent the content displayed by a single item
 */
inline fun <T> LazyListScope.itemsIndexed(
    items: Array<T>,
    noinline key: ((index: Int, item: T) -> Any)? = null,
    crossinline itemContent: @Composable LazyItemScope.(index: Int, item: T) -> Unit
) = items(items.size, if (key != null) { index: Int -> key(index, items[index]) } else null) {
    itemContent(it, items[it])
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
 * @param flingBehavior logic describing fling behavior.
 * @param content a block which describes the content. Inside this block you can use methods like
 * [LazyListScope.item] to add a single item or [LazyListScope.items] to add a list of items.
 */
@Composable
fun LazyRow(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    horizontalArrangement: Arrangement.Horizontal =
        if (!reverseLayout) Arrangement.Start else Arrangement.End,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    content: LazyListScope.() -> Unit
) {
    LazyList(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        verticalAlignment = verticalAlignment,
        horizontalArrangement = horizontalArrangement,
        isVertical = false,
        flingBehavior = flingBehavior,
        reverseLayout = reverseLayout,
        content = content
    )
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
 * @param flingBehavior logic describing fling behavior.
 * @param content a block which describes the content. Inside this block you can use methods like
 * [LazyListScope.item] to add a single item or [LazyListScope.items] to add a list of items.
 */
@Composable
fun LazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    content: LazyListScope.() -> Unit
) {
    LazyList(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        flingBehavior = flingBehavior,
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = verticalArrangement,
        isVertical = true,
        reverseLayout = reverseLayout,
        content = content
    )
}
