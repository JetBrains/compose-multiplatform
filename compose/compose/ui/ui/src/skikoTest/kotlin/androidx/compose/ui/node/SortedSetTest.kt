/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.node

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SortedSetTest {
    private fun <E: Comparable<*>> sortedSetOf(vararg elements: E): SortedSet<E> =
        sortedSetOf(compareBy { it }, *elements)

    private fun <E> sortedSetOf(comparator: Comparator<in E>, vararg elements: E): SortedSet<E> {
        val set = SortedSet(comparator)
        for (element in elements) {
            set.add(element)
            assertTrue(set.contains(element))
        }
        return set
    }

    private fun <E> assertOrderEquals(expect: Iterable<E>, actual: SortedSet<E>) {
        for (e in expect) {
            assertEquals(e, actual.first())
            assertTrue(actual.contains(e))
            assertTrue(actual.remove(e))
            assertFalse(actual.contains(e))
        }
        assertTrue(actual.isEmpty())
    }

    @Test
    fun correctOrder() {
        assertOrderEquals(listOf(1, 2, 5, 6), sortedSetOf(1, 2, 5, 6))
        assertOrderEquals(listOf(1, 2, 5, 6), sortedSetOf(2, 6, 1, 5))
        val numbers = (1..1000).map { Random.nextInt(10_000_000) }.distinct()
        val set = sortedSetOf(*numbers.toTypedArray())
        assertOrderEquals(numbers.sorted(), set)
    }

    @Test
    fun customComparator() {
        val set = sortedSetOf(compareBy { it.length }, "B", "AAA", "DD")
        assertOrderEquals(listOf("B", "DD", "AAA"), set)
    }
}
