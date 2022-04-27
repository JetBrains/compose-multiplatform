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
import androidx.compose.runtime.Immutable

/**
 * Represents the span of an item in a [LazyVerticalGrid].
 */
@Immutable
@kotlin.jvm.JvmInline
value class GridItemSpan internal constructor(private val packedValue: Long) {
    /**
     * The span of the item on the current line. This will be the horizontal span for items of
     * [LazyVerticalGrid].
     */
    @ExperimentalFoundationApi
    val currentLineSpan: Int get() = packedValue.toInt()
}

/**
 * Creates a [GridItemSpan] with a specified [currentLineSpan]. This will be the horizontal span
 * for an item of a [LazyVerticalGrid].
 */
fun GridItemSpan(currentLineSpan: Int) = GridItemSpan(currentLineSpan.toLong())

/**
 * Scope of lambdas used to calculate the spans of items in lazy grids.
 */
@LazyGridScopeMarker
sealed interface LazyGridItemSpanScope {
    /**
     * The max current line (horizontal for vertical grids) the item can occupy, such that
     * it will be positioned on the current line.
     *
     * For example if [LazyVerticalGrid] has 3 columns this value will be 3 for the first cell in
     * the line, 2 for the second cell, and 1 for the last one. If you return a span count larger
     * than [maxCurrentLineSpan] this means we can't fit this cell into the current line, so the
     * cell will be positioned on the next line.
     */
    val maxCurrentLineSpan: Int

    /**
     * The max line span (horizontal for vertical grids) an item can occupy. This will be the
     * number of columns in vertical grids or the number of rows in horizontal grids.
     *
     * For example if [LazyVerticalGrid] has 3 columns this value will be 3 for each cell.
     */
    val maxLineSpan: Int
}
