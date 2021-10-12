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

package androidx.compose.runtime.snapshots

import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.test.IgnoreJsTarget
import kotlinx.test._runBlocking

class SnapshotStateMapTests {
    @Test
    fun canCreateAnMapEmpty() {
        mutableStateMapOf<Int, Float>()
    }

    @Test
    fun canCreateAnInitializedMap() {
        mutableStateMapOf(1 to 1f, 2 to 2f, 3 to 3f)
    }

    @Test
    fun validateSize() {
        validateRead { map, normalMap ->
            assertEquals(normalMap.size, map.size)
        }
    }

    @Test
    fun validateContainsKey() {
        validateRead { map, normalMap ->
            for (value in listOf(1, 2, 3, 100, 200)) {
                assertEquals(normalMap.containsKey(value), map.containsKey(value))
            }
        }
    }

    @Test
    fun validate_containsValue() {
        validateRead { map, normalMap ->
            for (value in listOf(1f, 2f, 3f, 100f, 200f)) {
                assertEquals(normalMap.containsValue(value), map.containsValue(value))
            }
        }
    }

    @Test
    fun validateGet() {
        validateRead { map, normalMap ->
            for (entry in normalMap) {
                assertEquals(entry.value, map[entry.key])
            }
            assertEquals(normalMap[100], map[100])
        }
    }

    @Test
    fun validateIsEmpty() {
        validateRead { map, normalMap ->
            assertEquals(normalMap.isEmpty(), map.isEmpty())
        }
        validateRead(mutableStateMapOf()) { map, normalMap ->
            assertEquals(normalMap.isEmpty(), map.isEmpty())
        }
    }

    @Test
    fun verifyMutableMapEntriesAdd() {
        assertFailsWith(UnsupportedOperationException::class) {
            val map = defaultMap().toMutableMap()
            val entriesToAdd = mutableMapOf(100 to 100f)
            map.entries.add(entriesToAdd.entries.first())
        }
    }

    @Test
    fun validateEntriesAdd() {
        assertFailsWith(UnsupportedOperationException::class) {
            val map = defaultMap()
            val entriesToAdd = mutableMapOf(100 to 100f)
            map.entries.add(entriesToAdd.entries.first())
        }
    }

    @Test
    fun verifyMutableMapEntriesAddAll() {
        assertFailsWith(UnsupportedOperationException::class) {
            val map = defaultMap().toMutableMap()
            val entriesToAdd = mutableMapOf(100 to 100f, 200 to 200f)
            map.entries.addAll(entriesToAdd.entries)
        }
    }

    @Test
    fun validateEntriesAddAll() {
        assertFailsWith(UnsupportedOperationException::class) {
            val map = defaultMap()
            val entriesToAdd = mutableMapOf(100 to 100f, 200 to 200f)
            map.entries.addAll(entriesToAdd.entries)
        }
    }

    @Test
    fun validateEntriesSize() {
        validateRead { map, normalMap ->
            assertEquals(normalMap.entries.size, map.entries.size)
        }
    }

    @Test
    fun validateEntriesClear() {
        validateWrite { map ->
            map.entries.clear()
        }
    }

    @Test
    @IgnoreJsTarget
    fun validateEntriesIterator() {
        validateRead { map, normalMap ->
            for (entries in map.entries.zip(normalMap.entries)) {
                assertEquals(entries.second, entries.first)
            }
        }
    }

    @Test
    fun validateEntriesIteratorRemove() {
        validateWrite { map ->
            val iterator = map.entries.iterator()
            iterator.next()
            iterator.remove()
            iterator.next()
            iterator.next()
            iterator.remove()
        }
    }

    @Test
    fun validateEntriesContains() {
        validateRead { map, normalMap ->
            val one = map.entries.first()
            val normalOne = normalMap.entries.first()
            assertEquals(normalMap.entries.contains(normalOne), map.entries.contains(one))
            assertEquals(normalMap.entries.contains(one), map.entries.contains(normalOne))
            val independent = object : MutableMap.MutableEntry<Int, Float> {
                override val key = 1
                override var value = 1f
                override fun setValue(newValue: Float) = error("not supported")
            }
            assertEquals(normalMap.entries.contains(independent), map.entries.contains(independent))
        }
    }

    @Test
    fun validateEntriesContainsAll() {
        validateRead { map, normalMap ->
            val normalOne = normalMap.entries.first()
            val normalTwo = normalMap.entries.drop(1).first()
            val one = map.entries.first()
            val two = map.entries.drop(1).first()
            assertEquals(
                normalMap.entries.containsAll(listOf(normalOne, normalTwo)),
                map.entries.containsAll(listOf(one, two))
            )
            assertEquals(
                normalMap.entries.containsAll(listOf(one, two)),
                map.entries.containsAll(listOf(normalOne, normalTwo))
            )
            val independentOne = object : MutableMap.MutableEntry<Int, Float> {
                override val key = 1
                override var value = 1f
                override fun setValue(newValue: Float) = error("not supported")
            }
            val independentTwo = object : MutableMap.MutableEntry<Int, Float> {
                override val key = 1
                override var value = 1f
                override fun setValue(newValue: Float) = error("not supported")
            }
            assertEquals(
                normalMap.entries.containsAll(listOf(independentOne, independentTwo)),
                map.entries.containsAll(listOf(independentOne, independentTwo))
            )
        }
    }

    @Test
    fun validateEntriesRemove() {
        validateWrite { map ->
            map.entries.remove(map.entries.first())
        }
    }

    @Test
    fun validateEntriesRemoveAll() {
        validateWrite { map ->
            map.entries.removeAll(map.entries.filter { it.key % 2 == 0 })
        }
    }

    @Test
    fun validateEntriesRetainAll() {
        validateWrite { map ->
            map.entries.retainAll(map.entries.filter { it.key % 2 == 0 })
        }
    }

    @Test
    fun validateKeysSize() {
        validateRead { map, normalMap ->
            assertEquals(normalMap.keys.size, map.keys.size)
        }
    }

    @Test
    fun validateKeysClear() {
        validateWrite { map ->
            map.keys.clear()
        }
    }

    @Test
    fun validateKeysIsEmpty() {
        validateRead { map, normalMap ->
            assertEquals(normalMap.keys.isEmpty(), map.keys.isEmpty())
        }

        validateRead(mutableStateMapOf()) { map, normalMap ->
            assertEquals(normalMap.keys.isEmpty(), map.keys.isEmpty())
        }
    }

    @Test
    fun verifyMutableMapKeysAdd() {
        assertFailsWith(UnsupportedOperationException::class) {
            val map = mutableMapOf<Int, Float>()
            map.keys.add(1)
        }
    }

    @Test
    fun validateKeysAdd() {
        assertFailsWith(UnsupportedOperationException::class) {
            val map = mutableStateMapOf<Int, Float>()
            map.keys.add(1)
        }
    }

    @Test
    fun verifyMutableMapKeysAddAll() {
        assertFailsWith(UnsupportedOperationException::class) {
            val map = mutableMapOf<Int, Float>()
            map.keys.addAll(listOf(1, 2))
        }
    }

    @Test
    fun validateKeysAddAll() {
        assertFailsWith(UnsupportedOperationException::class) {
            val map = mutableStateMapOf<Int, Float>()
            map.keys.addAll(listOf(1, 2))
        }
    }

    @Test
    fun validateKeysIterator() {
        validateRead { map, normalMap ->
            map.keys.zip(normalMap.keys).forEach {
                assertEquals(it.second, it.first)
            }
        }
    }

    @Test
    fun validateKeysRemove() {
        validateWrite { map ->
            map.keys.remove(1)
            map.keys.remove(10)
        }
    }

    @Test
    fun validateKeysRemoveAll() {
        validateWrite { map ->
            map.keys.removeAll(map.keys.filter { it % 2 == 0 })
        }
    }

    @Test
    fun validateKeysRetainAll() {
        validateWrite { map ->
            map.keys.retainAll(map.keys.filter { it % 2 == 0 })
        }
    }

    @Test
    fun validateKeysContains() {
        validateRead { map, normalMap ->
            assertEquals(normalMap.keys.contains(1), map.keys.contains(1))
            assertEquals(normalMap.keys.contains(100), map.keys.contains(100))
        }
    }

    @Test
    fun validateKeysContainsAll() {
        validateRead { map, normalMap ->
            val l1 = listOf(1, 2, 3)
            val l2 = listOf(1, 2, 3, 100)
            assertEquals(
                normalMap.keys.containsAll(l1),
                map.keys.containsAll(l1)
            )
            assertEquals(
                normalMap.keys.containsAll(l2),
                map.keys.containsAll(l2)
            )
        }
    }

    @Test
    fun validateValuesSize() {
        validateRead { map, normalMap ->
            assertEquals(normalMap.values.size, map.values.size)
        }
    }

    @Test
    fun validateValuesClear() {
        validateWrite { map ->
            map.values.clear()
        }
    }

    @Test
    fun validateValuesIsEmpty() {
        validateRead { map, normalMap ->
            assertEquals(normalMap.values.isEmpty(), map.values.isEmpty())
        }

        validateRead(mutableStateMapOf()) { map, normalMap ->
            assertEquals(normalMap.values.isEmpty(), map.values.isEmpty())
        }
    }

    @Test
    fun verifyMutableMapValuesAdd() {
        assertFailsWith(UnsupportedOperationException::class) {
            val map = mutableMapOf<Int, Float>()
            map.values.add(1f)
        }
    }

    @Test
    fun validateValuesAdd() {
        assertFailsWith(UnsupportedOperationException::class) {
            val map = mutableStateMapOf<Int, Float>()
            map.values.add(1f)
        }
    }

    @Test
    fun verifyMutableMapValuesAddAll() {
        assertFailsWith(UnsupportedOperationException::class) {
            val map = mutableMapOf<Int, Float>()
            map.values.addAll(listOf(1f, 2f))
        }
    }

    @Test
    fun validateValuesAddAll() {
        assertFailsWith(UnsupportedOperationException::class) {
            val map = mutableStateMapOf<Int, Float>()
            map.values.addAll(listOf(1f, 2f))
        }
    }

    @Test
    fun validateValuesIterator() {
        validateRead { map, normalMap ->
            map.values.zip(normalMap.values).forEach {
                assertEquals(it.second, it.first)
            }
        }
    }

    @Test
    fun validateValuesRemove() {
        validateWrite { map ->
            map.values.remove(1f)
            map.values.remove(10f)
        }
    }

    @Test
    fun validateValuesRemoveAll() {
        validateWrite { map ->
            map.values.removeAll(map.values.filter { it > 2f })
        }
    }

    @Test
    fun validateValuesRetainAll() {
        validateWrite { map ->
            map.values.retainAll(map.values.filter { it > 2f })
        }
    }

    @Test
    fun validateValuesContains() {
        validateRead { map, normalMap ->
            assertEquals(normalMap.values.contains(1f), map.values.contains(1f))
            assertEquals(normalMap.values.contains(100f), map.values.contains(100f))
        }
    }

    @Test
    fun validateValuesContainsAll() {
        validateRead { map, normalMap ->
            val l1 = listOf(1f, 2f, 3f)
            val l2 = listOf(1f, 2f, 3f, 100f)
            assertEquals(
                normalMap.values.containsAll(l1),
                map.values.containsAll(l1)
            )
            assertEquals(
                normalMap.values.containsAll(l2),
                map.values.containsAll(l2)
            )
        }
    }

    @Test
    fun validateClear() {
        validateWrite { map ->
            map.clear()
        }
    }

    @Test
    fun validatePut() {
        validateWrite { map ->
            assertEquals(null, map.put(10, 10f))
            assertEquals(1f, map.put(1, 100f))
        }
    }

    @Test
    fun validatePutAll() {
        validateWrite { map ->
            map.putAll(listOf(1 to 20f, 100 to 100f))
        }
    }

    @Test
    fun validateRemove() {
        validateWrite { map ->
            assertEquals(1f, map.remove(1))
            assertEquals(null, map.remove(100))
        }
    }

    @Test
    fun validateMapsCanBeSnapshot() {
        val map = defaultMap()
        val snapshot = Snapshot.takeSnapshot()
        try {
            map[100] = 100f
            assertTrue(map.contains(100))
            snapshot.enter { assertFalse(map.contains(100)) }
        } finally {
            snapshot.dispose()
        }
    }

    @Test
    @IgnoreJsTarget
    @OptIn(ExperimentalCoroutinesApi::class)
    fun concurrentModificationInGlobal_put_new() = _runBlocking {
        repeat(100) {
            val map = mutableStateMapOf<Int, String>()
            coroutineScope {
                repeat(100) {
                    launch(Dispatchers.Default) {
                        map[it] = it.toString()
                    }
                }
            }

            repeat(100) {
                assertEquals(map[it], it.toString())
            }
        }
    }

    @Test
    @IgnoreJsTarget
    @OptIn(ExperimentalCoroutinesApi::class)
    fun concurrentModificationInGlobal_put_replace() = _runBlocking {
        repeat(100) {
            val map = mutableStateMapOf(*Array(100) { it to "default" })
            coroutineScope {
                repeat(100) {
                    launch(Dispatchers.Default) {
                        map[it] = it.toString()
                    }
                }
            }

            repeat(100) {
                assertEquals(map[it], it.toString())
            }
        }
    }

    @Test
    fun modificationAcrossSnapshots() {
        val map = mutableStateMapOf<Int, Int>()
        repeat(100) {
            Snapshot.withMutableSnapshot {
                map[it] = it
            }
        }
        repeat(100) {
            assertEquals(it, map[it])
        }
    }

    private fun validateRead(
        initialMap: MutableMap<Int, Float> = defaultMap(),
        block: (Map<Int, Float>, Map<Int, Float>) -> Unit
    ) {
        validateMaps(initialMap) { map, normalMap ->
            block(map, normalMap)
        }
    }

    private fun validateWrite(
        initialMap: MutableMap<Int, Float> = defaultMap(),
        block: (MutableMap<Int, Float>) -> Unit
    ) {
        validateMaps(initialMap) { map, normalMap ->
            block(normalMap)
            block(map)
            expected(normalMap, map)
        }
    }

    private fun validateMaps(
        map: MutableMap<Int, Float> = defaultMap(),
        block: (MutableMap<Int, Float>, MutableMap<Int, Float>) -> Unit
    ) {
        val normalMap = map.toMutableMap()
        block(map, normalMap)
    }

    private fun <K, V> expected(normalMap: Map<K, V>, map: Map<K, V>) {
        assertEquals(normalMap.size, map.size)
        for (entry in normalMap.entries) {
            assertEquals(map[entry.key], entry.value)
        }
    }

    private fun defaultMap() = mutableStateMapOf(
        1 to 1f, 2 to 2f, 3 to 3f, 4 to 4f, 5 to 1f, 6 to 2f, 7 to 3f, 8 to 4f
    )
}
