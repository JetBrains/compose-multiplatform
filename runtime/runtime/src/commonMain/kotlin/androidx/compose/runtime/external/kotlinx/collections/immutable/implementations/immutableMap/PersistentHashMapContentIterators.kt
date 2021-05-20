/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package androidx.compose.runtime.external.kotlinx.collections.immutable.implementations.immutableMap

import androidx.compose.runtime.external.kotlinx.collections.immutable.internal.assert

internal const val TRIE_MAX_HEIGHT = 7

internal abstract class TrieNodeBaseIterator<out K, out V, out T> : Iterator<T> {
    protected var buffer = TrieNode.EMPTY.buffer
        private set
    private var dataSize = 0
    protected var index = 0

    fun reset(buffer: Array<Any?>, dataSize: Int, index: Int) {
        this.buffer = buffer
        this.dataSize = dataSize
        this.index = index
    }

    fun reset(buffer: Array<Any?>, dataSize: Int) {
        reset(buffer, dataSize, 0)
    }

    fun hasNextKey(): Boolean {
        return index < dataSize
    }

    fun currentKey(): K {
        assert(hasNextKey())
        @Suppress("UNCHECKED_CAST")
        return buffer[index] as K
    }

    fun moveToNextKey() {
        assert(hasNextKey())
        index += 2
    }

    fun hasNextNode(): Boolean {
        assert(index >= dataSize)
        return index < buffer.size
    }

    fun currentNode(): TrieNode<out K, out V> {
        assert(hasNextNode())
        @Suppress("UNCHECKED_CAST")
        return buffer[index] as TrieNode<K, V>
    }

    fun moveToNextNode() {
        assert(hasNextNode())
        index++
    }

    override fun hasNext(): Boolean {
        return hasNextKey()
    }
}

internal class TrieNodeKeysIterator<out K, out V> : TrieNodeBaseIterator<K, V, K>() {
    override fun next(): K {
        assert(hasNextKey())
        index += 2
        @Suppress("UNCHECKED_CAST")
        return buffer[index - 2] as K
    }
}

internal class TrieNodeValuesIterator<out K, out V> : TrieNodeBaseIterator<K, V, V>() {
    override fun next(): V {
        assert(hasNextKey())
        index += 2
        @Suppress("UNCHECKED_CAST")
        return buffer[index - 1] as V
    }
}

internal class TrieNodeEntriesIterator<out K, out V> : TrieNodeBaseIterator<K, V, Map.Entry<K, V>>() {
    override fun next(): Map.Entry<K, V> {
        assert(hasNextKey())
        index += 2
        @Suppress("UNCHECKED_CAST")
        return MapEntry(buffer[index - 2] as K, buffer[index - 1] as V)
    }
}

internal open class MapEntry<out K, out V>(override val key: K, override val value: V) : Map.Entry<K, V> {
    override fun hashCode(): Int = key.hashCode() xor value.hashCode()
    override fun equals(other: Any?): Boolean =
            (other as? Map.Entry<*, *>)?.let { it.key == key && it.value == value } ?: false

    override fun toString(): String = key.toString() + "=" + value.toString()
}


internal abstract class PersistentHashMapBaseIterator<K, V, T>(
        node: TrieNode<K, V>,
        protected val path: Array<TrieNodeBaseIterator<K, V, T>>
) : Iterator<T> {

    protected var pathLastIndex = 0
    private var hasNext = true

    init {
        path[0].reset(node.buffer, ENTRY_SIZE * node.entryCount())
        pathLastIndex = 0
        ensureNextEntryIsReady()
    }

    private fun moveToNextNodeWithData(pathIndex: Int): Int {
        if (path[pathIndex].hasNextKey()) {
            return pathIndex
        }
        if (path[pathIndex].hasNextNode()) {
            val node = path[pathIndex].currentNode()
            if (pathIndex == TRIE_MAX_HEIGHT - 1) {     // collision
                path[pathIndex + 1].reset(node.buffer, node.buffer.size)
            } else {
                path[pathIndex + 1].reset(node.buffer, ENTRY_SIZE * node.entryCount())
            }
            return moveToNextNodeWithData(pathIndex + 1)
        }
        return -1
    }

    private fun ensureNextEntryIsReady() {
        if (path[pathLastIndex].hasNextKey()) {
            return
        }
        for(i in pathLastIndex downTo 0) {
            var result = moveToNextNodeWithData(i)

            if (result == -1 && path[i].hasNextNode()) {
                path[i].moveToNextNode()
                result = moveToNextNodeWithData(i)
            }
            if (result != -1) {
                pathLastIndex = result
                return
            }
            if (i > 0) {
                path[i - 1].moveToNextNode()
            }
            path[i].reset(TrieNode.EMPTY.buffer, 0)
        }
        hasNext = false
    }

    protected fun currentKey(): K {
        checkHasNext()
        return path[pathLastIndex].currentKey()
    }

    override fun hasNext(): Boolean {
        return hasNext
    }

    override fun next(): T {
        checkHasNext()
        val result = path[pathLastIndex].next()
        ensureNextEntryIsReady()
        return result
    }

    private fun checkHasNext() {
        if (!hasNext())
            throw NoSuchElementException()
    }
}

internal class PersistentHashMapEntriesIterator<K, V>(node: TrieNode<K, V>)
    : PersistentHashMapBaseIterator<K, V, Map.Entry<K, V>>(node, Array(TRIE_MAX_HEIGHT + 1) { TrieNodeEntriesIterator<K, V>() })

internal class PersistentHashMapKeysIterator<K, V>(node: TrieNode<K, V>)
    : PersistentHashMapBaseIterator<K, V, K>(node, Array(TRIE_MAX_HEIGHT + 1) { TrieNodeKeysIterator<K, V>() })

internal class PersistentHashMapValuesIterator<K, V>(node: TrieNode<K, V>)
    : PersistentHashMapBaseIterator<K, V, V>(node, Array(TRIE_MAX_HEIGHT + 1) { TrieNodeValuesIterator<K, V>() })
