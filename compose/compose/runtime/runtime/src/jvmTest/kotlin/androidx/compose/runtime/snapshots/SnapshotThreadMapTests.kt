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

import androidx.compose.runtime.SnapshotThreadLocal
import androidx.compose.runtime.internal.ThreadMap
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Test the internal ThreadMap
 */
class SnapshotThreadMapTests {
    @Test
    fun canCreateAMap() {
        val map = emptyThreadMap()
        assertNotNull(map)
    }

    @Test
    fun setOfEmptyFails() {
        val map = emptyThreadMap()
        val added = map.trySet(1, 1)
        assertFalse(added)
    }

    @Test
    fun canAddOneToEmpty() {
        val map = emptyThreadMap()
        val newMap = map.newWith(1, 1)
        assertNotEquals(map, newMap)
        assertEquals(1, newMap.get(1))
    }

    @Test
    fun canCreateForward() {
        val map = testMap(0 until 100)
        assertNotNull(map)
        for (i in 0 until 100) {
            assertEquals(i, map.get(i.toLong()))
        }
        for (i in -100 until 0) {
            assertNull(map.get(i.toLong()))
        }
        for (i in 100 until 200) {
            assertNull(map.get(i.toLong()))
        }
    }

    @Test
    fun canCreateBackward() {
        val map = testMap((0 until 100).reversed())
        assertNotNull(map)
        for (i in 0 until 100) {
            assertEquals(i, map.get(i.toLong()))
        }
        for (i in -100 until 0) {
            assertNull(map.get(i.toLong()))
        }
        for (i in 100 until 200) {
            assertNull(map.get(i.toLong()))
        }
    }

    @Test
    fun canCreateRandom() {
        val list = Array<Long>(100) { it.toLong() }
        val rand = Random(1337)
        list.shuffle(rand)
        var map = emptyThreadMap()
        for (item in list) {
            map = map.newWith(item, item)
        }
        for (i in 0 until 100) {
            assertEquals(i.toLong(), map.get(i.toLong()))
        }
        for (i in -100 until 0) {
            assertNull(map.get(i.toLong()))
        }
        for (i in 100 until 200) {
            assertNull(map.get(i.toLong()))
        }
    }

    @Test
    fun canRemoveOne() {
        val map = testMap(1..10)
        val set = map.trySet(5, null)
        assertTrue(set)
        for (i in 1..10) {
            if (i == 5) {
                assertNull(map.get(i.toLong()))
            } else {
                assertEquals(i, map.get(i.toLong()))
            }
        }
    }

    @Test
    fun canRemoveOneThenAddOne() {
        val map = testMap(1..10)
        val set = map.trySet(5, null)
        assertTrue(set)
        val newMap = map.newWith(11, 11)
        assertNull(newMap.get(5))
        assertEquals(11, newMap.get(11))
    }

    private fun emptyThreadMap() = ThreadMap(0, LongArray(0), arrayOfNulls(0))

    private fun testMap(intProgression: IntProgression): ThreadMap {
        var result = emptyThreadMap()
        for (i in intProgression) {
            result = result.newWith(i.toLong(), i)
        }
        return result
    }
}

/**
 * Test the thread lcoal variable
 */
class SnapshotThreadLocalTests {
    @Test
    fun canCreate() {
        val local = SnapshotThreadLocal<Int>()
        assertNotNull(local)
    }

    @Test
    fun initalValueIsNull() {
        val local = SnapshotThreadLocal<Int>()
        assertNull(local.get())
    }

    @Test
    fun canSetAndGetTheValue() {
        val local = SnapshotThreadLocal<Int>()
        local.set(100)
        assertEquals(100, local.get())
    }
}