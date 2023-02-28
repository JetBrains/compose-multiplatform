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

package androidx.compose.runtime.collection

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class SparseArrayImplTest {
    @Test
    fun addingValue_increasesSize() {
        val subject = IntMap<Int>(10)
        subject[1] = 2
        assertEquals(subject.size, 1)

        subject[2] = 3
        assertEquals(subject.size, 2)

        subject[1] = 5
        assertEquals(subject.size, 2)
    }

    @Test
    fun setValue_canBeRetrieved() {
        val subject = IntMap<Int>(10)
        val added = mutableSetOf<Int>()
        for (i in 1..1000) {
            val next = Random.nextInt(i)
            added.add(next)
            subject[next] = next
        }
        for (item in added) {
            assertEquals(subject[item], item)
        }
    }

    @Test
    fun removingValue_decreasesSize() {
        val (subject, added) = makeRandom100()
        val item = added.first()
        subject.remove(item)
        assertEquals(subject.size, added.size - 1)
        assertEquals(subject[item], null)
    }

    @Test
    fun removedValue_canBeSet() {
        val (subject, added) = makeRandom100()
        val item = added.first()
        subject.remove(item)
        subject[item] = -1
        assertEquals(subject[item], -1)
    }

    @Test
    fun clear_clears() {
        val (subject, added) = makeRandom100()
        subject.clear()
        for (item in added) {
            assertEquals(subject[item], null)
        }

        val item = added.first()
        subject[item] = -1
        assertEquals(subject[item], -1)
    }

    private fun makeRandom100(): Pair<IntMap<Int>, MutableSet<Int>> {
        val subject = IntMap<Int>(10)
        val added = mutableSetOf<Int>()
        for (i in 1..1000) {
            val next = Random.nextInt(i)
            added.add(next)
            subject[next] = next
        }
        for (item in added) {
            assertEquals(subject[item], item)
        }
        return subject to added
    }
}