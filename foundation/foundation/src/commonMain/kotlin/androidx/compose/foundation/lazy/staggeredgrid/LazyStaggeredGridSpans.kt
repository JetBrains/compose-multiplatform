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

/**
 * Utility class to remember grid lane assignments (spans) in a sliding window relative to requested
 * item position (usually reflected by scroll position).
 * Remembers the maximum range of remembered items is reflected by [MaxCapacity], if index is beyond
 * the bounds, [anchor] moves to reflect new position.
 */
internal class LazyStaggeredGridSpans {
    private var anchor = 0
    private var spans = IntArray(16)

    /**
     * Sets given span for given item index.
     */
    fun setSpan(item: Int, span: Int) {
        require(item >= 0) { "Negative spans are not supported" }
        ensureValidIndex(item)
        spans[item - anchor] = span + 1
    }

    /**
     * Get span for given item index.
     * @return span previously recorded for given item or [Unset] if it doesn't exist.
     */
    fun getSpan(item: Int): Int {
        if (item < lowerBound() || item >= upperBound()) {
            return Unset
        }
        return spans[item - anchor] - 1
    }

    /**
     * @return upper bound of currently valid span range
     */
    /* @VisibleForTests */
    fun upperBound(): Int = anchor + spans.size

    /**
     * @return lower bound of currently valid span range
     */
    /* @VisibleForTests */
    fun lowerBound(): Int = anchor

    /**
     * Delete remembered span assignments.
     */
    fun reset() {
        spans.fill(0)
    }

    /**
     * Find the previous item relative to [item] set to target span
     * @return found item index or -1 if it doesn't exist.
     */
    fun findPreviousItemIndex(item: Int, target: Int): Int {
        for (i in (item - 1) downTo 0) {
            val span = getSpan(i)
            if (span == target || span == Unset) {
                return i
            }
        }
        return -1
    }

    /**
     * Find the next item relative to [item] set to target span
     * @return found item index or [upperBound] if it doesn't exist.
     */
    fun findNextItemIndex(item: Int, target: Int): Int {
        for (i in item + 1 until upperBound()) {
            val span = getSpan(i)
            if (span == target || span == Unset) {
                return i
            }
        }
        return upperBound()
    }

    private fun ensureValidIndex(requestedIndex: Int) {
        val requestedCapacity = requestedIndex - anchor

        if (requestedCapacity in 0 until MaxCapacity) {
            // simplest path - just grow array to given capacity
            ensureCapacity(requestedCapacity + 1)
        } else {
            // requested index is beyond current span bounds
            // rebase anchor so that requested index is in the middle of span array
            val oldAnchor = anchor
            anchor = maxOf(requestedIndex - (spans.size / 2), 0)
            var delta = anchor - oldAnchor

            if (delta >= 0) {
                // copy previous span data if delta is smaller than span size
                if (delta < spans.size) {
                    spans.copyInto(
                        spans,
                        destinationOffset = 0,
                        startIndex = delta,
                        endIndex = spans.size
                    )
                }
                // fill the rest of the spans with default values
                spans.fill(0, maxOf(0, spans.size - delta), spans.size)
            } else {
                delta = -delta
                // check if we can grow spans to match delta
                if (spans.size + delta < MaxCapacity) {
                    // grow spans and leave space in the start
                    ensureCapacity(spans.size + delta + 1, delta)
                } else {
                    // otherwise, just move data that fits
                    if (delta < spans.size) {
                        spans.copyInto(
                            spans,
                            destinationOffset = delta,
                            startIndex = 0,
                            endIndex = spans.size - delta
                        )
                    }
                    // fill the rest of the spans with default values
                    spans.fill(0, 0, minOf(spans.size, delta))
                }
            }
        }
    }

    private fun ensureCapacity(capacity: Int, newOffset: Int = 0) {
        require(capacity <= MaxCapacity) {
            "Requested span capacity $capacity is larger than max supported: $MaxCapacity!"
        }
        if (spans.size < capacity) {
            var newSize = spans.size
            while (newSize < capacity) newSize *= 2
            spans = spans.copyInto(IntArray(newSize), destinationOffset = newOffset)
        }
    }

    companion object {
        private const val MaxCapacity = 131_072 // Closest to 100_000, 2 ^ 17
        internal const val Unset = -1
    }
}