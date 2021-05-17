/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable.implementations.immutableList

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.internal.ListImplementation.checkElementIndex
import kotlinx.collections.immutable.internal.ListImplementation.checkPositionIndex
import kotlinx.collections.immutable.internal.assert
import kotlinx.collections.immutable.mutate

/**
 * Persistent vector made of a trie of leaf buffers entirely filled with [MAX_BUFFER_SIZE] elements and a tail having
 * from 1 to [MAX_BUFFER_SIZE] elements.
 *
 * @param root the root of trie part of the vector, must contain at least one leaf buffer
 * @param tail the non-empty tail part of the vector
 * @param size the size of the vector, must be greater than [MAX_BUFFER_SIZE]
 * @param rootShift specifies the height of the trie structure, so that `rootShift = (height - 1) * LOG_MAX_BUFFER_SIZE`;
 *        elements in the [root] array are indexed with bits of the index starting from `rootShift` and until `rootShift + LOG_MAX_BUFFER_SIZE`.
 */
internal class PersistentVector<E>(private val root: Array<Any?>,
                                   private val tail: Array<Any?>,
                                   override val size: Int,
                                   private val rootShift: Int) : PersistentList<E>, AbstractPersistentList<E>() {

    init {
        require(size > MAX_BUFFER_SIZE) { "Trie-based persistent vector should have at least ${MAX_BUFFER_SIZE + 1} elements, got $size" }
        assert(size - rootSize(size) <= tail.size.coerceAtMost(MAX_BUFFER_SIZE))
    }

    private fun rootSize(): Int = rootSize(size)

    override fun add(element: E): PersistentList<E> {
        val tailSize = size - rootSize()
        if (tailSize < MAX_BUFFER_SIZE) {
            val newTail = tail.copyOf(MAX_BUFFER_SIZE)
            newTail[tailSize] = element
            return PersistentVector(root, newTail, size + 1, rootShift)
        }

        val newTail = presizedBufferWith(element)
        return pushFilledTail(root, tail, newTail)
    }

    /**
     * Appends the specified entirely filled [tail] as a leaf buffer to the next free position in the [root] trie.
     */
    private fun pushFilledTail(root: Array<Any?>, filledTail: Array<Any?>, newTail: Array<Any?>): PersistentVector<E> {
        if (size shr LOG_MAX_BUFFER_SIZE > 1 shl rootShift) {
            // if the root trie is filled entirely, promote it to the next level
            var newRoot = presizedBufferWith(root)
            val newRootShift = rootShift + LOG_MAX_BUFFER_SIZE
            newRoot = pushTail(newRoot, newRootShift, filledTail)
            return PersistentVector(newRoot, newTail, size + 1, newRootShift)
        }

        val newRoot = pushTail(root, rootShift, filledTail)
        return PersistentVector(newRoot, newTail, size + 1, rootShift)
    }

    /**
     * Appends the specified entirely filled [tail] as a leaf buffer to the next free position in the [root] trie.
     * The trie must not be filled entirely.
     */
    private fun pushTail(root: Array<Any?>?, shift: Int, tail: Array<Any?>): Array<Any?> {
        val bufferIndex = indexSegment(size - 1, shift) // size - 1 is the index of the last element in the new trie
        val newRootNode = root?.copyOf(MAX_BUFFER_SIZE) ?: arrayOfNulls<Any?>(MAX_BUFFER_SIZE)

        if (shift == LOG_MAX_BUFFER_SIZE) {
            newRootNode[bufferIndex] = tail
            // don't delve into the leaf level
        } else {
            @Suppress("UNCHECKED_CAST")
            newRootNode[bufferIndex] = pushTail(newRootNode[bufferIndex] as Array<Any?>?, shift - LOG_MAX_BUFFER_SIZE, tail)
        }
        return newRootNode
    }

    override fun add(index: Int, element: E): PersistentList<E> {
        checkPositionIndex(index, size)
        if (index == size) {
            return add(element)
        }

        val rootSize = rootSize()
        if (index >= rootSize) {
            return insertIntoTail(root, index - rootSize, element)
        }

        val elementCarry = ObjectRef(null)
        val newRoot = insertIntoRoot(root, rootShift, index, element, elementCarry)
        return insertIntoTail(newRoot, 0, elementCarry.value)
    }

    private fun insertIntoTail(root: Array<Any?>, tailIndex: Int, element: Any?): PersistentVector<E> {
        val tailSize = size - rootSize()
        val newTail = tail.copyOf(MAX_BUFFER_SIZE)
        if (tailSize < MAX_BUFFER_SIZE) {
            tail.copyInto(newTail, tailIndex + 1, tailIndex, tailSize)
            newTail[tailIndex] = element
            return PersistentVector(root, newTail, size + 1, rootShift)
        }

        val lastElement = tail[MAX_BUFFER_SIZE_MINUS_ONE]
        tail.copyInto(newTail, tailIndex + 1, tailIndex, tailSize - 1)
        newTail[tailIndex] = element
        return pushFilledTail(root, newTail, presizedBufferWith(lastElement))
    }

    /**
     * Insert the specified [element] into the [root] trie at the specified trie [index].
     *
     * [elementCarry] contains the last element of this trie that was popped out by the insertion operation.
     *
     * @return new root trie
     */
    private fun insertIntoRoot(root: Array<Any?>, shift: Int, index: Int, element: Any?, elementCarry: ObjectRef): Array<Any?> {
        val bufferIndex = indexSegment(index, shift)

        if (shift == 0) {
            val newRoot = if (bufferIndex == 0) arrayOfNulls<Any?>(MAX_BUFFER_SIZE) else root.copyOf(MAX_BUFFER_SIZE)
            root.copyInto(newRoot, bufferIndex + 1, bufferIndex, MAX_BUFFER_SIZE_MINUS_ONE)
            elementCarry.value = root[MAX_BUFFER_SIZE_MINUS_ONE]
            newRoot[bufferIndex] = element
            return newRoot
        }

        val newRoot = root.copyOf(MAX_BUFFER_SIZE)
        val lowerLevelShift = shift - LOG_MAX_BUFFER_SIZE

        @Suppress("UNCHECKED_CAST")
        newRoot[bufferIndex] = insertIntoRoot(root[bufferIndex] as Array<Any?>, lowerLevelShift, index, element, elementCarry)

        for (i in bufferIndex + 1 until MAX_BUFFER_SIZE) {
            if (newRoot[i] == null) break

            @Suppress("UNCHECKED_CAST")
            newRoot[i] = insertIntoRoot(root[i] as Array<Any?>, lowerLevelShift, 0, elementCarry.value, elementCarry)
        }

        return newRoot
    }

    override fun removeAt(index: Int): PersistentList<E> {
        checkElementIndex(index, size)
        val rootSize = rootSize()
        if (index >= rootSize) {
            return removeFromTailAt(root, rootSize, rootShift, index - rootSize)
        }
        val newRoot = removeFromRootAt(root, rootShift, index, ObjectRef(tail[0]))
        return removeFromTailAt(newRoot, rootSize, rootShift, 0)
    }

    private fun removeFromTailAt(root: Array<Any?>, rootSize: Int, shift: Int, index: Int): PersistentList<E> {
        val tailSize = size - rootSize
        assert(index < tailSize)

        if (tailSize == 1) {
            return pullLastBufferFromRoot(root, rootSize, shift)
        }
        val newTail = tail.copyOf(MAX_BUFFER_SIZE)
        if (index < tailSize - 1) {
            tail.copyInto(newTail, index, index + 1, tailSize)
        }
        newTail[tailSize - 1] = null
        return PersistentVector(root, newTail, rootSize + tailSize - 1, shift)
    }

    /**
     * Extracts the last entirely filled leaf buffer from the trie of this vector and makes it a tail in the returned [PersistentVector].
     *
     * Used when there are no elements left in current tail.
     *
     * Requires the trie to contain at least one leaf buffer.
     *
     * If the trie becomes empty after the operation, returns a tail-only vector ([SmallPersistentVector]).
     */
    private fun pullLastBufferFromRoot(root: Array<Any?>, rootSize: Int, shift: Int): PersistentList<E> {
        if (shift == 0) {
            val buffer = if (root.size == MUTABLE_BUFFER_SIZE) root.copyOf(MAX_BUFFER_SIZE) else root
            return SmallPersistentVector(buffer)
        }
        val tailCarry = ObjectRef(null)
        val newRoot = pullLastBuffer(root, shift, rootSize - 1, tailCarry)!!
        @Suppress("UNCHECKED_CAST")
        val newTail = tailCarry.value as Array<Any?>

        // check if the new root contains only one element
        if (newRoot[1] == null) {
            // demote the root trie to the lower level
            @Suppress("UNCHECKED_CAST")
            val lowerLevelRoot = newRoot[0] as Array<Any?>
            return PersistentVector(lowerLevelRoot, newTail, rootSize, shift - LOG_MAX_BUFFER_SIZE)
        }
        return PersistentVector(newRoot, newTail, rootSize, shift)
    }

    /**
     * Extracts the last leaf buffer from trie and returns new trie without it or `null` if there's no more leaf elements in this trie.
     *
     * [tailCarry] on output contains the extracted leaf buffer.
     */
    private fun pullLastBuffer(root: Array<Any?>, shift: Int, index: Int, tailCarry: ObjectRef): Array<Any?>? {
        val bufferIndex = indexSegment(index, shift)

        val newBufferAtIndex = if (shift == LOG_MAX_BUFFER_SIZE) {
            tailCarry.value = root[bufferIndex]
            null
        } else {
            @Suppress("UNCHECKED_CAST")
            pullLastBuffer(root[bufferIndex] as Array<Any?>, shift - LOG_MAX_BUFFER_SIZE, index, tailCarry)
        }

        if (newBufferAtIndex == null && bufferIndex == 0) {
            return null
        }

        val newRoot = root.copyOf(MAX_BUFFER_SIZE)
        newRoot[bufferIndex] = newBufferAtIndex
        return newRoot
    }

    /**
     * Removes element from trie at the specified trie [index].
     *
     * [tailCarry] on input contains the first element of the adjacent trie to fill the last vacant element with.
     * [tailCarry] on output contains the first element of this trie.
     *
     * @return the new root of the trie.
     */
    private fun removeFromRootAt(root: Array<Any?>, shift: Int, index: Int, tailCarry: ObjectRef): Array<Any?> {
        val bufferIndex = indexSegment(index, shift)

        if (shift == 0) {
            val newRoot = if (bufferIndex == 0) arrayOfNulls<Any?>(MAX_BUFFER_SIZE) else root.copyOf(MAX_BUFFER_SIZE)
            root.copyInto(newRoot, bufferIndex, bufferIndex + 1, MAX_BUFFER_SIZE)
            newRoot[MAX_BUFFER_SIZE - 1] = tailCarry.value
            tailCarry.value = root[bufferIndex]
            return newRoot
        }

        var bufferLastIndex = MAX_BUFFER_SIZE_MINUS_ONE
        if (root[bufferLastIndex] == null) {
            bufferLastIndex = indexSegment(rootSize() - 1, shift)
        }

        val newRoot = root.copyOf(MAX_BUFFER_SIZE)
        val lowerLevelShift = shift - LOG_MAX_BUFFER_SIZE

        for (i in bufferLastIndex downTo bufferIndex + 1) {
            @Suppress("UNCHECKED_CAST")
            newRoot[i] = removeFromRootAt(newRoot[i] as Array<Any?>, lowerLevelShift, 0, tailCarry)
        }
        @Suppress("UNCHECKED_CAST")
        newRoot[bufferIndex] = removeFromRootAt(newRoot[bufferIndex] as Array<Any?>, lowerLevelShift, index, tailCarry)

        return newRoot
    }

    override fun removeAll(predicate: (E) -> Boolean): PersistentList<E> {
        return builder().also { it.removeAllWithPredicate(predicate) }.build()
    }

    override fun builder(): PersistentVectorBuilder<E> {
        return PersistentVectorBuilder(this, root, tail, rootShift)
    }

    override fun listIterator(index: Int): ListIterator<E> {
        checkPositionIndex(index, size)
        @Suppress("UNCHECKED_CAST")
        return PersistentVectorIterator(root, tail as Array<E>, index, size, rootShift / LOG_MAX_BUFFER_SIZE + 1)
    }


    /** Returns either leaf buffer of the trie or the tail, that contains element with the specified [index]. */
    private fun bufferFor(index: Int): Array<Any?> {
        if (rootSize() <= index) {
            return tail
        }
        var buffer = root
        var shift = rootShift
        while (shift > 0) {
            @Suppress("UNCHECKED_CAST")
            buffer = buffer[indexSegment(index, shift)] as Array<Any?>
            shift -= LOG_MAX_BUFFER_SIZE
        }
        return buffer
    }

    override fun get(index: Int): E {
        checkElementIndex(index, size)
        val buffer = bufferFor(index)
        @Suppress("UNCHECKED_CAST")
        return buffer[index and MAX_BUFFER_SIZE_MINUS_ONE] as E
    }

    override fun set(index: Int, element: E): PersistentList<E> {
        checkElementIndex(index, size)
        if (rootSize() <= index) {
            val newTail = tail.copyOf(MAX_BUFFER_SIZE)
            newTail[index and MAX_BUFFER_SIZE_MINUS_ONE] = element
            return PersistentVector(root, newTail, size, rootShift)
        }

        val newRoot = setInRoot(root, rootShift, index, element)
        return PersistentVector(newRoot, tail, size, rootShift)
    }

    private fun setInRoot(root: Array<Any?>, shift: Int, index: Int, e: Any?): Array<Any?> {
        val bufferIndex = indexSegment(index, shift)
        val newRoot = root.copyOf(MAX_BUFFER_SIZE)
        if (shift == 0) {
            newRoot[bufferIndex] = e
        } else {
            @Suppress("UNCHECKED_CAST")
            newRoot[bufferIndex] = setInRoot(newRoot[bufferIndex] as Array<Any?>,
                    shift - LOG_MAX_BUFFER_SIZE, index, e)
        }
        return newRoot
    }
}
