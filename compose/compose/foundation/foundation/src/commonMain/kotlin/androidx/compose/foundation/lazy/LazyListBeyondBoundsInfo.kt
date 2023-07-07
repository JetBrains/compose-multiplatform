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

package androidx.compose.foundation.lazy

import androidx.compose.runtime.collection.mutableVectorOf

/**
 * This data structure is used to save information about the number of "beyond bounds items"
 * that we want to compose. These items are not within the visible bounds of the lazylist,
 * but we compose them because they are explicitly requested through the
 * [beyond bounds layout API][androidx.compose.ui.layout.BeyondBoundsLayout].
 *
 * When the LazyList receives a
 * [searchBeyondBounds][androidx.compose.ui.layout.BeyondBoundsLayout.searchBeyondBounds] request to
 * layout items beyond visible bounds, it creates an instance of [LazyListBeyondBoundsInfo] by using
 * the [addInterval] function. This returns the interval of items that are currently composed,
 * and we can edit this interval to control the number of beyond bounds items.
 *
 * There can be multiple intervals created at the same time, and LazyList merges all the
 * intervals to calculate the effective beyond bounds items.
 *
 * The [beyond bounds layout API][androidx.compose.ui.layout.BeyondBoundsLayout] is designed to be
 * synchronous, so once you are done using the items, call [removeInterval] to remove
 * the extra items you had requested.
 *
 * Note that when you clear an interval, the items in that interval might not be cleared right
 * away if another interval was created that has the same items. This is done to support two use
 * cases:
 *
 * 1. To allow items to be pinned while they are being scrolled into view.
 *
 * 2. To allow users to call
 * [searchBeyondBounds][androidx.compose.ui.layout.BeyondBoundsLayout.searchBeyondBounds]
 * from within the completion block of another searchBeyondBounds call.
 */
internal class LazyListBeyondBoundsInfo {
    private val beyondBoundsItems = mutableVectorOf<Interval>()

    /**
     * Create a beyond bounds interval. This can be used to specify which composed items we want to
     * retain. For instance, it can be used to force the measuring of items that are beyond the
     * visible bounds of a lazy list.
     *
     * @param start The starting index (inclusive) for this interval.
     * @param end The ending index (inclusive) for this interval.
     *
     * @return An interval that specifies which items we want to retain.
     */
    fun addInterval(start: Int, end: Int): Interval {
        return Interval(start, end).apply {
            beyondBoundsItems.add(this)
        }
    }

    /**
     * Clears the specified interval. Use this to remove the interval created by [addInterval].
     */
    fun removeInterval(interval: Interval) {
        beyondBoundsItems.remove(interval)
    }

    /**
     * Returns true if there are beyond bounds intervals.
     */
    fun hasIntervals(): Boolean = beyondBoundsItems.isNotEmpty()

    /**
     *  The effective start index after merging all the current intervals.
     */
    val start: Int
        get() {
            var minIndex = beyondBoundsItems.first().start
            beyondBoundsItems.forEach {
                if (it.start < minIndex) {
                    minIndex = it.start
                }
            }
            require(minIndex >= 0)
            return minIndex
        }

    /**
     *  The effective end index after merging all the current intervals.
     */
    val end: Int
        get() {
            var maxIndex = beyondBoundsItems.first().end
            beyondBoundsItems.forEach {
                if (it.end > maxIndex) {
                    maxIndex = it.end
                }
            }
            return maxIndex
        }

    /**
     * The Interval used to implement [LazyListBeyondBoundsInfo].
     */
    internal data class Interval(
        /** The start index for the interval. */
        val start: Int,

        /** The end index for the interval. */
        val end: Int
    ) {
        init {
            require(start >= 0)
            require(end >= start)
        }
    }
}
