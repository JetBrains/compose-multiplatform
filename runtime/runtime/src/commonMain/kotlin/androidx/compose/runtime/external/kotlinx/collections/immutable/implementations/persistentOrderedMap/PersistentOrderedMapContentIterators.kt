/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable.implementations.persistentOrderedMap

import kotlinx.collections.immutable.implementations.immutableMap.MapEntry

internal open class PersistentOrderedMapLinksIterator<K, V>(
        internal var nextKey: Any?,
        private val hashMap: Map<K, LinkedValue<V>>
) : Iterator<LinkedValue<V>> {
    internal var index = 0

    override fun hasNext(): Boolean {
        return index < hashMap.size
    }

    override fun next(): LinkedValue<V> {
        if (!hasNext()) {
            throw NoSuchElementException()
        }
        @Suppress("UNCHECKED_CAST")
        val result = hashMap.getOrElse(nextKey as K) {
            throw ConcurrentModificationException("Hash code of a key ($nextKey) has changed after it was added to the persistent map.")
        }
        index++
        nextKey = result.next
        return result
    }

}

internal class PersistentOrderedMapEntriesIterator<out K, out V>(map: PersistentOrderedMap<K, V>) : Iterator<Map.Entry<K, V>> {
    private val internal = PersistentOrderedMapLinksIterator(map.firstKey, map.hashMap)

    override fun hasNext(): Boolean {
        return internal.hasNext()
    }

    override fun next(): Map.Entry<K, V> {
        @Suppress("UNCHECKED_CAST")
        val nextKey = internal.nextKey as K
        val nextValue = internal.next().value
        return MapEntry(nextKey, nextValue)
    }
}

internal class PersistentOrderedMapKeysIterator<out K, out V>(map: PersistentOrderedMap<K, V>) : Iterator<K> {
    private val internal = PersistentOrderedMapLinksIterator(map.firstKey, map.hashMap)

    override fun hasNext(): Boolean {
        return internal.hasNext()
    }

    override fun next(): K {
        @Suppress("UNCHECKED_CAST")
        val nextKey = internal.nextKey as K
        internal.next()
        return nextKey
    }
}

internal class PersistentOrderedMapValuesIterator<out K, out V>(map: PersistentOrderedMap<K, V>) : Iterator<V> {
    private val internal = PersistentOrderedMapLinksIterator(map.firstKey, map.hashMap)

    override fun hasNext(): Boolean {
        return internal.hasNext()
    }

    override fun next(): V {
        return internal.next().value
    }
}