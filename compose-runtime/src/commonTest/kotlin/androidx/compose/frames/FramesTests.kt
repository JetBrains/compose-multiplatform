/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.frames

import androidx.compose.Stack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

const val OLD_STREET = "123 Any Street"
const val OLD_CITY = "AnyTown"
const val NEW_STREET = "456 New Street"
const val NEW_CITY = "AnyCity"

class FrameTest {

    @Test
    fun testCreatingAddress() {
                val address = frame {
            val address = Address(
                OLD_STREET,
                OLD_CITY
            )
            assertEquals(OLD_STREET, address.street)
            assertEquals(OLD_CITY, address.city)
            address
        }
        frame {
            assertEquals(OLD_STREET, address.street)
            assertEquals(OLD_CITY, address.city)
        }
    }

    @Test
    fun testModifyingAddress() {
        val address = frame {
            Address(
                OLD_STREET,
                OLD_CITY
            )
        }
        frame {
            address.street = NEW_STREET
        }
        frame {
            assertEquals(NEW_STREET, address.street)
            assertEquals(OLD_CITY, address.city)
        }
    }

    @Test
    fun testIsolation() {
        val address = frame {
            Address(
                OLD_STREET,
                OLD_CITY
            )
        }
        val f = suspended {
            address.street = NEW_STREET
        }
        frame {
            assertEquals(OLD_STREET, address.street)
        }
        restored(f) {
            assertEquals(NEW_STREET, address.street)
        }
        frame {
            assertEquals(NEW_STREET, address.street)
        }
    }

    @Test
    fun testRecordReuse() {
        val address = frame {
            Address(
                OLD_STREET,
                OLD_CITY
            )
        }
        assertEquals(1, address.firstFrameRecord.length)
        frame { address.street = NEW_STREET }
        assertEquals(2, address.firstFrameRecord.length)
        frame { address.street = "other street" }
        assertEquals(2, address.firstFrameRecord.length)
    }

    @Test
    fun testAborted() {
        val address = frame {
            Address(
                OLD_STREET,
                OLD_CITY
            )
        }
        aborted {
            address.street = NEW_STREET
            assertEquals(NEW_STREET, address.street)
        }
        frame {
            assertEquals(OLD_STREET, address.street)
        }
    }

    @Test
    fun testReuseAborted() {
        val address = frame {
            Address(
                OLD_STREET,
                OLD_CITY
            )
        }
        assertEquals(1, address.firstFrameRecord.length)
        aborted { address.street = NEW_STREET }
        assertEquals(2, address.firstFrameRecord.length)
        frame { address.street = "other street" }
        assertEquals(2, address.firstFrameRecord.length)
    }

    @Test
    fun testCommitAbortInteraction() {
        val address = frame {
            Address(
                OLD_STREET,
                OLD_CITY
            )
        }
        val frame1 = suspended {
            address.street = "From frame1"
        }
        val frame2 = suspended {
            address.street = "From frame2"
        }

        // New frames should see the old value
        frame {
            assertEquals(
                OLD_STREET,
                address.street
            )
        }

        // Aborting frame2 and committing frame1 should result in frame1
        abortHandler(frame2)

        // New frames should still see the old value
        frame {
            assertEquals(
                OLD_STREET,
                address.street
            )
        }

        // Commit frame1, new frames should see frame1's value
        commit(frame1)
        frame { assertEquals("From frame1", address.street) }
    }

    @Test
    fun testCollisionAB() {
        val address = frame {
            Address(
                OLD_STREET,
                OLD_CITY
            )
        }
        expectThrow<FrameAborted> {
            val frame1 = suspended {
                address.street = "From frame1"
            }
            val frame2 = suspended {
                address.street = "From frame2"
            }

            commit(frame1)

            // This should throw
            commit(frame2)
        }

        // New frames should see the value from the committed frame1
        frame {
            assertEquals("From frame1", address.street)
        }
    }

    @Test
    fun testCollisionBA() {
        val address = frame {
            Address(
                OLD_STREET,
                OLD_CITY
            )
        }
        expectThrow<FrameAborted> {
            val frame1 = suspended {
                address.street = "From frame1"
            }
            val frame2 = suspended {
                address.street = "From frame2"
            }

            commit(frame2)

            // This should throw
            commit(frame1)
        }

        // New frames should see the value from the committed frame2
        frame {
            assertEquals("From frame2", address.street)
        }
    }

    @Test
    fun testManyChangesInASingleFrame() {
        val changeCount = 1000
        val addresses = frame {
            (0..changeCount).map {
                Address(
                    OLD_STREET,
                    OLD_CITY
                )
            }
        }
        val frame1 = suspended {
            for (i in 0..changeCount) {
                addresses[i].street = "From index $i"
            }
            for (i in 0..changeCount) {
                assertEquals("From index $i", addresses[i].street)
            }
        }
        frame {
            for (i in 0..changeCount) {
                assertEquals(OLD_STREET, addresses[i].street)
            }
        }
        commit(frame1)
        frame {
            for (i in 0..changeCount) {
                assertEquals("From index $i", addresses[i].street)
            }
        }
    }

    @Test
    fun testManySimultaneousFrames() {
        val frameCount = 1000
        val frames = Stack<Frame>()
        val addresses = frame {
            (0..frameCount).map {
                Address(
                    OLD_STREET,
                    OLD_CITY
                )
            }
        }
        for (i in 0..frameCount) {
            frames.push(suspended { addresses[i].street = "From index $i" })
        }
        for (i in 0..frameCount) {
            commit(frames.pop())
        }
        for (i in 0..frameCount) {
            frame {
                assertEquals(
                    "From index $i",
                    addresses[i].street
                )
            }
        }
    }

    @Test
    fun testRaw() {
        val count = 1000
        val addresses = (0..count).map { AddressRaw(OLD_STREET) }
        for (i in 0..count) {
            addresses[i].street = "From index $i"
            assertEquals("From index $i", addresses[i].street)
        }
        for (i in 0..count) {
            assertEquals("From index $i", addresses[i].street)
        }
    }

    @Test
    fun testProp() {
        val count = 10000
        val addresses = (0..count).map { AddressProp(OLD_STREET) }
        for (i in 0..count) {
            addresses[i].street = "From index $i"
            assertEquals("From index $i", addresses[i].street)
        }
        for (i in 0..count) {
            assertEquals("From index $i", addresses[i].street)
        }
    }

    @Test
    fun testFrameObserver_ObserveRead_Single() {
        val address = frame {
            Address(
                OLD_STREET,
                OLD_CITY
            )
        }
        var read: Address? = null
        observeFrame({ obj ->
            read = obj as Address
        }) {
            assertEquals(OLD_STREET, address.street)
        }
        assertEquals(address, read)
    }

    @Test
    fun testFrameObserver_addReadObserver_Single() {
        val address = frame {
            Address(
                OLD_STREET,
                OLD_CITY
            )
        }
        var read: Address? = null
        var otherRead: Address? = null
        open({ obj -> read = obj as Address })
        try {
            observeAllReads({ obj -> otherRead = obj as Address }) {
                assertEquals(OLD_STREET, address.street)
            }
        } finally {
            commitHandler()
        }
        assertEquals(address, read)
        assertEquals(address, otherRead)
    }

    @Test
    fun testFrameObserver_ObserveCommit_Single() {
        val address = frame {
            Address(
                OLD_STREET,
                OLD_CITY
            )
        }
        var committed: Set<Any>? = null
        observeCommit({ framed: Set<Any>, _ -> committed = framed }) {
            frame {
                address.street = NEW_STREET
            }
        }
        assertTrue(committed?.contains(address) ?: false)
    }

    @Test
    fun testFrameObserver_OberveRead_Multiple() {
        val addressToRead = frame {
            List(100) {
                Address(
                    OLD_STREET,
                    OLD_CITY
                )
            }
        }
        val addressToIgnore = frame {
            List(100) {
                Address(
                    OLD_STREET,
                    OLD_CITY
                )
            }
        }
        val readAddresses = HashSet<Address>()
        observeFrame({ obj -> readAddresses.add(obj as Address) }) {
            for (address in addressToRead) {
                assertEquals(OLD_STREET, address.street)
            }
        }
        for (address in addressToRead) {
            assertTrue(
                readAddresses.contains(address),
                "Ensure a read callback was called for the address"
                )
        }
        for (address in addressToIgnore) {
            assertFalse(
                readAddresses.contains(address),
                "Ensure a read callback was not called for the address"
                )
        }
    }

    @Test
    fun testFrameObserver_ObserveCommit_Multiple() {
        val addressToWrite = frame {
            List(100) {
                Address(
                    OLD_STREET,
                    OLD_CITY
                )
            }
        }
        val addressToIgnore = frame {
            List(100) {
                Address(
                    OLD_STREET,
                    OLD_CITY
                )
            }
        }
        var committedAddresses = null as Set<Any>?
        observeCommit({ framed, _ -> committedAddresses = framed }) {
            frame {
                for (address in addressToWrite) {
                    address.street = NEW_STREET
                }
            }
        }
        for (address in addressToWrite) {
            assertTrue(
                committedAddresses?.contains(address) ?: false,
                "Ensure written address is in the set of committed objects"
                )
        }
        for (address in addressToIgnore) {
            assertFalse(
                committedAddresses?.contains(address) ?: false,
                "Ensure ignored addresses are not in the set of committed objects"
                )
        }
    }

    @Test
    fun testModelList_Isolated() {
        val addresses = frame {
            modelListOf(*(Array(100) {
                Address(
                    OLD_STREET,
                    OLD_CITY
                )
            }))
        }

        fun validateOriginal() {
            assertFalse(wasModified(addresses))
            assertEquals(100, addresses.size)

            // Iterate list
            for (address in addresses) {
                assertEquals(OLD_STREET, address.street)
            }
            assertFalse(wasModified(addresses))
        }

        fun validateNew() {
            assertEquals(101, addresses.size)

            // Iterate list
            for (i in 0 until 100) {
                assertEquals(OLD_STREET, addresses[i].street)
            }

            assertEquals(NEW_STREET, addresses[100].street)
        }

        frame { validateOriginal() }

        val frame1 = suspended {
            // Insert into the list
            addresses.add(
                Address(
                    NEW_STREET,
                    NEW_CITY
                )
            )

            validateNew()
        }

        frame { validateOriginal() }

        restored(frame1) {
            validateNew()
        }
    }

    @Test
    fun testModelList_ReadDoesNotModify() {
        val count = 10
        val addresses = frame {
            modelListOf(*(Array(count) {
                Address(
                    OLD_STREET,
                    OLD_CITY
                )
            }))
        }

        // size should not modify
        frame {
            assertEquals(count, addresses.size)
            assertFalse(wasModified(addresses))
        }

        // get should not modify
        frame {
            val address = addresses[0]
            assertEquals(OLD_STREET, address.street)
            assertFalse(wasModified(addresses))
            assertFalse(wasModified(address))
        }

        // Iteration should not modify
        frame {
            for (address in addresses) {
                assertEquals(OLD_STREET, address.street)
                assertFalse(wasModified(address))
            }
            assertFalse(wasModified(addresses))
        }

        // contains should not modify
        frame {
            val address = addresses[1]
            assertTrue(addresses.contains(address))
            assertFalse(wasModified(addresses))
        }

        // containsAll should not modify
        frame {
            val sublist = listOf(addresses[1], addresses[2])
            assertTrue(addresses.containsAll(sublist))
            assertFalse(wasModified(addresses))
        }

        // indexOf of should not modify
        frame {
            val address = addresses[5]
            assertEquals(5, addresses.indexOf(address))
            assertFalse(wasModified(addresses))
        }

        // IsEmpty should not modify
        frame {
            assertFalse(addresses.isEmpty())
            assertTrue(addresses.isNotEmpty())
            assertFalse(wasModified(addresses))
        }

        // lastIndexOf should not modify
        frame {
            val address = addresses[5]
            assertEquals(5, addresses.lastIndexOf(address))
            assertFalse(wasModified(addresses))
        }

        // listIterator should not modify
        frame {
            for (address in addresses.listIterator()) {
                assertEquals(OLD_STREET, address.street)
            }
            assertFalse(wasModified(addresses))
            for (address in addresses.listIterator(5)) {
                assertEquals(OLD_STREET, address.street)
            }
            assertFalse(wasModified(addresses))
        }
    }

    @Test
    fun testModelList_IterableList_Mutate() {
        val count = 10
        fun streetOf(index: Int) = "$OLD_STREET, Apt #$index"
        fun initial(index: Int) = Address(
            street = streetOf(index),
            city = OLD_CITY
        )
        fun initializer() = Array(count) { initial(it) }
        val addresses = frame {
            modelListOf(*initializer())
        }

        fun mutate(
            index: Int = 0,
            skip: Int = 1,
            block: (iterator: MutableListIterator<Address>) -> Unit
        ) {
            aborted {
                // Perform the action on a model list
                val iterator = addresses.listIterator(index)

                repeat(skip) {
                    assertTrue(iterator.hasNext())
                    iterator.next()
                }

                assertFalse(wasModified(addresses))
                block(iterator)
                assertTrue(wasModified(addresses))

                // Perform the action on an array list
                val normalList = ArrayList<Address>().apply {
                    repeat(count) {
                        this.add(initial(it))
                    }
                }
                val normalIterator = normalList.listIterator(index)
                repeat(skip) {
                    assertTrue(normalIterator.hasNext())
                    normalIterator.next()
                }
                block(normalIterator)

                // Asset the lists are the same
                assertEquals(normalList.size, addresses.size)
                addresses.forEachIndexed { index, address ->
                    assertEquals(address.street, normalList[index].street)
                    assertEquals(address.city, normalList[index].city)
                }
            }

            frame {
                assertEquals(addresses.size, count)
                addresses.forEachIndexed { index, address ->
                    assertEquals(streetOf(index), address.street)
                    assertEquals(OLD_CITY, address.city)
                }
            }
        }

        fun validate(block: (iterator: MutableListIterator<Address>) -> Unit) {
            mutate(block = block)
            mutate(skip = 3, block = block)
            mutate(index = 4, block = block)
            mutate(index = 2, skip = 3, block = block)
        }

        validate {
            it.remove()
        }

        // Expect listIterator.set to throw
        validate {
            it.set(
                Address(
                    NEW_STREET,
                    NEW_CITY
                )
            )
        }

        // Expect listIterator.add to throw
        validate {
            it.add(
                Address(
                    NEW_STREET,
                    NEW_CITY
                )
            )
        }

        mutate(index = 5) {
            it.previous()
            it.previous()
            it.previous()
            it.next()
            it.add(Address(NEW_STREET, NEW_CITY))
        }
    }

    @Test
    fun testModelList_MutateAcrossThreadThrows() {
        val count = 10
        val addresses = frame {
            modelListOf(*(Array(count) {
                Address(
                    street = "$OLD_STREET Apt#$it",
                    city = OLD_CITY
                )
            }))
        }

        val iterator = frame {
            addresses.listIterator().apply { next() }
        }

        frame {
            expectError {
                iterator.remove()
            }
        }
    }

    @Test
    fun testModelList_MutatingModifies() {
        val count = 10
        val addresses = frame {
            modelListOf(*(Array(count) {
                Address(
                    OLD_STREET,
                    OLD_CITY
                )
            }))
        }

        fun validate(block: () -> Unit) {
            aborted {
                assertFalse(wasModified(addresses))
                block()
                assertTrue(wasModified(addresses))
            }
            frame {
                assertEquals(addresses.size, count)
                for (address in addresses) {
                    assertEquals(address.street, OLD_STREET)
                    assertEquals(address.city, OLD_CITY)
                }
            }
        }

        // Expect add to modify
        validate { addresses.add(
            Address(
                NEW_STREET,
                OLD_CITY
            )
        ) }
        validate { addresses.add(5,
            Address(
                NEW_STREET,
                OLD_CITY
            )
        ) }

        // Expect addAll to modify
        validate {
            addresses.addAll(listOf(
                Address(
                    NEW_STREET,
                    NEW_CITY
                ),
                Address(
                    NEW_STREET,
                    NEW_CITY
                )
            ))
        }
        validate {
            addresses.addAll(
                5,
                listOf(
                    Address(
                        NEW_STREET,
                        NEW_CITY
                    ),
                    Address(
                        NEW_STREET,
                        NEW_CITY
                    )
                )
            )
        }

        // Expect clear to modify
        validate { addresses.clear() }

        // Expect remove to modify
        validate {
            val address = addresses[5]
            addresses.remove(address)
        }

        // Expect removeAll to modify
        validate { addresses.removeAll(listOf(addresses[5], addresses[6])) }

        // Expect removeAt to modify
        validate { addresses.removeAt(5) }

        // Expect retainAll to modify
        validate { addresses.retainAll(listOf(addresses[5], addresses[6])) }

        // Expect set to modify
        validate { addresses[5] = Address(
            NEW_STREET,
            NEW_CITY
        )
        }

        // Expect subList to modify
        validate { addresses.subList(5, 6) }
    }

    @Test
    fun testModelList_SingletonList() {
        val numbers = frame {
            modelListOf(1)
        }

        frame {
            // Modify one of the members
            numbers[0] = numbers[0] * 10
        }

        frame {
            assertEquals(numbers[0], 10)
        }
    }

    @Test
    fun testModelList_MutableIterator() {
        val numbers = frame {
            modelListOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        }

        frame {
            val iterator = numbers.iterator()
            while (iterator.hasNext()) {
                val current = iterator.next()
                if (current % 2 == 0) {
                    iterator.remove()
                }
            }
        }

        frame {
            for (i in numbers) {
                assertTrue(i and 1 == 1)
            }
        }
    }

    @Test
    fun testModelMap_Isolated() {
        val map = frame {
            modelMapOf(
                1 to "a",
                2 to "b",
                3 to "c",
                4 to "d"
            )
        }

        fun validateOld() {
            assertEquals(4, map.size)
            assertTrue(map.contains(1))
            assertTrue(map.contains(2))
            assertTrue(map.contains(3))
            assertTrue(map.contains(4))
            assertEquals(map[1], "a")
            assertEquals(map[2], "b")
            assertEquals(map[3], "c")
            assertEquals(map[4], "d")
        }

        fun validateNew() {
            assertEquals(5, map.size)
            assertTrue(map.contains(1))
            assertTrue(map.contains(2))
            assertTrue(map.contains(3))
            assertTrue(map.contains(4))
            assertTrue(map.contains(5))
            assertEquals(map[1], "a")
            assertEquals(map[2], "b")
            assertEquals(map[3], "c")
            assertEquals(map[4], "d")
            assertEquals(map[5], "e")
        }

        frame { validateOld() }

        val frame1 = suspended {
            validateOld()

            map[5] = "e"

            validateNew()
        }

        frame { validateOld() }
        restored(frame1) { validateNew() }
        frame { validateNew() }
    }

    @Test
    @Suppress("USELESS_IS_CHECK")
    fun testModelMap_ReadingDoesntModify() {
        val map = frame {
            modelMapOf(
                1 to "a",
                2 to "b",
                3 to "c",
                4 to "d"
            )
        }

        fun validateOld() {
            assertEquals(4, map.size)
            assertTrue(map.contains(1))
            assertTrue(map.contains(2))
            assertTrue(map.contains(3))
            assertTrue(map.contains(4))
            assertEquals(map[1], "a")
            assertEquals(map[2], "b")
            assertEquals(map[3], "c")
            assertEquals(map[4], "d")
        }

        fun validate(block: () -> Unit) {
            frame {
                validateOld()
                block()
                assertFalse(wasModified(map))
                validateOld()
            }
        }

        // size should not modify
        validate { assertEquals(4, map.size) }

        // contains should not modify
        validate { assertTrue(map.contains(1)) }

        // containsKey should not modify
        validate { assertTrue(map.containsKey(1)) }

        // containsValue should not modify
        validate { assertTrue(map.containsValue("a")) }

        // get should not modify
        validate { assertEquals("a", map[1]) }

        // isEmpty should not modify
        validate { assertFalse(map.isEmpty()) }
        validate { assertTrue(map.isNotEmpty()) }

        // iterating entries should not modify
        validate {
            for (entry in map) {
                assertTrue(entry.value is String)
                assertTrue(entry.key is Int)
            }
            for (entry in map.entries) {
                assertTrue(entry.value is String)
                assertTrue(entry.key is Int)
            }
        }

        // iterating keys should not modify
        validate {
            for (key in map.keys) {
                assertTrue(key is Int)
            }
        }

        // iterating values should not modify
        validate {
            for (value in map.values) {
                assertTrue(value is String)
            }
        }
    }

    @Test
    fun testModelMap_Mutation() {
        val map = frame {
            modelMapOf(
                1 to "a",
                2 to "b",
                3 to "c",
                4 to "d"
            )
        }

        fun validateOld() {
            assertEquals(4, map.size)
            assertTrue(map.contains(1))
            assertTrue(map.contains(2))
            assertTrue(map.contains(3))
            assertTrue(map.contains(4))
            assertEquals(map[1], "a")
            assertEquals(map[2], "b")
            assertEquals(map[3], "c")
            assertEquals(map[4], "d")
        }

        fun validate(block: () -> Unit) {
            aborted {
                assertFalse(wasModified(map))
                validateOld()
                block()
                assertTrue(wasModified(map))
            }
        }

        // clear should modify
        validate { map.clear() }

        // put should modify
        validate { map[5] = "e" }

        // putAll should modify
        validate { map.putAll(mapOf(5 to "e", 6 to "f")) }

        // remove should modify
        validate { map.remove(3) }
    }

    @Test
    fun testModelMap_MutateThrows() {
        val map = frame {
            modelMapOf(
                1 to "a",
                2 to "b",
                3 to "c",
                4 to "d"
            )
        }

        fun validateOld() {
            assertEquals(4, map.size)
            assertTrue(map.contains(1))
            assertTrue(map.contains(2))
            assertTrue(map.contains(3))
            assertTrue(map.contains(4))
            assertEquals(map[1], "a")
            assertEquals(map[2], "b")
            assertEquals(map[3], "c")
            assertEquals(map[4], "d")
        }

        fun validate(block: () -> Unit) {
            frame {
                assertFalse(wasModified(map))
                validateOld()
                expectError {
                    block()
                }
                assertFalse(wasModified(map))
            }
        }

        // Expect mutating through entries to throw
        validate { map.entries.add(map.entries.first()) }
        validate {
            map.entries.addAll(listOf(map.entries.first(), map.entries.drop(1).first()))
        }
        validate { map.entries.clear() }
        validate { map.entries.remove(map.entries.first()) }
        validate {
            map.entries.removeAll(listOf(map.entries.first(), map.entries.drop(1).first()))
        }
        validate {
            map.entries.retainAll(listOf(map.entries.first(), map.entries.drop(1).first()))
        }
        validate {
            val iterator = map.entries.iterator()
            iterator.next()
            iterator.remove()
        }

        // Expect mutating through keys to throw
        validate { map.keys.add(map.keys.first()) }
        validate { map.keys.addAll(listOf(map.keys.first(), map.keys.drop(1).first())) }
        validate { map.keys.clear() }
        validate { map.keys.remove(map.keys.first()) }
        validate { map.keys.removeAll(listOf(map.keys.first(), map.keys.drop(1).first())) }
        validate { map.keys.retainAll(listOf(map.keys.first(), map.keys.drop(1).first())) }
        validate {
            val iterator = map.keys.iterator()
            iterator.next()
            iterator.remove()
        }

        // Expect mutating through values to throw
        validate { map.values.add(map.values.first()) }
        validate { map.values.addAll(listOf(map.values.first(), map.values.drop(1).first())) }
        validate { map.values.clear() }
        validate { map.values.remove(map.values.first()) }
        validate {
            map.values.removeAll(listOf(map.values.first(), map.values.drop(1).first()))
        }
        validate {
            map.values.retainAll(listOf(map.values.first(), map.values.drop(1).first()))
        }
        validate {
            val iterator = map.values.iterator()
            iterator.next()
            iterator.remove()
        }
    }

    @Test
    fun testGlobalReadObserverSurvivesFrameSwitch() {
        val address1 = frame {
            Address(
                OLD_STREET,
                OLD_CITY
            )
        }
        val address2 = frame {
            Address(
                NEW_STREET,
                NEW_CITY
            )
        }
        val address3 = frame {
            Address(
                OLD_STREET,
                NEW_CITY
            )
        }
        val readAddresses = HashSet<Address>()

        observeAllReads({ readAddresses.add(it as Address) }) {
            frame {
                // read 1
                address1.city
            }
            frame {
                // read 2
                address2.city
            }
        }
        frame {
            // read 3 outside of observeReads
            address3.city
        }

        assertTrue(readAddresses.contains(address1))
        assertTrue(readAddresses.contains(address2))
        assertFalse(readAddresses.contains(address3))
    }
}

fun expectError(block: () -> Unit) {
    var thrown = false
    try {
        block()
    } catch (e: IllegalStateException) {
        thrown = true
    }
    assertTrue(thrown)
}

// Helpers for the above tests

inline fun <T> frame(crossinline block: () -> T): T {
    open(false)
    try {
        return block()
    } catch (e: Exception) {
        abortHandler()
        throw e
    } finally {
        commitHandler()
    }
}

inline fun <T> observeFrame(noinline observer: FrameReadObserver, crossinline block: () -> T): T {
    open(observer)
    try {
        return block()
    } catch (e: Exception) {
        abortHandler()
        throw e
    } finally {
        commitHandler()
    }
}

inline fun <T> observeCommit(
    noinline observer: FrameCommitObserver,
    crossinline block: () -> T
): T {
    val unregister = registerCommitObserver(observer)
    try {
        return block()
    } finally {
        unregister()
    }
}

inline fun suspended(crossinline block: () -> Unit): Frame {
    open(false)
    try {
        block()
        return suspend()
    } catch (e: Exception) {
        abortHandler()
        throw e
    }
}

inline fun <T> restored(frame: Frame, crossinline block: () -> T): T {
    restore(frame)
    try {
        return block()
    } catch (e: Exception) {
        abortHandler()
        throw e
    } finally {
        commitHandler()
    }
}

inline fun aborted(crossinline block: () -> Unit) {
    open(false)
    try {
        block()
    } finally {
        abortHandler()
    }
}

inline fun <reified T : Throwable> expectThrow(
    crossinline block: () -> Unit
) {
    var thrown = false
    try {
        block()
    } catch (e: Throwable) {
        assertTrue(e is T)
        thrown = true
    }
    assertTrue(thrown)
}

val Record.length: Int
    get() {
        var current: Record? = this
        var len = 0
        while (current != null) {
            len++
            current = current.next
        }
        return len
    }

class AddressRaw(var street: String)

class AddressProp(streetValue: String) {
    var _street = streetValue

    var street: String
        get() = _street
        set(value) { _street = value }
}

// Provide an implemtation of java.util.Collection.removeIf for common target
fun <T> MutableIterable<T>.removeIf(predicate: (T) -> Boolean) {
    val iterator = this.iterator()
    while (iterator.hasNext()) {
        val value = iterator.next()
        if (predicate(value)) iterator.remove()
    }
}