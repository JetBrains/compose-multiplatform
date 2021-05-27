/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package androidx.compose.runtime.external.kotlinx.collections.immutable.implementations.persistentOrderedMap

import androidx.compose.runtime.external.kotlinx.collections.immutable.ImmutableCollection
import androidx.compose.runtime.external.kotlinx.collections.immutable.ImmutableSet

internal class PersistentOrderedMapEntries<K, V>(private val map: PersistentOrderedMap<K, V>) : ImmutableSet<Map.Entry<K, V>>, AbstractSet<Map.Entry<K, V>>() {
    override val size: Int get() = map.size

    override fun contains(element: Map.Entry<K, V>): Boolean {
        // TODO: Eliminate this check after KT-30016 gets fixed.
        @Suppress("USELESS_CAST")
        if ((element as Any?) !is Map.Entry<*, *>) return false
        return map[element.key]?.let { candidate -> candidate == element.value }
                ?: (element.value == null && map.containsKey(element.key))
    }

    override fun iterator(): Iterator<Map.Entry<K, V>> {
        return PersistentOrderedMapEntriesIterator(map)
    }
}

internal class PersistentOrderedMapKeys<K, V>(private val map: PersistentOrderedMap<K, V>) : ImmutableSet<K>, AbstractSet<K>() {
    override val size: Int
        get() = map.size

    override fun contains(element: K): Boolean {
        return map.containsKey(element)
    }

    override fun iterator(): Iterator<K> {
        return PersistentOrderedMapKeysIterator(map)
    }
}

internal class PersistentOrderedMapValues<K, V>(private val map: PersistentOrderedMap<K, V>) : ImmutableCollection<V>, AbstractCollection<V>() {
    override val size: Int
        get() = map.size

    override fun contains(element: V): Boolean {
        return map.containsValue(element)
    }

    override fun iterator(): Iterator<V> {
        return PersistentOrderedMapValuesIterator(map)
    }
}