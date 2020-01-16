/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.frames

import androidx.compose.Immutable

/**
 * An implementation of a bit set that that is optimized around for the top 128 bits and
 * sparse access for bits below that. Is is O(1) to set, clear and get the bit value of the top 128
 * values of the set. Below lowerBound it is O(log N) to get a bit and O(N) to set or clear a bit
 * where N is the number of bits set below lowerBound. Clearing a cleared bit or setting a set bit
 * is the same complexity of get.
 *
 * The set is immutable and calling the set or clear methods produce the modified set, leaving the
 * previous set unmodified. If the operation does not modify the set, such as setting a set bit or
 * clearing a clear bit, returns the same instance.
 *
 * This class is highly biased to a bits being set at the top 128 values of the range and bits lower
 * than the that range to be mostly or completely clear.
 *
 * This class does not implement equals intentionally. Equals is hard and expensive as a normal form
 * for a particular set is not guaranteed (that is, two sets that compare equal might have
 * different field values). As Frames does not need this, it is not implemented.
 */
@Immutable
internal class FrameIdSet private constructor(
    // Bit set from (lowerBound + 64)-(lowerBound+127) of the set
    private val upperSet: Long,
    // Bit set from (lowerBound)-(lowerBound+63) of the set
    private val lowerSet: Long,
    // Lower bound of the bit set. All values above lowerBound+127 are clear.
    // Values between lowerBound and lowerBound+127 are recorded in lowerSet and upperSet
    private val lowerBound: Int,
    // A sorted array of the index of bits set below lowerBound
    private val belowBound: IntArray?
) {

    /**
     * The the value of the bit at index [bit]
     */
    fun get(bit: Int): Boolean {
        val offset = bit - lowerBound
        if (offset >= 0 && offset < Long.SIZE_BITS) {
            return (1L shl offset) and lowerSet != 0L
        } else if (offset >= Long.SIZE_BITS && offset < Long.SIZE_BITS * 2) {
            return (1L shl (offset - Long.SIZE_BITS)) and upperSet != 0L
        } else if (offset > 0) {
            return false
        } else return belowBound?.let {
            it.binarySearch(bit) >= 0
        } ?: false
    }

    /**
     * Produce a copy of this set with the addition of the bit at index [bit] set.
     */
    fun set(bit: Int): FrameIdSet {
        val offset = bit - lowerBound
        if (offset >= 0 && offset < Long.SIZE_BITS) {
            val mask = 1L shl offset
            if (lowerSet and mask == 0L) {
                return FrameIdSet(
                    upperSet = upperSet,
                    lowerSet = lowerSet or mask,
                    lowerBound = lowerBound,
                    belowBound = belowBound
                )
            }
        } else if (offset >= Long.SIZE_BITS && offset < Long.SIZE_BITS * 2) {
            val mask = 1L shl (offset - Long.SIZE_BITS)
            if (upperSet and mask == 0L) {
                return FrameIdSet(
                    upperSet = upperSet or mask,
                    lowerSet = lowerSet,
                    lowerBound = lowerBound,
                    belowBound = belowBound
                )
            }
        } else if (offset >= Long.SIZE_BITS * 2) {
            if (!get(bit)) {
                // Shift the bit array down
                var newUpperSet = upperSet
                var newLowerSet = lowerSet
                var newLowerBound = lowerBound
                var newBelowBound: MutableList<Int>? = null
                val targetLowerBound = (bit + 1) / Long.SIZE_BITS * Long.SIZE_BITS
                while (newLowerBound < targetLowerBound) {
                    // Shift the lower set into the array
                    if (newLowerSet != 0L) {
                        if (newBelowBound == null)
                            newBelowBound = mutableListOf<Int>().apply {
                                belowBound?.let {
                                    it.forEach { this.add(it) }
                                }
                            }
                        repeat(Long.SIZE_BITS) { bitOffset ->
                            if (newLowerSet and (1L shl bitOffset) != 0L) {
                                newBelowBound.add(bitOffset + newLowerBound)
                            }
                        }
                    }
                    if (newUpperSet == 0L) {
                        newLowerBound = targetLowerBound
                        newLowerSet = 0L
                        break
                    }
                    newLowerSet = newUpperSet
                    newUpperSet = 0
                    newLowerBound += Long.SIZE_BITS
                }

                return FrameIdSet(
                    newUpperSet,
                    newLowerSet,
                    newLowerBound,
                    newBelowBound?.toIntArray() ?: belowBound
                ).set(bit)
            }
        } else {
            val array = belowBound
                ?: return FrameIdSet(upperSet, lowerSet, lowerBound, intArrayOf(bit))

            val location = array.binarySearch(bit)
            if (location < 0) {
                val insertLocation = -(location + 1)
                val newSize = array.size + 1
                val newBelowBound = IntArray(newSize)
                array.copyInto(
                    destination = newBelowBound,
                    destinationOffset = 0,
                    startIndex = 0,
                    endIndex = insertLocation
                )
                array.copyInto(
                    destination = newBelowBound,
                    destinationOffset = insertLocation + 1,
                    startIndex = insertLocation,
                    endIndex = newSize - 1
                )
                newBelowBound[insertLocation] = bit
                return FrameIdSet(upperSet, lowerSet, lowerBound, newBelowBound)
            }
        }

        // No changes
        return this
    }

    /**
     * Produce a copy of this set with the addition of the bit at index [bit] cleared.
     */
    fun clear(bit: Int): FrameIdSet {
        val offset = bit - lowerBound
        if (offset >= 0 && offset < Long.SIZE_BITS) {
            val mask = 1L shl offset
            if (lowerSet and mask != 0L) {
                return FrameIdSet(
                    upperSet = upperSet,
                    lowerSet = lowerSet and mask.inv(),
                    lowerBound = lowerBound,
                    belowBound = belowBound
                )
            }
        } else if (offset >= Long.SIZE_BITS && offset < Long.SIZE_BITS * 2) {
            val mask = 1L shl (offset - Long.SIZE_BITS)
            if (upperSet and mask != 0L) {
                return FrameIdSet(
                    upperSet = upperSet and mask.inv(),
                    lowerSet = lowerSet,
                    lowerBound = lowerBound,
                    belowBound = belowBound
                )
            }
        } else if (offset < 0) {
            val array = belowBound
            if (array != null) {
                val location = array.binarySearch(bit)
                if (location >= 0) {
                    val newSize = array.size - 1
                    if (newSize == 0) {
                        return FrameIdSet(upperSet, lowerSet, lowerBound, null)
                    }
                    val newBelowBound = IntArray(newSize)
                    if (location > 0) {
                        array.copyInto(
                            destination = newBelowBound,
                            destinationOffset = 0,
                            startIndex = 0,
                            endIndex = location
                        )
                    }
                    if (location < newSize) {
                        array.copyInto(
                            destination = newBelowBound,
                            destinationOffset = location,
                            startIndex = location + 1,
                            endIndex = newSize + 1
                        )
                    }
                    return FrameIdSet(upperSet, lowerSet, lowerBound, newBelowBound)
                }
            }
        }

        return this
    }

    companion object {
        /**
         * An empty frame it set
         */
        val EMPTY = FrameIdSet(0, 0, 0, null)
    }
}

internal fun IntArray.binarySearch(value: Int): Int {
    var low = 0
    var high = size - 1

    while (low <= high) {
        val mid = (low + high).ushr(1)
        val midVal = get(mid)
        if (value > midVal)
            low = mid + 1
        else if (value < midVal)
            high = mid - 1
        else
            return mid
    }
    return -(low + 1)
}
