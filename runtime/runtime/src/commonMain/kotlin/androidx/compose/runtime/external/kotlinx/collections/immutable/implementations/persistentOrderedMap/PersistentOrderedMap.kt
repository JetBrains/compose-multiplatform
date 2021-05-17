/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable.implementations.persistentOrderedMap

import kotlinx.collections.immutable.ImmutableCollection
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.implementations.immutableMap.PersistentHashMap
import kotlinx.collections.immutable.internal.EndOfChain
import kotlinx.collections.immutable.mutate

internal class LinkedValue<V>(val value: V, val previous: Any?, val next: Any?) {
    /** Constructs LinkedValue for a new single entry */
    constructor(value: V) : this(value, EndOfChain, EndOfChain)
    /** Constructs LinkedValue for a new last entry */
    constructor(value: V, previous: Any?) : this(value, previous, EndOfChain)

    fun withValue(newValue: V) = LinkedValue(newValue, previous, next)
    fun withPrevious(newPrevious: Any?) = LinkedValue(value, newPrevious, next)
    fun withNext(newNext: Any?) = LinkedValue(value, previous, newNext)

    val hasNext get() = next !== EndOfChain
    val hasPrevious get() = previous !== EndOfChain
}

internal class PersistentOrderedMap<K, V>(
        internal val firstKey: Any?,
        internal val lastKey: Any?,
        internal val hashMap: PersistentHashMap<K, LinkedValue<V>>
) : AbstractMap<K, V>(), PersistentMap<K, V> {

    override val size: Int get() = hashMap.size

    override val keys: ImmutableSet<K>
        get() {
            return PersistentOrderedMapKeys(this)
        }

    override val values: ImmutableCollection<V>
        get() {
            return PersistentOrderedMapValues(this)
        }

    override val entries: ImmutableSet<Map.Entry<K, V>>
        get() {
            return createEntries()
        }

    private fun createEntries(): ImmutableSet<Map.Entry<K, V>> {
        return PersistentOrderedMapEntries(this)
    }

    // TODO: compiler bug: this bridge should be generated automatically
    @PublishedApi
    internal fun getEntries(): Set<Map.Entry<K, V>> {
        return createEntries()
    }

    override fun containsKey(key: K): Boolean = hashMap.containsKey(key)

    override fun get(key: K): V? = hashMap[key]?.value

    override fun put(key: K, value: @UnsafeVariance V): PersistentOrderedMap<K, V> {
        if (isEmpty()) {
            val newMap = hashMap.put(key, LinkedValue(value))
            return PersistentOrderedMap(key, key, newMap)
        }

        val links = hashMap[key]
        if (links != null) {
            if (links.value === value) {
                return this
            }
            val newMap = hashMap.put(key, links.withValue(value))
            return PersistentOrderedMap(firstKey, lastKey, newMap)
        }

        @Suppress("UNCHECKED_CAST")
        val lastKey = lastKey as K
        val lastLinks = hashMap[lastKey]!!
//        assert(!lastLink.hasNext)
        val newMap = hashMap
                .put(lastKey, lastLinks.withNext(key))
                .put(key, LinkedValue(value, previous = lastKey))
        return PersistentOrderedMap(firstKey, key, newMap)
    }

    override fun remove(key: K): PersistentOrderedMap<K, V> {
        val links = hashMap[key] ?: return this

        var newMap = hashMap.remove(key)
        if (links.hasPrevious) {
            val previousLinks = newMap[links.previous]!!
//            assert(previousLinks.next == key)
            @Suppress("UNCHECKED_CAST")
            newMap = newMap.put(links.previous as K, previousLinks.withNext(links.next))
        }
        if (links.hasNext) {
            val nextLinks = newMap[links.next]!!
//            assert(nextLinks.previous == key)
            @Suppress("UNCHECKED_CAST")
            newMap = newMap.put(links.next as K, nextLinks.withPrevious(links.previous))
        }

        val newFirstKey = if (!links.hasPrevious) links.next else firstKey
        val newLastKey = if (!links.hasNext) links.previous else lastKey
        return PersistentOrderedMap(newFirstKey, newLastKey, newMap)
    }

    override fun remove(key: K, value: @UnsafeVariance V): PersistentOrderedMap<K, V> {
        val links = hashMap[key] ?: return this
        return if (links.value == value) this.remove(key) else this
    }

    override fun putAll(m: Map<out K, @UnsafeVariance V>): PersistentMap<K, V> {
        return this.mutate { it.putAll(m) }
    }

    override fun clear(): PersistentMap<K, V> {
        return emptyOf()
    }

    override fun builder(): PersistentMap.Builder<K, V> {
        return PersistentOrderedMapBuilder(this)
    }

    internal companion object {
        private val EMPTY = PersistentOrderedMap<Nothing, Nothing>(EndOfChain, EndOfChain, PersistentHashMap.emptyOf())
        @Suppress("UNCHECKED_CAST")
        internal fun <K, V> emptyOf(): PersistentOrderedMap<K, V> = EMPTY as PersistentOrderedMap<K, V>
    }
}