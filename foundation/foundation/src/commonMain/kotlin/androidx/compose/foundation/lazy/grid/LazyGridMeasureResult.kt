/*
 * Copyright 2021 The Android Open Source Project
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
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.unit.IntSize

/**
 * The result of the measure pass for lazy list layout.
 */
@OptIn(ExperimentalFoundationApi::class)
internal class LazyGridMeasureResult(
    // properties defining the scroll position:
    /** The new first visible line of items.*/
    val firstVisibleLine: LazyMeasuredLine?,
    /** The new value for [LazyGridState.firstVisibleItemScrollOffset].*/
    val firstVisibleLineScrollOffset: Int,
    /** True if there is some space available to continue scrolling in the forward direction.*/
    val canScrollForward: Boolean,
    /** The amount of scroll consumed during the measure pass.*/
    val consumedScroll: Float,
    /** MeasureResult defining the layout.*/
    measureResult: MeasureResult,
    // properties representing the info needed for LazyListLayoutInfo:
    /** see [LazyGridLayoutInfo.visibleItemsInfo] */
    override val visibleItemsInfo: List<LazyGridItemInfo>,
    /** see [LazyGridLayoutInfo.viewportStartOffset] */
    override val viewportStartOffset: Int,
    /** see [LazyGridLayoutInfo.viewportEndOffset] */
    override val viewportEndOffset: Int,
    /** see [LazyGridLayoutInfo.totalItemsCount] */
    override val totalItemsCount: Int,
    /** see [LazyGridLayoutInfo.reverseLayout] */
    override val reverseLayout: Boolean,
    /** see [LazyGridLayoutInfo.orientation] */
    override val orientation: Orientation,
    /** see [LazyGridLayoutInfo.afterContentPadding] */
    override val afterContentPadding: Int
) : LazyGridLayoutInfo, MeasureResult by measureResult {
    override val viewportSize: IntSize
        get() = IntSize(width, height)
    override val beforeContentPadding: Int get() = -viewportStartOffset
}
