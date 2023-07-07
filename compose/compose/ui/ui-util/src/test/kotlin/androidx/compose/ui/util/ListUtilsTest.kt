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

package androidx.compose.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ListUtilsTest {
    @Test
    fun regularIteration() {
        val list = listOf(1, 5, 10)
        val otherList = mutableListOf<Int>()
        list.fastForEach {
            otherList.add(it)
        }
        // The correct iteration order, all items in there
        assertEquals(otherList, list)
    }

    @Test
    fun shortIteration() {
        val list = listOf(1, 5, 10)
        val otherList = mutableListOf<Int>()
        list.fastForEach {
            if (it == 5) {
                return@fastForEach
            }
            otherList.add(it)
        }
        // Should have only one item in it
        assertEquals(2, otherList.size)
        assertEquals(1, otherList[0])
        assertEquals(10, otherList[1])
    }

    @Test
    fun regularIterationIndexed() {
        val list = listOf(1, 5, 10)
        val otherList = mutableListOf<Int>()
        val otherIndices = mutableListOf<Int>()
        list.fastForEachIndexed { index, item ->
            otherList.add(item)
            otherIndices.add(index)
        }
        // The correct iteration order, all items in there
        assertEquals(list, otherList)
        assertEquals(listOf(0, 1, 2), otherIndices)
    }

    @Test
    fun shortIterationIndexed() {
        val list = listOf(1, 5, 10)
        val otherList = mutableListOf<Int>()
        val otherIndices = mutableListOf<Int>()
        list.fastForEachIndexed { index, item ->
            if (item == 5) {
                return@fastForEachIndexed
            }
            otherList.add(item)
            otherIndices.add(index)
        }
        // Should have only one item in it
        assertEquals(2, otherList.size)
        assertEquals(2, otherIndices.size)
        assertEquals(1, otherList[0])
        assertEquals(10, otherList[1])
        assertEquals(listOf(0, 2), otherIndices)
    }

    @Test
    fun anyEmpty() {
        val list = listOf<Int>()
        assertFalse(list.fastAny { it > 0 })
    }

    @Test
    fun anyNotFound() {
        val list = listOf(0, -1, -500)
        assertFalse(list.fastAny { it > 0 })
    }

    @Test
    fun anyFound() {
        val list = listOf(0, -1, -500, 1)
        assertTrue(list.fastAny { it > 0 })
    }

    @Test
    fun firstOrNullNotFound() {
        val list = listOf(0, -1, -500)
        assertNull(list.fastFirstOrNull { it > 0 })
    }
    @Test
    fun firstOrNullFound() {
        val list = listOf(0, -1, -500, 1)
        assertEquals(1, list.fastFirstOrNull { it > 0 })
    }
}