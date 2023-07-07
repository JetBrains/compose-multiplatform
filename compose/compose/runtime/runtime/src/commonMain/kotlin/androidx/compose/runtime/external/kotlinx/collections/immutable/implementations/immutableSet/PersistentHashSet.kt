/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package androidx.compose.runtime.external.kotlinx.collections.immutable.implementations.immutableSet

import androidx.compose.runtime.external.kotlinx.collections.immutable.PersistentSet
import androidx.compose.runtime.external.kotlinx.collections.immutable.mutate

internal class PersistentHashSet<E>(internal val node: TrieNode<E>,
                                    override val size: Int): AbstractSet<E>(), PersistentSet<E> {
    override fun contains(element: E): Boolean {
        return node.contains(element.hashCode(), element, 0)
    }

    override fun add(element: E): PersistentSet<E> {
        val newNode = node.add(element.hashCode(), element, 0)
        if (node === newNode) { return this }
        return PersistentHashSet(newNode, size + 1)
    }

    override fun addAll(elements: Collection<E>): PersistentSet<E> {
        return this.mutate { it.addAll(elements) }
    }

    override fun remove(element: E): PersistentSet<E> {
        val newNode = node.remove(element.hashCode(), element, 0)
        if (node === newNode) { return this }
        return PersistentHashSet(newNode, size - 1)
    }

    override fun removeAll(elements: Collection<E>): PersistentSet<E> {
        return mutate { it.removeAll(elements) }
    }

    override fun removeAll(predicate: (E) -> Boolean): PersistentSet<E> {
        return mutate { it.removeAll(predicate) }
    }

    override fun retainAll(elements: Collection<E>): PersistentSet<E> {
        return mutate { it.retainAll(elements) }
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

    override fun clear(): PersistentSet<E> {
        return PersistentHashSet.emptyOf()
    }

    override fun iterator(): Iterator<E> {
        return PersistentHashSetIterator(node)
    }

    override fun builder(): PersistentSet.Builder<E> {
        return PersistentHashSetBuilder(this)
    }

    internal companion object {
        private val EMPTY = PersistentHashSet(TrieNode.EMPTY, 0)
        internal fun <E> emptyOf(): PersistentSet<E> = PersistentHashSet.EMPTY
    }
}
