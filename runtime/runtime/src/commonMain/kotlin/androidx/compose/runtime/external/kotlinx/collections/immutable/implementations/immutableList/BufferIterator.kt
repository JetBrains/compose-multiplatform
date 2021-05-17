/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable.implementations.immutableList

internal class BufferIterator<out T>(
        private val buffer: Array<T>,
        index: Int,
        size: Int
) : AbstractListIterator<T>(index, size) {
    override fun next(): T {
        if (!hasNext()) {
            throw NoSuchElementException()
        }
        return buffer[index++]
    }

    override fun previous(): T {
        if (!hasPrevious()) {
            throw NoSuchElementException()
        }
        return buffer[--index]
    }
}