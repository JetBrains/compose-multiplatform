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

internal class IdentityArrayMap<Key : Any, Value : Any?>(capacity: Int = 16) {
    internal var keys = arrayOfNulls<Any?>(capacity)
    internal var values = arrayOfNulls<Any?>(capacity)
    internal var size = 0

    fun isEmpty() = size == 0
    fun isNotEmpty() = size > 0

    operator fun contains(key: Key): Boolean = find(key) >= 0

    operator fun get(key: Key): Value? {
        val index = find(key)
        @Suppress("UNCHECKED_CAST")
        return if (index >= 0) values[index] as Value else null
    }

    operator fun set(key: Key, value: Value) {
        val index = find(key)
        if (index >= 0) {
            values[index] = value
        } else {
            val insertIndex = -(index + 1)
            val resize = size == keys.size
            val destKeys = if (resize) {
                arrayOfNulls(size * 2)
            } else keys
            keys.copyInto(
                destination = destKeys,
                destinationOffset = insertIndex + 1,
                startIndex = insertIndex,
                endIndex = size
            )
            if (resize) {
                keys.copyInto(
                    destination = destKeys,
                    endIndex = insertIndex
                )
            }
            destKeys[insertIndex] = key
            keys = destKeys
            val destValues = if (resize) {
                arrayOfNulls(size * 2)
            } else values
            values.copyInto(
                destination = destValues,
                destinationOffset = insertIndex + 1,
                startIndex = insertIndex,
                endIndex = size
            )
            if (resize) {
                values.copyInto(
                    destination = destValues,
                    endIndex = insertIndex
                )
            }
            destValues[insertIndex] = value
            values = destValues
            size++
        }
    }

    fun remove(key: Key): Boolean {
        val index = find(key)
        if (index >= 0) {
            val size = size
            val keys = keys
            val values = values
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
            val newSize = size - 1
            keys[newSize] = null
            values[newSize] = null
            this.size = newSize
            return true
        }
        return false
    }

    @Suppress("UNCHECKED_CAST")
    inline fun removeValueIf(block: (value: Value) -> Boolean) {
        var current = 0
        for (index in 0 until size) {
            val value = values[index] as Value
            if (!block(value)) {
                if (current != index) {
                    keys[current] = keys[index]
                    values[current] = value
                }
                current++
            }
        }
        if (size > current) {
            for (index in current until size) {
                keys[index] = null
                values[index] = null
            }
            size = current
        }
    }

    inline fun forEach(block: (key: Key, value: Value) -> Unit) {
        for (index in 0 until size) {
            @Suppress("UNCHECKED_CAST")
            block(keys[index] as Key, values[index] as Value)
        }
    }

    /**
     * Returns the index into [keys] of the found [key], or the negative index - 1 of the
     * position in which it would be if it were found.
     */
    private fun find(key: Any?): Int {
        val keyIdentity = identityHashCode(key)
        var low = 0
        var high = size - 1

        while (low <= high) {
            val mid = (low + high).ushr(1)
            val midKey = keys[mid]
            val midKeyHash = identityHashCode(midKey)
            when {
                midKeyHash < keyIdentity -> low = mid + 1
                midKeyHash > keyIdentity -> high = mid - 1
                key === midKey -> return mid
                else -> return findExactIndex(mid, key, keyIdentity)
            }
        }
        return -(low + 1)
    }

    /**
     * When multiple keys share the same [identityHashCode], then we must find the specific
     * index of the target item. This method assumes that [midIndex] has already been checked
     * for an exact match for [key], but will look at nearby values to find the exact item index.
     * If no match is found, the negative index - 1 of the position in which it would be will
     * be returned, which is always after the last key with the same [identityHashCode].
     */
    private fun findExactIndex(midIndex: Int, key: Any?, keyHash: Int): Int {
        // hunt down first
        for (i in midIndex - 1 downTo 0) {
            val k = keys[i]
            if (k === key) {
                return i
            }
            if (identityHashCode(k) != keyHash) {
                break // we've gone too far
            }
        }

        for (i in midIndex + 1 until size) {
            val k = keys[i]
            if (k === key) {
                return i
            }
            if (identityHashCode(k) != keyHash) {
                // We've gone too far. We should insert here.
                return -(i + 1)
            }
        }

        // We should insert at the end
        return -(size + 1)
    }
}