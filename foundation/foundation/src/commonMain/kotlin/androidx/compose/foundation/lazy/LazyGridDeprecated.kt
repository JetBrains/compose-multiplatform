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

@file:Suppress("UNUSED_PARAMETER", "unused", "DEPRECATION_ERROR")

package androidx.compose.foundation.lazy

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp

@Deprecated(
    "LazyVerticalGrid was moved to .grid subpackage",
    replaceWith = ReplaceWith(
        "LazyVerticalGrid(columns = cells, modifier = modifier, state = state, " +
            "contentPadding = contentPadding, reverseLayout = reverseLayout, " +
            "verticalArrangement = verticalArrangement, horizontalArrangement = " +
            "horizontalArrangement, flingBehavior = flingBehavior, userScrollEnabled " +
            "= userScrollEnabled) { content() }",
        "androidx.compose.foundation.lazy.grid.LazyVerticalGrid"
    ),
    level = DeprecationLevel.ERROR
)
@Suppress("ModifierParameter")
@ExperimentalFoundationApi
@Composable
fun LazyVerticalGrid(
    cells: GridCells,
    modifier: Modifier = error("placeholder"),
    state: LazyGridState = error("placeholder"),
    contentPadding: PaddingValues = error("placeholder"),
    reverseLayout: Boolean = error("placeholder"),
    verticalArrangement: Arrangement.Vertical = error("placeholder"),
    horizontalArrangement: Arrangement.Horizontal = error("placeholder"),
    flingBehavior: FlingBehavior = error("placeholder"),
    userScrollEnabled: Boolean = error("placeholder"),
    content: LazyGridScope.() -> Unit
) {
    error("LazyVerticalGrid was moved into .grid subpackage")
}

@Deprecated(
    "GridCells was moved to .grid subpackage",
    replaceWith = ReplaceWith(
        "GridCells",
        "androidx.compose.foundation.lazy.grid.GridCells"
    ),
    level = DeprecationLevel.ERROR
)
@ExperimentalFoundationApi
object GridCells : androidx.compose.foundation.lazy.grid.GridCells {
    @ExperimentalFoundationApi
    fun Fixed(count: Int) = GridCells

    @ExperimentalFoundationApi
    fun Adaptive(minSize: Dp) = GridCells

    override fun Density.calculateCrossAxisCellSizes(availableSize: Int, spacing: Int): List<Int> {
        error("GridCells was moved into .grid subpackage")
    }
}

@Deprecated(
    "GridItemSpan was moved to .grid subpackage",
    replaceWith = ReplaceWith(
        "GridItemSpan",
        "androidx.compose.foundation.lazy.grid.GridItemSpan"
    ),
    level = DeprecationLevel.ERROR
)
@ExperimentalFoundationApi
class GridItemSpan private constructor() {
    @ExperimentalFoundationApi
    val currentLineSpan: Int = 0
}

@Deprecated(
    "GridItemSpan was moved to .grid subpackage",
    replaceWith = ReplaceWith(
        "GridItemSpan()",
        "androidx.compose.foundation.lazy.grid.GridItemSpan"
    ),
    level = DeprecationLevel.ERROR
)
@ExperimentalFoundationApi
fun GridItemSpan(currentLineSpan: Int) = GridItemSpan(currentLineSpan.toLong())

@Deprecated(
    "LazyGridState was moved to .grid subpackage",
    replaceWith = ReplaceWith(
        "LazyGridState",
        "androidx.compose.foundation.lazy.grid.LazyGridState"
    ),
    level = DeprecationLevel.ERROR
)
@ExperimentalFoundationApi
class LazyGridState

@Deprecated(
    "rememberLazyGridState was moved to .grid subpackage",
    replaceWith = ReplaceWith(
        "rememberLazyGridState()",
        "androidx.compose.foundation.lazy.grid.rememberLazyGridState"
    ),
    level = DeprecationLevel.ERROR
)
@ExperimentalFoundationApi
@Composable
fun rememberLazyGridState(
    initialFirstVisibleItemIndex: Int = 0,
    initialFirstVisibleItemScrollOffset: Int = 0
): LazyGridState {
    error("rememberLazyGridState was moved into .grid subpackage")
}

@Deprecated(
    "items was moved to .grid subpackage",
    replaceWith = ReplaceWith("items()", "androidx.compose.foundation.lazy.grid.items"),
    level = DeprecationLevel.HIDDEN
)
@ExperimentalFoundationApi
fun <T> LazyGridScope.items(
    items: List<T>,
    key: ((item: T) -> Any)? = null,
    span: (LazyGridItemSpanScope.(item: T) -> GridItemSpan)? = null,
    contentType: (item: T) -> Any? = { null },
    itemContent: @Composable LazyGridItemScope.(item: T) -> Unit
) {
    error("LazyVerticalGrid was moved into .grid subpackage")
}

@Deprecated(
    "items was moved to .grid subpackage",
    replaceWith = ReplaceWith("itemsIndexed()", "androidx.compose.foundation.lazy.grid.items"),
    level = DeprecationLevel.HIDDEN
)
@ExperimentalFoundationApi
fun <T> LazyGridScope.itemsIndexed(
    items: List<T>,
    key: ((index: Int, item: T) -> Any)? = null,
    span: (LazyGridItemSpanScope.(index: Int, item: T) -> GridItemSpan)? = null,
    contentType: (index: Int, item: T) -> Any? = { _, _ -> null },
    itemContent: @Composable LazyGridItemScope.(index: Int, item: T) -> Unit
) {
    error("LazyVerticalGrid was moved into .grid subpackage")
}

@Deprecated(
    "items was moved to .grid subpackage",
    replaceWith = ReplaceWith("items()", "androidx.compose.foundation.lazy.grid.items"),
    level = DeprecationLevel.HIDDEN
)
@ExperimentalFoundationApi
fun <T> LazyGridScope.items(
    items: Array<T>,
    key: ((item: T) -> Any)? = null,
    span: (LazyGridItemSpanScope.(item: T) -> GridItemSpan)? = null,
    contentType: (item: T) -> Any? = { null },
    itemContent: @Composable LazyGridItemScope.(item: T) -> Unit
) {
    error("LazyVerticalGrid was moved into .grid subpackage")
}

@Deprecated(
    "items was moved to .grid subpackage",
    replaceWith = ReplaceWith("itemsIndexed()", "androidx.compose.foundation.lazy.grid.items"),
    level = DeprecationLevel.HIDDEN
)
@ExperimentalFoundationApi
fun <T> LazyGridScope.itemsIndexed(
    items: Array<T>,
    key: ((index: Int, item: T) -> Any)? = null,
    span: (LazyGridItemSpanScope.(index: Int, item: T) -> GridItemSpan)? = null,
    contentType: (index: Int, item: T) -> Any? = { _, _ -> null },
    itemContent: @Composable LazyGridItemScope.(index: Int, item: T) -> Unit
) {
    error("LazyVerticalGrid was moved into .grid subpackage")
}
