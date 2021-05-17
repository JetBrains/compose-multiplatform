/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable.implementations.immutableMap

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.internal.DeltaCounter
import kotlinx.collections.immutable.internal.MutabilityOwnership

internal class PersistentHashMapBuilder<K, V>(private var map: PersistentHashMap<K, V>) : PersistentMap.Builder<K, V>, AbstractMutableMap<K, V>() {
    internal var ownership = MutabilityOwnership()
        private set
    internal var node = map.node
    internal var operationResult: V? = null
    internal var modCount = 0

    // Size change implies structural changes.
    override var size = map.size
        set(value) {
            field = value
            modCount++
        }

    override fun build(): PersistentHashMap<K, V> {
        map = if (node === map.node) {
            map
        } else {
            ownership = MutabilityOwnership()
            PersistentHashMap(node, size)
        }
        return map
    }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() {
            return PersistentHashMapBuilderEntries(this)
        }

    override val keys: MutableSet<K>
        get() {
            return PersistentHashMapBuilderKeys(this)
        }

    override val values: MutableCollection<V>
        get() {
            return PersistentHashMapBuilderValues(this)
        }

    override fun containsKey(key: K): Boolean {
        return node.containsKey(key.hashCode(), key, 0)
    }

    override fun get(key: K): V? {
        return node.get(key.hashCode(), key, 0)
    }

    override fun put(key: K, value: @UnsafeVariance V): V? {
        operationResult = null
        node = node.mutablePut(key.hashCode(), key, value, 0, this)
        return operationResult
    }

    override fun putAll(from: Map<out K, V>) {
        val map = from as? PersistentHashMap ?: (from as? PersistentHashMapBuilder)?.build()
        if (map != null) @Suppress("UNCHECKED_CAST") {
            val intersectionCounter = DeltaCounter()
            val oldSize = this.size
            node = node.mutablePutAll(map.node as TrieNode<K, V>, 0, intersectionCounter, this)
            val newSize = oldSize + map.size - intersectionCounter.count
            if(oldSize != newSize) this.size = newSize
        }
        else super.putAll(from)
    }

    override fun remove(key: K): V? {
        operationResult = null
        @Suppress("UNCHECKED_CAST")
        node = node.mutableRemove(key.hashCode(), key, 0, this) ?: TrieNode.EMPTY as TrieNode<K, V>
        return operationResult
    }

    fun remove(key: K, value: V): Boolean {
        val oldSize = size
        @Suppress("UNCHECKED_CAST")
        node = node.mutableRemove(key.hashCode(), key, value, 0, this) ?: TrieNode.EMPTY as TrieNode<K, V>
        return oldSize != size
    }

    override fun clear() {
        @Suppress("UNCHECKED_CAST")
        node = TrieNode.EMPTY as TrieNode<K, V>
        size = 0
    }
}