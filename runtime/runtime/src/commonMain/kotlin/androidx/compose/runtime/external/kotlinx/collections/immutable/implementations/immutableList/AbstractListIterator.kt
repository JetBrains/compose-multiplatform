/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package androidx.compose.runtime.external.kotlinx.collections.immutable.implementations.immutableList

internal abstract class AbstractListIterator<out E>(var index: Int, var size: Int) : ListIterator<E> {
    override fun hasNext(): Boolean {
        return index < size
    }

    override fun hasPrevious(): Boolean {
        return index > 0
    }

    override fun nextIndex(): Int {
        return index
    }

    override fun previousIndex(): Int {
        return index - 1
    }

    internal fun checkHasNext() {
        if (!hasNext())
            throw NoSuchElementException()
    }

    internal fun checkHasPrevious() {
        if (!hasPrevious())
            throw NoSuchElementException()
    }
}


internal class SingleElementListIterator<E>(private val element: E, index: Int): AbstractListIterator<E>(index, 1) {
    override fun next(): E {
        checkHasNext()
        index++
        return element
    }

    override fun previous(): E {
        checkHasPrevious()
        index--
        return element
    }
}