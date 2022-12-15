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

package androidx.compose.foundation.lazy.staggeredgrid

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.IntervalList
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan.Companion.FullLine

/**
 * Span defines a number of lanes (columns in vertical grid/rows in horizontal grid) for
 * staggered grid items.
 * Two variations of span are supported:
 *   - item taking a single lane ([SingleLane]);
 *   - item all lanes in line ([FullLine]).
 * By default, staggered grid uses [SingleLane] for all items.
 */
@ExperimentalFoundationApi
class StaggeredGridItemSpan private constructor(internal val value: Int) {
    companion object {
        /**
         * Force item to occupy whole line in cross axis.
         */
        val FullLine = StaggeredGridItemSpan(0)

        /**
         * Force item to use a single lane.
         */
        val SingleLane = StaggeredGridItemSpan(1)
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal class LazyStaggeredGridSpanProvider(
    val intervals: IntervalList<LazyStaggeredGridIntervalContent>
) {
    fun isFullSpan(itemIndex: Int): Boolean {
        if (itemIndex !in 0 until intervals.size) return false
        intervals[itemIndex].run {
            val span = value.span
            val localIndex = itemIndex - startIndex

            return span != null && span(localIndex) === FullLine
        }
    }
}