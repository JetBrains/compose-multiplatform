/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package androidx.compose.runtime.external.kotlinx.collections.immutable.implementations.immutableList

import androidx.compose.runtime.external.kotlinx.collections.immutable.ImmutableList
import androidx.compose.runtime.external.kotlinx.collections.immutable.PersistentList
import androidx.compose.runtime.external.kotlinx.collections.immutable.internal.ListImplementation.checkElementIndex
import androidx.compose.runtime.external.kotlinx.collections.immutable.internal.ListImplementation.checkPositionIndex
import androidx.compose.runtime.external.kotlinx.collections.immutable.internal.assert
import androidx.compose.runtime.external.kotlinx.collections.immutable.mutate

internal class SmallPersistentVector<E>(private val buffer: Array<Any?>) : ImmutableList<E>, AbstractPersistentList<E>() {

    init {
        assert(buffer.size <= MAX_BUFFER_SIZE)
    }

    override val size: Int
        get() = buffer.size

    private fun bufferOfSize(size: Int): Array<Any?> {
        return arrayOfNulls<Any?>(size)
    }

    override fun add(element: E): PersistentList<E> {
        if (size < MAX_BUFFER_SIZE) {
            val newBuffer = buffer.copyOf(size + 1)
            newBuffer[size] = element
            return SmallPersistentVector(newBuffer)
        }
        val tail = presizedBufferWith(element)
        return PersistentVector(buffer, tail, size + 1, 0)
    }

    override fun addAll(elements: Collection<E>): PersistentList<E> {
        if (size + elements.size <= MAX_BUFFER_SIZE) {
            val newBuffer = buffer.copyOf(size + elements.size)
            // TODO: investigate performance of elements.toArray + copyInto
            var index = size
            for (element in elements) {
                newBuffer[index++] = element
            }
            return SmallPersistentVector(newBuffer)
        }
        return mutate { it.addAll(elements) }
    }

    override fun removeAll(predicate: (E) -> Boolean): PersistentList<E> {
        var newBuffer = buffer
        var newSize = size

        var anyRemoved = false

        for (index in 0 until size) {
            @Suppress("UNCHECKED_CAST")
            val element = buffer[index] as E

            if (predicate(element)) {
                if (!anyRemoved) {
                    newBuffer = buffer.copyOf()
                    newSize = index

                    anyRemoved = true
                }
            } else if (anyRemoved) {
                newBuffer[newSize++] = element
            }
        }
        return when (newSize) {
            size -> this
            0 -> EMPTY
            else -> SmallPersistentVector(newBuffer.copyOfRange(0, newSize))
        }
    }

    override fun addAll(index: Int, c: Collection<E>): PersistentList<E> {
        checkPositionIndex(index, size)
        if (size + c.size <= MAX_BUFFER_SIZE) {
            val newBuffer = bufferOfSize(size + c.size)
            buffer.copyInto(newBuffer, endIndex = index)
            buffer.copyInto(newBuffer, index + c.size, index, size)
            var position = index
            for (element in c) {
                newBuffer[position++] = element
            }
            return SmallPersistentVector(newBuffer)
        }
        return mutate { it.addAll(index, c) }
    }

    override fun add(index: Int, element: E): PersistentList<E> {
        checkPositionIndex(index, size)
        if (index == size) {
            return add(element)
        }

        if (size < MAX_BUFFER_SIZE) {
            // TODO: copyOf + one copyInto?
            val newBuffer = bufferOfSize(size + 1)
            buffer.copyInto(newBuffer, endIndex = index)
            buffer.copyInto(newBuffer, index + 1, index, size)
            newBuffer[index] = element
            return SmallPersistentVector(newBuffer)
        }

        val root = buffer.copyOf()
        buffer.copyInto(root, index + 1, index, size - 1)
        root[index] = element
        val tail = presizedBufferWith(buffer[MAX_BUFFER_SIZE_MINUS_ONE])
        return PersistentVector(root, tail, size + 1, 0)
    }

    override fun removeAt(index: Int): PersistentList<E> {
        checkElementIndex(index, size)
        if (size == 1) {
            return EMPTY
        }
        val newBuffer = buffer.copyOf(size - 1)
        buffer.copyInto(newBuffer, index, index + 1, size)
        return SmallPersistentVector(newBuffer)
    }

    override fun builder(): PersistentList.Builder<E> {
        return PersistentVectorBuilder(this, null, buffer, 0)
    }

    override fun indexOf(element: E): Int {
        return buffer.indexOf(element)
    }

    override fun lastIndexOf(element: E): Int {
        return buffer.lastIndexOf(element)
    }

    override fun listIterator(index: Int): ListIterator<E> {
        checkPositionIndex(index, size)
        @Suppress("UNCHECKED_CAST")
        return BufferIterator(buffer as Array<E>, index, size)
    }

    override fun get(index: Int): E {
        // TODO: use elementAt(index)?
        checkElementIndex(index, size)
        @Suppress("UNCHECKED_CAST")
        return buffer[index] as E
    }

    override fun set(index: Int, element: E): PersistentList<E> {
        checkElementIndex(index, size)
        val newBuffer = buffer.copyOf()
        newBuffer[index] = element
        return SmallPersistentVector(newBuffer)
    }

    companion object {
        val EMPTY = SmallPersistentVector<Nothing>(emptyArray())
    }
}