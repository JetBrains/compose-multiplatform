/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package androidx.compose.runtime.external.kotlinx.collections.immutable.implementations.immutableList

import androidx.compose.runtime.external.kotlinx.collections.immutable.PersistentList
import androidx.compose.runtime.external.kotlinx.collections.immutable.internal.ListImplementation.checkElementIndex
import androidx.compose.runtime.external.kotlinx.collections.immutable.internal.ListImplementation.checkPositionIndex
import androidx.compose.runtime.external.kotlinx.collections.immutable.internal.MutabilityOwnership
import androidx.compose.runtime.external.kotlinx.collections.immutable.internal.assert
import androidx.compose.runtime.external.kotlinx.collections.immutable.internal.modCount

internal class PersistentVectorBuilder<E>(private var vector: PersistentList<E>,
                                          private var vectorRoot: Array<Any?>?,
                                          private var vectorTail: Array<Any?>,
                                          internal var rootShift: Int) : AbstractMutableList<E>(), PersistentList.Builder<E> {
    private var ownership = MutabilityOwnership()
    internal var root = vectorRoot
        private set
    internal var tail = vectorTail
        private set
    override var size = vector.size
        private set

    internal fun getModCount() = modCount

    override fun build(): PersistentList<E> {
        vector = if (root === vectorRoot && tail === vectorTail) {
            vector
        } else {
            ownership = MutabilityOwnership()
            vectorRoot = root
            vectorTail = tail
            if (root == null) {
                if (tail.isEmpty()) {
                    persistentVectorOf()
                } else {
                    SmallPersistentVector(tail.copyOf(size))
                }
            } else {
                PersistentVector(root!!, tail, size, rootShift)
            }
        }
        return vector
    }

    private fun rootSize(): Int {
        if (size <= MAX_BUFFER_SIZE) {
            return 0
        }
        return rootSize(size)
    }

    private fun tailSize(size: Int): Int {
        if (size <= MAX_BUFFER_SIZE) {
            return size
        }
        return size - rootSize(size)
    }

    private fun tailSize(): Int {
        return tailSize(size)
    }

    private fun isMutable(buffer: Array<Any?>): Boolean {
        return buffer.size == MUTABLE_BUFFER_SIZE && buffer[MUTABLE_BUFFER_SIZE - 1] === ownership
    }

    /**
     * Checks if [buffer] is mutable and returns it or its mutable copy.
     */
    private fun makeMutable(buffer: Array<Any?>?): Array<Any?> {
        if (buffer == null) {
            return mutableBuffer()
        }
        if (isMutable(buffer)) {
            return buffer
        }
        return buffer.copyInto(mutableBuffer(), endIndex = buffer.size.coerceAtMost(MAX_BUFFER_SIZE))
    }

    private fun makeMutableShiftingRight(buffer: Array<Any?>, distance: Int): Array<Any?> {
        if (isMutable(buffer)) {
            return buffer.copyInto(buffer, distance, 0, MAX_BUFFER_SIZE - distance)
        }
        return buffer.copyInto(mutableBuffer(), distance, 0, MAX_BUFFER_SIZE - distance)
    }

    private fun mutableBufferWith(element: Any?): Array<Any?> {
        val buffer = arrayOfNulls<Any?>(MUTABLE_BUFFER_SIZE)
        buffer[0] = element
        buffer[MUTABLE_BUFFER_SIZE - 1] = ownership
        return buffer
    }

    private fun mutableBuffer(): Array<Any?> {
        val buffer = arrayOfNulls<Any?>(MUTABLE_BUFFER_SIZE)
        buffer[MUTABLE_BUFFER_SIZE - 1] = ownership
        return buffer
    }

    override fun add(element: E): Boolean {
        modCount += 1

        val tailSize = tailSize()
        if (tailSize < MAX_BUFFER_SIZE) {
            val mutableTail = makeMutable(tail)
            mutableTail[tailSize] = element
            this.tail = mutableTail
            this.size += 1
        } else {
            val newTail = mutableBufferWith(element)
            this.pushFilledTail(root, tail, newTail)
        }
        return true
    }

    /**
     * Appends the specified entirely filled [tail] as a leaf buffer to the next free position in the [root] trie.
     */
    private fun pushFilledTail(root: Array<Any?>?, filledTail: Array<Any?>, newTail: Array<Any?>) = when {
        size shr LOG_MAX_BUFFER_SIZE > 1 shl rootShift -> {
            // if the root trie is filled entirely, promote it to the next level
            this.root = pushTail(mutableBufferWith(root), filledTail, rootShift + LOG_MAX_BUFFER_SIZE)
            this.tail = newTail
            this.rootShift += LOG_MAX_BUFFER_SIZE
            this.size += 1
        }
        root == null -> {
            this.root = filledTail
            this.tail = newTail
            this.size += 1
        }
        else -> {
            this.root = pushTail(root, filledTail, rootShift)
            this.tail = newTail
            this.size += 1
        }
    }

    /**
     * Appends the specified entirely filled [tail] as a leaf buffer to the next free position in the [root] trie.
     * The trie must not be filled entirely.
     */
    private fun pushTail(root: Array<Any?>?, tail: Array<Any?>, shift: Int): Array<Any?> {
        val index = indexSegment(size - 1, shift)
        val mutableRoot = makeMutable(root)

        if (shift == LOG_MAX_BUFFER_SIZE) {
            mutableRoot[index] = tail
        } else {
            @Suppress("UNCHECKED_CAST")
            mutableRoot[index] = pushTail(mutableRoot[index] as Array<Any?>?, tail, shift - LOG_MAX_BUFFER_SIZE)
        }
        return mutableRoot
    }

    override fun addAll(elements: Collection<E>): Boolean {
        if (elements.isEmpty()) {
            return false
        }

        modCount++

        val tailSize = tailSize()
        val elementsIterator = elements.iterator()

        if (MAX_BUFFER_SIZE - tailSize >= elements.size) {
            // there is enough space in tail, add all elements to it
            tail = copyToBuffer(makeMutable(tail), tailSize, elementsIterator)
            size += elements.size
        } else {
            val buffersSize = (elements.size + tailSize - 1) / MAX_BUFFER_SIZE
            val buffers = arrayOfNulls<Array<Any?>?>(buffersSize)

            // fill remained space of tail
            buffers[0] = copyToBuffer(makeMutable(tail), tailSize, elementsIterator)
            // fill other buffers
            for (index in 1 until buffersSize) {
                buffers[index] = copyToBuffer(mutableBuffer(), 0, elementsIterator)
            }

            // add buffers to the root, rootShift is updated appropriately there
            @Suppress("UNCHECKED_CAST")
            root = pushBuffersIncreasingHeightIfNeeded(root, rootSize(), buffers as Array<Array<Any?>>)
            // create new tail and copy remained elements there
            tail = copyToBuffer(mutableBuffer(), 0, elementsIterator)
            size += elements.size
        }

        return true
    }

    private fun copyToBuffer(buffer: Array<Any?>, bufferIndex: Int, sourceIterator: Iterator<Any?>): Array<Any?> {
        var index = bufferIndex
        while (index < MAX_BUFFER_SIZE && sourceIterator.hasNext()) {
            buffer[index++] = sourceIterator.next()
        }
        return buffer
    }

    /**
     * Adds all buffers from [buffers] as leaf nodes to the [root].
     * If the [root] has less available leaves for the buffers, height of the trie is increased.
     *
     * Returns root of the resulting trie.
     */
    private fun pushBuffersIncreasingHeightIfNeeded(root: Array<Any?>?, rootSize: Int, buffers: Array<Array<Any?>>): Array<Any?> {
        val buffersIterator = buffers.iterator()

        var mutableRoot = when {
            rootSize shr LOG_MAX_BUFFER_SIZE < 1 shl rootShift ->
                // if the root trie is not filled entirely, fill it
                pushBuffers(root, rootSize, rootShift, buffersIterator)
            else ->
                // root is filled entirely, make it mutable
                makeMutable(root)
        }

        // here root is filled entirely or/and all buffers are already placed

        while (buffersIterator.hasNext()) {
            // some buffers left, so root is filled entirely. promote root to the next level
            rootShift += LOG_MAX_BUFFER_SIZE
            mutableRoot = mutableBufferWith(mutableRoot)

            pushBuffers(mutableRoot, 1 shl rootShift, rootShift, buffersIterator)
        }

        return mutableRoot
    }

    /**
     * Adds buffers from the [buffersIterator] as leaf nodes.
     * As the result [root] is entirely filled, or all buffers are added.
     *
     * Returns the resulting root.
     */
    private fun pushBuffers(root: Array<Any?>?, rootSize: Int, shift: Int, buffersIterator: Iterator<Array<Any?>>): Array<Any?> {
        check(buffersIterator.hasNext())
        check(shift >= 0)

        if (shift == 0) {
            return buffersIterator.next()
        }

        val mutableRoot = makeMutable(root)
        var index = indexSegment(rootSize, shift)

        @Suppress("UNCHECKED_CAST")
        mutableRoot[index] =
                pushBuffers(mutableRoot[index] as Array<Any?>?, rootSize, shift - LOG_MAX_BUFFER_SIZE, buffersIterator)

        while (++index < MAX_BUFFER_SIZE && buffersIterator.hasNext()) {
            @Suppress("UNCHECKED_CAST")
            mutableRoot[index] =
                    pushBuffers(mutableRoot[index] as Array<Any?>?, 0, shift - LOG_MAX_BUFFER_SIZE, buffersIterator)
        }
        return mutableRoot
    }

    override fun add(index: Int, element: E) {
        checkPositionIndex(index, size)

        if (index == size) {
            add(element)
            return
        }

        modCount += 1

        val rootSize = rootSize()
        if (index >= rootSize) {
            insertIntoTail(root, index - rootSize, element)
            return
        }

        val elementCarry = ObjectRef(null)
        val newRest = insertIntoRoot(root!!, rootShift, index, element, elementCarry)
        @Suppress("UNCHECKED_CAST")
        insertIntoTail(newRest, 0, elementCarry.value as E)
    }

    private fun insertIntoTail(root: Array<Any?>?, index: Int, element: E) {
        val tailSize = tailSize()
        val mutableTail = makeMutable(tail)
        if (tailSize < MAX_BUFFER_SIZE) {
            tail.copyInto(mutableTail, index + 1, index, tailSize)
            mutableTail[index] = element
            this.root = root
            this.tail = mutableTail
            this.size += 1
        } else {
            val lastElement = tail[MAX_BUFFER_SIZE_MINUS_ONE]
            tail.copyInto(mutableTail, index + 1, index, MAX_BUFFER_SIZE_MINUS_ONE)
            mutableTail[index] = element
            pushFilledTail(root, mutableTail, mutableBufferWith(lastElement))
        }
    }

    /**
     * Insert the specified [element] into the [root] trie at the specified trie [index].
     *
     * [elementCarry] contains the last element of this trie that was popped out by the insertion operation.
     *
     * @return new root trie or this modified trie, if it's already mutable
     */
    private fun insertIntoRoot(root: Array<Any?>, shift: Int, index: Int, element: Any?, elementCarry: ObjectRef): Array<Any?> {
        val bufferIndex = indexSegment(index, shift)

        if (shift == 0) {
            elementCarry.value = root[MAX_BUFFER_SIZE_MINUS_ONE]
            val mutableRoot = root.copyInto(makeMutable(root), bufferIndex + 1, bufferIndex, MAX_BUFFER_SIZE_MINUS_ONE)
            mutableRoot[bufferIndex] = element
            return mutableRoot
        }

        val mutableRoot = makeMutable(root)
        val lowerLevelShift = shift - LOG_MAX_BUFFER_SIZE

        @Suppress("UNCHECKED_CAST")
        mutableRoot[bufferIndex] =
                insertIntoRoot(mutableRoot[bufferIndex] as Array<Any?>, lowerLevelShift, index, element, elementCarry)

        for (i in bufferIndex + 1 until MAX_BUFFER_SIZE) {
            if (mutableRoot[i] == null) break
            @Suppress("UNCHECKED_CAST")
            mutableRoot[i] =
                    insertIntoRoot(mutableRoot[i] as Array<Any?>, lowerLevelShift, 0, elementCarry.value, elementCarry)
        }

        return mutableRoot
    }

    override fun addAll(index: Int, elements: Collection<E>): Boolean {
        checkPositionIndex(index, size)

        if (index == size) {
            return addAll(elements)
        }
        if (elements.isEmpty()) {
            return false
        }

        modCount++

        val unaffectedElementsCount = (index shr LOG_MAX_BUFFER_SIZE) shl LOG_MAX_BUFFER_SIZE
        val buffersSize = (size - unaffectedElementsCount + elements.size - 1) / MAX_BUFFER_SIZE

        if (buffersSize == 0) {
            assert(index >= rootSize())

            val startIndex = index and MAX_BUFFER_SIZE_MINUS_ONE
            val endIndex = (index + elements.size - 1) and MAX_BUFFER_SIZE_MINUS_ONE // inclusive

            // Copy the unaffected tail prefix and shift the affected tail suffix to the end
            val newTail = tail.copyInto(makeMutable(tail), endIndex + 1, startIndex, tailSize())
            // Copy the specified elements to the new tail
            copyToBuffer(newTail, startIndex, elements.iterator())

            tail = newTail
            size += elements.size
            return true
        }

        val buffers = arrayOfNulls<Array<Any?>?>(buffersSize)

        val tailSize = tailSize()
        val newTailSize = tailSize(size + elements.size)

        val newTail: Array<Any?>
        when {
            index >= rootSize() -> {
                newTail = mutableBuffer()
                splitToBuffers(elements, index, tail, tailSize, buffers, buffersSize, newTail)
            }
            newTailSize > tailSize -> {
                val rightShift = newTailSize - tailSize
                newTail = makeMutableShiftingRight(tail, rightShift)

                insertIntoRoot(elements, index, rightShift, buffers, buffersSize, newTail)
            }
            else -> {
                newTail = tail.copyInto(mutableBuffer(), 0, tailSize - newTailSize, tailSize)

                val rightShift = MAX_BUFFER_SIZE - (tailSize - newTailSize)
                val lastBuffer = makeMutableShiftingRight(tail, rightShift)

                buffers[buffersSize - 1] = lastBuffer

                insertIntoRoot(elements, index, rightShift, buffers, buffersSize - 1, lastBuffer)
            }
        }

        @Suppress("UNCHECKED_CAST")
        root = pushBuffersIncreasingHeightIfNeeded(root, unaffectedElementsCount, buffers as Array<Array<Any?>>)
        tail = newTail
        size += elements.size

        return true
    }

    /**
     * Inserts the [elements] into the [root] at the given [index].
     *
     * Affected elements are copied to the [buffers] split into [nullBuffers] buffers.
     * Elements that do not fit [nullBuffers] buffers are copied to the [nextBuffer].
     */
    private fun insertIntoRoot(
            elements: Collection<E>,
            index: Int,
            rightShift: Int,
            buffers: Array<Array<Any?>?>,
            nullBuffers: Int,
            nextBuffer: Array<Any?>
    ) {
        checkNotNull(root)

        val startLeafIndex = index shr LOG_MAX_BUFFER_SIZE
        val startLeaf = shiftLeafBuffers(startLeafIndex, rightShift, buffers, nullBuffers, nextBuffer)

        val lastLeafIndex = (rootSize() shr LOG_MAX_BUFFER_SIZE) - 1
        val newNullBuffers = nullBuffers - (lastLeafIndex - startLeafIndex)
        val newNextBuffer = if (newNullBuffers < nullBuffers) buffers[newNullBuffers]!! else nextBuffer

        splitToBuffers(elements, index, startLeaf, MAX_BUFFER_SIZE, buffers, newNullBuffers, newNextBuffer)
    }

    /**
     * Shifts elements in the [root] to the right by the given [rightShift] position starting from the end.
     *
     * Shifting stops when elements of the leaf at [startLeafIndex] are reached.
     * Last elements whose indexes become bigger than [rootSize] are copied to the [nextBuffer].
     * Shifted leaves are stored in the [buffers] starting from the given [nullBuffers] index.
     *
     * Returns leaf at the [startLeafIndex].
     */
    private fun shiftLeafBuffers(
            startLeafIndex: Int,
            rightShift: Int,
            buffers: Array<Array<Any?>?>,
            nullBuffers: Int,
            nextBuffer: Array<Any?>
    ): Array<Any?> {
        checkNotNull(root)

        val leafCount = rootSize() shr LOG_MAX_BUFFER_SIZE

        val leafBufferIterator = leafBufferIterator(leafCount)   // start from the last leaf
        var bufferIndex = nullBuffers
        var buffer = nextBuffer

        while (leafBufferIterator.previousIndex() != startLeafIndex) {
            val currentBuffer = leafBufferIterator.previous()

            currentBuffer.copyInto(buffer, 0, MAX_BUFFER_SIZE - rightShift, MAX_BUFFER_SIZE)
            buffer = makeMutableShiftingRight(currentBuffer, rightShift)
            buffers[--bufferIndex] = buffer
        }

        return leafBufferIterator.previous()
    }

    /**
     * Inserts [elements] into [startBuffer] of size [startBufferSize] and splits the result into [nullBuffers] buffers.
     *
     * Elements that do not fit [nullBuffers] buffers are copied to the [nextBuffer].
     */
    private fun splitToBuffers(
            elements: Collection<E>,
            index: Int,
            startBuffer: Array<Any?>,
            startBufferSize: Int,
            buffers: Array<Array<Any?>?>,
            nullBuffers: Int,
            nextBuffer: Array<Any?>
    ) {
        check(nullBuffers >= 1)

        val firstBuffer = makeMutable(startBuffer)
        buffers[0] = firstBuffer

        var newNextBuffer = nextBuffer
        var newNullBuffers = nullBuffers

        val startBufferStartIndex = index and MAX_BUFFER_SIZE_MINUS_ONE
        val endBufferEndIndex = (index + elements.size - 1) and MAX_BUFFER_SIZE_MINUS_ONE // inclusive

        val elementsToShift = startBufferSize - startBufferStartIndex

        if (endBufferEndIndex + elementsToShift < MAX_BUFFER_SIZE) {
            firstBuffer.copyInto(newNextBuffer, endBufferEndIndex + 1, startBufferStartIndex, startBufferSize)
        } else {
            val toCopyToLast = endBufferEndIndex + elementsToShift - MAX_BUFFER_SIZE + 1
            if (nullBuffers == 1) {
                newNextBuffer = firstBuffer
            } else {
                newNextBuffer = mutableBuffer()
                buffers[--newNullBuffers] = newNextBuffer
            }
            firstBuffer.copyInto(nextBuffer, 0, startBufferSize - toCopyToLast, startBufferSize)
            firstBuffer.copyInto(newNextBuffer, endBufferEndIndex + 1, startBufferStartIndex, startBufferSize - toCopyToLast)
        }

        val elementsIterator = elements.iterator()

        copyToBuffer(firstBuffer, startBufferStartIndex, elementsIterator)
        for (i in 1 until newNullBuffers) {
            buffers[i] = copyToBuffer(mutableBuffer(), 0, elementsIterator)
        }
        copyToBuffer(newNextBuffer, 0, elementsIterator)
    }

    override fun get(index: Int): E {
        checkElementIndex(index, size)

        val buffer = bufferFor(index)
        @Suppress("UNCHECKED_CAST")
        return buffer[index and MAX_BUFFER_SIZE_MINUS_ONE] as E
    }

    private fun bufferFor(index: Int): Array<Any?> {
        if (rootSize() <= index) {
            return tail
        }
        var buffer = root!!
        var shift = rootShift
        while (shift > 0) {
            @Suppress("UNCHECKED_CAST")
            buffer = buffer[indexSegment(index, shift)] as Array<Any?>
            shift -= LOG_MAX_BUFFER_SIZE
        }
        return buffer
    }

    override fun removeAt(index: Int): E {
        checkElementIndex(index, size)

        modCount += 1

        val rootSize = rootSize()
        if (index >= rootSize) {
            @Suppress("UNCHECKED_CAST")
            return removeFromTailAt(root, rootSize, rootShift, index - rootSize) as E
        }
        val elementCarry = ObjectRef(tail[0])
        val newRoot = removeFromRootAt(root!!, rootShift, index, elementCarry)
        removeFromTailAt(newRoot, rootSize, rootShift, 0)
        @Suppress("UNCHECKED_CAST")
        return elementCarry.value as E
    }

    private fun removeFromTailAt(root: Array<Any?>?, rootSize: Int, shift: Int, index: Int): Any? {
        val tailSize = size - rootSize
        assert(index < tailSize)

        val removedElement: Any?
        if (tailSize == 1) {
            removedElement = tail[0]
            pullLastBufferFromRoot(root, rootSize, shift)
        } else {
            removedElement = tail[index]
            val mutableTail = tail.copyInto(makeMutable(tail), index, index + 1, tailSize)
            mutableTail[tailSize - 1] = null
            this.root = root
            this.tail = mutableTail
            this.size = rootSize + tailSize - 1
            this.rootShift = shift
        }
        return removedElement
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
            val removedElement = root[bufferIndex]
            val mutableRoot = root.copyInto(makeMutable(root), bufferIndex, bufferIndex + 1, MAX_BUFFER_SIZE)
            mutableRoot[MAX_BUFFER_SIZE - 1] = tailCarry.value
            tailCarry.value = removedElement
            return mutableRoot
        }

        var bufferLastIndex = MAX_BUFFER_SIZE_MINUS_ONE
        if (root[bufferLastIndex] == null) {
            bufferLastIndex = indexSegment(rootSize() - 1, shift)
        }

        val mutableRoot = makeMutable(root)
        val lowerLevelShift = shift - LOG_MAX_BUFFER_SIZE

        for (i in bufferLastIndex downTo bufferIndex + 1) {
            @Suppress("UNCHECKED_CAST")
            mutableRoot[i] = removeFromRootAt(mutableRoot[i] as Array<Any?>, lowerLevelShift, 0, tailCarry)
        }
        @Suppress("UNCHECKED_CAST")
        mutableRoot[bufferIndex] =
                removeFromRootAt(mutableRoot[bufferIndex] as Array<Any?>, lowerLevelShift, index, tailCarry)

        return mutableRoot
    }

    /**
     * Extracts the last entirely filled leaf buffer from the trie of this vector and makes it a tail in this
     *
     * Used when there are no elements left in current tail.
     *
     * Requires the trie to contain at least one leaf buffer.
     */
    private fun pullLastBufferFromRoot(root: Array<Any?>?, rootSize: Int, shift: Int) {
        if (shift == 0) {
            this.root = null
            this.tail = root ?: emptyArray()
            this.size = rootSize
            this.rootShift = shift
            return
        }

        val tailCarry = ObjectRef(null)
        val newRoot = pullLastBuffer(root!!, shift, rootSize, tailCarry)!!
        @Suppress("UNCHECKED_CAST")
        this.tail = tailCarry.value as Array<Any?>
        this.size = rootSize

        // check if the new root contains only one element
        if (newRoot[1] == null) {
            // demote the root trie to the lower level
            @Suppress("UNCHECKED_CAST")
            this.root = newRoot[0] as Array<Any?>?
            this.rootShift = shift - LOG_MAX_BUFFER_SIZE
        } else {
            this.root = newRoot
            this.rootShift = shift
        }
    }

    /**
     * Extracts the last leaf buffer from trie and returns new trie without it or `null` if there's no more leaf elements in this trie.
     *
     * [tailCarry] on output contains the extracted leaf buffer.
     */
    private fun pullLastBuffer(root: Array<Any?>, shift: Int, rootSize: Int, tailCarry: ObjectRef): Array<Any?>? {
        val bufferIndex = indexSegment(rootSize - 1, shift)

        val newBufferAtIndex = if (shift == LOG_MAX_BUFFER_SIZE) {
            tailCarry.value = root[bufferIndex]
            null
        } else {
            @Suppress("UNCHECKED_CAST")
            pullLastBuffer(root[bufferIndex] as Array<Any?>, shift - LOG_MAX_BUFFER_SIZE, rootSize, tailCarry)
        }
        if (newBufferAtIndex == null && bufferIndex == 0) {
            return null
        }

        val mutableRoot = makeMutable(root)
        mutableRoot[bufferIndex] = newBufferAtIndex
        return mutableRoot
    }

    override fun removeAll(elements: Collection<E>): Boolean {
        return removeAllWithPredicate { elements.contains(it) }
    }

    fun removeAllWithPredicate(predicate: (E) -> Boolean): Boolean {
        val anyRemoved = removeAll(predicate)
        if (anyRemoved) {
            modCount++
        }
        return anyRemoved
    }

    // Does not update `modCount`.
    private fun removeAll(predicate: (E) -> Boolean): Boolean {
        val tailSize = tailSize()
        val bufferRef = ObjectRef(null)

        if (root == null) {
            return removeAllFromTail(predicate, tailSize, bufferRef) != tailSize
        }

        val leafIterator = leafBufferIterator(0)
        var bufferSize = MAX_BUFFER_SIZE

        // skip unaffected leaves
        while (bufferSize == MAX_BUFFER_SIZE && leafIterator.hasNext()) {
            bufferSize = removeAll(predicate, leafIterator.next(), MAX_BUFFER_SIZE, bufferRef)
        }

        // if no elements from the root match the predicate, check the tail
        if (bufferSize == MAX_BUFFER_SIZE) {
            assert(!leafIterator.hasNext())
            val newTailSize = removeAllFromTail(predicate, tailSize, bufferRef)
            if (newTailSize == 0) {
                // all elements of the tail was removed, pull the last leaf from the root to make it the tail
                pullLastBufferFromRoot(root, size, rootShift)
            }
            return newTailSize != tailSize
        }

        // handle affected leaves reusing mutable ones
        val unaffectedElementsCount = leafIterator.previousIndex() shl LOG_MAX_BUFFER_SIZE

        val buffers = mutableListOf<Array<Any?>>()
        val recyclableBuffers = mutableListOf<Array<Any?>>()

        while (leafIterator.hasNext()) {
            val leaf = leafIterator.next()
            bufferSize = recyclableRemoveAll(predicate, leaf, MAX_BUFFER_SIZE, bufferSize, bufferRef, recyclableBuffers, buffers)
        }

        // handle the tail
        val newTailSize = recyclableRemoveAll(predicate, tail, tailSize, bufferSize, bufferRef, recyclableBuffers, buffers)

        @Suppress("UNCHECKED_CAST")
        val newTail = bufferRef.value as Array<Any?>
        newTail.fill(null, newTailSize, MAX_BUFFER_SIZE)

        // build the root
        val newRoot = if (buffers.isEmpty()) root!! else pushBuffers(root, unaffectedElementsCount, rootShift, buffers.iterator())
        val newRootSize = unaffectedElementsCount + (buffers.size shl LOG_MAX_BUFFER_SIZE)

        root = retainFirst(newRoot, newRootSize)
        tail = newTail
        size = newRootSize + newTailSize

        return true
    }

    /**
     * Retains first [size] elements of the [root].
     *
     * If the height of the root is bigger than needed to store [size] elements, it's decreased.
     */
    private fun retainFirst(root: Array<Any?>, size: Int): Array<Any?>? {
        check(size and MAX_BUFFER_SIZE_MINUS_ONE == 0)

        if (size == 0) {
            rootShift = 0
            return null
        }

        val lastIndex = size - 1
        var newRoot = root
        while (lastIndex shr rootShift == 0) {
            rootShift -= LOG_MAX_BUFFER_SIZE
            @Suppress("UNCHECKED_CAST")
            newRoot = newRoot[0] as Array<Any?>
        }
        return nullifyAfter(newRoot, lastIndex, rootShift)
    }

    /**
     * Nullifies nodes cells after the specified [index].
     *
     * Used to prevent memory leaks after reusing nodes.
     */
    private fun nullifyAfter(root: Array<Any?>, index: Int, shift: Int): Array<Any?> {
        check(shift >= 0)

        if (shift == 0) {
            // the `root` is a leaf buffer.
            // As leaf buffers can't be filled partially, return the `root` as is.
            return root
        }

        val lastIndex = indexSegment(index, shift)
        @Suppress("UNCHECKED_CAST")
        val newChild = nullifyAfter(root[lastIndex] as Array<Any?>, index, shift - LOG_MAX_BUFFER_SIZE)

        var newRoot = root
        if (lastIndex < MAX_BUFFER_SIZE_MINUS_ONE && newRoot[lastIndex + 1] != null) {
            if (isMutable(newRoot)) {
                newRoot.fill(null, lastIndex + 1, MAX_BUFFER_SIZE)
            }
            newRoot = newRoot.copyInto(mutableBuffer(), 0, 0, lastIndex + 1)
        }
        if (newChild !== newRoot[lastIndex]) {
            newRoot = makeMutable(newRoot)
            newRoot[lastIndex] = newChild
        }

        return newRoot
    }

    /**
     * Copies elements of the [tail] buffer of size [tailSize] that do not match the given [predicate] to a new buffer.
     *
     * If the [tail] is mutable, it is reused to store non-matching elements.
     * If non of the elements match the [predicate], no buffers are created and elements are not copied.
     * [bufferRef] stores the newly created buffer, or the [tail] if a new buffer was not created.
     *
     * Returns the filled size of the buffer stored in the [bufferRef].
     */
    private fun removeAllFromTail(predicate: (E) -> Boolean, tailSize: Int, bufferRef: ObjectRef): Int {
        val newTailSize = removeAll(predicate, tail, tailSize, bufferRef)

        if (newTailSize == tailSize) {
            assert(bufferRef.value === tail)
            return tailSize
        }

        @Suppress("UNCHECKED_CAST")
        val newTail = bufferRef.value as Array<Any?>
        newTail.fill(null, newTailSize, tailSize)

        tail = newTail
        size -= tailSize - newTailSize

        return newTailSize
    }

    /**
     * Copies elements of the given [buffer] of size [bufferSize] that do not match the given [predicate] to a new buffer.
     *
     * If the [buffer] is mutable, it is reused to store non-matching elements.
     * If non of the elements match the [predicate], no buffers are created and elements are not copied.
     * [bufferRef] stores the newly created buffer, or the [buffer] if a new buffer was not created.
     *
     * Returns the filled size of the buffer stored in the [bufferRef].
     */
    private fun removeAll(
            predicate: (E) -> Boolean,
            buffer: Array<Any?>,
            bufferSize: Int,
            bufferRef: ObjectRef
    ): Int {
        var newBuffer = buffer
        var newBufferSize = bufferSize

        var anyRemoved = false

        for (index in 0 until bufferSize) {
            @Suppress("UNCHECKED_CAST")
            val element = buffer[index] as E

            if (predicate(element)) {
                if (!anyRemoved) {
                    newBuffer = makeMutable(buffer)
                    newBufferSize = index

                    anyRemoved = true
                }
            } else if (anyRemoved) {
                newBuffer[newBufferSize++] = element
            }
        }

        bufferRef.value = newBuffer

        return newBufferSize
    }

    /**
     * Copied elements of the given [buffer] of size [bufferSize] that do not match the given [predicate]
     * to the buffer stored in the given [bufferRef] starting at [toBufferSize].
     *
     * If the buffer gets filled entirely, it is added to [buffers] and a new buffer is created or
     * reused from the [recyclableBuffers] to hold the rest of the non-matching elements.
     * [bufferRef] stores the newly created buffer if a new buffer was created.
     *
     * Returns the filled size of the buffer stored in the [bufferRef].
     */
    private fun recyclableRemoveAll(
            predicate: (E) -> Boolean,
            buffer: Array<Any?>,
            bufferSize: Int,
            toBufferSize: Int,
            bufferRef: ObjectRef,
            recyclableBuffers: MutableList<Array<Any?>>,
            buffers: MutableList<Array<Any?>>
    ): Int {
        if (isMutable(buffer)) {
            recyclableBuffers.add(buffer)
        }

        @Suppress("UNCHECKED_CAST")
        val toBuffer = bufferRef.value as Array<Any?>

        var newToBuffer = toBuffer
        var newToBufferSize = toBufferSize

        for (index in 0 until bufferSize) {
            @Suppress("UNCHECKED_CAST")
            val element = buffer[index] as E

            if (!predicate(element)) {
                if (newToBufferSize == MAX_BUFFER_SIZE) {
                    newToBuffer = if (recyclableBuffers.isNotEmpty()) {
                        recyclableBuffers.removeAt(recyclableBuffers.size - 1)
                    } else {
                        mutableBuffer()
                    }
                    newToBufferSize = 0
                }

                newToBuffer[newToBufferSize++] = element
            }
        }

        bufferRef.value = newToBuffer

        if (toBuffer !== bufferRef.value) {
            buffers.add(toBuffer)
        }

        return newToBufferSize
    }

    override fun set(index: Int, element: E): E {
        // TODO: Should list[i] = list[i] make it mutable?
        checkElementIndex(index, size)
        if (rootSize() <= index) {
            val mutableTail = makeMutable(tail)

            // Creating new tail implies structural change.
            if (mutableTail !== tail) { modCount++ }

            val tailIndex = index and MAX_BUFFER_SIZE_MINUS_ONE
            val oldElement = mutableTail[tailIndex]
            mutableTail[tailIndex] = element
            this.tail = mutableTail
            @Suppress("UNCHECKED_CAST")
            return oldElement as E
        }

        val oldElementCarry = ObjectRef(null)
        this.root = setInRoot(root!!, rootShift, index, element, oldElementCarry)
        @Suppress("UNCHECKED_CAST")
        return oldElementCarry.value as E
    }

    private fun setInRoot(root: Array<Any?>, shift: Int, index: Int, e: E, oldElementCarry: ObjectRef): Array<Any?> {
        val bufferIndex = indexSegment(index, shift)
        val mutableRoot = makeMutable(root)

        if (shift == 0) {
            // Creating new leaf implies structural change.
            // Actually, while descending to this leaf several nodes could be recreated.
            // However, this builder is exclusive owner of this leaf iff it is exclusive owner of all leaf's ancestors.
            // Hence, checking recreation of this leaf is enough to determine if a structural change occurred.
            if (mutableRoot !== root) { modCount++ }

            oldElementCarry.value = mutableRoot[bufferIndex]
            mutableRoot[bufferIndex] = e
            return mutableRoot
        }
        @Suppress("UNCHECKED_CAST")
        mutableRoot[bufferIndex] =
                setInRoot(mutableRoot[bufferIndex] as Array<Any?>, shift - LOG_MAX_BUFFER_SIZE, index, e, oldElementCarry)
        return mutableRoot
    }

    override fun iterator(): MutableIterator<E> {
        return this.listIterator()
    }

    override fun listIterator(): MutableListIterator<E> {
        return this.listIterator(0)
    }

    override fun listIterator(index: Int): MutableListIterator<E> {
        checkPositionIndex(index, size)
        return PersistentVectorMutableIterator(this, index)
    }

    private fun leafBufferIterator(index: Int): ListIterator<Array<Any?>> {
        checkNotNull(root)

        val leafCount = rootSize() shr LOG_MAX_BUFFER_SIZE

        checkPositionIndex(index, leafCount)

        if (rootShift == 0) {
            return SingleElementListIterator(root!!, index)
        }

        val trieHeight = rootShift / LOG_MAX_BUFFER_SIZE
        return TrieIterator(root!!, index, leafCount, trieHeight)
    }
}
