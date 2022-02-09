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

package androidx.compose.runtime.collection

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IdentityArrayIntMapTests {

    @Test
    fun emptyConstruction() {
        val m = IdentityArrayIntMap()
        assertEquals(0, m.size)
    }

    @Test
    fun canAddValues() {
        val map = IdentityArrayIntMap()
        val keys = Array<Any>(100) { Any() }
        for (i in keys.indices) {
            map.add(keys[i], i)
        }
        for (i in keys.indices) {
            assertEquals(i, map[keys[i]])
        }
        map.removeValueIf { key, value ->
            assertEquals(keys[value], key)
            false
        }
    }

    @Test
    fun canRemoveValues() {
        val map = IdentityArrayIntMap()
        val keys = Array<Any>(100) { Any() }
        for (i in keys.indices) {
            map.add(keys[i], i)
        }
        for (i in keys.indices step 2) {
            map.remove(keys[i])
        }
        assertEquals(50, map.size)
        map.removeValueIf { key, value ->
            assertEquals(keys[value], key)
            assertTrue(value % 2 == 1)
            false
        }
    }

    @Test
    fun canRemoveIfValues() {
        val map = IdentityArrayIntMap()
        val keys = Array<Any>(100) { Any() }
        for (i in keys.indices) {
            map.add(keys[i], i)
        }
        map.removeValueIf { _, value -> value % 2 == 0 }
        assertEquals(50, map.size)
    }

    @Test
    fun canReplaceValues() {
        val map = IdentityArrayIntMap()
        val keys = Array<Any>(100) { Any() }
        for (i in keys.indices) {
            map.add(keys[i], i)
        }

        for (i in keys.indices) {
            map.add(keys[i], i + 100)
        }

        assertEquals(100, map.size)
        for (i in keys.indices) {
            assertEquals(i + 100, map[keys[i]])
        }
    }

    @Test
    fun anyFindsCorrectValue() {
        val map = IdentityArrayIntMap()
        val keys = Array(100) { Any() }
        for (i in keys.indices) {
            map.add(keys[i], i)
        }
        assertTrue(map.any { _, value -> value == 20 })
        assertFalse(map.any { _, value -> value > 100 })
    }

    @Test
    fun canForEach() {
        val map = IdentityArrayIntMap()
        val keys = Array(100) { Any() }
        for (i in keys.indices) {
            map.add(keys[i], i)
        }
        map.forEach { key, value ->
            assertEquals(keys.indexOf(key), value)
        }
    }
}