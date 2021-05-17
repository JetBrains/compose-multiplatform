/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable.implementations.immutableMap

import kotlinx.collections.immutable.ImmutableCollection
import kotlinx.collections.immutable.ImmutableSet

internal class PersistentHashMapEntries<K, V>(private val map: PersistentHashMap<K, V>) : ImmutableSet<Map.Entry<K, V>>, AbstractSet<Map.Entry<K, V>>() {
    override val size: Int get() = map.size

    override fun contains(element: Map.Entry<K, V>): Boolean {
        // TODO: Eliminate this check after KT-30016 gets fixed.
        if ((element as Any?) !is Map.Entry<*, *>) return false
        return map[element.key]?.let { candidate -> candidate == element.value }
                ?: (element.value == null && map.containsKey(element.key))
    }

    override fun iterator(): Iterator<Map.Entry<K, V>> {
        return PersistentHashMapEntriesIterator(map.node)
    }
}

internal class PersistentHashMapKeys<K, V>(private val map: PersistentHashMap<K, V>) : ImmutableSet<K>, AbstractSet<K>() {
    override val size: Int
        get() = map.size

    override fun contains(element: K): Boolean {
        return map.containsKey(element)
    }

    override fun iterator(): Iterator<K> {
        return PersistentHashMapKeysIterator(map.node)
    }
}

internal class PersistentHashMapValues<K, V>(private val map: PersistentHashMap<K, V>) : ImmutableCollection<V>, AbstractCollection<V>() {
    override val size: Int
        get() = map.size

    override fun contains(element: V): Boolean {
        return map.containsValue(element)
    }

    override fun iterator(): Iterator<V> {
        return PersistentHashMapValuesIterator(map.node)
    }
}
