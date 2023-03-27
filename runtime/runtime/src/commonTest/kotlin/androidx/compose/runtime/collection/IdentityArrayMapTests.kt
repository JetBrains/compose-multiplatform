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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class Key(val value: Int)

class IdentityArrayMapTests {
    private val keys = Array(100) { Key(it) }

    @Test
    fun canCreateEmptyMap() {
        val map = IdentityArrayMap<Key, Any>()
        assertTrue(map.isEmpty(), "map is not empty")
    }

    @Test
    fun canSetAndGetValues() {
        val map = IdentityArrayMap<Key, String>()
        map[keys[1]] = "One"
        map[keys[2]] = "Two"
        assertEquals("One", map[keys[1]], "map key 1")
        assertEquals("Two", map[keys[2]], "map key 2")
        assertEquals(null, map[keys[3]], "map key 3")
    }

    @Test
    fun canSetAndGetManyValues() {
        val map = IdentityArrayMap<Key, String>()
        repeat(keys.size) {
            map[keys[it]] = it.toString()
        }
        repeat(keys.size) {
            assertEquals(it.toString(), map[keys[it]], "map key $it")
        }
    }

    @Test
    fun canRemoveValues() {
        val map = IdentityArrayMap<Key, Int>()
        repeat(keys.size) {
            map[keys[it]] = it
        }
        map.removeValueIf { value -> value % 2 == 0 }
        assertEquals(keys.size / 2, map.size)
        for (i in 1 until keys.size step 2) {
            assertEquals(i, map[keys[i]], "map key $i")
        }
        for (i in 0 until keys.size step 2) {
            assertEquals(null, map[keys[i]], "map key $i")
        }
        map.removeValueIf { true }
        assertEquals(0, map.size, "map is not empty after removing everything")
    }

    @Test
    fun canRemoveKeys() {
        val map = IdentityArrayMap<Key, Int>()
        repeat(keys.size) {
            map[keys[it]] = it
        }
        map.removeIf { key, _ -> key.value % 2 == 0 }
        assertEquals(keys.size / 2, map.size)
        for (i in 1 until keys.size step 2) {
            assertEquals(i, map[keys[i]], "map key $i")
        }
        for (i in 0 until keys.size step 2) {
            assertEquals(null, map[keys[i]], "map key $i")
        }
        map.removeIf { _, _ -> true }
        assertEquals(0, map.size, "map is not empty after removing everything")
    }

    @Test
    fun canForEachKeysAndValues() {
        val map = IdentityArrayMap<Key, String>()
        repeat(100) {
            map[keys[it]] = it.toString()
        }
        assertEquals(100, map.size)
        var count = 0
        map.forEach { key, value ->
            assertEquals(key.value.toString(), value, "map key ${key.value}")
            count++
        }
        assertEquals(map.size, count, "forEach didn't loop the expected number of times")
    }

    @Test
    fun canRemoveItems() {
        val map = IdentityArrayMap<Key, String>()
        repeat(100) {
            map[keys[it]] = it.toString()
        }

        repeat(100) {
            assertEquals(100 - it, map.size)
            val removed = map.remove(keys[it])
            assertEquals(removed, it.toString(), "Expected to remove $it for ${keys[it]}")
            if (it > 0) {
                assertNull(
                    map.remove(keys[it - 1]),
                    "Expected item ${it - 1} to already be removed"
                )
            }
        }
    }

    @Test // b/195621739
    fun canRemoveWhenFull() {
        val map = IdentityArrayMap<Key, String>()
        repeat(16) {
            map[keys[it]] = it.toString()
        }
        repeat(16) {
            val key = keys[it]
            val removed = map.remove(key)
            assertNotNull(removed)
            assertFalse(map.contains(key))
        }
        assertTrue(map.isEmpty())
    }

    @Test
    fun canClear() {
        val map = IdentityArrayMap<Key, String>()
        repeat(16) {
            map[keys[it]] = it.toString()
        }
        map.clear()
        assertTrue(map.isEmpty())
        assertEquals(0, map.size, "map size should be 0 after calling clear")
    }
}