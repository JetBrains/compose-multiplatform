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

package androidx.compose.collection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalCollectionApi::class)
@RunWith(JUnit4::class)
class MutableVectorTest {
    val list: MutableVector<Int> = mutableVectorOf(1, 2, 3, 4, 5)

    @Test
    fun emptyConstruction() {
        val l = mutableVectorOf<String>()
        assertEquals(0, l.size)
        assertEquals(16, l.content.size)
        repeat(16) {
            assertNull(l.content[it])
        }
    }

    @Test
    fun sizeConstruction() {
        val l = MutableVector<String>(4)
        assertEquals(4, l.content.size)
        repeat(4) {
            assertNull(l.content[it])
        }
    }

    @Test
    fun contentConstruction() {
        val l = mutableVectorOf("a", "b", "c")
        assertEquals(3, l.size)
        assertEquals("a", l[0])
        assertEquals("b", l[1])
        assertEquals("c", l[2])
        assertEquals(3, l.content.size)
        repeat(2) {
            val l2 = mutableVectorOf(1, 2, 3, 4, 5)
            assertTrue(list.contentEquals(l2))
            l2.removeAt(0)
        }
    }

    @Test
    fun initConstruction() {
        val l = MutableVector(5) { it + 1 }
        assertTrue(l.contentEquals(list))
    }

    @Test
    fun get() {
        assertEquals(1, list[0])
        assertEquals(5, list[4])
    }

    @Test
    fun isEmpty() {
        assertFalse(list.isEmpty())
        assertTrue(mutableVectorOf<String>().isEmpty())
    }

    @Test
    fun isNotEmpty() {
        assertTrue(list.isNotEmpty())
        assertFalse(mutableVectorOf<String>().isNotEmpty())
    }

    @Test
    fun any() {
        assertTrue(list.any { it == 5 })
        assertTrue(list.any { it == 1 })
        assertFalse(list.any { it == 0 })
    }

    @Test
    fun forEach() {
        val copy = mutableVectorOf<Int>()
        list.forEach { copy += it }
        assertTrue(copy.contentEquals(list))
    }

    @Test
    fun forEachReversed() {
        val copy = mutableVectorOf<Int>()
        list.forEachReversed { copy += it }
        assertTrue(copy.contentEquals(mutableVectorOf(5, 4, 3, 2, 1)))
    }

    @Test
    fun forEachIndexed() {
        val copy = mutableVectorOf<Int>()
        val indices = mutableVectorOf<Int>()
        list.forEachIndexed { index, item ->
            copy += item
            indices += index
        }
        assertTrue(copy.contentEquals(list))
        assertTrue(indices.contentEquals(mutableVectorOf(0, 1, 2, 3, 4)))
    }

    @Test
    fun forEachReversedIndexed() {
        val copy = mutableVectorOf<Int>()
        val indices = mutableVectorOf<Int>()
        list.forEachReversedIndexed { index, item ->
            copy += item
            indices += index
        }
        assertTrue(copy.contentEquals(mutableVectorOf(5, 4, 3, 2, 1)))
        assertTrue(indices.contentEquals(mutableVectorOf(4, 3, 2, 1, 0)))
    }

    @Test
    fun indexOfFirst() {
        assertEquals(0, list.indexOfFirst { it == 1 })
        assertEquals(4, list.indexOfFirst { it == 5 })
        assertEquals(-1, list.indexOfFirst { it == 0 })
        assertEquals(0, mutableVectorOf("a", "a").indexOfFirst { it == "a" })
    }

    @Test
    fun indexOfLast() {
        assertEquals(0, list.indexOfLast { it == 1 })
        assertEquals(4, list.indexOfLast { it == 5 })
        assertEquals(-1, list.indexOfLast { it == 0 })
        assertEquals(1, mutableVectorOf("a", "a").indexOfLast { it == "a" })
    }

    @Test
    fun contains() {
        assertTrue(list.contains(5))
        assertTrue(list.contains(1))
        assertFalse(list.contains(0))
    }

    @Test
    fun containsAllList() {
        assertTrue(list.containsAll(listOf(2, 3, 1)))
        assertFalse(list.containsAll(listOf(2, 3, 6)))
    }

    @Test
    fun containsAllVector() {
        assertTrue(list.containsAll(mutableVectorOf(2, 3, 1)))
        assertFalse(list.containsAll(mutableVectorOf(2, 3, 6)))
    }

    @Test
    fun lastIndexOf() {
        assertEquals(4, list.lastIndexOf(5))
        assertEquals(1, list.lastIndexOf(2))
        val copy = mutableVectorOf<Int>()
        copy.addAll(list)
        copy.addAll(list)
        assertEquals(5, copy.lastIndexOf(1))
    }

    @Test
    fun map() {
        val mapped = list.map { it - 1 }
        repeat(5) {
            assertEquals(it, mapped[it])
        }
        assertEquals(5, mapped.size)
    }

    @Test
    fun mapIndexed() {
        val mapped = list.mapIndexed { index, item ->
            index + item
        }
        assertEquals(5, mapped.size)
        repeat(5) {
            assertEquals(it * 2 + 1, mapped[it])
        }
    }

    @Test
    fun mapIndexedNotNull() {
        val mapped = list.mapIndexedNotNull { index, item ->
            if (item == 5) null else index + item
        }
        assertEquals(4, mapped.size)
        repeat(4) {
            assertEquals(it * 2 + 1, mapped[it])
        }
    }

    @Test
    fun mapNotNull() {
        val mapped = list.mapNotNull { item ->
            if (item == 5) null else item - 1
        }
        assertEquals(4, mapped.size)
        repeat(4) {
            assertEquals(it, mapped[it])
        }
    }

    @Test
    fun first() {
        assertEquals(1, list.first())
    }

    @Test(expected = NoSuchElementException::class)
    fun firstException() {
        mutableVectorOf<String>().first()
    }

    @Test
    fun firstOrNull() {
        assertEquals(1, list.firstOrNull())
        assertNull(mutableVectorOf<Int>().firstOrNull())
    }

    @Test
    fun firstWithPredicate() {
        assertEquals(5, list.first { it == 5 })
        assertEquals(1, mutableVectorOf(1, 5).first { it != 0 })
    }

    @Test(expected = NoSuchElementException::class)
    fun firstWithPredicateException() {
        mutableVectorOf<String>().first { it == "Hello" }
    }

    @Test
    fun firstOrNullWithPredicate() {
        assertEquals(5, list.firstOrNull { it == 5 })
        assertNull(list.firstOrNull { it == 0 })
    }

    @Test
    fun last() {
        assertEquals(5, list.last())
    }

    @Test(expected = NoSuchElementException::class)
    fun lastException() {
        mutableVectorOf<String>().last()
    }

    @Test
    fun lastOrNull() {
        assertEquals(5, list.lastOrNull())
        assertNull(mutableVectorOf<Int>().lastOrNull())
    }

    @Test
    fun lastWithPredicate() {
        assertEquals(1, list.last { it == 1 })
        assertEquals(5, mutableVectorOf(1, 5).last { it != 0 })
    }

    @Test(expected = NoSuchElementException::class)
    fun lastWithPredicateException() {
        mutableVectorOf<String>().last { it == "Hello" }
    }

    @Test
    fun lastOrNullWithPredicate() {
        assertEquals(1, list.lastOrNull { it == 1 })
        assertNull(list.lastOrNull { it == 0 })
    }

    @Test
    fun sumBy() {
        assertEquals(15, list.sumBy { it })
    }

    @Test
    fun fold() {
        assertEquals("12345", list.fold("") { acc, i -> acc + i.toString() })
    }

    @Test
    fun foldIndexed() {
        assertEquals("01-12-23-34-45-", list.foldIndexed("") { index, acc, i ->
            "$acc$index$i-"
        })
    }

    @Test
    fun foldRight() {
        assertEquals("54321", list.foldRight("") { i, acc -> acc + i.toString() })
    }

    @Test
    fun foldRightIndexed() {
        assertEquals("45-34-23-12-01-", list.foldRightIndexed("") { index, i, acc ->
            "$acc$index$i-"
        })
    }

    @Test
    fun add() {
        val l = mutableVectorOf(1, 2, 3)
        l += 4
        l.add(5)
        assertTrue(l.contentEquals(list))
    }

    @Test
    fun addAtIndex() {
        val l = mutableVectorOf(2, 4)
        l.add(2, 5)
        l.add(0, 1)
        l.add(2, 3)
        assertTrue(l.contentEquals(list))
    }

    @Test
    fun addAllListAtIndex() {
        val l = listOf(4)
        val l2 = listOf(1, 2)
        val l3 = listOf(5)
        val l4 = mutableVectorOf(3)
        assertTrue(l4.addAll(1, l3))
        assertTrue(l4.addAll(0, l2))
        assertTrue(l4.addAll(3, l))
        assertFalse(l4.addAll(0, emptyList()))
        assertTrue(l4.contentEquals(list))
    }

    @Test
    fun addAllVectorAtIndex() {
        val l = mutableVectorOf(4)
        val l2 = mutableVectorOf(1, 2)
        val l3 = mutableVectorOf(5)
        val l4 = mutableVectorOf(3)
        assertTrue(l4.addAll(1, l3))
        assertTrue(l4.addAll(0, l2))
        assertTrue(l4.addAll(3, l))
        assertFalse(l4.addAll(0, mutableVectorOf()))
        assertTrue(l4.contentEquals(list))
    }

    @Test
    fun addAllList() {
        val l = listOf(3, 4, 5)
        val l2 = mutableVectorOf(1, 2)
        assertTrue(l2.addAll(l))
        assertFalse(l2.addAll(emptyList()))
    }

    @Test
    fun addAllVector() {
        val l = mutableVectorOf(3, 4, 5)
        val l2 = mutableVectorOf(1, 2)
        assertTrue(l2.addAll(l))
        assertFalse(l2.addAll(mutableVectorOf()))
    }

    @Test
    fun clear() {
        val l = mutableVectorOf<Int>()
        l.addAll(list)
        assertTrue(l.isNotEmpty())
        l.clear()
        assertTrue(l.isEmpty())
        repeat(5) {
            l.content[it] == null
        }
    }

    @Test
    fun remove() {
        val l = mutableVectorOf(1, 2, 3, 4, 5)
        l.remove(3)
        assertTrue(l.contentEquals(mutableVectorOf(1, 2, 4, 5)))
        assertNull(l.content[4])
    }

    @Test
    fun removeAt() {
        val l = mutableVectorOf(1, 2, 3, 4, 5)
        l.removeAt(2)
        assertTrue(l.contentEquals(mutableVectorOf(1, 2, 4, 5)))
        assertNull(l.content[4])
    }

    @Test
    fun set() {
        val l = mutableVectorOf(0, 0, 0, 0, 0)
        l[0] = 1
        l[4] = 5
        l[2] = 3
        l[1] = 2
        l[3] = 4
        assertTrue(l.contentEquals(list))
    }

    @Test
    fun ensureCapacity() {
        val l = mutableVectorOf(1)
        assertEquals(1, l.content.size)
        l.ensureCapacity(5)
        assertEquals(5, l.content.size)
    }

    @Test
    fun removeAllList() {
        assertFalse(list.removeAll(listOf(0, 10, 15)))
        val l = mutableVectorOf(0, 1, 15, 10, 2, 3, 4, 5, 20, 5)
        assertTrue(l.removeAll(listOf(20, 0, 15, 10, 5)))
        assertTrue(l.contentEquals(list))
    }

    @Test
    fun removeAllVector() {
        assertFalse(list.removeAll(mutableVectorOf(0, 10, 15)))
        val l = mutableVectorOf(0, 1, 15, 10, 2, 3, 4, 5, 20, 5)
        assertTrue(l.removeAll(mutableVectorOf(20, 0, 15, 10, 5)))
        assertTrue(l.contentEquals(list))
    }

    @Test
    fun contentEquals() {
        assertTrue(list.contentEquals(
            mutableVectorOf(
                1,
                2,
                3,
                4,
                5
            )
        ))
        assertFalse(list.contentEquals(
            mutableVectorOf(
                2,
                1,
                3,
                4,
                5
            )
        ))
        assertFalse(list.contentEquals(
            mutableVectorOf(
                1,
                2,
                3,
                4,
                5,
                6
            )
        ))
    }
}