/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable.implementations.persistentOrderedSet

internal open class PersistentOrderedSetIterator<E>(private var nextElement: Any?,
                                                    internal val map: Map<E, Links>) : Iterator<E> {
    internal var index = 0

    override fun hasNext(): Boolean {
        return index < map.size
    }

    override fun next(): E {
        checkHasNext()

        @Suppress("UNCHECKED_CAST")
        val result = nextElement as E
        index++
        nextElement = map.getOrElse(result) {
            throw ConcurrentModificationException("Hash code of an element ($result) has changed after it was added to the persistent set.")
        }.next
        return result
    }

    private fun checkHasNext() {
        if (!hasNext())
            throw NoSuchElementException()
    }
}
