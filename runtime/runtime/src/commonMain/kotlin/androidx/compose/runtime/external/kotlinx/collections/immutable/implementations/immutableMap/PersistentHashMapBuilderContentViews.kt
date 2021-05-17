/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable.implementations.immutableMap

// intermediate abstract class to workaround KT-43321
internal abstract class AbstractMapBuilderEntries<E : Map.Entry<K, V>, K, V> : AbstractMutableSet<E>() {
    final override fun contains(element: E): Boolean {
        // TODO: Eliminate this check after KT-30016 gets fixed.
        if ((element as? Any?) !is Map.Entry<*, *>) return false
        return containsEntry(element)
    }
    abstract fun containsEntry(element: Map.Entry<K, V>): Boolean

    final override fun remove(element: E): Boolean {
        // TODO: Eliminate this check after KT-30016 gets fixed.
        if ((element as? Any?) !is Map.Entry<*, *>) return false
        return removeEntry(element)
    }
    abstract fun removeEntry(element: Map.Entry<K, V>): Boolean
}

internal class PersistentHashMapBuilderEntries<K, V>(private val builder: PersistentHashMapBuilder<K, V>)
    : AbstractMapBuilderEntries<MutableMap.MutableEntry<K, V>, K, V>() {
    override fun add(element: MutableMap.MutableEntry<K, V>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun clear() {
        builder.clear()
    }

    override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, V>> {
        return PersistentHashMapBuilderEntriesIterator(builder)
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

internal class PersistentHashMapBuilderKeys<K, V>(private val builder: PersistentHashMapBuilder<K, V>) : MutableSet<K>, AbstractMutableSet<K>() {
    override fun add(element: K): Boolean {
        throw UnsupportedOperationException()
    }

    override fun clear() {
        builder.clear()
    }

    override fun iterator(): MutableIterator<K> {
        return PersistentHashMapBuilderKeysIterator(builder)
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

internal class PersistentHashMapBuilderValues<K, V>(private val builder: PersistentHashMapBuilder<K, V>) : MutableCollection<V>, AbstractMutableCollection<V>() {
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
        return PersistentHashMapBuilderValuesIterator(builder)
    }
}
