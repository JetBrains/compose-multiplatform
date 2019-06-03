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

package androidx.compose

import java.lang.ref.WeakReference

/**
 * A map from a key to a set of values used for keeping the relation between some
 * entities and a models changes of which this entities are observing.
 *
 * Two main differences from a regular Map<K, Set<V>>:
 * 1) Object.hashCode is not used, so the values can be mutable and change their hashCode value
 * 2) Objects are stored with WeakReference to prevent leaking them.
*/
class ObserverMap<K : Any, V : Any> {

    private val map = mutableMapOf<WeakIdentity<K>, MutableSet<WeakIdentity<V>>>()

    /**
     * Adds a [value] into a set associated with this [key].
     */
    fun add(key: K, value: V) {
        val models = map.getOrPut(WeakIdentity(key)) { mutableSetOf() }
        models.add(WeakIdentity(value))
    }

    /**
     * Removes all the values associated with this [key].
     *
     * @return the list of values removed from the set as a result of this operation.
     */
    fun remove(key: K): List<V> {
        return map.remove(WeakIdentity(key))?.mapNotNull { it.value } ?: emptyList()
    }

    /**
     * Removes exact [value] from the set associated with this [key].
     */
    fun remove(key: K, value: V) {
        map[WeakIdentity(key)]?.remove(WeakIdentity(value))
    }

    /**
     * Returns `true` when the map contains the given key and value
     */
    fun contains(key: K, value: V): Boolean {
        return map[WeakIdentity(key)]?.contains(WeakIdentity(value)) ?: false
    }

    /**
     * Clears all the keys and values from the map.
     */
    fun clear() {
        map.clear()
    }

    /**
     * @return a list of values associated with the provided [keys].
     */
    operator fun get(keys: Iterable<K>): List<V> {
        val set = mutableSetOf<WeakIdentity<V>>()
        keys.forEach { key ->
            map[WeakIdentity(key)]?.let(set::addAll)
        }
        return set.mapNotNull { it.value }
    }

    /**
     * Clears all the values that match the given [predicate] from all the sets.
     */
    fun clearValues(predicate: (V) -> Boolean) {
        val iterator = map.iterator()
        while (iterator.hasNext()) {
            val (key, set) = iterator.next()
            if (key.value == null) {
                iterator.remove()
            } else {
                set.removeAll { it.value?.let(predicate) ?: true }
                if (set.isEmpty()) {
                    iterator.remove()
                }
            }
        }
    }
}

/**
 * Ignore the object's implementation of hashCode and equals as they will change for data classes
 * that are mutated. The read observer needs to track the object identity, not the object value.
 */
private class WeakIdentity<T : Any>(value: T) {
    // Save the hash code of value as it might be reclaimed making value.hashCode inaccessible
    private val myHc = System.identityHashCode(value)

    // Preserve a weak reference to the value to prevent read observers from leaking observed values
    private val weakValue = WeakReference(value)

    // Ignore the equality of value and use object identity instead
    override fun equals(other: Any?): Boolean =
        this === other || (other is WeakIdentity<*>) && other.value === value && value !== null

    override fun hashCode(): Int = myHc

    val value: T? get() = weakValue.get()
}