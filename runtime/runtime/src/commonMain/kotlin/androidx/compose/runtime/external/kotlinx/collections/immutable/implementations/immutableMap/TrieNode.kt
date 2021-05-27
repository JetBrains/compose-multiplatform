/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package androidx.compose.runtime.external.kotlinx.collections.immutable.implementations.immutableMap

import androidx.compose.runtime.external.kotlinx.collections.immutable.internal.DeltaCounter
import androidx.compose.runtime.external.kotlinx.collections.immutable.internal.MutabilityOwnership
import androidx.compose.runtime.external.kotlinx.collections.immutable.internal.assert
import androidx.compose.runtime.external.kotlinx.collections.immutable.internal.forEachOneBit


internal const val MAX_BRANCHING_FACTOR = 32
internal const val LOG_MAX_BRANCHING_FACTOR = 5
internal const val MAX_BRANCHING_FACTOR_MINUS_ONE = MAX_BRANCHING_FACTOR - 1
internal const val ENTRY_SIZE = 2
internal const val MAX_SHIFT = 30

/**
 * Gets trie index segment of the specified [index] at the level specified by [shift].
 *
 * `shift` equal to zero corresponds to the root level.
 * For each lower level `shift` increments by [LOG_MAX_BRANCHING_FACTOR].
 */
internal fun indexSegment(index: Int, shift: Int): Int =
        (index shr shift) and MAX_BRANCHING_FACTOR_MINUS_ONE

private fun <K, V> Array<Any?>.insertEntryAtIndex(keyIndex: Int, key: K, value: V): Array<Any?> {
    val newBuffer = arrayOfNulls<Any?>(this.size + ENTRY_SIZE)
    this.copyInto(newBuffer, endIndex = keyIndex)
    this.copyInto(newBuffer, keyIndex + ENTRY_SIZE, startIndex = keyIndex, endIndex = this.size)
    newBuffer[keyIndex] = key
    newBuffer[keyIndex + 1] = value
    return newBuffer
}

private fun Array<Any?>.replaceEntryWithNode(keyIndex: Int, nodeIndex: Int, newNode: TrieNode<*, *>): Array<Any?> {
    val newNodeIndex = nodeIndex - ENTRY_SIZE  // place where to insert new node in the new buffer
    val newBuffer = arrayOfNulls<Any?>(this.size - ENTRY_SIZE + 1)
    this.copyInto(newBuffer, endIndex = keyIndex)
    this.copyInto(newBuffer, keyIndex, startIndex = keyIndex + ENTRY_SIZE, endIndex = nodeIndex)
    newBuffer[newNodeIndex] = newNode
    this.copyInto(newBuffer, newNodeIndex + 1, startIndex = nodeIndex, endIndex = this.size)
    return newBuffer
}

private fun <K, V> Array<Any?>.replaceNodeWithEntry(nodeIndex: Int, keyIndex: Int, key: K, value: V): Array<Any?> {
    val newBuffer = this.copyOf(this.size + 1)
    newBuffer.copyInto(newBuffer, nodeIndex + 2, nodeIndex + 1, this.size)
    newBuffer.copyInto(newBuffer, keyIndex + 2, keyIndex , nodeIndex)
    newBuffer[keyIndex] = key
    newBuffer[keyIndex + 1] = value
    return newBuffer
}

private fun Array<Any?>.removeEntryAtIndex(keyIndex: Int): Array<Any?> {
    val newBuffer = arrayOfNulls<Any?>(this.size - ENTRY_SIZE)
    this.copyInto(newBuffer, endIndex = keyIndex)
    this.copyInto(newBuffer, keyIndex, startIndex = keyIndex + ENTRY_SIZE, endIndex = this.size)
    return newBuffer
}

private fun Array<Any?>.removeNodeAtIndex(nodeIndex: Int): Array<Any?> {
    val newBuffer = arrayOfNulls<Any?>(this.size - 1)
    this.copyInto(newBuffer, endIndex = nodeIndex)
    this.copyInto(newBuffer, nodeIndex, startIndex = nodeIndex + 1, endIndex = this.size)
    return newBuffer
}



internal class TrieNode<K, V>(
        private var dataMap: Int,
        private var nodeMap: Int,
        buffer: Array<Any?>,
        private val ownedBy: MutabilityOwnership?
) {
    constructor(dataMap: Int, nodeMap: Int, buffer: Array<Any?>) : this(dataMap, nodeMap, buffer, null)

    internal class ModificationResult<K, V>(var node: TrieNode<K, V>, val sizeDelta: Int) {
        inline fun replaceNode(operation: (TrieNode<K, V>) -> TrieNode<K, V>): ModificationResult<K, V> =
                apply { node = operation(node) }
    }

    private fun asInsertResult() = ModificationResult(this, 1)
    private fun asUpdateResult() = ModificationResult(this, 0)

    internal var buffer: Array<Any?> = buffer
        private set

    /** Returns number of entries stored in this trie node (not counting subnodes) */
    internal fun entryCount(): Int = dataMap.countOneBits()

    // here and later:
    // positionMask — an int in form 2^n, i.e. having the single bit set, whose ordinal is a logical position in buffer


    /** Returns true if the data bit map has the bit specified by [positionMask] set, indicating there's a data entry in the buffer at that position. */
    internal fun hasEntryAt(positionMask: Int): Boolean {
        return dataMap and positionMask != 0
    }

    /** Returns true if the node bit map has the bit specified by [positionMask] set, indicating there's a subtrie node in the buffer at that position. */
    private fun hasNodeAt(positionMask: Int): Boolean {
        return nodeMap and positionMask != 0
    }

    /** Gets the index in buffer of the data entry key corresponding to the position specified by [positionMask]. */
    internal fun entryKeyIndex(positionMask: Int): Int {
        return ENTRY_SIZE * (dataMap and (positionMask - 1)).countOneBits()
    }

    /** Gets the index in buffer of the subtrie node entry corresponding to the position specified by [positionMask]. */
    internal fun nodeIndex(positionMask: Int): Int {
        return buffer.size - 1 - (nodeMap and (positionMask - 1)).countOneBits()
    }

    /** Retrieves the buffer element at the given [keyIndex] as key of a data entry. */
    private fun keyAtIndex(keyIndex: Int): K {
        @Suppress("UNCHECKED_CAST")
        return buffer[keyIndex] as K
    }

    /** Retrieves the buffer element next to the given [keyIndex] as value of a data entry. */
    private fun valueAtKeyIndex(keyIndex: Int): V {
        @Suppress("UNCHECKED_CAST")
        return buffer[keyIndex + 1] as V
    }

    /** Retrieves the buffer element at the given [nodeIndex] as subtrie node. */
    internal fun nodeAtIndex(nodeIndex: Int): TrieNode<K, V> {
        @Suppress("UNCHECKED_CAST")
        return buffer[nodeIndex] as TrieNode<K, V>
    }

    private fun insertEntryAt(positionMask: Int, key: K, value: V): TrieNode<K, V> {
//        assert(!hasEntryAt(positionMask))

        val keyIndex = entryKeyIndex(positionMask)
        val newBuffer = buffer.insertEntryAtIndex(keyIndex, key, value)
        return TrieNode(dataMap or positionMask, nodeMap, newBuffer)
    }

    private fun mutableInsertEntryAt(positionMask: Int, key: K, value: V, owner: MutabilityOwnership): TrieNode<K, V> {
//        assert(!hasEntryAt(positionMask))

        val keyIndex = entryKeyIndex(positionMask)
        if (ownedBy === owner) {
            buffer = buffer.insertEntryAtIndex(keyIndex, key, value)
            dataMap = dataMap or positionMask
            return this
        }
        val newBuffer = buffer.insertEntryAtIndex(keyIndex, key, value)
        return TrieNode(dataMap or positionMask, nodeMap, newBuffer, owner)
    }

    private fun updateValueAtIndex(keyIndex: Int, value: V): TrieNode<K, V> {
//        assert(buffer[keyIndex + 1] !== value)

        val newBuffer = buffer.copyOf()
        newBuffer[keyIndex + 1] = value
        return TrieNode(dataMap, nodeMap, newBuffer)
    }

    private fun mutableUpdateValueAtIndex(keyIndex: Int, value: V, mutator: PersistentHashMapBuilder<K, V>): TrieNode<K, V> {
//        assert(buffer[keyIndex + 1] !== value)

        // If the [mutator] is exclusive owner of this node, update value at specified index in-place.
        if (ownedBy === mutator.ownership) {
            buffer[keyIndex + 1] = value
            return this
        }
        // Structural change due to node replacement.
        mutator.modCount++
        // Create new node with updated value at specified index.
        val newBuffer = buffer.copyOf()
        newBuffer[keyIndex + 1] = value
        return TrieNode(dataMap, nodeMap, newBuffer, mutator.ownership)
    }

    /** The given [newNode] must not be a part of any persistent map instance. */
    private fun updateNodeAtIndex(nodeIndex: Int, positionMask: Int, newNode: TrieNode<K, V>): TrieNode<K, V> {
//        assert(buffer[nodeIndex] !== newNode)
        val newNodeBuffer = newNode.buffer
        if (newNodeBuffer.size == 2 && newNode.nodeMap == 0) {
            if (buffer.size == 1) {
//                assert(dataMap == 0 && nodeMap xor positionMask == 0)
                newNode.dataMap = nodeMap
                return newNode
            }

            val keyIndex = entryKeyIndex(positionMask)
            val newBuffer = buffer.replaceNodeWithEntry(nodeIndex, keyIndex, newNodeBuffer[0], newNodeBuffer[1])
            return TrieNode(dataMap xor positionMask, nodeMap xor positionMask, newBuffer)
        }

        val newBuffer = buffer.copyOf(buffer.size)
        newBuffer[nodeIndex] = newNode
        return TrieNode(dataMap, nodeMap, newBuffer)
    }

    /** The given [newNode] must not be a part of any persistent map instance. */
    private fun mutableUpdateNodeAtIndex(nodeIndex: Int, newNode: TrieNode<K, V>, owner: MutabilityOwnership): TrieNode<K, V> {
//        assert(buffer[nodeIndex] !== newNode)

        // nodes (including collision nodes) that have only one entry are upped if they have no siblings
        if (buffer.size == 1 && newNode.buffer.size == ENTRY_SIZE && newNode.nodeMap == 0) {
//          assert(dataMap == 0 && nodeMap xor positionMask == 0)
            newNode.dataMap = nodeMap
            return newNode
        }

        if (ownedBy === owner) {
            buffer[nodeIndex] = newNode
            return this
        }
        val newBuffer = buffer.copyOf()
        newBuffer[nodeIndex] = newNode
        return TrieNode(dataMap, nodeMap, newBuffer, owner)
    }

    private fun removeNodeAtIndex(nodeIndex: Int, positionMask: Int): TrieNode<K, V>? {
//        assert(hasNodeAt(positionMask))
        if (buffer.size == 1) return null

        val newBuffer = buffer.removeNodeAtIndex(nodeIndex)
        return TrieNode(dataMap, nodeMap xor positionMask, newBuffer)
    }

    private fun mutableRemoveNodeAtIndex(nodeIndex: Int, positionMask: Int, owner: MutabilityOwnership): TrieNode<K, V>? {
//        assert(hasNodeAt(positionMask))
        if (buffer.size == 1) return null

        if (ownedBy === owner) {
            buffer = buffer.removeNodeAtIndex(nodeIndex)
            nodeMap = nodeMap xor positionMask
            return this
        }
        val newBuffer = buffer.removeNodeAtIndex(nodeIndex)
        return TrieNode(dataMap, nodeMap xor positionMask, newBuffer, owner)
    }


    private fun bufferMoveEntryToNode(keyIndex: Int, positionMask: Int, newKeyHash: Int,
                                      newKey: K, newValue: V, shift: Int, owner: MutabilityOwnership?): Array<Any?> {
        val storedKey = keyAtIndex(keyIndex)
        val storedKeyHash = storedKey.hashCode()
        val storedValue = valueAtKeyIndex(keyIndex)
        val newNode = makeNode(storedKeyHash, storedKey, storedValue,
                newKeyHash, newKey, newValue, shift + LOG_MAX_BRANCHING_FACTOR, owner)

        val nodeIndex = nodeIndex(positionMask) + 1 // place where to insert new node in the current buffer

        return buffer.replaceEntryWithNode(keyIndex, nodeIndex, newNode)
    }


    private fun moveEntryToNode(keyIndex: Int, positionMask: Int, newKeyHash: Int,
                                newKey: K, newValue: V, shift: Int): TrieNode<K, V> {
//        assert(hasEntryAt(positionMask))
//        assert(!hasNodeAt(positionMask))

        val newBuffer = bufferMoveEntryToNode(keyIndex, positionMask, newKeyHash, newKey, newValue, shift, null)
        return TrieNode(dataMap xor positionMask, nodeMap or positionMask, newBuffer)
    }

    private fun mutableMoveEntryToNode(keyIndex: Int, positionMask: Int, newKeyHash: Int,
                                       newKey: K, newValue: V, shift: Int, owner: MutabilityOwnership): TrieNode<K, V> {
//        assert(hasEntryAt(positionMask))
//        assert(!hasNodeAt(positionMask))

        if (ownedBy === owner) {
            buffer = bufferMoveEntryToNode(keyIndex, positionMask, newKeyHash, newKey, newValue, shift, owner)
            dataMap = dataMap xor positionMask
            nodeMap = nodeMap or positionMask
            return this
        }
        val newBuffer = bufferMoveEntryToNode(keyIndex, positionMask, newKeyHash, newKey, newValue, shift, owner)
        return TrieNode(dataMap xor positionMask, nodeMap or positionMask, newBuffer, owner)
    }

    /** Creates a new TrieNode for holding two given key value entries */
    private fun makeNode(keyHash1: Int, key1: K, value1: V,
                         keyHash2: Int, key2: K, value2: V, shift: Int, owner: MutabilityOwnership?): TrieNode<K, V> {
        if (shift > MAX_SHIFT) {
//            assert(key1 != key2)
            // when two key hashes are entirely equal: the last level subtrie node stores them just as unordered list
            return TrieNode(0, 0, arrayOf(key1, value1, key2, value2), owner)
        }

        val setBit1 = indexSegment(keyHash1, shift)
        val setBit2 = indexSegment(keyHash2, shift)

        if (setBit1 != setBit2) {
            val nodeBuffer = if (setBit1 < setBit2) {
                arrayOf(key1, value1, key2, value2)
            } else {
                arrayOf(key2, value2, key1, value1)
            }
            return TrieNode((1 shl setBit1) or (1 shl setBit2), 0, nodeBuffer, owner)
        }
        // hash segments at the given shift are equal: move these entries into the subtrie
        val node = makeNode(keyHash1, key1, value1, keyHash2, key2, value2, shift + LOG_MAX_BRANCHING_FACTOR, owner)
        return TrieNode(0, 1 shl setBit1, arrayOf<Any?>(node), owner)
    }

    private fun removeEntryAtIndex(keyIndex: Int, positionMask: Int): TrieNode<K, V>? {
//        assert(hasEntryAt(positionMask))
        if (buffer.size == ENTRY_SIZE) return null
        val newBuffer = buffer.removeEntryAtIndex(keyIndex)
        return TrieNode(dataMap xor positionMask, nodeMap, newBuffer)
    }

    private fun mutableRemoveEntryAtIndex(keyIndex: Int, positionMask: Int, mutator: PersistentHashMapBuilder<K, V>): TrieNode<K, V>? {
//        assert(hasEntryAt(positionMask))
        mutator.size--
        mutator.operationResult = valueAtKeyIndex(keyIndex)
        if (buffer.size == ENTRY_SIZE) return null

        if (ownedBy === mutator.ownership) {
            buffer = buffer.removeEntryAtIndex(keyIndex)
            dataMap = dataMap xor positionMask
            return this
        }
        val newBuffer = buffer.removeEntryAtIndex(keyIndex)
        return TrieNode(dataMap xor positionMask, nodeMap, newBuffer, mutator.ownership)
    }

    private fun collisionRemoveEntryAtIndex(i: Int): TrieNode<K, V>? {
        if (buffer.size == ENTRY_SIZE) return null
        val newBuffer = buffer.removeEntryAtIndex(i)
        return TrieNode(0, 0, newBuffer)
    }

    private fun mutableCollisionRemoveEntryAtIndex(i: Int, mutator: PersistentHashMapBuilder<K, V>): TrieNode<K, V>? {
        mutator.size--
        mutator.operationResult = valueAtKeyIndex(i)
        if (buffer.size == ENTRY_SIZE) return null

        if (ownedBy === mutator.ownership) {
            buffer = buffer.removeEntryAtIndex(i)
            return this
        }
        val newBuffer = buffer.removeEntryAtIndex(i)
        return TrieNode(0, 0, newBuffer, mutator.ownership)
    }

    private fun collisionContainsKey(key: K): Boolean {
        for (i in 0 until buffer.size step ENTRY_SIZE) {
            if (key == buffer[i]) return true
        }
        return false
    }

    private fun collisionGet(key: K): V? {
        for (i in 0 until buffer.size step ENTRY_SIZE) {
            if (key == keyAtIndex(i)) {
                return valueAtKeyIndex(i)
            }
        }
        return null
    }

    private fun collisionPut(key: K, value: V): ModificationResult<K, V>? {
        for (i in 0 until buffer.size step ENTRY_SIZE) {
            if (key == keyAtIndex(i)) {
                if (value === valueAtKeyIndex(i)) {
                    return null
                }
                val newBuffer = buffer.copyOf()
                newBuffer[i + 1] = value
                return TrieNode<K, V>(0, 0, newBuffer).asUpdateResult()
            }
        }
        val newBuffer = buffer.insertEntryAtIndex(0, key, value)
        return TrieNode<K, V>(0, 0, newBuffer).asInsertResult()
    }

    private fun mutableCollisionPut(key: K, value: V, mutator: PersistentHashMapBuilder<K, V>): TrieNode<K, V> {
        // Check if there is an entry with the specified key.
        for (i in 0 until buffer.size step ENTRY_SIZE) {
            if (key == keyAtIndex(i)) { // found entry with the specified key
                mutator.operationResult = valueAtKeyIndex(i)

                // If the [mutator] is exclusive owner of this node, update value of the entry in-place.
                if (ownedBy === mutator.ownership) {
                    buffer[i + 1] = value
                    return this
                }

                // Structural change due to node replacement.
                mutator.modCount++
                // Create new node with updated entry value.
                val newBuffer = buffer.copyOf()
                newBuffer[i + 1] = value
                return TrieNode(0, 0, newBuffer, mutator.ownership)
            }
        }
        // Create new collision node with the specified entry added to it.
        mutator.size++
        val newBuffer = buffer.insertEntryAtIndex(0, key, value)
        return TrieNode(0, 0, newBuffer, mutator.ownership)
    }

    private fun collisionRemove(key: K): TrieNode<K, V>? {
        for (i in 0 until buffer.size step ENTRY_SIZE) {
            if (key == keyAtIndex(i)) {
                return collisionRemoveEntryAtIndex(i)
            }
        }
        return this
    }

    private fun mutableCollisionRemove(key: K, mutator: PersistentHashMapBuilder<K, V>): TrieNode<K, V>? {
        for (i in 0 until buffer.size step ENTRY_SIZE) {
            if (key == keyAtIndex(i)) {
                return mutableCollisionRemoveEntryAtIndex(i, mutator)
            }
        }
        return this
    }

    private fun collisionRemove(key: K, value: V): TrieNode<K, V>? {
        for (i in 0 until buffer.size step ENTRY_SIZE) {
            if (key == keyAtIndex(i) && value == valueAtKeyIndex(i)) {
                return collisionRemoveEntryAtIndex(i)
            }
        }
        return this
    }

    private fun mutableCollisionRemove(key: K, value: V, mutator: PersistentHashMapBuilder<K, V>): TrieNode<K, V>? {
        for (i in 0 until buffer.size step ENTRY_SIZE) {
            if (key == keyAtIndex(i) && value == valueAtKeyIndex(i)) {
                return mutableCollisionRemoveEntryAtIndex(i, mutator)
            }
        }
        return this
    }

    private fun mutableCollisionPutAll(otherNode: TrieNode<K, V>,
                                       intersectionCounter: DeltaCounter,
                                       owner: MutabilityOwnership): TrieNode<K, V> {
        assert(nodeMap == 0)
        assert(dataMap == 0)
        assert(otherNode.nodeMap == 0)
        assert(otherNode.dataMap == 0)
        val tempBuffer = this.buffer.copyOf(newSize = this.buffer.size + otherNode.buffer.size)
        var i = this.buffer.size
        for (j in 0 until otherNode.buffer.size step ENTRY_SIZE) {
            @Suppress("UNCHECKED_CAST")
            if (!this.collisionContainsKey(otherNode.buffer[j] as K)) {
                tempBuffer[i] = otherNode.buffer[j]
                tempBuffer[i + 1] = otherNode.buffer[j + 1]
                i += ENTRY_SIZE
            } else intersectionCounter.count++
        }

        return when (val newSize = i) {
            this.buffer.size -> this
            otherNode.buffer.size -> otherNode
            tempBuffer.size -> TrieNode(0, 0, tempBuffer, owner)
            else -> TrieNode(0, 0, tempBuffer.copyOf(newSize), owner)
        }
    }

    private fun mutablePutAllFromOtherNodeCell(other: TrieNode<K, V>,
                                               positionMask: Int,
                                               shift: Int,
                                               intersectionCounter: DeltaCounter,
                                               mutator: PersistentHashMapBuilder<K, V>): TrieNode<K, V> {
        return when {
            other.hasNodeAt(positionMask) -> {
                mutablePutAll(
                        other.nodeAtIndex(other.nodeIndex(positionMask)),
                        shift + LOG_MAX_BRANCHING_FACTOR,
                        intersectionCounter,
                        mutator
                )
            }
            other.hasEntryAt(positionMask) -> {
                val keyIndex = other.entryKeyIndex(positionMask)
                val key = other.keyAtIndex(keyIndex)
                val value = other.valueAtKeyIndex(keyIndex)
                val oldSize = mutator.size
                val newNode = mutablePut(
                        key.hashCode(),
                        key,
                        value,
                        shift + LOG_MAX_BRANCHING_FACTOR,
                        mutator
                )
                if (mutator.size == oldSize) {
                    intersectionCounter.count++
                }
                newNode
            }
            else -> this
        }
    }

    private fun calculateSize(): Int {
        if (nodeMap == 0) return buffer.size / ENTRY_SIZE
        val numValues = dataMap.countOneBits()
        var result = numValues
        for(i in (numValues * ENTRY_SIZE) until buffer.size) {
            result += nodeAtIndex(i).calculateSize()
        }
        return result
    }

    private fun elementsIdentityEquals(otherNode: TrieNode<K, V>): Boolean {
        if (this === otherNode) return true
        if (nodeMap != otherNode.nodeMap) return false
        if (dataMap != otherNode.dataMap) return false
        for (i in 0 until buffer.size) {
            if(buffer[i] !== otherNode.buffer[i]) return false
        }
        return true
    }

    fun containsKey(keyHash: Int, key: K, shift: Int): Boolean {
        val keyPositionMask = 1 shl indexSegment(keyHash, shift)

        if (hasEntryAt(keyPositionMask)) { // key is directly in buffer
            return key == keyAtIndex(entryKeyIndex(keyPositionMask))
        }
        if (hasNodeAt(keyPositionMask)) { // key is in node
            val targetNode = nodeAtIndex(nodeIndex(keyPositionMask))
            if (shift == MAX_SHIFT) {
                return targetNode.collisionContainsKey(key)
            }
            return targetNode.containsKey(keyHash, key, shift + LOG_MAX_BRANCHING_FACTOR)
        }

        // key is absent
        return false
    }

    fun get(keyHash: Int, key: K, shift: Int): V? {
        val keyPositionMask = 1 shl indexSegment(keyHash, shift)

        if (hasEntryAt(keyPositionMask)) { // key is directly in buffer
            val keyIndex = entryKeyIndex(keyPositionMask)

            if (key == keyAtIndex(keyIndex)) {
                return valueAtKeyIndex(keyIndex)
            }
            return null
        }
        if (hasNodeAt(keyPositionMask)) { // key is in node
            val targetNode = nodeAtIndex(nodeIndex(keyPositionMask))
            if (shift == MAX_SHIFT) {
                return targetNode.collisionGet(key)
            }
            return targetNode.get(keyHash, key, shift + LOG_MAX_BRANCHING_FACTOR)
        }

        // key is absent
        return null
    }

    fun mutablePutAll(otherNode: TrieNode<K, V>,
                      shift: Int,
                      intersectionCounter: DeltaCounter,
                      mutator: PersistentHashMapBuilder<K, V>): TrieNode<K, V> {
        if (this === otherNode) {
            intersectionCounter += calculateSize()
            return this
        }
        // the collision case
        if (shift > MAX_SHIFT) {
            return mutableCollisionPutAll(otherNode, intersectionCounter, mutator.ownership)
        }

        // new nodes are where either of the old ones were
        var newNodeMap = nodeMap or otherNode.nodeMap
        // entries stay being entries only if one bits were in exactly one of input nodes
        // but not in the new data nodes
        var newDataMap = dataMap xor otherNode.dataMap and newNodeMap.inv()
        // (**) now, this is tricky: we have a number of entry-entry pairs and we don't know yet whether
        // they result in an entry (if they are equal) or a new node (if they are not)
        // but we want to keep it to single allocation, so we check and mark equal ones here
        (dataMap and otherNode.dataMap).forEachOneBit { positionMask, _ ->
            val leftKey = this.keyAtIndex(this.entryKeyIndex(positionMask))
            val rightKey = otherNode.keyAtIndex(otherNode.entryKeyIndex(positionMask))
            // if they are equal, put them in the data map
            if (leftKey == rightKey) newDataMap = newDataMap or positionMask
            // if they are not, put them in the node map
            else newNodeMap = newNodeMap or positionMask
            // we can use this later to skip calling equals() again
        }
        assert(newNodeMap and newDataMap == 0)
        val mutableNode = when {
            this.ownedBy == mutator.ownership && this.dataMap == newDataMap && this.nodeMap == newNodeMap -> this
            else -> {
                val newBuffer = arrayOfNulls<Any>(newDataMap.countOneBits() * ENTRY_SIZE + newNodeMap.countOneBits())
                TrieNode(newDataMap, newNodeMap, newBuffer)
            }
        }
        newNodeMap.forEachOneBit { positionMask, index ->
            val newNodeIndex = mutableNode.buffer.size - 1 - index
            mutableNode.buffer[newNodeIndex] = when {
                hasNodeAt(positionMask) -> {
                    val before = nodeAtIndex(nodeIndex(positionMask))
                    before.mutablePutAllFromOtherNodeCell(otherNode, positionMask, shift, intersectionCounter, mutator)
                }

                otherNode.hasNodeAt(positionMask) -> {
                    val before = otherNode.nodeAtIndex(otherNode.nodeIndex(positionMask))
                    before.mutablePutAllFromOtherNodeCell(this, positionMask, shift, intersectionCounter, mutator)
                }

                else -> { // two entries, and they are not equal by key (see ** above)
                    val thisKeyIndex = this.entryKeyIndex(positionMask)
                    val thisKey = this.keyAtIndex(thisKeyIndex)
                    val thisValue = this.valueAtKeyIndex(thisKeyIndex)
                    val otherKeyIndex = otherNode.entryKeyIndex(positionMask)
                    val otherKey = otherNode.keyAtIndex(otherKeyIndex)
                    val otherValue = otherNode.valueAtKeyIndex(otherKeyIndex)
                    makeNode(
                            thisKey.hashCode(),
                            thisKey,
                            thisValue,
                            otherKey.hashCode(),
                            otherKey,
                            otherValue,
                            shift + LOG_MAX_BRANCHING_FACTOR,
                            mutator.ownership
                    )
                }
            }
        }
        newDataMap.forEachOneBit { positionMask, index ->
            val newKeyIndex = index * ENTRY_SIZE
            when {
                !otherNode.hasEntryAt(positionMask) -> {
                    val oldKeyIndex = this.entryKeyIndex(positionMask)
                    mutableNode.buffer[newKeyIndex] = this.keyAtIndex(oldKeyIndex)
                    mutableNode.buffer[newKeyIndex + 1] = this.valueAtKeyIndex(oldKeyIndex)
                }
                // there is either only one entry in otherNode, or
                // both entries are here => they are equal, see ** above
                // so just overwrite that
                else -> {
                    val oldKeyIndex = otherNode.entryKeyIndex(positionMask)
                    mutableNode.buffer[newKeyIndex] = otherNode.keyAtIndex(oldKeyIndex)
                    mutableNode.buffer[newKeyIndex + 1] = otherNode.valueAtKeyIndex(oldKeyIndex)
                    if (this.hasEntryAt(positionMask)) intersectionCounter.count++
                }
            }
        }
        return when {
            this.elementsIdentityEquals(mutableNode) -> this
            otherNode.elementsIdentityEquals(mutableNode) -> otherNode
            else -> mutableNode
        }
    }

    fun put(keyHash: Int, key: K, value: @UnsafeVariance V, shift: Int): ModificationResult<K, V>? {
        val keyPositionMask = 1 shl indexSegment(keyHash, shift)

        if (hasEntryAt(keyPositionMask)) { // key is directly in buffer
            val keyIndex = entryKeyIndex(keyPositionMask)

            if (key == keyAtIndex(keyIndex)) {
                if (valueAtKeyIndex(keyIndex) === value) return null

                return updateValueAtIndex(keyIndex, value).asUpdateResult()
            }
            return moveEntryToNode(keyIndex, keyPositionMask, keyHash, key, value, shift).asInsertResult()
        }
        if (hasNodeAt(keyPositionMask)) { // key is in node
            val nodeIndex = nodeIndex(keyPositionMask)

            val targetNode = nodeAtIndex(nodeIndex)
            val putResult = if (shift == MAX_SHIFT) {
                targetNode.collisionPut(key, value) ?: return null
            } else {
                targetNode.put(keyHash, key, value, shift + LOG_MAX_BRANCHING_FACTOR) ?: return null
            }
            return putResult.replaceNode { node -> updateNodeAtIndex(nodeIndex, keyPositionMask, node) }
        }

        // no entry at this key hash segment
        return insertEntryAt(keyPositionMask, key, value).asInsertResult()
    }

    fun mutablePut(keyHash: Int, key: K, value: @UnsafeVariance V, shift: Int, mutator: PersistentHashMapBuilder<K, V>): TrieNode<K, V> {
        val keyPositionMask = 1 shl indexSegment(keyHash, shift)

        if (hasEntryAt(keyPositionMask)) { // key is directly in buffer
            val keyIndex = entryKeyIndex(keyPositionMask)

            if (key == keyAtIndex(keyIndex)) {
                mutator.operationResult = valueAtKeyIndex(keyIndex)
                if (valueAtKeyIndex(keyIndex) === value) {
                    return this
                }

                return mutableUpdateValueAtIndex(keyIndex, value, mutator)
            }
            mutator.size++
            return mutableMoveEntryToNode(keyIndex, keyPositionMask, keyHash, key, value, shift, mutator.ownership)
        }
        if (hasNodeAt(keyPositionMask)) { // key is in node
            val nodeIndex = nodeIndex(keyPositionMask)

            val targetNode = nodeAtIndex(nodeIndex)
            val newNode = if (shift == MAX_SHIFT) {
                targetNode.mutableCollisionPut(key, value, mutator)
            } else {
                targetNode.mutablePut(keyHash, key, value, shift + LOG_MAX_BRANCHING_FACTOR, mutator)
            }
            if (targetNode === newNode) {
                return this
            }
            return mutableUpdateNodeAtIndex(nodeIndex, newNode, mutator.ownership)
        }

        // key is absent
        mutator.size++
        return mutableInsertEntryAt(keyPositionMask, key, value, mutator.ownership)
    }

    fun remove(keyHash: Int, key: K, shift: Int): TrieNode<K, V>? {
        val keyPositionMask = 1 shl indexSegment(keyHash, shift)

        if (hasEntryAt(keyPositionMask)) { // key is directly in buffer
            val keyIndex = entryKeyIndex(keyPositionMask)

            if (key == keyAtIndex(keyIndex)) {
                return removeEntryAtIndex(keyIndex, keyPositionMask)
            }
            return this
        }
        if (hasNodeAt(keyPositionMask)) { // key is in node
            val nodeIndex = nodeIndex(keyPositionMask)

            val targetNode = nodeAtIndex(nodeIndex)
            val newNode = if (shift == MAX_SHIFT) {
                targetNode.collisionRemove(key)
            } else {
                targetNode.remove(keyHash, key, shift + LOG_MAX_BRANCHING_FACTOR)
            }
            return replaceNode(targetNode, newNode, nodeIndex, keyPositionMask)
        }

        // key is absent
        return this
    }

    private fun replaceNode(targetNode: TrieNode<K, V>, newNode: TrieNode<K, V>?, nodeIndex: Int, positionMask: Int) = when {
        newNode == null ->
            removeNodeAtIndex(nodeIndex, positionMask)
        targetNode !== newNode ->
            updateNodeAtIndex(nodeIndex, positionMask, newNode)
        else ->
            this
    }

    fun mutableRemove(keyHash: Int, key: K, shift: Int, mutator: PersistentHashMapBuilder<K, V>): TrieNode<K, V>? {
        val keyPositionMask = 1 shl indexSegment(keyHash, shift)

        if (hasEntryAt(keyPositionMask)) { // key is directly in buffer
            val keyIndex = entryKeyIndex(keyPositionMask)

            if (key == keyAtIndex(keyIndex)) {
                return mutableRemoveEntryAtIndex(keyIndex, keyPositionMask, mutator)
            }
            return this
        }
        if (hasNodeAt(keyPositionMask)) { // key is in node
            val nodeIndex = nodeIndex(keyPositionMask)

            val targetNode = nodeAtIndex(nodeIndex)
            val newNode = if (shift == MAX_SHIFT) {
                targetNode.mutableCollisionRemove(key, mutator)
            } else {
                targetNode.mutableRemove(keyHash, key, shift + LOG_MAX_BRANCHING_FACTOR, mutator)
            }
            return mutableReplaceNode(targetNode, newNode, nodeIndex, keyPositionMask, mutator.ownership)
        }

        // key is absent
        return this
    }

    private fun mutableReplaceNode(targetNode: TrieNode<K, V>, newNode: TrieNode<K, V>?, nodeIndex: Int, positionMask: Int, owner: MutabilityOwnership) = when {
        newNode == null ->
            mutableRemoveNodeAtIndex(nodeIndex, positionMask, owner)
        ownedBy === owner || targetNode !== newNode ->
            mutableUpdateNodeAtIndex(nodeIndex, newNode, owner)
        else ->
            this
    }

    fun remove(keyHash: Int, key: K, value: @UnsafeVariance V, shift: Int): TrieNode<K, V>? {
        val keyPositionMask = 1 shl indexSegment(keyHash, shift)

        if (hasEntryAt(keyPositionMask)) { // key is directly in buffer
            val keyIndex = entryKeyIndex(keyPositionMask)

            if (key == keyAtIndex(keyIndex) && value == valueAtKeyIndex(keyIndex)) {
                return removeEntryAtIndex(keyIndex, keyPositionMask)
            }
            return this
        }
        if (hasNodeAt(keyPositionMask)) { // key is in node
            val nodeIndex = nodeIndex(keyPositionMask)

            val targetNode = nodeAtIndex(nodeIndex)
            val newNode = if (shift == MAX_SHIFT) {
                targetNode.collisionRemove(key, value)
            } else {
                targetNode.remove(keyHash, key, value, shift + LOG_MAX_BRANCHING_FACTOR)
            }
            return replaceNode(targetNode, newNode, nodeIndex, keyPositionMask)
        }

        // key is absent
        return this
    }

    fun mutableRemove(keyHash: Int, key: K, value: @UnsafeVariance V, shift: Int, mutator: PersistentHashMapBuilder<K, V>): TrieNode<K, V>? {
        val keyPositionMask = 1 shl indexSegment(keyHash, shift)

        if (hasEntryAt(keyPositionMask)) { // key is directly in buffer
            val keyIndex = entryKeyIndex(keyPositionMask)

            if (key == keyAtIndex(keyIndex) && value == valueAtKeyIndex(keyIndex)) {
                return mutableRemoveEntryAtIndex(keyIndex, keyPositionMask, mutator)
            }
            return this
        }
        if (hasNodeAt(keyPositionMask)) { // key is in node
            val nodeIndex = nodeIndex(keyPositionMask)

            val targetNode = nodeAtIndex(nodeIndex)
            val newNode = if (shift == MAX_SHIFT) {
                targetNode.mutableCollisionRemove(key, value, mutator)
            } else {
                targetNode.mutableRemove(keyHash, key, value, shift + LOG_MAX_BRANCHING_FACTOR, mutator)
            }
            return mutableReplaceNode(targetNode, newNode, nodeIndex, keyPositionMask, mutator.ownership)
        }

        // key is absent
        return this
    }

    // For testing trie structure
    internal fun accept(visitor: (node: TrieNode<K, V>, shift: Int, hash: Int, dataMap: Int, nodeMap: Int) -> Unit) {
        accept(visitor, 0, 0)
    }

    private fun accept(
            visitor: (node: TrieNode<K, V>, shift: Int, hash: Int, dataMap: Int, nodeMap: Int) -> Unit,
            hash: Int,
            shift: Int
    ) {
        visitor(this, shift, hash, dataMap, nodeMap)

        var nodePositions = nodeMap
        while (nodePositions != 0) {
            val mask = nodePositions.takeLowestOneBit()
//            assert(hasNodeAt(mask))

            val hashSegment = mask.countTrailingZeroBits()

            val childNode = nodeAtIndex(nodeIndex(mask))
            childNode.accept(visitor, hash + (hashSegment shl shift), shift + LOG_MAX_BRANCHING_FACTOR)

            nodePositions -= mask
        }
    }

    internal companion object {
        internal val EMPTY = TrieNode<Nothing, Nothing>(0, 0, emptyArray())
    }
}