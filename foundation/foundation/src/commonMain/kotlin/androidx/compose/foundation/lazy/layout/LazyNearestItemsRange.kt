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

/**
 * Returns a range of indexes which contains at least [extraItemCount] items near
 * the first visible item. It is optimized to return the same range for small changes in the
 * [firstVisibleItem] value so we do not regenerate the map on each scroll.
 *
 * It uses the idea of sliding window as an optimization, so user can scroll up to this number of
 * items until we have to regenerate the key to index map.
 *
 * @param firstVisibleItem currently visible item
 * @param slidingWindowSize size of the sliding window for the nearest item calculation
 * @param extraItemCount minimum amount of items near the first item we want to have mapping for.
 */
internal fun calculateNearestItemsRange(
    firstVisibleItem: Int,
    slidingWindowSize: Int,
    extraItemCount: Int
): IntRange {
    val slidingWindowStart = slidingWindowSize * (firstVisibleItem / slidingWindowSize)

    val start = maxOf(slidingWindowStart - extraItemCount, 0)
    val end = slidingWindowStart + slidingWindowSize + extraItemCount
    return start until end
}