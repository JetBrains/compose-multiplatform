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

/**
 * Maps values to a set of scopes using the [identityHashCode] for both the value and the
 * scope for uniqueness.
 */
@OptIn(ExperimentalContracts::class)
internal class IdentityScopeMap<T : Any> {
    /**
     * The array of indices into [values] and [scopeSets], in the order that they are sorted
     * in the [IdentityScopeMap]. The length of the used values is [size], and all remaining values
     * are the unused indices in [values] and [scopeSets].
     */
    @PublishedApi
    internal var valueOrder: IntArray = IntArray(50) { it }

    /**
     * The [identityHashCode] for the keys in the collection. We never use the actual
     * values
     */
    @PublishedApi
    internal var values: Array<Any?> = arrayOfNulls(50)

    /**
     * The [IdentityArraySet]s for values, in the same index order as [values], indexed
     * by [valueOrder]. The consumed values may extend beyond [size] if a value has been removed.
     */
    @PublishedApi
    internal var scopeSets: Array<IdentityArraySet<T>?> = arrayOfNulls(50)

    /**
     * The number of values in the map.
     */
    @PublishedApi
    internal var size = 0

    /**
     * Returns the value at the given [index] order in the map.
     */
    @Suppress("NOTHING_TO_INLINE")
    private inline fun valueAt(index: Int): Any {
        return values[valueOrder[index]]!!
    }

    /**
     * Returns the [IdentityArraySet] for the value at the given [index] order in the map.
     */
    private fun scopeSetAt(index: Int): IdentityArraySet<T> {
        return scopeSets[valueOrder[index]]!!
    }

    /**
     * Adds a [value]/[scope] pair to the map and returns `true` if it was added or `false` if
     * it already existed.
     */
    fun add(value: Any, scope: T): Boolean {
        val valueSet = getOrCreateIdentitySet(value)
        return valueSet.add(scope)
    }

    /**
     * Returns true if any scopes are associated with [element]
     */
    operator fun contains(element: Any): Boolean = find(element) >= 0

    /**
     * Executes [block] for all scopes mapped to the given [value].
     */
    inline fun forEachScopeOf(value: Any, block: (scope: T) -> Unit) {
        val index = find(value)
        if (index >= 0) {
            scopeSetAt(index).forEach(block)
        }
    }

    /**
     * Returns the existing [IdentityArraySet] for the given [value] or creates a new one
     * and insertes it into the map and returns it.
     */
    private fun getOrCreateIdentitySet(value: Any): IdentityArraySet<T> {
        val index: Int
        if (size > 0) {
            index = find(value)

            if (index >= 0) {
                return scopeSetAt(index)
            }
        } else {
            index = -1
        }

        val insertIndex = -(index + 1)

        if (size < valueOrder.size) {
            val valueIndex = valueOrder[size]
            values[valueIndex] = value
            val scopeSet = scopeSets[valueIndex] ?: IdentityArraySet<T>().also {
                scopeSets[valueIndex] = it
            }

            // insert into the right location in keyOrder
            if (insertIndex < size) {
                valueOrder.copyInto(
                    destination = valueOrder,
                    destinationOffset = insertIndex + 1,
                    startIndex = insertIndex,
                    endIndex = size
                )
            }
            valueOrder[insertIndex] = valueIndex
            size++
            return scopeSet
        }

        // We have to increase the size of all arrays
        val newSize = valueOrder.size * 2
        val valueIndex = size
        scopeSets = scopeSets.copyOf(newSize)
        val scopeSet = IdentityArraySet<T>()
        scopeSets[valueIndex] = scopeSet
        values = values.copyOf(newSize)
        values[valueIndex] = value

        val newKeyOrder = IntArray(newSize)
        for (i in size + 1 until newSize) {
            newKeyOrder[i] = i
        }

        if (insertIndex < size) {
            valueOrder.copyInto(
                destination = newKeyOrder,
                destinationOffset = insertIndex + 1,
                startIndex = insertIndex,
                endIndex = size
            )
        }
        newKeyOrder[insertIndex] = valueIndex
        if (insertIndex > 0) {
            valueOrder.copyInto(
                destination = newKeyOrder,
                endIndex = insertIndex
            )
        }
        valueOrder = newKeyOrder
        size++
        return scopeSet
    }

    /**
     * Removes all values and scopes from the map
     */
    fun clear() {
        for (i in 0 until scopeSets.size) {
            scopeSets[i]?.clear()
            valueOrder[i] = i
            values[i] = null
        }

        size = 0
    }

    /**
     * Remove [scope] from the scope set for [value]. If the scope set is empty after [scope] has
     * been remove the reference to [value] is removed as well.
     *
     * @param value the key of the scope map
     * @param scope the scope being removed
     * @return true if the value was removed from the scope
     */
    fun remove(value: Any, scope: T): Boolean {
        val index = find(value)
        if (index >= 0) {
            val valueOrderIndex = valueOrder[index]
            val set = scopeSets[valueOrderIndex] ?: return false
            val removed = set.remove(scope)
            if (set.size == 0) {
                val startIndex = index + 1
                val endIndex = size
                if (startIndex < endIndex) {
                    valueOrder.copyInto(
                        destination = valueOrder,
                        destinationOffset = index,
                        startIndex = startIndex,
                        endIndex = endIndex
                    )
                }
                valueOrder[size - 1] = valueOrderIndex
                values[valueOrderIndex] = null
                size--
            }
            return removed
        }
        return false
    }

    /**
     * Removes all scopes that match [predicate]. If all scopes for a given value have been
     * removed, that value is removed also.
     */
    inline fun removeValueIf(predicate: (scope: T) -> Boolean) {
        removingScopes { scopeSet ->
            scopeSet.removeValueIf(predicate)
        }
    }

    /**
     * Removes given scope from all sets. If all scopes for a given value are removed, that value
     * is removed as well.
     */
    fun removeScope(scope: T) {
        removingScopes { scopeSet ->
            scopeSet.remove(scope)
        }
    }

    private inline fun removingScopes(removalOperation: (IdentityArraySet<T>) -> Unit) {
        var destinationIndex = 0
        for (i in 0 until size) {
            val valueIndex = valueOrder[i]
            val set = scopeSets[valueIndex]!!
            removalOperation(set)
            if (set.size > 0) {
                if (destinationIndex != i) {
                    // We'll bubble-up the now-free key-order by swapping the index with the one
                    // we're copying from. This means that the set can be reused later.
                    val destinationKeyOrder = valueOrder[destinationIndex]
                    valueOrder[destinationIndex] = valueIndex
                    valueOrder[i] = destinationKeyOrder
                }
                destinationIndex++
            }
        }
        // Remove hard references to values that are no longer in the map
        for (i in destinationIndex until size) {
            values[valueOrder[i]] = null
        }
        size = destinationIndex
    }

    /**
     * Returns the index into [valueOrder] of the found [value] of the
     * value, or the negative index - 1 of the position in which it would be if it were found.
     */
    private fun find(value: Any?): Int {
        val valueIdentity = identityHashCode(value)
        var low = 0
        var high = size - 1

        while (low <= high) {
            val mid = (low + high).ushr(1)
            val midValue = valueAt(mid)
            val midValHash = identityHashCode(midValue)
            when {
                midValHash < valueIdentity -> low = mid + 1
                midValHash > valueIdentity -> high = mid - 1
                value === midValue -> return mid
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
            val v = valueAt(i)
            if (v === value) {
                return i
            }
            if (identityHashCode(v) != valueHash) {
                break // we've gone too far
            }
        }

        for (i in midIndex + 1 until size) {
            val v = valueAt(i)
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