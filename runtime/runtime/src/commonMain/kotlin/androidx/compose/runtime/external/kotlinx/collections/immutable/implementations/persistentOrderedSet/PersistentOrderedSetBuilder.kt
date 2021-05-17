/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable.implementations.persistentOrderedSet

import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.internal.EndOfChain
import kotlinx.collections.immutable.internal.assert

internal class PersistentOrderedSetBuilder<E>(private var set: PersistentOrderedSet<E>) : AbstractMutableSet<E>(), PersistentSet.Builder<E> {
    internal var firstElement = set.firstElement
    private var lastElement = set.lastElement
    internal val hashMapBuilder = set.hashMap.builder()

    override val size: Int
        get() = hashMapBuilder.size

    override fun build(): PersistentSet<E> {
        val newMap = hashMapBuilder.build()
        set = if (newMap === set.hashMap) {
            assert(firstElement === set.firstElement)
            assert(lastElement === set.lastElement)
            set
        } else {
            PersistentOrderedSet(firstElement, lastElement, newMap)
        }
        return set
    }

    override fun contains(element: E): Boolean {
        return hashMapBuilder.containsKey(element)
    }

    override fun add(element: E): Boolean {
        if (hashMapBuilder.containsKey(element)) {
            return false
        }
        if (isEmpty()) {
            firstElement = element
            lastElement = element
            hashMapBuilder[element] = Links()
            return true
        }

        val lastLinks = hashMapBuilder[lastElement]!!
//        assert(!lastLinks.hasNext)
        @Suppress("UNCHECKED_CAST")
        hashMapBuilder[lastElement as E] = lastLinks.withNext(element)
        hashMapBuilder[element] = Links(previous = lastElement)
        lastElement = element

        return true
    }

    override fun remove(element: E): Boolean {
        val links = hashMapBuilder.remove(element) ?: return false

        if (links.hasPrevious) {
            val previousLinks = hashMapBuilder[links.previous]!!
//            assert(previousLinks.next == element)
            @Suppress("UNCHECKED_CAST")
            hashMapBuilder[links.previous as E] = previousLinks.withNext(links.next)
        } else {
            firstElement = links.next
        }
        if (links.hasNext) {
            val nextLinks = hashMapBuilder[links.next]!!
//            assert(nextLinks.previous == element)
            @Suppress("UNCHECKED_CAST")
            hashMapBuilder[links.next as E] = nextLinks.withPrevious(links.previous)
        } else {
            lastElement = links.previous
        }

        return true
    }

    override fun clear() {
        hashMapBuilder.clear()
        firstElement = EndOfChain
        lastElement = EndOfChain
    }

    override fun iterator(): MutableIterator<E> {
        return PersistentOrderedSetMutableIterator(this)
    }
}