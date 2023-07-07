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

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class IdentityScopeMapTest {
    private val map = IdentityScopeMap<Scope>()

    private val scopeList = listOf(Scope(10), Scope(12), Scope(1), Scope(30), Scope(10))
    private val valueList = listOf(Value("A"), Value("B"))

    @Test
    fun emptyConstruction() {
        val m = IdentityScopeMap<Test>()
        assertEquals(0, m.size)
    }

    @Test
    fun add() {
        scopeList.forEach { scope ->
            valueList.forEach { value ->
                map.add(value, scope)
            }
        }
        assertEquals(valueList.size, map.size)
        val verifierList = mutableListOf<Scope>()
        valueList.forEach { value ->
            map.forEachScopeOf(value) {
                verifierList += it
            }
            assertEquals(scopeList.size, verifierList.size)
            verifierList.clear()
        }
    }

    @Test
    fun forEachScopeOf() {
        map.add(valueList[0], scopeList[0])
        map.add(valueList[0], scopeList[1])
        map.add(valueList[1], scopeList[2])

        var count = 0
        map.forEachScopeOf(valueList[1]) { scope ->
            assertEquals(scopeList[2], scope)
            count++
        }
        assertEquals(1, count)

        val verifierSet = mutableListOf<Scope>()
        map.forEachScopeOf(valueList[0]) { scope ->
            verifierSet += scope
        }
        assertEquals(2, verifierSet.size)
        assertTrue(verifierSet.contains(scopeList[0]))
        assertTrue(verifierSet.contains(scopeList[1]))
    }

    @Test
    fun clear() {
        map.add(valueList[0], scopeList[0])
        map.add(valueList[1], scopeList[1])
        map.clear()
        assertEquals(0, map.size)
        assertEquals(0, map.scopeSets[0]!!.size)
        assertEquals(0, map.scopeSets[1]!!.size)
    }

    @Test
    fun removeScope() {
        val valueC = Value("C")
        map.add(valueList[0], scopeList[0])
        map.add(valueList[0], scopeList[1])
        map.add(valueList[1], scopeList[2])
        map.add(valueC, scopeList[3])

        // remove a non existent value, should leave the map unmodified
        val removed1 = map.remove(valueC, scopeList[0])
        assertFalse(removed1)
        assertEquals(3, map.size)

        // Remove a reference which should remove the value
        val removed2 = map.remove(valueList[1], scopeList[2])
        assertTrue(removed2)
        assertEquals(2, map.size)
    }

    @Test
    fun removeValueIf() {
        val valueC = Value("C")
        map.add(valueList[0], scopeList[0])
        map.add(valueList[0], scopeList[1])
        map.add(valueList[1], scopeList[2])
        map.add(valueC, scopeList[3])

        // remove a scope that won't cause any values to be removed:
        map.removeValueIf { scope ->
            scope === scopeList[1]
        }
        assertEquals(3, map.size)

        // remove the last scope in a set:
        map.removeValueIf { scope ->
            scope === scopeList[2]
        }
        assertEquals(2, map.size)
        assertEquals(0, map.scopeSets[map.valueOrder[2]]!!.size)

        map.forEachScopeOf(valueList[1]) {
            fail("There shouldn't be any scopes for this value")
        }
    }

    @Test
    fun canUseIn() {
        val valueC = Value("C")
        map.add(valueList[0], scopeList[0])
        map.add(valueList[0], scopeList[1])
        map.add(valueList[1], scopeList[2])
        map.add(valueC, scopeList[3])

        assertTrue(valueList[0] in map)
        assertTrue(valueList[1] in map)
        assertTrue(valueC in map)
        assertFalse(Value("D") in map)
    }

    /**
     * Validate the test maintains the internal assumptions of the map.
     */
    @AfterTest
    fun validateMap() {
        // Ensure that no duplicates exist in value-order and all indexes are represented
        val pendingRepresentation = mutableSetOf(*map.values.indices.toList().toTypedArray())
        map.valueOrder.forEach {
            assertTrue(it in pendingRepresentation, "Index $it was duplicated")
            pendingRepresentation.remove(it)
        }
        assertTrue(pendingRepresentation.isEmpty(), "Not all indexes are in the valueOrder map")

        // Ensure values are non-null and sets are not empty for index < size and values are
        // null and sets are empty or missing for >= size
        val size = map.size
        map.valueOrder.forEachIndexed { index, order ->
            val value = map.values[order]
            val set = map.scopeSets[order]
            if (index < size) {
                assertNotNull(value, "A value was unexpectedly null")
                assertNotNull(set, "A set was unexpectedly null")
                assertTrue(set.size > 0, "An empty set wasn't collected")
            } else {
                assertNull(value, "A reference to a removed value was retained")
                assertTrue(
                    actual = set == null || set.size == 0,
                    message = "A non-empty set was dropped"
                )
            }
        }
    }

    data class Scope(val item: Int)
    data class Value(val s: String)
}