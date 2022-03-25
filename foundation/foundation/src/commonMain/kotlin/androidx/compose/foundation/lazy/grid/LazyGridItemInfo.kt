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

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * Contains useful information about an individual item in lazy grids like [LazyVerticalGrid].
 *
 * @see LazyGridLayoutInfo
 */
sealed interface LazyGridItemInfo {
    /**
     * The index of the item in the grid.
     */
    val index: Int

    /**
     * The key of the item which was passed to the item() or items() function.
     */
    val key: Any

    /**
     * The offset of the item in pixels. It is relative to the top start of the lazy grid container.
     */
    val offset: IntOffset

    /**
     * The row occupied by the top start point of the item.
     * If this is unknown, for example while this item is animating to exit the viewport and is
     * still visible, the value will be [UnknownRow].
     */
    val row: Int

    /**
     * The column occupied by the top start point of the item.
     * If this is unknown, for example while this item is animating to exit the viewport and is
     * still visible, the value will be [UnknownColumn].
     */
    val column: Int

    /**
     * The pixel size of the item. Note that if you emit multiple layouts in the composable
     * slot for the item then this size will be calculated as the max of their sizes.
     */
    val size: IntSize

    companion object {
        /**
         * Possible value for [row], when they are unknown. This can happen when the item is
         * visible while animating to exit the viewport.
         */
        const val UnknownRow = -1
        /**
         * Possible value for [column], when they are unknown. This can happen when the item is
         * visible while animating to exit the viewport.
         */
        const val UnknownColumn = -1
    }
}
