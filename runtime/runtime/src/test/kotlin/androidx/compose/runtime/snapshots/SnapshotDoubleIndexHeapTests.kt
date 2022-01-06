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

import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SnapshotDoubleIndexHeapTests {

    @Test
    fun canCreateADoubleIndexHeap() {
        val heap = SnapshotDoubleIndexHeap()
        assertNotNull(heap)
    }

    @Test
    fun canAddAndRemoveNumbersInSequence() {
        val heap = SnapshotDoubleIndexHeap()
        val handles = IntArray(100)
        repeat(100) {
            handles[it] = heap.add(it)
        }
        repeat(100) {
            assertEquals(it, heap.lowestOrDefault(-1))
            heap.remove(handles[it])
        }
        assertEquals(0, heap.size)
    }

    @Test
    fun canInsertAndRemoveRandomNumbersWithDuplicate() {
        val heap = SnapshotDoubleIndexHeap()
        val random = Random(1377)
        val toAdd = IntArray(5000) { random.nextInt(0 until 300) }.toMutableList()
        val toRemove = mutableListOf<Pair<Int, Int>>()

        while (toAdd.size > 0 || toRemove.size > 0) {
            val shouldAdd = random.nextInt(toAdd.size + toRemove.size) < toAdd.size
            if (shouldAdd) {
                val indexToAdd = random.nextInt(toAdd.size)
                val value = toAdd[indexToAdd]
                val handle = heap.add(value)
                toRemove.add(value to handle)
                toAdd.removeAt(indexToAdd)
            } else {
                val indexToRemove = random.nextInt(toRemove.size)
                val (value, handle) = toRemove[indexToRemove]
                assertTrue(heap.lowestOrDefault(-1) <= value)
                heap.remove(handle)
                toRemove.removeAt(indexToRemove)
            }

            heap.validate()
            for ((value, handle) in toRemove) {
                heap.validateHandle(handle, value)
            }
            val lowestAdded = toRemove.fold(400) { lowest, (value, _) ->
                if (value < lowest) value else lowest
            }
            assertEquals(lowestAdded, heap.lowestOrDefault(400))
        }
    }
}