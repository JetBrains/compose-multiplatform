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

import androidx.compose.ui.layout.Placeable

/**
 * The result of the measure pass for lazy list layout.
 */
internal class LazyListMeasureResult(
    // properties defining the scroll position:
    /** The new first visible item.*/
    val firstVisibleItem: LazyMeasuredItem?,
    /** The new value for [LazyListState.firstVisibleItemScrollOffset].*/
    val firstVisibleItemScrollOffset: Int,
    /** True if there is some space available to continue scrolling in the forward direction.*/
    val canScrollForward: Boolean,
    /** The amount of scroll consumed during the measure pass.*/
    val consumedScroll: Float,
    /** List of items which were composed, but are not a part of [visibleItemsInfo].*/
    val composedButNotVisibleItems: List<LazyMeasuredItem>?,
    // properties to be used by the Layout's measure result
    /** The calculated layout width */
    val layoutWidth: Int,
    /** The calculated layout height */
    val layoutHeight: Int,
    /** The placement block */
    val placementBlock: Placeable.PlacementScope.() -> Unit,
    // properties representing the info needed for LazyListLayoutInfo
    /** see [LazyListLayoutInfo.visibleItemsInfo] */
    override val visibleItemsInfo: List<LazyListItemInfo>,
    /** see [LazyListLayoutInfo.viewportStartOffset] */
    override val viewportStartOffset: Int,
    /** see [LazyListLayoutInfo.viewportEndOffset] */
    override val viewportEndOffset: Int,
    /** see [LazyListLayoutInfo.totalItemsCount] */
    override val totalItemsCount: Int,
) : LazyListLayoutInfo
