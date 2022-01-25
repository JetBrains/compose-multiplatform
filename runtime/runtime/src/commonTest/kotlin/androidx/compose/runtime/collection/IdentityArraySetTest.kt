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

package androidx.compose.runtime.collection

import androidx.compose.runtime.identityHashCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IdentityArraySetTest {
    private val set: IdentityArraySet<Stuff> = IdentityArraySet()

    private val list = listOf(Stuff(10), Stuff(12), Stuff(1), Stuff(30), Stuff(10))

    @Test
    fun emptyConstruction() {
        val s = IdentityArraySet<Stuff>()
        assertEquals(0, s.size)
    }

    @Test
    fun addValueForward() {
        list.forEach { set.add(it) }
        assertEquals(list.size, set.size)
        var previousItem = set[0]
        for (i in 1 until set.size) {
            val item = set[i]
            assertTrue(identityHashCode(previousItem) < identityHashCode(item))
            previousItem = item
        }
    }

    @Test
    fun addValueReversed() {
        list.asReversed().forEach { set.add(it) }
        assertEquals(list.size, set.size)
        var previousItem = set[0]
        for (i in 1 until set.size) {
            val item = set[i]
            assertTrue(identityHashCode(previousItem) < identityHashCode(item))
            previousItem = item
        }
    }

    @Test
    fun addExistingValue() {
        list.forEach { set.add(it) }
        list.asReversed().forEach { set.add(it) }

        assertEquals(list.size, set.size)
        var previousItem = set[0]
        for (i in 1 until set.size) {
            val item = set[i]
            assertTrue(identityHashCode(previousItem) < identityHashCode(item))
            previousItem = item
        }
    }

    @Test
    fun clear() {
        list.forEach { set.add(it) }
        set.clear()

        assertEquals(0, set.size)
        set.values.forEach {
            assertNull(it)
        }
    }

    @Test
    fun remove() {
        list.forEach { set.add(it) }

        // remove a value that doesn't exist:
        val removed = set.remove(Stuff(10))
        assertEquals(list.size, set.size)
        assertFalse(removed)

        // remove a value in the middle:
        testRemoveValueAtIndex(set.size / 2)

        // remove the last value
        testRemoveValueAtIndex(set.size - 1)

        // remove a first value
        testRemoveValueAtIndex(0)
    }

    @Test
    fun removeValueIf() {
        list.forEach { set.add(it) }

        set.removeValueIf { it.item == 10 }

        // Make sure we've removed both items
        assertEquals(list.size - 2, set.size)
        set.forEach { assertNotEquals(10, it.item) }
        assertNull(set.values[set.size])
        assertNull(set.values[set.size + 1])
    }

    @Test
    fun growSet() {
        val verifierSet = mutableSetOf<Stuff>()
        repeat(100) {
            val stuff = Stuff(it)
            set.add(stuff)
            verifierSet.add(stuff)
        }
        assertEquals(100, set.size)
        set.forEach { verifierSet.remove(it) }
        assertEquals(0, verifierSet.size)
    }

    @Test
    fun canUseAsSetOfT() {
        val stuff = Array(100) { Stuff(it) }
        for (i in 0 until 100 step 2) {
            set.add(stuff[i])
        }
        val setOfT: Set<Stuff> = set
        for (i in 0 until 100) {
            val expected = i % 2 == 0
            if (expected) {
                assertTrue(stuff[i] in set)
                assertTrue(stuff[i] in setOfT)
            } else {
                assertFalse(stuff[i] in set)
                assertFalse(stuff[i] in setOfT)
            }
        }
        for (element in setOfT) {
            assertTrue(element.item % 2 == 0)
            assertEquals(element, stuff[element.item])
        }

        set.add(stuff[1])
        assertTrue(stuff[1] in setOfT)

        assertTrue(setOfT.containsAll(listOf(stuff[0], stuff[1], stuff[2])))
    }

    private fun testRemoveValueAtIndex(index: Int) {
        val value = set[index]
        val initialSize = set.size
        val removed = set.remove(value)
        assertEquals(initialSize - 1, set.size)
        assertTrue(removed)
        assertNull(set.values[set.size])
        set.forEach { assertNotSame(value, it) }
    }

    data class Stuff(val item: Int)
}