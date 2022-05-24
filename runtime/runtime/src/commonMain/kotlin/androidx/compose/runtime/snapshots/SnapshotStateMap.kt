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

package androidx.compose.runtime.snapshots

import androidx.compose.runtime.Stable
import androidx.compose.runtime.external.kotlinx.collections.immutable.PersistentMap
import androidx.compose.runtime.external.kotlinx.collections.immutable.persistentHashMapOf
import androidx.compose.runtime.synchronized
import androidx.compose.runtime.createSynchronizedObject
import kotlin.jvm.JvmName

/**
 * An implementation of [MutableMap] that can be observed and snapshot. This is the result type
 * created by [androidx.compose.runtime.mutableStateMapOf].
 *
 * This class closely implements the same semantics as [HashMap].
 *
 * @see androidx.compose.runtime.mutableStateMapOf
 */
@Stable
class SnapshotStateMap<K, V> : MutableMap<K, V>, StateObject {
    override var firstStateRecord: StateRecord =
        StateMapStateRecord<K, V>(persistentHashMapOf())
        private set

    override fun prependStateRecord(value: StateRecord) {
        @Suppress("UNCHECKED_CAST")
        firstStateRecord = value as StateMapStateRecord<K, V>
    }

    override val size get() = readable.map.size
    override fun containsKey(key: K) = readable.map.containsKey(key)
    override fun containsValue(value: V) = readable.map.containsValue(value)
    override fun get(key: K) = readable.map[key]
    override fun isEmpty() = readable.map.isEmpty()
    override val entries: MutableSet<MutableMap.MutableEntry<K, V>> = SnapshotMapEntrySet(this)
    override val keys: MutableSet<K> = SnapshotMapKeySet(this)
    override val values: MutableCollection<V> = SnapshotMapValueSet(this)

    override fun clear() = update { persistentHashMapOf() }
    override fun put(key: K, value: V): V? = mutate { it.put(key, value) }
    override fun putAll(from: Map<out K, V>) = mutate { it.putAll(from) }
    override fun remove(key: K): V? = mutate { it.remove(key) }

    internal val modification get() = readable.modification

    internal fun removeValue(value: V) =
        entries.firstOrNull { it.value == value }?.let { remove(it.key); true } == true

    @Suppress("UNCHECKED_CAST")
    internal val readable: StateMapStateRecord<K, V>
        get() = (firstStateRecord as StateMapStateRecord<K, V>).readable(this)

    internal inline fun removeIf(predicate: (MutableMap.MutableEntry<K, V>) -> Boolean): Boolean {
        var removed = false
        mutate {
            for (entry in this.entries) {
                if (predicate(entry)) {
                    it.remove(entry.key)
                    removed = true
                }
            }
        }
        return removed
    }

    internal inline fun any(predicate: (Map.Entry<K, V>) -> Boolean): Boolean {
        for (entry in readable.map.entries) {
            if (predicate(entry)) return true
        }
        return false
    }

    internal inline fun all(predicate: (Map.Entry<K, V>) -> Boolean): Boolean {
        for (entry in readable.map.entries) {
            if (!predicate(entry)) return false
        }
        return true
    }

    /**
     * An internal function used by the debugger to display the value of the current value of the
     * mutable state object without triggering read observers.
     */
    @Suppress("unused")
    internal val debuggerDisplayValue: Map<K, V>
        @JvmName("getDebuggerDisplayValue")
        get() = withCurrent { map }

    private inline fun <R> withCurrent(block: StateMapStateRecord<K, V>.() -> R): R =
        @Suppress("UNCHECKED_CAST")
        (firstStateRecord as StateMapStateRecord<K, V>).withCurrent(block)

    private inline fun <R> writable(block: StateMapStateRecord<K, V>.() -> R): R =
        @Suppress("UNCHECKED_CAST")
        (firstStateRecord as StateMapStateRecord<K, V>).writable(this, block)

    private inline fun <R> mutate(block: (MutableMap<K, V>) -> R): R {
        var result: R
        while (true) {
            var oldMap: PersistentMap<K, V>? = null
            var currentModification = 0
            synchronized(sync) {
                val current = withCurrent { this }
                oldMap = current.map
                currentModification = current.modification
            }
            val builder = oldMap!!.builder()
            result = block(builder)
            val newMap = builder.build()
            if (newMap == oldMap || synchronized(sync) {
                writable {
                    if (modification == currentModification) {
                        map = newMap
                        modification++
                        true
                    } else false
                }
            }
            ) break
        }
        return result
    }

    private inline fun update(block: (PersistentMap<K, V>) -> PersistentMap<K, V>) = withCurrent {
        val newMap = block(map)
        if (newMap !== map) synchronized(sync) {
            writable {
                map = newMap
                modification++
            }
        }
    }

    /**
     * Implementation class of [SnapshotStateMap]. Do not use.
     */
    internal class StateMapStateRecord<K, V> internal constructor(
        internal var map: PersistentMap<K, V>
    ) : StateRecord() {
        internal var modification = 0
        override fun assign(value: StateRecord) {
            @Suppress("UNCHECKED_CAST")
            val other = (value as StateMapStateRecord<K, V>)
            synchronized(sync) {
                map = other.map
                modification = other.modification
            }
        }

        override fun create(): StateRecord = StateMapStateRecord(map)
    }
}

private abstract class SnapshotMapSet<K, V, E>(
    val map: SnapshotStateMap<K, V>
) : MutableSet<E> {
    override val size: Int get() = map.size
    override fun clear() = map.clear()
    override fun isEmpty() = map.isEmpty()
}

private class SnapshotMapEntrySet<K, V>(
    map: SnapshotStateMap<K, V>
) : SnapshotMapSet<K, V, MutableMap.MutableEntry<K, V>>(map) {
    override fun add(element: MutableMap.MutableEntry<K, V>) = unsupported()
    override fun addAll(elements: Collection<MutableMap.MutableEntry<K, V>>) = unsupported()
    override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, V>> =
        StateMapMutableEntriesIterator(map, map.readable.map.entries.iterator())
    override fun remove(element: MutableMap.MutableEntry<K, V>) =
        map.remove(element.key) != null
    override fun removeAll(elements: Collection<MutableMap.MutableEntry<K, V>>): Boolean {
        var removed = false
        for (element in elements) {
            removed = map.remove(element.key) != null || removed
        }
        return removed
    }
    override fun retainAll(elements: Collection<MutableMap.MutableEntry<K, V>>): Boolean {
        val entries = elements.associate { it.key to it.value }
        return map.removeIf { !entries.containsKey(it.key) || entries[it.key] != it.value }
    }
    override fun contains(element: MutableMap.MutableEntry<K, V>): Boolean {
        return map[element.key] == element.value
    }
    override fun containsAll(elements: Collection<MutableMap.MutableEntry<K, V>>): Boolean {
        return elements.all { contains(it) }
    }
}

private class SnapshotMapKeySet<K, V>(map: SnapshotStateMap<K, V>) : SnapshotMapSet<K, V, K>(map) {
    override fun add(element: K) = unsupported()
    override fun addAll(elements: Collection<K>) = unsupported()
    override fun iterator() = StateMapMutableKeysIterator(map, map.readable.map.entries.iterator())
    override fun remove(element: K): Boolean = map.remove(element) != null
    override fun removeAll(elements: Collection<K>): Boolean {
        var removed = false
        elements.forEach {
            removed = map.remove(it) != null || removed
        }
        return removed
    }
    override fun retainAll(elements: Collection<K>): Boolean {
        val set = elements.toSet()
        return map.removeIf { it.key !in set }
    }
    override fun contains(element: K) = map.contains(element)
    override fun containsAll(elements: Collection<K>): Boolean = elements.all { map.contains(it) }
}

private class SnapshotMapValueSet<K, V>(
    map: SnapshotStateMap<K, V>
) : SnapshotMapSet<K, V, V>(map) {
    override fun add(element: V) = unsupported()
    override fun addAll(elements: Collection<V>) = unsupported()
    override fun iterator() =
        StateMapMutableValuesIterator(map, map.readable.map.entries.iterator())
    override fun remove(element: V): Boolean = map.removeValue(element)
    override fun removeAll(elements: Collection<V>): Boolean {
        val set = elements.toSet()
        return map.removeIf { it.value in set }
    }
    override fun retainAll(elements: Collection<V>): Boolean {
        val set = elements.toSet()
        return map.removeIf { it.value !in set }
    }
    override fun contains(element: V) = map.containsValue(element)
    override fun containsAll(elements: Collection<V>): Boolean {
        return elements.all { map.containsValue(it) }
    }
}

/**
 * This lock is used to ensure that the value of modification and the map in the state record,
 * when used together, are atomically read and written.
 *
 * A global sync object is used to avoid having to allocate a sync object and initialize a monitor
 * for each instance the map. This avoids additional allocations but introduces some contention
 * between maps. As there is already contention on the global snapshot lock to write so the
 * additional contention introduced by this lock is nominal.
 *
 * In code the requires this lock and calls `writable` (or other operation that acquires the
 * snapshot global lock), this lock *MUST* be acquired first to avoid deadlocks.
 */
private val sync = createSynchronizedObject()

private abstract class StateMapMutableIterator<K, V>(
    val map: SnapshotStateMap<K, V>,
    val iterator: Iterator<Map.Entry<K, V>>
) {
    protected var modification = map.modification
    protected var current: Map.Entry<K, V>? = null
    protected var next: Map.Entry<K, V>? = null

    init { advance() }

    fun remove() = modify {
        val value = current

        if (value != null) {
            map.remove(value.key)
            current = null
        } else {
            throw IllegalStateException()
        }
    }

    fun hasNext() = next != null

    protected fun advance() {
        current = next
        next = if (iterator.hasNext()) iterator.next() else null
    }

    protected inline fun <T> modify(block: () -> T): T {
        if (map.modification != modification) {
            throw ConcurrentModificationException()
        }
        return block().also { modification = map.modification }
    }
}

private class StateMapMutableEntriesIterator<K, V>(
    map: SnapshotStateMap<K, V>,
    iterator: Iterator<Map.Entry<K, V>>
) : StateMapMutableIterator<K, V>(map, iterator), MutableIterator<MutableMap.MutableEntry<K, V>> {
    override fun next(): MutableMap.MutableEntry<K, V> {
        advance()
        if (current != null) {
            return object : MutableMap.MutableEntry<K, V> {
                override val key = current!!.key
                override var value = current!!.value
                override fun setValue(newValue: V): V = modify {
                    val result = value
                    map[key] = newValue
                    value = newValue
                    return result
                }
            }
        } else {
            throw IllegalStateException()
        }
    }
}

private class StateMapMutableKeysIterator<K, V>(
    map: SnapshotStateMap<K, V>,
    iterator: Iterator<Map.Entry<K, V>>
) : StateMapMutableIterator<K, V>(map, iterator), MutableIterator<K> {
    override fun next(): K {
        val result = next ?: throw IllegalStateException()
        advance()
        return result.key
    }
}

private class StateMapMutableValuesIterator<K, V>(
    map: SnapshotStateMap<K, V>,
    iterator: Iterator<Map.Entry<K, V>>
) : StateMapMutableIterator<K, V>(map, iterator), MutableIterator<V> {
    override fun next(): V {
        val result = next ?: throw IllegalStateException()
        advance()
        return result.value
    }
}

internal fun unsupported(): Nothing {
    throw UnsupportedOperationException()
}
