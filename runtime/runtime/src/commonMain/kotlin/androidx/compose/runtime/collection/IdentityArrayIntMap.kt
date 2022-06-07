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

package androidx.compose.runtime.collection

import androidx.compose.runtime.identityHashCode
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalContracts::class)
internal class IdentityArrayIntMap {
    @PublishedApi
    internal var size = 0

    @PublishedApi
    internal var keys: Array<Any?> = arrayOfNulls(4)

    @PublishedApi
    internal var values: IntArray = IntArray(4)

    operator fun get(key: Any): Int {
        val index = find(key)
        return if (index >= 0) values[index] else error("Key not found")
    }
    /**
     * Add [value] to the set and return `true` if it was added or `false` if it already existed.
     */
    fun add(key: Any, value: Int) {
        val index: Int
        if (size > 0) {
            index = find(key)
            if (index >= 0) {
                values[index] = value
                return
            }
        } else {
            index = -1
        }

        val insertIndex = -(index + 1)

        if (size == keys.size) {
            val newKeys = arrayOfNulls<Any>(keys.size * 2)
            val newValues = IntArray(keys.size * 2)
            keys.copyInto(
                destination = newKeys,
                destinationOffset = insertIndex + 1,
                startIndex = insertIndex,
                endIndex = size
            )
            values.copyInto(
                destination = newValues,
                destinationOffset = insertIndex + 1,
                startIndex = insertIndex,
                endIndex = size
            )
            keys.copyInto(
                destination = newKeys,
                endIndex = insertIndex
            )
            values.copyInto(
                destination = newValues,
                endIndex = insertIndex
            )
            keys = newKeys
            values = newValues
        } else {
            keys.copyInto(
                destination = keys,
                destinationOffset = insertIndex + 1,
                startIndex = insertIndex,
                endIndex = size
            )
            values.copyInto(
                destination = values,
                destinationOffset = insertIndex + 1,
                startIndex = insertIndex,
                endIndex = size
            )
        }
        keys[insertIndex] = key
        values[insertIndex] = value
        size++
    }

    /**
     * Remove [key] from the map.
     */
    fun remove(key: Any): Boolean {
        val index = find(key)
        if (index >= 0) {
            if (index < size - 1) {
                keys.copyInto(
                    destination = keys,
                    destinationOffset = index,
                    startIndex = index + 1,
                    endIndex = size
                )
                values.copyInto(
                    destination = values,
                    destinationOffset = index,
                    startIndex = index + 1,
                    endIndex = size
                )
            }
            size--
            keys[size] = null
            return true
        }
        return false
    }

    /**
     * Removes all values that match [predicate].
     */
    inline fun removeValueIf(predicate: (Any, Int) -> Boolean) {
        var destinationIndex = 0
        for (i in 0 until size) {
            @Suppress("UNCHECKED_CAST")
            val key = keys[i] as Any
            val value = values[i]
            if (!predicate(key, value)) {
                if (destinationIndex != i) {
                    keys[destinationIndex] = key
                    values[destinationIndex] = value
                }
                destinationIndex++
            }
        }
        for (i in destinationIndex until size) {
            keys[i] = null
        }
        size = destinationIndex
    }

    inline fun any(predicate: (Any, Int) -> Boolean): Boolean {
        for (i in 0 until size) {
            if (predicate(keys[i] as Any, values[i])) return true
        }
        return false
    }

    inline fun forEach(block: (Any, Int) -> Unit) {
        for (i in 0 until size) {
            block(keys[i] as Any, values[i])
        }
    }

    /**
     * Returns the index of [key] in the set or the negative index - 1 of the location where
     * it would have been if it had been in the set.
     */
    private fun find(key: Any?): Int {
        var low = 0
        var high = size - 1
        val valueIdentity = identityHashCode(key)

        while (low <= high) {
            val mid = (low + high).ushr(1)
            val midVal = keys[mid]
            val midIdentity = identityHashCode(midVal)
            when {
                midIdentity < valueIdentity -> low = mid + 1
                midIdentity > valueIdentity -> high = mid - 1
                midVal === key -> return mid
                else -> return findExactIndex(mid, key, valueIdentity)
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
            val v = keys[i]
            if (v === value) {
                return i
            }
            if (identityHashCode(v) != valueHash) {
                break // we've gone too far
            }
        }

        for (i in midIndex + 1 until size) {
            val v = keys[i]
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
}