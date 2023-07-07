/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package androidx.compose.runtime.external.kotlinx.collections.immutable.implementations.immutableSet

import androidx.compose.runtime.external.kotlinx.collections.immutable.PersistentSet
import androidx.compose.runtime.external.kotlinx.collections.immutable.internal.DeltaCounter
import androidx.compose.runtime.external.kotlinx.collections.immutable.internal.MutabilityOwnership

internal class PersistentHashSetBuilder<E>(private var set: PersistentHashSet<E>) : AbstractMutableSet<E>(), PersistentSet.Builder<E> {
    internal var ownership = MutabilityOwnership()
        private set
    internal var node = set.node
        private set
    internal var modCount = 0
        private set

    // Size change implies structural changes.
    override var size = set.size
        set(value) {
            field = value
            modCount++
        }

    override fun build(): PersistentHashSet<E> {
        set = if (node === set.node) {
            set
        } else {
            ownership = MutabilityOwnership()
            PersistentHashSet(node, size)
        }
        return set
    }

    override fun contains(element: E): Boolean {
        return node.contains(element.hashCode(), element, 0)
    }

    override fun add(element: E): Boolean {
        val size = this.size
        node = node.mutableAdd(element.hashCode(), element, 0, this)
        return size != this.size
    }

    override fun addAll(elements: Collection<E>): Boolean {
        val set = elements as? PersistentHashSet ?: (elements as? PersistentHashSetBuilder)?.build()
        if (set !== null) {
            val deltaCounter = DeltaCounter()
            val size = this.size
            val result = node.mutableAddAll(set.node, 0, deltaCounter, this)
            val newSize = size + elements.size - deltaCounter.count
            if (size != newSize) {
                this.node = result
                this.size = newSize
            }
            return size != this.size
        }
        return super.addAll(elements)
    }

    override fun retainAll(elements: Collection<E>): Boolean {
        val set = elements as? PersistentHashSet ?: (elements as? PersistentHashSetBuilder)?.build()
        if (set !== null) {
            val deltaCounter = DeltaCounter()
            val size = this.size
            val result = node.mutableRetainAll(set.node, 0, deltaCounter, this)
            when (val newSize = deltaCounter.count) {
                0 -> clear()
                size -> {}
                else -> {
                    @Suppress("UNCHECKED_CAST")
                    this.node = result as TrieNode<E>
                    this.size = newSize
                }
            }
            return size != this.size
        }
        return super.retainAll(elements)
    }

    override fun removeAll(elements: Collection<E>): Boolean {
        val set = elements as? PersistentHashSet ?: (elements as? PersistentHashSetBuilder)?.build()
        if (set !== null) {
            val counter = DeltaCounter()
            val size = this.size
            val result = node.mutableRemoveAll(set.node, 0, counter, this)

            when (val newSize = size - counter.count) {
                0 -> clear()
                size -> {}
                else -> {
                    @Suppress("UNCHECKED_CAST")
                    this.node = result as TrieNode<E>
                    this.size = newSize
                }
            }
            return size != this.size
        }
        return super.removeAll(elements)
    }

    override fun containsAll(elements: Collection<E>): Boolean {
        if (elements is PersistentHashSet) {
            return node.containsAll(elements.node, 0)
        }
        if (elements is PersistentHashSetBuilder) {
            return node.containsAll(elements.node, 0)
        }
        return super.containsAll(elements)
    }

    override fun remove(element: E): Boolean {
        val size = this.size
        @Suppress("UNCHECKED_CAST")
        node = node.mutableRemove(element.hashCode(), element, 0, this)
        return size != this.size
    }

    override fun clear() {
        @Suppress("UNCHECKED_CAST")
        node = TrieNode.EMPTY as TrieNode<E>
        size = 0
    }

    override fun iterator(): MutableIterator<E> {
        return PersistentHashSetMutableIterator(this)
    }
}