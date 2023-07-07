/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package androidx.compose.runtime.external.kotlinx.collections.immutable.implementations.immutableList

/**
 * The class responsible for iterating over elements of the [PersistentVectorBuilder].
 *
 * There are two parts where the elements of the builder are located: root and tail.
 * [TrieIterator] is responsible for iterating over elements located at root,
 * whereas tail elements are iterated directly from this class.
 */
internal class PersistentVectorMutableIterator<T>(
        private val builder: PersistentVectorBuilder<T>,
        index: Int
) : MutableListIterator<T>, AbstractListIterator<T>(index, builder.size) {

    /**
     * The modCount this iterator is aware of.
     * Used to check if the [PersistentVectorBuilder] was modified outside this iterator.
     */
    private var expectedModCount = builder.getModCount()
    /**
     * Iterates over leaves of the builder.root trie.
     * This property is equal to null if builder.root is null.
     */
    private var trieIterator: TrieIterator<T>? = null
    /**
     * Index of the element this iterator returned from last invocation of next() or previous().
     * Used to remove or set new value at this index.
     * This property is set to -1 when method `add(element: T)` or `remove()` gets invoked.
     */
    private var lastIteratedIndex = -1

    init {
        setupTrieIterator()
    }

    override fun previous(): T {
        checkForComodification()
        checkHasPrevious()

        lastIteratedIndex = index - 1

        @Suppress("UNCHECKED_CAST")
        val trieIterator = this.trieIterator ?: return builder.tail[--index] as T
        if (index > trieIterator.size) {
            @Suppress("UNCHECKED_CAST")
            return builder.tail[--index - trieIterator.size] as T
        }
        index--
        return trieIterator.previous()
    }

    override fun next(): T {
        checkForComodification()
        checkHasNext()

        lastIteratedIndex = index

        @Suppress("UNCHECKED_CAST")
        val trieIterator = this.trieIterator ?: return builder.tail[index++] as T
        if (trieIterator.hasNext()) {
            index++
            return trieIterator.next()
        }
        @Suppress("UNCHECKED_CAST")
        return builder.tail[index++ - trieIterator.size] as T
    }

    private fun reset() {
        size = builder.size
        expectedModCount = builder.getModCount()
        lastIteratedIndex = -1

        setupTrieIterator()
    }

    private fun setupTrieIterator() {
        val root = builder.root
        if (root == null) {
            trieIterator = null
            return
        }

        val trieSize = rootSize(builder.size)
        val trieIndex = index.coerceAtMost(trieSize)
        val trieHeight = builder.rootShift / LOG_MAX_BUFFER_SIZE + 1
        if (trieIterator == null) {
            trieIterator = TrieIterator(root, trieIndex, trieSize, trieHeight)
        } else {
            trieIterator!!.reset(root, trieIndex, trieSize, trieHeight)
        }
    }

    override fun add(element: T) {
        checkForComodification()

        builder.add(index, element)
        index++
        reset()
    }

    override fun remove() {
        checkForComodification()
        checkHasIterated()

        builder.removeAt(lastIteratedIndex)
        if (lastIteratedIndex < index) index = lastIteratedIndex
        reset()
    }

    override fun set(element: T) {
        checkForComodification()
        checkHasIterated()

        builder[lastIteratedIndex] = element

        expectedModCount = builder.getModCount()
        setupTrieIterator()
    }

    private fun checkForComodification() {
        if (expectedModCount != builder.getModCount())
            throw ConcurrentModificationException()
    }

    private fun checkHasIterated() {
        if (lastIteratedIndex == -1)
            throw IllegalStateException()
    }
}