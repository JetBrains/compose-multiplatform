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

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.unit.IntSize
import kotlin.jvm.JvmDefaultWithCompatibility

/**
 * Contains useful information about the currently displayed layout state of lazy lists like
 * [LazyColumn] or [LazyRow]. For example you can get the list of currently displayed item.
 *
 * Use [LazyListState.layoutInfo] to retrieve this
 */
@JvmDefaultWithCompatibility
interface LazyListLayoutInfo {
    /**
     * The list of [LazyListItemInfo] representing all the currently visible items.
     */
    val visibleItemsInfo: List<LazyListItemInfo>

    /**
     * The start offset of the layout's viewport in pixels. You can think of it as a minimum offset
     * which would be visible. Usually it is 0, but it can be negative if non-zero [beforeContentPadding]
     * was applied as the content displayed in the content padding area is still visible.
     *
     * You can use it to understand what items from [visibleItemsInfo] are fully visible.
     */
    val viewportStartOffset: Int

    /**
     * The end offset of the layout's viewport in pixels. You can think of it as a maximum offset
     * which would be visible. It is the size of the lazy list layout minus [beforeContentPadding].
     *
     * You can use it to understand what items from [visibleItemsInfo] are fully visible.
     */
    val viewportEndOffset: Int

    /**
     * The total count of items passed to [LazyColumn] or [LazyRow].
     */
    val totalItemsCount: Int

    /**
     * The size of the viewport in pixels. It is the lazy list layout size including all the
     * content paddings.
     */
    val viewportSize: IntSize get() = IntSize.Zero

    /**
     * The orientation of the lazy list.
     */
    val orientation: Orientation get() = Orientation.Vertical

    /**
     * True if the direction of scrolling and layout is reversed.
     */
    val reverseLayout: Boolean get() = false

    /**
     * The content padding in pixels applied before the first item in the direction of scrolling.
     * For example it is a top content padding for LazyColumn with reverseLayout set to false.
     */
    val beforeContentPadding: Int get() = 0

    /**
     * The content padding in pixels applied after the last item in the direction of scrolling.
     * For example it is a bottom content padding for LazyColumn with reverseLayout set to false.
     */
    val afterContentPadding: Int get() = 0
}
