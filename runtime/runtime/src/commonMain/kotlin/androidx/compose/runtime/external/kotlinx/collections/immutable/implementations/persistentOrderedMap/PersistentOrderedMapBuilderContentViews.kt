/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable.implementations.persistentOrderedMap

import kotlinx.collections.immutable.implementations.immutableMap.AbstractMapBuilderEntries

internal class PersistentOrderedMapBuilderEntries<K, V>(private val builder: PersistentOrderedMapBuilder<K, V>)
    : AbstractMapBuilderEntries<MutableMap.MutableEntry<K, V>, K, V>() {
    override fun add(element: MutableMap.MutableEntry<K, V>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun clear() {
        builder.clear()
    }

    override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, V>> {
        return PersistentOrderedMapBuilderEntriesIterator(builder)
    }

    override fun removeEntry(element: Map.Entry<K, V>): Boolean {
        return builder.remove(element.key, element.value)
    }

    override val size: Int
        get() = builder.size

    override fun containsEntry(element: Map.Entry<K, V>): Boolean {
        return builder[element.key]?.let { candidate -> candidate == element.value }
                ?: (element.value == null && builder.containsKey(element.key))
    }
}

internal class PersistentOrderedMapBuilderKeys<K, V>(private val builder: PersistentOrderedMapBuilder<K, V>) : MutableSet<K>, AbstractMutableSet<K>() {
    override fun add(element: K): Boolean {
        throw UnsupportedOperationException()
    }

    override fun clear() {
        builder.clear()
    }

    override fun iterator(): MutableIterator<K> {
        return PersistentOrderedMapBuilderKeysIterator(builder)
    }

    override fun remove(element: K): Boolean {
        if (builder.containsKey(element)) {
            builder.remove(element)
            return true
        }
        return false
    }

    override val size: Int
        get() = builder.size

    override fun contains(element: K): Boolean {
        return builder.containsKey(element)
    }
}

internal class PersistentOrderedMapBuilderValues<K, V>(private val builder: PersistentOrderedMapBuilder<K, V>) : MutableCollection<V>, AbstractMutableCollection<V>() {
    override val size: Int
        get() = builder.size

    override fun contains(element: V): Boolean {
        return builder.containsValue(element)
    }

    override fun add(element: V): Boolean {
        throw UnsupportedOperationException()
    }

    override fun clear() {
        builder.clear()
    }

    override fun iterator(): MutableIterator<V> {
        return PersistentOrderedMapBuilderValuesIterator(builder)
    }
}