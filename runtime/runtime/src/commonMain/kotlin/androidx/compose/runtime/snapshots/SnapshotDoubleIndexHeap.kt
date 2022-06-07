/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.compose.runtime.TestOnly

/**
 * This class maintains returns the lowest number of all the number it is given and can return that
 * number in O(1) time. Adding a number is at worst O(log N). Adding a number returns a handled that
 * can be later used to remove the number also with at worst O(log N).
 *
 * The data structure used is a heap, the first stage of a heap sort. As values are added and
 * removed the heap invariants are reestablished for the new value by either shifting values up
 * or down in the heap.
 *
 * This class is used to track the lowest pinning snapshot id. A pinning snapshot id is either the
 * lowest snapshot in its invalid list or its own id if its invalid list is empty.
 *
 * If any snapshot object has two records below the lowest pinned snapshot then the lowest snapshot
 * id can be reused as it will never be selected as the current record of the object because the
 * record with the higher id will always be selected instead.
 */
internal class SnapshotDoubleIndexHeap {
    var size = 0
        private set
    // An array of values which are the snapshot ids
    private var values = IntArray(INITIAL_CAPACITY)

    // An array of where the value's handle is in the handles array.
    private var index = IntArray(INITIAL_CAPACITY)

    // An array of handles which tracks where the value is the values array. Free handles are stored
    // as a single linked list using the array value as the link to the next free handle location.
    // It is initialized with 1, 2, 3, ... which produces a linked list of all handles free starting
    // at 0.
    private var handles = IntArray(INITIAL_CAPACITY) { it + 1 }

    // The first free handle.
    private var firstFreeHandle = 0

    fun lowestOrDefault(default: Int = 0) = if (size > 0) values[0] else default

    /**
     * Add a value to the heap by adding it to the end of the heap and then shifting it up until
     * it is either at the root or its parent is less or equal to it.
     */
    fun add(value: Int): Int {
        ensure(size + 1)
        val i = size++
        val handle = allocateHandle()
        values[i] = value
        index[i] = handle
        handles[handle] = i
        shiftUp(i)
        return handle
    }

    /**
     * Remove a value by using the index to locate where it is in the heap then replacing its
     * location with the last member of the heap and shifting it up or down depending to restore
     * the heap invariants.
     */
    fun remove(handle: Int) {
        val i = handles[handle]
        swap(i, size - 1)
        size--
        shiftUp(i)
        shiftDown(i)
        freeHandle(handle)
    }

    /**
     * Validate that the heap invariants hold.
     */
    @TestOnly
    fun validate() {
        for (index in 1 until size) {
            val parent = ((index + 1) shr 1) - 1
            if (values[parent] > values[index]) error("Index $index is out of place")
        }
    }

    /**
     * Validate that the handle refers to the expected value.
     */
    @TestOnly
    fun validateHandle(handle: Int, value: Int) {
        val i = handles[handle]
        if (index[i] != handle) error("Index for handle $handle is corrupted")
        if (values[i] != value)
            error("Value for handle $handle was ${values[i]} but was supposed to be $value")
    }

    /**
     * Shift a value at [index] until its parent is less than it is or it is at index 0.
     */
    private fun shiftUp(index: Int) {
        val values = values
        val value = values[index]
        var current = index
        while (current > 0) {
            val parent = ((current + 1) shr 1) - 1
            if (values[parent] > value) {
                swap(parent, current)
                current = parent
                continue
            }
            break
        }
    }

    /**
     * Shift a value at [index] down by comparing it to the least of its children and swapping with
     * it if the child is less than it is, continuing until the index has no children.
     */
    private fun shiftDown(index: Int) {
        val values = values
        val half = size shr 1
        var current = index
        while (current < half) {
            val right = (current + 1) shl 1
            val left = right - 1
            if (right < size && values[right] < values[left]) {
                if (values[right] < values[current]) {
                    swap(right, current)
                    current = right
                } else
                    return
            } else if (values[left] < values[current]) {
                swap(left, current)
                current = left
            } else
                return
        }
    }

    /**
     * Swap the values at index [a] and [b]. This is used to restore the heap invariants in
     * [shiftUp] and [shiftDown]. It also ensures that the [index] and [handles] are updated to
     * account for the swap.
     */
    private fun swap(a: Int, b: Int) {
        val values = values
        val index = index
        val handles = handles
        var t = values[a]
        values[a] = values[b]
        values[b] = t
        t = index[a]
        index[a] = index[b]
        index[b] = t
        handles[index[a]] = a
        handles[index[b]] = b
    }

    /**
     * Ensure that the heap can contain at least [atLeast] elements.
     */
    private fun ensure(atLeast: Int) {
        val capacity = values.size
        if (atLeast <= capacity) return
        val newCapacity = capacity * 2
        val newValues = IntArray(newCapacity)
        val newIndex = IntArray(newCapacity)
        values.copyInto(newValues)
        index.copyInto(newIndex)
        values = newValues
        index = newIndex
    }

    /**
     * Allocate a free handle, growing the list of handles if necessary.
     */
    private fun allocateHandle(): Int {
        val capacity = handles.size
        if (firstFreeHandle >= capacity) {
            val newHandles = IntArray(capacity * 2) { it + 1 }
            handles.copyInto(newHandles)
            handles = newHandles
        }
        val handle = firstFreeHandle
        firstFreeHandle = handles[firstFreeHandle]
        return handle
    }

    /**
     * Free a handle by adding it to the free list of handles which is a linked list of handles
     * stored in the handles array as a linked list of indexes.
     */
    private fun freeHandle(handle: Int) {
        handles[handle] = firstFreeHandle
        firstFreeHandle = handle
    }
}

private const val INITIAL_CAPACITY = 16