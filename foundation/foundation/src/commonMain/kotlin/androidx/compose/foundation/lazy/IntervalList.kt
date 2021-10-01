/*
 * Copyright 2021 The Android Open Source Project
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

internal class IntervalHolder<T>(
    val startIndex: Int,
    val size: Int,
    val content: T
)

internal interface IntervalList<T> {
    val intervals: List<IntervalHolder<T>>
    val totalSize: Int
}

internal class MutableIntervalList<T> : IntervalList<T> {
    private val _intervals = mutableListOf<IntervalHolder<T>>()
    override val intervals: List<IntervalHolder<T>> = _intervals
    override var totalSize = 0
        private set

    fun add(size: Int, content: T) {
        if (size == 0) {
            return
        }

        val interval = IntervalHolder(
            startIndex = totalSize,
            size = size,
            content = content
        )
        totalSize += size
        _intervals.add(interval)
    }
}

internal fun <T> IntervalList<T>.intervalForIndex(index: Int) =
    intervals[intervalIndexForItemIndex(index)]

internal fun <T> IntervalList<T>.intervalIndexForItemIndex(index: Int) =
    if (index < 0 || index >= totalSize) {
        throw IndexOutOfBoundsException("Index $index, size $totalSize")
    } else {
        findIndexOfHighestValueLesserThan(intervals, index)
    }

/**
 * Finds the index of the [list] which contains the highest value of [IntervalHolder.startIndex]
 * that is less than or equal to the given [value].
 */
private fun <T> findIndexOfHighestValueLesserThan(list: List<IntervalHolder<T>>, value: Int): Int {
    var left = 0
    var right = list.lastIndex

    while (left < right) {
        val middle = left + (right - left) / 2

        val middleValue = list[middle].startIndex
        if (middleValue == value) {
            return middle
        }

        if (middleValue < value) {
            left = middle + 1

            // Verify that the left will not be bigger than our value
            if (value < list[left].startIndex) {
                return middle
            }
        } else {
            right = middle - 1
        }
    }

    return left
}
