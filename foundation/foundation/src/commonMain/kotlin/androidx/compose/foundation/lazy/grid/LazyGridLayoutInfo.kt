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
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.unit.IntSize

/**
 * Contains useful information about the currently displayed layout state of lazy grids like
 * [LazyVerticalGrid]. For example you can get the list of currently displayed items.
 *
 * Use [LazyGridState.layoutInfo] to retrieve this
 */
@ExperimentalFoundationApi
interface LazyGridLayoutInfo {
    /**
     * The list of [LazyGridItemInfo] representing all the currently visible items.
     */
    val visibleItemsInfo: List<LazyGridItemInfo>

    /**
     * The start offset of the layout's viewport. You can think of it as a minimum offset which
     * would be visible. Usually it is 0, but it can be negative if a content padding was applied
     * as the content displayed in the content padding area is still visible.
     *
     * You can use it to understand what items from [visibleItemsInfo] are fully visible.
     */
    val viewportStartOffset: Int

    /**
     * The end offset of the layout's viewport. You can think of it as a maximum offset which
     * would be visible. Usually it is a size of the lazy grid container minus a content padding.
     *
     * You can use it to understand what items from [visibleItemsInfo] are fully visible.
     */
    val viewportEndOffset: Int

    /**
     * The total count of items passed to [LazyVerticalGrid].
     */
    val totalItemsCount: Int

    /**
     * The size of the viewport. It is the lazy grid layout size including all the content paddings.
     */
    val viewportSize: IntSize

    /**
     * The orientation of the lazy grid.
     */
    val orientation: Orientation

    /**
     * True if the direction of scrolling and layout is reversed.
     */
    val reverseLayout: Boolean
}
