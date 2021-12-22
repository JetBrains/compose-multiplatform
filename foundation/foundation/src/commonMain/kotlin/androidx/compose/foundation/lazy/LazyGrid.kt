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
import androidx.compose.foundation.fastMapIndexedNotNull
import androidx.compose.foundation.fastMaxOfOrNull
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.list.IntervalHolder
import androidx.compose.foundation.lazy.list.MutableIntervalList
import androidx.compose.foundation.lazy.list.intervalForIndex
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

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
 * @param content the [LazyListScope] which describes the content
 */
@ExperimentalFoundationApi
@Composable
fun LazyVerticalGrid(
    cells: GridCells,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: LazyGridScope.() -> Unit
) {
    when (cells) {
        is GridCells.Fixed ->
            FixedLazyGrid(
                nColumns = cells.count,
                modifier = modifier,
                state = state,
                horizontalArrangement = horizontalArrangement,
                verticalArrangement = verticalArrangement,
                contentPadding = contentPadding,
                content = content
            )
        is GridCells.Adaptive ->
            BoxWithConstraints(
                modifier = modifier
            ) {
                val nColumns = maxOf((maxWidth / cells.minSize).toInt(), 1)
                FixedLazyGrid(
                    nColumns = nColumns,
                    state = state,
                    horizontalArrangement = horizontalArrangement,
                    verticalArrangement = verticalArrangement,
                    contentPadding = contentPadding,
                    content = content
                )
            }
    }
}

/**
 * This class describes how cells form columns in vertical grids or rows in horizontal grids.
 */
@ExperimentalFoundationApi
sealed class GridCells {
    /**
     * Combines cells with fixed number rows or columns.
     *
     * For example, for the vertical [LazyVerticalGrid] Fixed(3) would mean that there are 3 columns 1/3
     * of the parent wide.
     */
    @ExperimentalFoundationApi
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
    class Adaptive(val minSize: Dp) : GridCells()
}

/**
 * Receiver scope which is used by [LazyVerticalGrid].
 */
@ExperimentalFoundationApi
interface LazyGridScope {
    /**
     * Adds a single item to the scope.
     *
     * @param span the span of the item. Default is 1x1. It is good practice to leave it `null`
     * when this matches the intended behavior, as providing a custom implementation impacts
     * performance
     * @param content the content of the item
     */
    fun item(
        span: (LazyGridItemSpanScope.() -> GridItemSpan)? = null,
        content: @Composable LazyItemScope.() -> Unit
    )

    /**
     * Adds a [count] of items.
     *
     * @param count the items count
     * @param span define custom spans for the items. Default is 1x1. It is good practice to
     * leave it `null` when this matches the intended behavior, as providing a custom
     * implementation impacts performance
     * @param itemContent the content displayed by a single item
     */
    fun items(
        count: Int,
        span: (LazyGridItemSpanScope.(index: Int) -> GridItemSpan)? = null,
        itemContent: @Composable LazyItemScope.(index: Int) -> Unit
    )
}

/**
 * Adds a list of items.
 *
 * @param items the data list
 * @param spans define custom spans for the items. Default is 1x1. It is good practice to
 * leave it `null` when this matches the intended behavior, as providing a custom implementation
 * impacts performance
 * @param itemContent the content displayed by a single item
 */
@ExperimentalFoundationApi
inline fun <T> LazyGridScope.items(
    items: List<T>,
    noinline spans: (LazyGridItemSpanScope.(item: T) -> GridItemSpan)? = null,
    crossinline itemContent: @Composable LazyItemScope.(item: T) -> Unit
) = items(
    items.size,
    spans?.let { { spans(items[it]) } }
) {
    itemContent(items[it])
}

/**
 * Adds a list of items where the content of an item is aware of its index.
 *
 * @param items the data list
 * @param spans define custom spans for the items. Default is 1x1. It is good practice to leave
 * it `null` when this matches the intended behavior, as providing a custom implementation
 * impacts performance
 * @param itemContent the content displayed by a single item
 */
@ExperimentalFoundationApi
inline fun <T> LazyGridScope.itemsIndexed(
    items: List<T>,
    noinline spans: (LazyGridItemSpanScope.(index: Int, item: T) -> GridItemSpan)? = null,
    crossinline itemContent: @Composable LazyItemScope.(index: Int, item: T) -> Unit
) = items(
    items.size,
    spans?.let { { spans(it, items[it]) } }
) {
    itemContent(it, items[it])
}

/**
 * Adds an array of items.
 *
 * @param items the data array
 * @param spans define custom spans for the items. Default is 1x1. It is good practice to leave
 * it `null` when this matches the intended behavior, as providing a custom implementation
 * impacts performance
 * @param itemContent the content displayed by a single item
 */
@ExperimentalFoundationApi
inline fun <T> LazyGridScope.items(
    items: Array<T>,
    noinline spans: (LazyGridItemSpanScope.(item: T) -> GridItemSpan)? = null,
    crossinline itemContent: @Composable LazyItemScope.(item: T) -> Unit
) = items(
    items.size,
    spans?.let { { spans(items[it]) } }
) {
    itemContent(items[it])
}

/**
 * Adds an array of items where the content of an item is aware of its index.
 *
 * @param items the data array
 * @param spans define custom spans for the items. Default is 1x1. It is good practice to leave
 * it `null` when this matches the intended behavior, as providing a custom implementation
 * impacts performance
 * @param itemContent the content displayed by a single item
 */
@ExperimentalFoundationApi
inline fun <T> LazyGridScope.itemsIndexed(
    items: Array<T>,
    noinline spans: (LazyGridItemSpanScope.(index: Int, item: T) -> GridItemSpan)? = null,
    crossinline itemContent: @Composable LazyItemScope.(index: Int, item: T) -> Unit
) = items(
    items.size,
    spans?.let { { spans(it, items[it]) } }
) {
    itemContent(it, items[it])
}

@Composable
@ExperimentalFoundationApi
private fun FixedLazyGrid(
    nColumns: Int,
    modifier: Modifier = Modifier,
    state: LazyListState,
    contentPadding: PaddingValues,
    verticalArrangement: Arrangement.Vertical,
    horizontalArrangement: Arrangement.Horizontal,
    content: LazyGridScope.() -> Unit
) {
    LazyColumn(
        modifier = modifier,
        state = state,
        verticalArrangement = verticalArrangement,
        contentPadding = contentPadding
    ) {
        val scope = LazyGridScopeImpl(nColumns)
        scope.apply(content)

        val rows = if (!scope.hasCustomSpans) {
            // We know exactly how many rows we have as the grid layout is known.
            if (scope.totalSize == 0) 0 else 1 + (scope.totalSize - 1) / nColumns
        } else {
            // Worst case. We could do smarter, but this should become a non-problem when
            // we switch to a LazyLayout implementation. TODO
            scope.totalSize
        }
        items(rows) { rowIndex ->
            val rowContent = scope.contentFor(rowIndex, this)
            if (rowContent.isNotEmpty()) {
                ItemRow(nColumns, horizontalArrangement, rowContent)
            }
        }
    }
}

@ExperimentalFoundationApi
internal class LazyGridScopeImpl(private val nColumns: Int) : LazyGridScope {
    private class IntervalData(
        val content: LazyItemScope.(Int) -> (@Composable () -> Unit),
        val span: LazyGridItemSpanScope.(Int) -> GridItemSpan
    )

    private val intervals = MutableIntervalList<IntervalData>()
    internal var hasCustomSpans = false

    /** Caches the index of the first item on lines 0, [bucketSize], 2 * [bucketSize], etc. */
    private val bucketStartItemIndex = ArrayList<Int>().apply { add(0) }
    /**
     * The interval at each we will store the starting element of lines. These will be then
     * used to calculate the layout of arbitrary lines, by starting from the closest
     * known "bucket start". The smaller the bucketSize, the smaller cost for calculating layout
     * of arbitrary lines but the higher memory usage for [bucketStartItemIndex].
     */
    private val bucketSize get() = sqrt(1.0 * totalSize / nColumns).toInt() + 1
    /** Caches the last calculated line index, useful when scrolling in main axis direction. */
    private var lastLineIndex = 0
    /** Caches the starting item index on [lastLineIndex]. */
    private var lastLineStartItemIndex = 0
    /**
     * Caches a calculated bucket, this is useful when scrolling in reverse main axis
     * direction. We cannot only keep the last element, as we would not know previous max span.
     */
    private var cachedBucketIndex = -1
    /**
     * Caches layout of [cachedBucketIndex], this is useful when scrolling in reverse main axis
     * direction. We cannot only keep the last element, as we would not know previous max span.
     */
    private val cachedBucket = mutableListOf<Int>()
    /**
     * Caches the last interval we binary searched for. We might not need to recalculate
     * for subsequent queries, as they tend to be localised.
     */
    private var lastInterval: IntervalHolder<IntervalData>? = null

    private val DefaultSpan: LazyGridItemSpanScope.(Int) -> GridItemSpan = { GridItemSpan(1) }

    val totalSize get() = intervals.totalSize

    fun contentFor(lineIndex: Int, scope: LazyItemScope): List<Pair<@Composable () -> Unit, Int>> {
        if (!hasCustomSpans) {
            // Quick return when all spans are 1x1 - in this case we can easily calculate positions.
            return contentForLineStartingWith(lineIndex * nColumns, lineIndex, scope)
        }

        val bucket = lineIndex / bucketSize
        // We can calculate the items on the line from the closest cached bucket start item.
        var currentLine = min(bucket, bucketStartItemIndex.size - 1) * bucketSize
        var currentItemIndex = bucketStartItemIndex[min(bucket, bucketStartItemIndex.size - 1)]
        // ... but try using the more localised cached values.
        if (lastLineIndex in currentLine..lineIndex) {
            // The last calculated value is a better start point. Common when scrolling main axis.
            currentLine = lastLineIndex
            currentItemIndex = lastLineStartItemIndex
        } else if (bucket == cachedBucketIndex && lineIndex - currentLine < cachedBucket.size) {
            // It happens that the needed line start is fully cached. Common when scrolling in
            // reverse main axis, as we decided to cacheThisBucket previously.
            currentItemIndex = cachedBucket[lineIndex - currentLine]
            currentLine = lineIndex
        }

        val cacheThisBucket = currentLine % bucketSize == 0 &&
            lineIndex - currentLine in 2 until bucketSize
        if (cacheThisBucket) {
            cachedBucketIndex = bucket
            cachedBucket.clear()
        }

        check(currentLine <= lineIndex)
        while (currentLine < lineIndex && currentItemIndex < totalSize) {
            if (cacheThisBucket) {
                cachedBucket.add(currentItemIndex)
            }

            var spansUsed = 0
            while (spansUsed < nColumns && currentItemIndex < totalSize) {
                spansUsed +=
                    spanOf(currentItemIndex++, currentLine, spansUsed, nColumns - spansUsed)
            }
            ++currentLine
            if (currentLine % bucketSize == 0) {
                val currentLineBucket = currentLine / bucketSize
                // This should happen, as otherwise this should have been used as starting point.
                check(bucketStartItemIndex.size == currentLineBucket)
                bucketStartItemIndex.add(currentItemIndex)
            }
        }

        lastLineIndex = lineIndex
        lastLineStartItemIndex = currentItemIndex
        return contentForLineStartingWith(currentItemIndex, lineIndex, scope)
    }

    private fun contentForLineStartingWith(
        itemIndex: Int,
        lineIndex: Int,
        scope: LazyItemScope
    ): List<Pair<@Composable () -> Unit, Int>> {
        val lineContent = ArrayList<Pair<@Composable () -> Unit, Int>>(nColumns)

        var currentItemIndex = itemIndex
        var spansUsed = 0
        while (spansUsed < nColumns && currentItemIndex < totalSize) {
            val span = spanOf(currentItemIndex, lineIndex, spansUsed, nColumns - spansUsed)
            lineContent.add(contentOf(currentItemIndex, scope) to span)
            ++currentItemIndex
            spansUsed += span
        }

        return lineContent
    }

    private fun contentOf(itemIndex: Int, scope: LazyItemScope): @Composable () -> Unit {
        val interval = cachedIntervalForIndex(itemIndex)
        return interval.content.content(scope, itemIndex - interval.startIndex)
    }

    private fun spanOf(itemIndex: Int, row: Int, column: Int, maxSpan: Int): Int {
        val interval = cachedIntervalForIndex(itemIndex)
        return with(LazyGridItemSpanScopeImpl) {
            itemRow = row
            itemColumn = column
            maxCurrentLineSpan = maxSpan

            interval.content.span(this, itemIndex - interval.startIndex)
                .currentLineSpan
                .coerceIn(1, maxSpan)
        }
    }

    private object LazyGridItemSpanScopeImpl : LazyGridItemSpanScope {
        override var itemRow = 0
        override var itemColumn = 0
        override var maxCurrentLineSpan = 0
    }

    private fun cachedIntervalForIndex(itemIndex: Int) = lastInterval.let {
        if (it != null && itemIndex in it.startIndex until it.startIndex + it.size) {
            it
        } else {
            intervals.intervalForIndex(itemIndex).also { lastInterval = it }
        }
    }

    override fun item(
        span: (LazyGridItemSpanScope.() -> GridItemSpan)?,
        content: @Composable (LazyItemScope.() -> Unit)
    ) {
        intervals.add(1, IntervalData({ { content() } }, span?.let { { span() } } ?: DefaultSpan))
        if (span != null) hasCustomSpans = true
    }

    override fun items(
        count: Int,
        span: (LazyGridItemSpanScope.(Int) -> GridItemSpan)?,
        itemContent: @Composable (LazyItemScope.(index: Int) -> Unit)
    ) {
        intervals.add(count, IntervalData({ { itemContent(it) } }, span ?: DefaultSpan))
        if (span != null) hasCustomSpans = true
    }
}

@Composable
private fun ItemRow(
    nColumns: Int,
    horizontalArrangement: Arrangement.Horizontal,
    rowContent: List<Pair<@Composable () -> Unit, Int>>
) {
    Layout(content = {
        rowContent.fastForEach {
            it.first.invoke()
        }
    }) { measurables, constraints ->
        check(measurables.size == rowContent.size)
        if (measurables.isEmpty()) {
            return@Layout layout(constraints.minWidth, constraints.minHeight) {}
        }

        val spacing = horizontalArrangement.spacing.roundToPx()
        val columnSize = max(constraints.maxWidth - spacing * (nColumns - 1), 0) / nColumns
        var remainder = max(
            constraints.maxWidth - columnSize * nColumns - spacing * (nColumns - 1),
            0
        )

        val placeables = measurables.fastMapIndexedNotNull { index, measurable ->
            val span = rowContent[index].second
            val remainderUsed = min(remainder, span)
            remainder -= remainderUsed
            val width = span * columnSize + remainderUsed + spacing * (span - 1)
            measurable.measure(Constraints.fixedWidth(width))
        }

        layout(constraints.maxWidth, placeables.fastMaxOfOrNull { it.height }!!) {
            var x = 0
            placeables.fastForEach { placeable ->
                placeable.placeRelative(x, 0)
                x += placeable.width + spacing
            }
        }
    }
}
