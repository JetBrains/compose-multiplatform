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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A vertically scrolling list that only composes and lays out the currently visible items.
 *
 * See [LazyColumnForIndexed] if you need to have both item and index params in [itemContent].
 * See [LazyRowFor] if you are looking for a horizontally scrolling version.
 *
 * @sample androidx.compose.foundation.samples.LazyColumnForSample
 *
 * @param items the backing list of data to display
 * @param modifier the modifier to apply to this layout
 * @param state the state object to be used to control or observe the list's state
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
 * @param horizontalAlignment the horizontal alignment applied to the items
 * @param itemContent emits the UI for an item from [items] list. May emit any number of components,
 * which will be stacked vertically. Note that [LazyColumnFor] can start scrolling incorrectly
 * if you emit nothing and then lazily recompose with the real content, so even if you load the
 * content asynchronously please reserve some space for the item, for example using [Spacer].
 * Use [LazyColumnForIndexed] if you need to have both index and item params.
 */
@OptIn(InternalLayoutApi::class)
@Composable
fun <T> LazyColumnFor(
    items: List<T>,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    itemContent: @Composable LazyItemScope.(T) -> Unit
) {
    LazyColumn(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = verticalArrangement,
        reverseLayout = reverseLayout
    ) {
        items(items, itemContent)
    }
}

/**
 * A vertically scrolling list that only composes and lays out the currently visible items.
 *
 * It is the variant of [LazyColumnFor] which provides both index and item as params for
 * [itemContent].
 *
 * See [LazyRowForIndexed] if you are looking for a horizontally scrolling version.
 *
 * @sample androidx.compose.foundation.samples.LazyColumnForIndexedSample
 *
 * @param items the backing list of data to display
 * @param modifier the modifier to apply to this layout
 * @param state the state object to be used to control or observe the list's state
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
 * @param horizontalAlignment the horizontal alignment applied to the items
 * @param itemContent emits the UI for an item from [items] list. It has two params: first one is
 * an index in the [items] list, and the second one is the item at this index from [items] list.
 * May emit any number of components, which will be stacked vertically. Note that
 * [LazyColumnForIndexed] can start scrolling incorrectly if you emit nothing and then lazily
 * recompose with the real content, so even if you load the content asynchronously please reserve
 * some space for the item, for example using [Spacer].
 */
@OptIn(InternalLayoutApi::class)
@Composable
fun <T> LazyColumnForIndexed(
    items: List<T>,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    itemContent: @Composable LazyItemScope.(index: Int, item: T) -> Unit
) {
    LazyColumn(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = verticalArrangement,
        reverseLayout = reverseLayout
    ) {
        itemsIndexed(items, itemContent)
    }
}

/**
 * A horizontally scrolling list that only composes and lays out the currently visible items.
 *
 * See [LazyRowForIndexed] if you need to have both item and index params in [itemContent].
 * See [LazyColumnFor] if you are looking for a vertically scrolling version.
 *
 * @sample androidx.compose.foundation.samples.LazyRowForSample
 *
 * @param items the backing list of data to display.
 * @param modifier the modifier to apply to this layout.
 * @param state the state object to be used to control or observe the list's state.
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
 * @param verticalAlignment the vertical alignment applied to the items.
 * @param itemContent emits the UI for an item from [items] list. May emit any number of components,
 * which will be stacked horizontally. Note that [LazyRowFor] can start scrolling incorrectly
 * if you emit nothing and then lazily recompose with the real content, so even if you load the
 * content asynchronously please reserve some space for the item, for example using [Spacer].
 * Use [LazyRowForIndexed] if you need to have both index and item params.
 */
@OptIn(InternalLayoutApi::class)
@Composable
fun <T> LazyRowFor(
    items: List<T>,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    horizontalArrangement: Arrangement.Horizontal =
        if (!reverseLayout) Arrangement.Start else Arrangement.End,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    itemContent: @Composable LazyItemScope.(T) -> Unit
) {
    LazyRow(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        verticalAlignment = verticalAlignment,
        horizontalArrangement = horizontalArrangement,
        reverseLayout = reverseLayout
    ) {
        items(items, itemContent)
    }
}

/**
 * A horizontally scrolling list that only composes and lays out the currently visible items.
 *
 * It is the variant of [LazyRowFor] which provides both index and item as params for [itemContent].
 *
 * See [LazyColumnForIndexed] if you are looking for a vertically scrolling version.
 *
 * @sample androidx.compose.foundation.samples.LazyRowForIndexedSample
 *
 * @param items the backing list of data to display.
 * @param modifier the modifier to apply to this layout.
 * @param state the state object to be used to control or observe the list's state.
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
 * @param verticalAlignment the vertical alignment applied to the items.
 * @param itemContent emits the UI for an item from [items] list. It has two params: first one is
 * an index in the [items] list, and the second one is the item at this index from [items] list.
 * May emit any number of components, which will be stacked horizontally. Note that
 * [LazyRowForIndexed] can start scrolling incorrectly if you emit nothing and then lazily
 * recompose with the real content, so even if you load the content asynchronously please reserve
 * some space for the item, for example using [Spacer].
 */
@OptIn(InternalLayoutApi::class)
@Composable
fun <T> LazyRowForIndexed(
    items: List<T>,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    horizontalArrangement: Arrangement.Horizontal =
        if (!reverseLayout) Arrangement.Start else Arrangement.End,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    itemContent: @Composable LazyItemScope.(index: Int, item: T) -> Unit
) {
    LazyRow(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        verticalAlignment = verticalAlignment,
        horizontalArrangement = horizontalArrangement,
        reverseLayout = reverseLayout
    ) {
        itemsIndexed(items, itemContent)
    }
}
