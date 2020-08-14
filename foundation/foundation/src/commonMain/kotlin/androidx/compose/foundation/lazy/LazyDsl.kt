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

import androidx.compose.foundation.layout.InnerPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@RequiresOptIn(
    "This is an experimental API for demonstrating how LazyColumn / LazyRow should work" +
            "using a DSL implementation. This is a prototype and its implementation is not suited" +
            " for PagedList or large lists."
)
annotation class ExperimentalLazyDsl

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
    fun <T : Any> items(
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
    fun <T : Any> itemsIndexed(
        items: List<T>,
        itemContent: @Composable LazyItemScope.(index: Int, item: T) -> Unit
    )
}

private class LazyListScopeImpl : LazyListScope {
    // TODO: Avoid allocating per-item composable by saving the composable for a range of items
    val allItemsContent = mutableListOf<@Composable LazyItemScope.() -> Unit>()

    override fun <T : Any> items(
        items: List<T>,
        itemContent: @Composable LazyItemScope.(item: T) -> Unit
    ) {
        items.forEach {
            allItemsContent.add {
                itemContent(it)
            }
        }
    }

    override fun item(content: @Composable LazyItemScope.() -> Unit) {
        allItemsContent.add(content)
    }

    override fun <T : Any> itemsIndexed(
        items: List<T>,
        itemContent: @Composable LazyItemScope.(index: Int, item: T) -> Unit
    ) {
        items.forEachIndexed { index, item -> allItemsContent.add { itemContent(index, item) } }
    }
}

/**
 * The DSL implementation of a horizontally scrolling list that only composes and lays out the
 * currently visible items.
 * This API is not stable yet, please consider using [LazyRowFor] instead.
 *
 * @param modifier the modifier to apply to this layout
 * @param contentPadding specify a padding around the whole content
 * @param verticalGravity the vertical gravity applied to the items
 * @param content the [LazyListScope] which describes the content
 */
@Composable
@ExperimentalLazyDsl
fun LazyRow(
    modifier: Modifier = Modifier,
    contentPadding: InnerPadding = InnerPadding(0.dp),
    verticalGravity: Alignment.Vertical = Alignment.Top,
    content: LazyListScope.() -> Unit
) {
    val scope = LazyListScopeImpl()
    scope.apply(content)

    LazyFor(
        itemsCount = scope.allItemsContent.size,
        modifier = modifier,
        contentPadding = contentPadding,
        verticalGravity = verticalGravity,
        isVertical = false
    ) { index ->
        {
            scope.allItemsContent[index].invoke(this)
        }
    }
}

/**
 * The DSL implementation of a vertically scrolling list that only composes and lays out the
 * currently visible items.
 * This API is not stable yet, please consider using [LazyColumnFor] instead.
 *
 * @param modifier the modifier to apply to this layout
 * @param contentPadding specify a padding around the whole content
 * @param horizontalGravity the horizontal gravity applied to the items
 * @param content the [LazyListScope] which describes the content
 */
@Composable
@ExperimentalLazyDsl
fun LazyColumn(
    modifier: Modifier = Modifier,
    contentPadding: InnerPadding = InnerPadding(0.dp),
    horizontalGravity: Alignment.Horizontal = Alignment.Start,
    content: LazyListScope.() -> Unit
) {
    val scope = LazyListScopeImpl()
    scope.apply(content)

    LazyFor(
        itemsCount = scope.allItemsContent.size,
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalGravity = horizontalGravity,
        isVertical = true
    ) { index ->
        {
            scope.allItemsContent[index].invoke(this)
        }
    }
}
