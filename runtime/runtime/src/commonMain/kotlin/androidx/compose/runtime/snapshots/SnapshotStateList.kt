/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.runtime.snapshots

import androidx.compose.runtime.Stable
import androidx.compose.runtime.external.kotlinx.collections.immutable.PersistentList
import androidx.compose.runtime.external.kotlinx.collections.immutable.persistentListOf

/**
 * An implementation of [MutableList] that can be observed and snapshot. This is the result type
 * created by [androidx.compose.runtime.mutableStateListOf].
 *
 * This class closely implements the same semantics as [ArrayList].
 *
 * @see androidx.compose.runtime.mutableStateListOf
 */
@Stable
class SnapshotStateList<T> : MutableList<T>, StateObject {
    override var firstStateRecord: StateRecord =
        StateListStateRecord<T>(persistentListOf())
        private set

    override fun prependStateRecord(value: StateRecord) {
        value.next = firstStateRecord
        @Suppress("UNCHECKED_CAST")
        firstStateRecord = value as StateListStateRecord<T>
    }

    internal val modification: Int get() = withCurrent { modification }

    @Suppress("UNCHECKED_CAST")
    internal val readable: StateListStateRecord<T> get() =
        (firstStateRecord as StateListStateRecord<T>).readable(this)

    /**
     * This is an internal implementation class of [SnapshotStateList]. Do not use.
     */
    internal class StateListStateRecord<T> internal constructor(
        internal var list: PersistentList<T>
    ) : StateRecord() {
        internal var modification = 0
        override fun assign(value: StateRecord) {
            @Suppress("UNCHECKED_CAST")
            list = (value as StateListStateRecord<T>).list
            modification = value.modification
        }

        override fun create(): StateRecord = StateListStateRecord(list)
    }

    override val size: Int get() = readable.list.size
    override fun contains(element: T) = readable.list.contains(element)
    override fun containsAll(elements: Collection<T>) = readable.list.containsAll(elements)
    override fun get(index: Int) = readable.list[index]
    override fun indexOf(element: T): Int = readable.list.indexOf(element)
    override fun isEmpty() = readable.list.isEmpty()
    override fun iterator(): MutableIterator<T> = listIterator()
    override fun lastIndexOf(element: T) = readable.list.lastIndexOf(element)
    override fun listIterator(): MutableListIterator<T> = StateListIterator(this, 0)
    override fun listIterator(index: Int): MutableListIterator<T> = StateListIterator(this, index)
    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> {
        require(fromIndex in 0..toIndex && toIndex <= size)
        return SubList(this, fromIndex, toIndex)
    }
    override fun add(element: T) = conditionalUpdate { it.add(element) }
    override fun add(index: Int, element: T) = update { it.add(index, element) }
    override fun addAll(index: Int, elements: Collection<T>) = mutate {
        it.addAll(index, elements)
    }
    override fun addAll(elements: Collection<T>) = conditionalUpdate { it.addAll(elements) }
    override fun clear() = writable { list = persistentListOf() }
    override fun remove(element: T) = conditionalUpdate { it.remove(element) }
    override fun removeAll(elements: Collection<T>) = conditionalUpdate { it.removeAll(elements) }
    override fun removeAt(index: Int): T = get(index).also { update { it.removeAt(index) } }
    override fun retainAll(elements: Collection<T>) = mutate { it.retainAll(elements) }
    override fun set(index: Int, element: T): T = get(index).also {
        update { it.set(index, element) }
    }

    fun removeRange(fromIndex: Int, toIndex: Int) {
        mutate {
            it.subList(fromIndex, toIndex).clear()
        }
    }

    private inline fun <R> writable(block: StateListStateRecord<T>.() -> R): R =
        @Suppress("UNCHECKED_CAST")
        (firstStateRecord as StateListStateRecord<T>).writable(this, block)

    private inline fun <R> withCurrent(block: StateListStateRecord<T>.() -> R): R =
        @Suppress("UNCHECKED_CAST")
        (firstStateRecord as StateListStateRecord<T>).withCurrent(block)

    private inline fun <R> mutate(block: (MutableList<T>) -> R): R =
        withCurrent {
            val builder = list.builder()
            val result = block(builder)
            val newList = builder.build()
            if (newList !== list) writable {
                list = newList
                modification++
            }
            result
        }

    private inline fun update(block: (PersistentList<T>) -> PersistentList<T>) = withCurrent {
        val newList = block(list)
        if (newList !== list) writable {
            list = newList
            modification++
        }
    }

    private inline fun conditionalUpdate(block: (PersistentList<T>) -> PersistentList<T>): Boolean =
        withCurrent {
            val newList = block(list)
            if (newList !== list) writable {
                list = newList
                modification++
                true
            } else false
        }
}

private fun modificationError(): Nothing =
    error("Cannot modify a state list through an iterator")

private fun validateRange(index: Int, size: Int) {
    if (index !in 0 until size) {
        throw IndexOutOfBoundsException("index ($index) is out of bound of [0, $size)")
    }
}

private class StateListIterator<T>(
    val list: SnapshotStateList<T>,
    offset: Int
) : MutableListIterator<T> {
    private var index = offset - 1
    private var modification = list.modification

    override fun hasPrevious() = index >= 0

    override fun nextIndex() = index + 1

    override fun previous(): T {
        validateModification()
        validateRange(index, list.size)
        return list[index].also { index-- }
    }

    override fun previousIndex(): Int = index

    override fun add(element: T) {
        validateModification()
        list.add(index + 1, element)
        index++
        modification = list.modification
    }

    override fun hasNext() = index < list.size - 1

    override fun next(): T {
        validateModification()
        val newIndex = index + 1
        validateRange(newIndex, list.size)
        return list[newIndex].also { index = newIndex }
    }

    override fun remove() {
        validateModification()
        list.removeAt(index)
        index--
        modification = list.modification
    }

    override fun set(element: T) {
        validateModification()
        list.set(index, element)
        modification = list.modification
    }

    private fun validateModification() {
        if (list.modification != modification) {
            throw ConcurrentModificationException()
        }
    }
}

private class SubList<T>(
    val parentList: SnapshotStateList<T>,
    fromIndex: Int,
    toIndex: Int
) : MutableList<T> {
    private val offset = fromIndex
    private var modification = parentList.modification
    override var size = toIndex - fromIndex
        private set

    override fun contains(element: T): Boolean = indexOf(element) >= 0
    override fun containsAll(elements: Collection<T>): Boolean = elements.all { contains(it) }
    override fun get(index: Int): T {
        validateModification()
        validateRange(index, size)
        return parentList[offset + index]
    }

    override fun indexOf(element: T): Int {
        validateModification()
        (offset until offset + size).forEach {
            if (element == parentList[it]) return it - offset
        }
        return -1
    }

    override fun isEmpty(): Boolean = size == 0

    override fun iterator(): MutableIterator<T> = listIterator()

    override fun lastIndexOf(element: T): Int {
        validateModification()
        var index = offset + size - 1
        while (index >= offset) {
            if (element == parentList[index]) return index - offset
            index--
        }
        return -1
    }

    override fun add(element: T): Boolean {
        validateModification()
        parentList.add(offset + size, element)
        size++
        modification = parentList.modification
        return true
    }

    override fun add(index: Int, element: T) {
        validateModification()
        parentList.add(offset + index, element)
        size++
        modification = parentList.modification
    }

    override fun addAll(index: Int, elements: Collection<T>): Boolean {
        validateModification()
        val result = parentList.addAll(index + offset, elements)
        if (result) {
            size += elements.size
            modification = parentList.modification
        }
        return result
    }

    override fun addAll(elements: Collection<T>): Boolean = addAll(size, elements)

    override fun clear() {
        if (size > 0) {
            validateModification()
            parentList.removeRange(offset, offset + size)
            size = 0
            modification = parentList.modification
        }
    }

    override fun listIterator(): MutableListIterator<T> = listIterator(0)
    override fun listIterator(index: Int): MutableListIterator<T> {
        validateModification()
        var current = index - 1
        return object : MutableListIterator<T> {
            override fun hasPrevious() = current >= 0
            override fun nextIndex(): Int = current + 1
            override fun previous(): T {
                val oldCurrent = current
                validateRange(oldCurrent, size)
                current = oldCurrent - 1
                return this@SubList[oldCurrent]
            }
            override fun previousIndex(): Int = current
            override fun add(element: T) = modificationError()
            override fun hasNext(): Boolean = current < size - 1
            override fun next(): T {
                val newCurrent = current + 1
                validateRange(newCurrent, size)
                current = newCurrent
                return this@SubList[newCurrent]
            }
            override fun remove() = modificationError()
            override fun set(element: T) = modificationError()
        }
    }

    override fun remove(element: T): Boolean {
        val index = indexOf(element)
        return if (index >= 0) {
            removeAt(index)
            true
        } else false
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        var removed = false
        for (element in elements) {
            removed = remove(element) || removed
        }
        return removed
    }

    override fun removeAt(index: Int): T {
        validateModification()
        return parentList.removeAt(offset + index).also {
            size--
            modification = parentList.modification
        }
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        validateModification()
        var index = offset + size - 1
        var removed = false
        while (index >= offset) {
            if (parentList[index] !in elements) {
                if (!removed) {
                    removed = true
                }
                parentList.removeAt(index)
                size--
            }
            index--
        }
        if (removed)
            modification = parentList.modification
        return removed
    }

    override fun set(index: Int, element: T): T {
        validateRange(index, size)
        validateModification()
        val result = parentList.set(index + offset, element)
        modification = parentList.modification
        return result
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> {
        require(fromIndex in 0..toIndex && toIndex <= size)
        validateModification()
        return SubList(parentList, fromIndex + offset, toIndex + offset)
    }

    private fun validateModification() {
        if (parentList.modification != modification) {
            throw ConcurrentModificationException()
        }
    }
}
