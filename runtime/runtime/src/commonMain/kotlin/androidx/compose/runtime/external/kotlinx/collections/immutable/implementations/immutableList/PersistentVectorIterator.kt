/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable.implementations.immutableList

internal class PersistentVectorIterator<out T>(root: Array<Any?>,
                                               private val tail: Array<T>,
                                               index: Int,
                                               size: Int,
                                               trieHeight: Int) : AbstractListIterator<T>(index, size) {
    private val trieIterator: TrieIterator<T>

    init {
        val trieSize = rootSize(size)
        val trieIndex = index.coerceAtMost(trieSize)
        trieIterator = TrieIterator(root, trieIndex, trieSize, trieHeight)
    }

    override fun next(): T {
        checkHasNext()
        if (trieIterator.hasNext()) {
            index++
            return trieIterator.next()
        }
        return tail[index++ - trieIterator.size]
    }

    override fun previous(): T {
        checkHasPrevious()
        if (index > trieIterator.size) {
            return tail[--index - trieIterator.size]
        }
        index--
        return trieIterator.previous()
    }
}