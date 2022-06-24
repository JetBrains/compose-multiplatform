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

package androidx.compose.runtime.collection

import androidx.compose.runtime.identityHashCode
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * A set of values using an array as the backing store, ordered using [identityHashCode] for
 * both sorting and uniqueness.
 */
@OptIn(ExperimentalContracts::class)
internal class IdentityArraySet<T : Any> : Set<T> {
    override var size = 0

    @PublishedApi
    internal var values: Array<Any?> = arrayOfNulls(16)

    /**
     * Returns true if the set contains [element]
     */
    override operator fun contains(element: T) = find(element) >= 0

    /**
     * Return the item at the given [index].
     */
    operator fun get(index: Int): T {
        @Suppress("UNCHECKED_CAST")
        return values[index] as T
    }

    /**
     * Add [value] to the set and return `true` if it was added or `false` if it already existed.
     */
    fun add(value: T): Boolean {
        val index: Int
        if (size > 0) {
            index = find(value)

            if (index >= 0) {
                return false
            }
        } else {
            index = -1
        }

        val insertIndex = -(index + 1)

        if (size == values.size) {
            val newSorted = arrayOfNulls<Any>(values.size * 2)
            values.copyInto(
                destination = newSorted,
                destinationOffset = insertIndex + 1,
                startIndex = insertIndex,
                endIndex = size
            )
            values.copyInto(
                destination = newSorted,
                endIndex = insertIndex
            )
            values = newSorted
        } else {
            values.copyInto(
                destination = values,
                destinationOffset = insertIndex + 1,
                startIndex = insertIndex,
                endIndex = size
            )
        }
        values[insertIndex] = value
        size++
        return true
    }

    /**
     * Remove all values from the set.
     */
    fun clear() {
        for (i in 0 until size) {
            values[i] = null
        }

        size = 0
    }

    /**
     * Call [block] for all items in the set.
     */
    inline fun fastForEach(block: (T) -> Unit) {
        contract { callsInPlace(block) }
        for (i in 0 until size) {
            block(this[i])
        }
    }

    /**
     * Return true if the set is empty.
     */
    override fun isEmpty() = size == 0

    /**
     * Returns true if the set is not empty.
     */
    fun isNotEmpty() = size > 0

    /**
     * Remove [value] from the set.
     */
    fun remove(value: T): Boolean {
        val index = find(value)
        if (index >= 0) {
            if (index < size - 1) {
                values.copyInto(
                    destination = values,
                    destinationOffset = index,
                    startIndex = index + 1,
                    endIndex = size
                )
            }
            size--
            values[size] = null
            return true
        }
        return false
    }

    /**
     * Removes all values that match [predicate].
     */
    inline fun removeValueIf(predicate: (T) -> Boolean) {
        var destinationIndex = 0
        for (i in 0 until size) {
            @Suppress("UNCHECKED_CAST")
            val item = values[i] as T
            if (!predicate(item)) {
                if (destinationIndex != i) {
                    values[destinationIndex] = item
                }
                destinationIndex++
            }
        }
        for (i in destinationIndex until size) {
            values[i] = null
        }
        size = destinationIndex
    }

    /**
     * Returns the index of [value] in the set or the negative index - 1 of the location where
     * it would have been if it had been in the set.
     */
    private fun find(value: Any?): Int {
        var low = 0
        var high = size - 1
        val valueIdentity = identityHashCode(value)

        while (low <= high) {
            val mid = (low + high).ushr(1)
            val midVal = get(mid)
            val midIdentity = identityHashCode(midVal)
            when {
                midIdentity < valueIdentity -> low = mid + 1
                midIdentity > valueIdentity -> high = mid - 1
                midVal === value -> return mid
                else -> return findExactIndex(mid, value, valueIdentity)
            }
        }
        return -(low + 1)
    }

    /**
     * When multiple items share the same [identityHashCode], then we must find the specific
     * index of the target item. This method assumes that [midIndex] has already been checked
     * for an exact match for [value], but will look at nearby values to find the exact item index.
     * If no match is found, the negative index - 1 of the position in which it would be will
     * be returned, which is always after the last item with the same [identityHashCode].
     */
    private fun findExactIndex(midIndex: Int, value: Any?, valueHash: Int): Int {
        // hunt down first
        for (i in midIndex - 1 downTo 0) {
            val v = values[i]
            if (v === value) {
                return i
            }
            if (identityHashCode(v) != valueHash) {
                break // we've gone too far
            }
        }

        for (i in midIndex + 1 until size) {
            val v = values[i]
            if (v === value) {
                return i
            }
            if (identityHashCode(v) != valueHash) {
                // We've gone too far. We should insert here.
                return -(i + 1)
            }
        }

        // We should insert at the end
        return -(size + 1)
    }

    /**
     * Return true if all elements of [elements] are in the set.
     */
    override fun containsAll(elements: Collection<T>) = elements.all { contains(it) }

    /**
     * Return an iterator for the set.
     */
    @Suppress("UNCHECKED_CAST")
    override fun iterator(): Iterator<T> = object : Iterator<T> {
        var index = 0
        override fun hasNext(): Boolean = index < size
        override fun next(): T = this@IdentityArraySet.values[index++] as T
    }
}
