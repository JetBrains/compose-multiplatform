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
 * Utility class to remember grid lane assignments in a sliding window relative to requested
 * item position (usually reflected by scroll position).
 * Remembers the maximum range of remembered items is reflected by [MaxCapacity], if index is beyond
 * the bounds, [anchor] moves to reflect new position.
 */
internal class LazyStaggeredGridLaneInfo {
    private var anchor = 0
    private var lanes = IntArray(16)
    private val spannedItems = ArrayDeque<SpannedItem>()

    private class SpannedItem(val index: Int, var gaps: IntArray)

    /**
     * Sets given lane for given item index.
     */
    fun setLane(itemIndex: Int, lane: Int) {
        require(itemIndex >= 0) { "Negative lanes are not supported" }
        ensureValidIndex(itemIndex)
        lanes[itemIndex - anchor] = lane + 1
    }

    /**
     * Get lane for given item index.
     * @return lane previously recorded for given item or [Unset] if it doesn't exist.
     */
    fun getLane(itemIndex: Int): Int {
        if (itemIndex < lowerBound() || itemIndex >= upperBound()) {
            return Unset
        }
        return lanes[itemIndex - anchor] - 1
    }

    /**
     * Checks whether item can be in the target lane
     * @param itemIndex item to check lane for
     * @param targetLane lane it should belong to
     */
    fun assignedToLane(itemIndex: Int, targetLane: Int): Boolean {
        val lane = getLane(itemIndex)
        return lane == targetLane || lane == Unset || lane == FullSpan
    }

    /**
     * @return upper bound of currently valid item range
     */
    /* @VisibleForTests */
    fun upperBound(): Int = anchor + lanes.size

    /**
     * @return lower bound of currently valid item range
     */
    /* @VisibleForTests */
    fun lowerBound(): Int = anchor

    /**
     * Delete remembered lane assignments.
     */
    fun reset() {
        lanes.fill(0)
        spannedItems.clear()
    }

    /**
     * Find the previous item relative to [itemIndex] set to target lane
     * @return found item index or -1 if it doesn't exist.
     */
    fun findPreviousItemIndex(itemIndex: Int, targetLane: Int): Int {
        for (i in (itemIndex - 1) downTo 0) {
            if (assignedToLane(i, targetLane)) {
                return i
            }
        }
        return -1
    }

    /**
     * Find the next item relative to [itemIndex] set to target lane
     * @return found item index or [upperBound] if it doesn't exist.
     */
    fun findNextItemIndex(itemIndex: Int, targetLane: Int): Int {
        for (i in itemIndex + 1 until upperBound()) {
            if (assignedToLane(i, targetLane)) {
                return i
            }
        }
        return upperBound()
    }

    fun ensureValidIndex(requestedIndex: Int) {
        val requestedCapacity = requestedIndex - anchor

        if (requestedCapacity in 0 until MaxCapacity) {
            // simplest path - just grow array to given capacity
            ensureCapacity(requestedCapacity + 1)
        } else {
            // requested index is beyond current span bounds
            // rebase anchor so that requested index is in the middle of span array
            val oldAnchor = anchor
            anchor = maxOf(requestedIndex - (lanes.size / 2), 0)
            var delta = anchor - oldAnchor

            if (delta >= 0) {
                // copy previous span data if delta is smaller than span size
                if (delta < lanes.size) {
                    lanes.copyInto(
                        lanes,
                        destinationOffset = 0,
                        startIndex = delta,
                        endIndex = lanes.size
                    )
                }
                // fill the rest of the spans with default values
                lanes.fill(0, maxOf(0, lanes.size - delta), lanes.size)
            } else {
                delta = -delta
                // check if we can grow spans to match delta
                if (lanes.size + delta < MaxCapacity) {
                    // grow spans and leave space in the start
                    ensureCapacity(lanes.size + delta + 1, delta)
                } else {
                    // otherwise, just move data that fits
                    if (delta < lanes.size) {
                        lanes.copyInto(
                            lanes,
                            destinationOffset = delta,
                            startIndex = 0,
                            endIndex = lanes.size - delta
                        )
                    }
                    // fill the rest of the spans with default values
                    lanes.fill(0, 0, minOf(lanes.size, delta))
                }
            }
        }

        // ensure full item spans beyond saved index are forgotten to save memory

        while (spannedItems.isNotEmpty() && spannedItems.first().index < lowerBound()) {
            spannedItems.removeFirst()
        }

        while (spannedItems.isNotEmpty() && spannedItems.last().index > upperBound()) {
            spannedItems.removeLast()
        }
    }

    fun setGaps(itemIndex: Int, gaps: IntArray?) {
        val foundIndex = spannedItems.binarySearchBy(itemIndex) { it.index }
        if (foundIndex < 0) {
            if (gaps == null) {
                return
            }
            // not found, insert new element
            val insertionIndex = -(foundIndex + 1)
            spannedItems.add(insertionIndex, SpannedItem(itemIndex, gaps))
        } else {
            if (gaps == null) {
                // found, but gaps are reset, remove item
                spannedItems.removeAt(foundIndex)
            } else {
                // found, update gaps
                spannedItems[foundIndex].gaps = gaps
            }
        }
    }

    fun getGaps(itemIndex: Int): IntArray? {
        val foundIndex = spannedItems.binarySearchBy(itemIndex) { it.index }
        return spannedItems.getOrNull(foundIndex)?.gaps
    }

    private fun ensureCapacity(capacity: Int, newOffset: Int = 0) {
        require(capacity <= MaxCapacity) {
            "Requested item capacity $capacity is larger than max supported: $MaxCapacity!"
        }
        if (lanes.size < capacity) {
            var newSize = lanes.size
            while (newSize < capacity) newSize *= 2
            lanes = lanes.copyInto(IntArray(newSize), destinationOffset = newOffset)
        }
    }

    companion object {
        private const val MaxCapacity = 131_072 // Closest to 100_000, 2 ^ 17
        internal const val Unset = -1
        internal const val FullSpan = -2
    }
}