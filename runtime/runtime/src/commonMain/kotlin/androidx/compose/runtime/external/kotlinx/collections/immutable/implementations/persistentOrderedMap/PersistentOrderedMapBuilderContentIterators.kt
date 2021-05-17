/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable.implementations.persistentOrderedMap

import kotlinx.collections.immutable.implementations.immutableMap.MapEntry
import kotlinx.collections.immutable.internal.EndOfChain

internal open class PersistentOrderedMapBuilderLinksIterator<K, V>(
        private var nextKey: Any?,
        internal val builder: PersistentOrderedMapBuilder<K, V>
) : MutableIterator<LinkedValue<V>> {

    internal var lastIteratedKey: Any? = EndOfChain
    private var nextWasInvoked = false
    private var expectedModCount = builder.hashMapBuilder.modCount
    internal var index = 0

    override fun hasNext(): Boolean {
        return index < builder.size
    }

    override fun next(): LinkedValue<V> {
        checkForComodification()
        checkHasNext()
        lastIteratedKey = nextKey
        nextWasInvoked = true
        index++
        @Suppress("UNCHECKED_CAST")
        val result = builder.hashMapBuilder.getOrElse(nextKey as K) {
            throw ConcurrentModificationException("Hash code of a key ($nextKey) has changed after it was added to the persistent map.")
        }
        nextKey = result.next
        return result
    }

    override fun remove() {
        checkNextWasInvoked()
        builder.remove(lastIteratedKey)
        lastIteratedKey = null
        nextWasInvoked = false
        expectedModCount = builder.hashMapBuilder.modCount
        index--
    }

    private fun checkHasNext() {
        if (!hasNext())
            throw NoSuchElementException()
    }

    private fun checkNextWasInvoked() {
        if (!nextWasInvoked)
            throw IllegalStateException()
    }

    private fun checkForComodification() {
        if (builder.hashMapBuilder.modCount != expectedModCount)
            throw ConcurrentModificationException()
    }
}

internal class PersistentOrderedMapBuilderEntriesIterator<K, V>(map: PersistentOrderedMapBuilder<K, V>): MutableIterator<MutableMap.MutableEntry<K, V>> {
    private val internal = PersistentOrderedMapBuilderLinksIterator(map.firstKey, map)

    override fun hasNext(): Boolean {
        return internal.hasNext()
    }

    override fun next(): MutableMap.MutableEntry<K, V> {
        val links = internal.next()
        @Suppress("UNCHECKED_CAST")
        return MutableMapEntry(internal.builder.hashMapBuilder, internal.lastIteratedKey as K, links)
    }

    override fun remove() {
        internal.remove()
    }
}

private class MutableMapEntry<K, V>(private val mutableMap: MutableMap<K, LinkedValue<V>>,
                                    key: K,
                                    private var links: LinkedValue<V>) : MapEntry<K, V>(key, links.value), MutableMap.MutableEntry<K, V> {
    override val value: V
        get() = links.value

    override fun setValue(newValue: V): V {
        val result = links.value
        links = links.withValue(newValue)
        mutableMap[key] = links
        return result
    }
}

internal class PersistentOrderedMapBuilderKeysIterator<out K, out V>(map: PersistentOrderedMapBuilder<K, V>): MutableIterator<K> {
    private val internal = PersistentOrderedMapBuilderLinksIterator(map.firstKey, map)

    override fun hasNext(): Boolean {
        return internal.hasNext()
    }

    override fun next(): K {
        internal.next()
        @Suppress("UNCHECKED_CAST")
        return internal.lastIteratedKey as K
    }

    override fun remove() {
        internal.remove()
    }
}

internal class PersistentOrderedMapBuilderValuesIterator<out K, out V>(map: PersistentOrderedMapBuilder<K, V>): MutableIterator<V> {
    private val internal = PersistentOrderedMapBuilderLinksIterator(map.firstKey, map)

    override fun hasNext(): Boolean {
        return internal.hasNext()
    }

    override fun next(): V {
        return internal.next().value
    }

    override fun remove() {
        internal.remove()
    }
}