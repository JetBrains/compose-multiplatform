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
import androidx.compose.util.synchronized
import androidx.compose.util.createSynchronizedObject
import kotlin.jvm.JvmName

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
            synchronized(sync) {
                @Suppress("UNCHECKED_CAST")
                list = (value as StateListStateRecord<T>).list
                modification = value.modification
            }
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
    override fun addAll(index: Int, elements: Collection<T>) = mutateBoolean {
        it.addAll(index, elements)
    }

    override fun addAll(elements: Collection<T>) = conditionalUpdate { it.addAll(elements) }
    override fun clear() {
        synchronized(sync) {
            writable {
                list = persistentListOf()
                modification++
            }
        }
    }
    override fun remove(element: T) = conditionalUpdate { it.remove(element) }
    override fun removeAll(elements: Collection<T>) = conditionalUpdate { it.removeAll(elements) }
    override fun removeAt(index: Int): T = get(index).also { update { it.removeAt(index) } }
    override fun retainAll(elements: Collection<T>) = mutateBoolean { it.retainAll(elements) }
    override fun set(index: Int, element: T): T = get(index).also {
        update { it.set(index, element) }
    }

    fun removeRange(fromIndex: Int, toIndex: Int) {
        mutate {
            it.subList(fromIndex, toIndex).clear()
        }
    }

    internal fun retainAllInRange(elements: Collection<T>, start: Int, end: Int): Int {
        val startSize = size
        mutate<Unit> {
            it.subList(start, end).retainAll(elements)
        }
        return startSize - size
    }

    /**
     * An internal function used by the debugger to display the value of the current list without
     * triggering read observers.
     */
    @Suppress("unused")
    internal val debuggerDisplayValue: List<T>
        @JvmName("getDebuggerDisplayValue")
        get() = withCurrent { list }

    private inline fun <R> writable(block: StateListStateRecord<T>.() -> R): R =
        @Suppress("UNCHECKED_CAST")
        (firstStateRecord as StateListStateRecord<T>).writable(this, block)

    private inline fun <R> withCurrent(block: StateListStateRecord<T>.() -> R): R =
        @Suppress("UNCHECKED_CAST")
        (firstStateRecord as StateListStateRecord<T>).withCurrent(block)

    private fun mutateBoolean(block: (MutableList<T>) -> Boolean): Boolean = mutate(block)

    private inline fun <R> mutate(block: (MutableList<T>) -> R): R {
        var result: R
        while (true) {
            var oldList: PersistentList<T>? = null
            var currentModification = 0
            synchronized(sync) {
                val current = withCurrent { this }
                currentModification = current.modification
                oldList = current.list
            }
            val builder = oldList!!.builder()
            result = block(builder)
            val newList = builder.build()
            if (newList == oldList || synchronized(sync) {
                writable {
                    if (modification == currentModification) {
                        list = newList
                        modification++
                        true
                    } else false
                }
            }
            ) break
        }
        return result
    }

    private inline fun update(block: (PersistentList<T>) -> PersistentList<T>) {
        conditionalUpdate(block)
    }

    private inline fun conditionalUpdate(block: (PersistentList<T>) -> PersistentList<T>) =
        run {
            val result: Boolean
            while (true) {
                var oldList: PersistentList<T>? = null
                var currentModification = 0
                synchronized(sync) {
                    val current = withCurrent { this }
                    currentModification = current.modification
                    oldList = current.list
                }
                val newList = block(oldList!!)
                if (newList == oldList) {
                    result = false
                    break
                }
                if (synchronized(sync) {
                    writable {
                        if (modification == currentModification) {
                            list = newList
                            modification++
                            true
                        } else false
                    }
                }
                ) {
                    result = true
                    break
                }
            }
            result
        }
}

/**
 * This lock is used to ensure that the value of modification and the list in the state record,
 * when used together, are atomically read and written.
 *
 * A global sync object is used to avoid having to allocate a sync object and initialize a monitor
 * for each instance the list. This avoid additional allocations but introduces some contention
 * between lists. As there is already contention on the global snapshot lock to write so the
 * additional contention introduced by this lock is nominal.
 *
 * In code the requires this lock and calls `writable` (or other operation that acquires the
 * snapshot global lock), this lock *MUST* be acquired first to avoid deadlocks.
 */
private val sync = createSynchronizedObject()

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
        val removed = parentList.retainAllInRange(elements, offset, offset + size)
        if (removed > 0) {
            modification = parentList.modification
            size -= removed
        }
        return removed > 0
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
