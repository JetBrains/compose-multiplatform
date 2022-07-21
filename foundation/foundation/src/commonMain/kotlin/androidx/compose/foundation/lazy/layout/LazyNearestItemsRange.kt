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

package androidx.compose.foundation.lazy.layout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.structuralEqualityPolicy

/**
 * Calculate and memoize range of indexes which contains at least [extraItemCount] items near
 * the first visible item. It is optimized to return the same range for small changes in the
 * firstVisibleItem value so we do not regenerate the map on each scroll.
 *
 * @param firstVisibleItemIndex Provider of the first item index currently visible on screen.
 * @param slidingWindowSize Number items user can scroll up to this number of items until we have to
 * regenerate item mapping.
 * @param extraItemCount  The minimum amount of items near the first visible item we want
 * to have mapping for.
 * @return range of indexes with items near current the first visible position.
 */
@ExperimentalFoundationApi
@Composable
fun rememberLazyNearestItemsRangeState(
    firstVisibleItemIndex: () -> Int,
    slidingWindowSize: () -> Int,
    extraItemCount: () -> Int
): State<IntRange> =
    remember(firstVisibleItemIndex, slidingWindowSize, extraItemCount) {
        derivedStateOf(structuralEqualityPolicy()) {
            calculateNearestItemsRange(
                firstVisibleItemIndex(),
                slidingWindowSize(),
                extraItemCount()
            )
        }
    }

/**
 * Returns a range of indexes which contains at least [extraItemCount] items near
 * the first visible item. It is optimized to return the same range for small changes in the
 * firstVisibleItem value so we do not regenerate the map on each scroll.
 */
private fun calculateNearestItemsRange(
    firstVisibleItem: Int,
    slidingWindowSize: Int,
    extraItemCount: Int
): IntRange {
    val slidingWindowStart = slidingWindowSize * (firstVisibleItem / slidingWindowSize)

    val start = maxOf(slidingWindowStart - extraItemCount, 0)
    val end = slidingWindowStart + slidingWindowSize + extraItemCount
    return start until end
}