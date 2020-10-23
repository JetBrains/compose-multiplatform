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

import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.gesture.scrollorientationlocking.Orientation
import androidx.compose.ui.layout.ExperimentalSubcomposeLayoutApi
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LayoutDirectionAmbient
import androidx.compose.ui.unit.LayoutDirection
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
 * @param contentPadding convenience param to specify a padding around the whole content. This will
 * add padding for the content after it has been clipped, which is not possible via [modifier]
 * param. Note that it is *not* a padding applied for each item's content
 * @param horizontalAlignment the horizontal alignment applied to the items
 * @param itemContent emits the UI for an item from [items] list. May emit any number of components,
 * which will be stacked vertically. Note that [LazyColumnFor] can start scrolling incorrectly
 * if you emit nothing and then lazily recompose with the real content, so even if you load the
 * content asynchronously please reserve some space for the item, for example using [Spacer].
 * Use [LazyColumnForIndexed] if you need to have both index and item params.
 */
@Composable
fun <T> LazyColumnFor(
    items: List<T>,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    itemContent: @Composable LazyItemScope.(T) -> Unit
) {
    LazyFor(
        itemsCount = items.size,
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        horizontalAlignment = horizontalAlignment,
        isVertical = true
    ) { index ->
        val item = items[index]
        {
            key(index) {
                itemContent(item)
            }
        }
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
 * @param contentPadding convenience param to specify a padding around the whole content. This will
 * add padding for the content after it has been clipped, which is not possible via [modifier]
 * param. Note that it is *not* a padding applied for each item's content
 * @param horizontalAlignment the horizontal alignment applied to the items
 * @param itemContent emits the UI for an item from [items] list. It has two params: first one is
 * an index in the [items] list, and the second one is the item at this index from [items] list.
 * May emit any number of components, which will be stacked vertically. Note that
 * [LazyColumnForIndexed] can start scrolling incorrectly if you emit nothing and then lazily
 * recompose with the real content, so even if you load the content asynchronously please reserve
 * some space for the item, for example using [Spacer].
 */
@Composable
fun <T> LazyColumnForIndexed(
    items: List<T>,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    itemContent: @Composable LazyItemScope.(index: Int, item: T) -> Unit
) {
    LazyFor(
        itemsCount = items.size,
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        horizontalAlignment = horizontalAlignment,
        isVertical = true
    ) { index ->
        val item = items[index]
        {
            key(index) {
                itemContent(index, item)
            }
        }
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
 * @param items the backing list of data to display
 * @param modifier the modifier to apply to this layout
 * @param state the state object to be used to control or observe the list's state
 * @param contentPadding convenience param to specify a padding around the whole content. This will
 * add padding for the content after it has been clipped, which is not possible via [modifier]
 * param. Note that it is *not* a padding applied for each item's content
 * @param verticalAlignment the vertical alignment applied to the items
 * @param itemContent emits the UI for an item from [items] list. May emit any number of components,
 * which will be stacked horizontally. Note that [LazyRowFor] can start scrolling incorrectly
 * if you emit nothing and then lazily recompose with the real content, so even if you load the
 * content asynchronously please reserve some space for the item, for example using [Spacer].
 * Use [LazyRowForIndexed] if you need to have both index and item params.
 */
@Composable
fun <T> LazyRowFor(
    items: List<T>,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    itemContent: @Composable LazyItemScope.(T) -> Unit
) {
    LazyFor(
        itemsCount = items.size,
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        verticalAlignment = verticalAlignment,
        isVertical = false
    ) { index ->
        val item = items[index]
        {
            key(index) {
                itemContent(item)
            }
        }
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
 * @param items the backing list of data to display
 * @param modifier the modifier to apply to this layout
 * @param state the state object to be used to control or observe the list's state
 * @param contentPadding convenience param to specify a padding around the whole content. This will
 * add padding for the content after it has been clipped, which is not possible via [modifier]
 * param. Note that it is *not* a padding applied for each item's content
 * @param verticalAlignment the vertical alignment applied to the items
 * @param itemContent emits the UI for an item from [items] list. It has two params: first one is
 * an index in the [items] list, and the second one is the item at this index from [items] list.
 * May emit any number of components, which will be stacked horizontally. Note that
 * [LazyRowForIndexed] can start scrolling incorrectly if you emit nothing and then lazily
 * recompose with the real content, so even if you load the content asynchronously please reserve
 * some space for the item, for example using [Spacer].
 */
@Composable
fun <T> LazyRowForIndexed(
    items: List<T>,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    itemContent: @Composable LazyItemScope.(index: Int, item: T) -> Unit
) {
    LazyFor(
        itemsCount = items.size,
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        verticalAlignment = verticalAlignment,
        isVertical = false
    ) { index ->
        val item = items[index]
        {
            key(index) {
                itemContent(index, item)
            }
        }
    }
}

@Suppress("NOTHING_TO_INLINE")
@Composable
@OptIn(ExperimentalSubcomposeLayoutApi::class)
internal inline fun LazyFor(
    itemsCount: Int,
    modifier: Modifier = Modifier,
    state: LazyListState,
    contentPadding: PaddingValues,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    isVertical: Boolean,
    noinline itemContentFactory: LazyItemScope.(Int) -> @Composable () -> Unit
) {
    val reverseDirection = LayoutDirectionAmbient.current == LayoutDirection.Rtl && !isVertical

    val cachingItemContentFactory = remember { CachingItemContentFactory(itemContentFactory) }
    cachingItemContentFactory.itemContentFactory = itemContentFactory

    SubcomposeLayout<DataIndex>(
        modifier
            .scrollable(
                orientation = if (isVertical) Orientation.Vertical else Orientation.Horizontal,
                reverseDirection = reverseDirection,
                controller = state.scrollableController
            )
            .clipToBounds()
            .padding(contentPadding)
            .then(state.remeasurementModifier)
    ) { constraints ->
        // this will update the scope object if the constrains have been changed
        cachingItemContentFactory.updateItemScope(this, constraints)

        state.measure(
            this,
            constraints,
            isVertical,
            horizontalAlignment,
            verticalAlignment,
            itemsCount,
            cachingItemContentFactory
        )
    }
}
