package androidx.compose.frames

import junit.framework.TestCase
import org.junit.Assert
import java.util.ArrayDeque
import kotlin.reflect.KClass

const val OLD_STREET = "123 Any Street"
const val OLD_CITY = "AnyTown"
const val NEW_STREET = "456 New Street"
const val NEW_CITY = "AnyCity"

class FrameTest : TestCase() {

    fun testCreatingAddress() {
        val address = frame {
            val address = Address(
                OLD_STREET,
                OLD_CITY
            )
            Assert.assertEquals(OLD_STREET, address.street)
            Assert.assertEquals(OLD_CITY, address.city)
            address
        }
        frame {
            Assert.assertEquals(OLD_STREET, address.street)
            Assert.assertEquals(OLD_CITY, address.city)
        }
    }

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
            Assert.assertEquals(NEW_STREET, address.street)
            Assert.assertEquals(OLD_CITY, address.city)
        }
    }

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
            Assert.assertEquals(OLD_STREET, address.street)
        }
        restored(f) {
            Assert.assertEquals(NEW_STREET, address.street)
        }
        frame {
            Assert.assertEquals(NEW_STREET, address.street)
        }
    }

    fun testRecordReuse() {
        val address = frame {
            Address(
                OLD_STREET,
                OLD_CITY
            )
        }
        Assert.assertEquals(1, address.firstFrameRecord.length)
        frame { address.street = NEW_STREET }
        Assert.assertEquals(2, address.firstFrameRecord.length)
        frame { address.street = "other street" }
        Assert.assertEquals(2, address.firstFrameRecord.length)
    }

    fun testAborted() {
        val address = frame {
            Address(
                OLD_STREET,
                OLD_CITY
            )
        }
        aborted {
            address.street = NEW_STREET
            Assert.assertEquals(NEW_STREET, address.street)
        }
        frame {
            Assert.assertEquals(OLD_STREET, address.street)
        }
    }

    fun testReuseAborted() {
        val address = frame {
            Address(
                OLD_STREET,
                OLD_CITY
            )
        }
        Assert.assertEquals(1, address.firstFrameRecord.length)
        aborted { address.street = NEW_STREET }
        Assert.assertEquals(2, address.firstFrameRecord.length)
        frame { address.street = "other street" }
        Assert.assertEquals(2, address.firstFrameRecord.length)
    }

    fun testSpeculation() {
        val address = frame {
            Address(
                OLD_STREET,
                OLD_CITY
            )
        }
        speculation {
            address.street = NEW_STREET
            Assert.assertEquals(NEW_STREET, address.street)
        }
        frame {
            Assert.assertEquals(OLD_STREET, address.street)
        }
    }

    fun testSpeculationIsolation() {
        val address = frame {
            Address(
                OLD_STREET,
                OLD_CITY
            )
        }
        speculate()
        address.street = NEW_STREET
        val speculation = androidx.compose.frames.suspend()
        frame {
            Assert.assertEquals(OLD_STREET, address.street)
        }
        restore(speculation)
        Assert.assertEquals(NEW_STREET, address.street)
        abortHandler()
        frame {
            Assert.assertEquals(OLD_STREET, address.street)
        }
    }

    fun testReuseSpeculation() {
        val address = frame {
            Address(
                OLD_STREET,
                OLD_CITY
            )
        }
        Assert.assertEquals(1, address.firstFrameRecord.length)
        speculation { address.street = NEW_STREET }
        Assert.assertEquals(2, address.firstFrameRecord.length)
        frame { address.street = "other street" }
        Assert.assertEquals(2, address.firstFrameRecord.length)
    }

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
            Assert.assertEquals(
                OLD_STREET,
                address.street
            )
        }

        // Aborting frame2 and committing frame1 should result in frame1
        abortHandler(frame2)

        // New frames should still see the old value
        frame {
            Assert.assertEquals(
                OLD_STREET,
                address.street
            )
        }

        // Commit frame1, new frames should see frame1's value
        commit(frame1)
        frame { Assert.assertEquals("From frame1", address.street) }
    }

    fun testCollisionAB() {
        val address = frame {
            Address(
                OLD_STREET,
                OLD_CITY
            )
        }
        expectThrow(FrameAborted::class) {
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
            Assert.assertEquals("From frame1", address.street)
        }
    }

    fun testCollisionBA() {
        val address = frame {
            Address(
                OLD_STREET,
                OLD_CITY
            )
        }
        expectThrow(FrameAborted::class) {
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
            Assert.assertEquals("From frame2", address.street)
        }
    }

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
                Assert.assertEquals("From index $i", addresses[i].street)
            }
        }
        frame {
            for (i in 0..changeCount) {
                Assert.assertEquals(OLD_STREET, addresses[i].street)
            }
        }
        commit(frame1)
        frame {
            for (i in 0..changeCount) {
                Assert.assertEquals("From index $i", addresses[i].street)
            }
        }
    }

    fun testManySimultaneousFrames() {
        val frameCount = 1000
        val frames = ArrayDeque<Frame>()
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
            commit(frames.remove())
        }
        for (i in 0..frameCount) {
            frame {
                Assert.assertEquals(
                    "From index $i",
                    addresses[i].street
                )
            }
        }
    }

    fun testRaw() {
        val count = 1000
        val addresses = (0..count).map { AddressRaw(OLD_STREET) }
        for (i in 0..count) {
            addresses[i].street = "From index $i"
            Assert.assertEquals("From index $i", addresses[i].street)
        }
        for (i in 0..count) {
            Assert.assertEquals("From index $i", addresses[i].street)
        }
    }

    fun testProp() {
        val count = 10000
        val addresses = (0..count).map { AddressProp(OLD_STREET) }
        for (i in 0..count) {
            addresses[i].street = "From index $i"
            Assert.assertEquals("From index $i", addresses[i].street)
        }
        for (i in 0..count) {
            Assert.assertEquals("From index $i", addresses[i].street)
        }
    }

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
            Assert.assertEquals(OLD_STREET, address.street)
        }
        Assert.assertEquals(address, read)
    }

    fun testFrameObserver_addReadObserver_Single() {
        val address = frame {
            Address(
                OLD_STREET,
                OLD_CITY
            )
        }
        var read: Address? = null
        var otherRead: Address? = null
        val frame = open({ obj -> read = obj as Address })
        try {
            frame.observeReads({ obj -> otherRead = obj as Address }) {
                Assert.assertEquals(OLD_STREET, address.street)
            }
            Assert.assertEquals(1, frame.readObservers.size)
        } finally {
            commitHandler()
        }
        Assert.assertEquals(address, read)
        Assert.assertEquals(address, otherRead)
    }

    fun testFrameObserver_ObserveCommit_Single() {
        val address = frame {
            Address(
                OLD_STREET,
                OLD_CITY
            )
        }
        var committed: Set<Any>? = null
        observeCommit({ framed: Set<Any> -> committed = framed }) {
            frame {
                address.street = NEW_STREET
            }
        }
        Assert.assertTrue(committed?.contains(address) ?: false)
    }

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
                Assert.assertEquals(OLD_STREET, address.street)
            }
        }
        for (address in addressToRead) {
            Assert.assertTrue(
                "Ensure a read callback was called for the address",
                readAddresses.contains(address)
            )
        }
        for (address in addressToIgnore) {
            Assert.assertFalse(
                "Ensure a read callback was not called for the address",
                readAddresses.contains(address)
            )
        }
    }

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
        observeCommit({ framed -> committedAddresses = framed }) {
            frame {
                for (address in addressToWrite) {
                    address.street = NEW_STREET
                }
            }
        }
        for (address in addressToWrite) {
            Assert.assertTrue(
                "Ensure written address is in the set of committed objects",
                committedAddresses?.contains(address) ?: false
            )
        }
        for (address in addressToIgnore) {
            Assert.assertFalse(
                "Ensure ignored addresses are not in the set of committed objects",
                committedAddresses?.contains(address) ?: false
            )
        }
    }

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
                Assert.assertEquals(OLD_STREET, address.street)
            }
            assertFalse(wasModified(addresses))
        }

        fun validateNew() {
            assertEquals(101, addresses.size)

            // Iterate list
            for (i in 0 until 100) {
                Assert.assertEquals(OLD_STREET, addresses[i].street)
            }

            Assert.assertEquals(NEW_STREET, addresses[100].street)
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

    fun testModelList_MutateThrows() {
        val count = 10
        val addresses = frame {
            modelListOf(*(Array(count) {
                Address(
                    OLD_STREET,
                    OLD_CITY
                )
            }))
        }

        // Expect iterator.remove to throw
        frame {
            val iterator = addresses.iterator()
            assertTrue(iterator.hasNext())
            iterator.next()
            expectError {
                iterator.remove()
            }
            assertFalse(wasModified(addresses))
        }

        // Expect listIterator.remove to throw
        frame {
            val iterator = addresses.listIterator()
            assertTrue(iterator.hasNext())
            iterator.next()
            expectError {
                iterator.remove()
            }
            assertFalse(wasModified(addresses))
        }

        // Expect listIterator.set to throw
        frame {
            val iterator = addresses.listIterator()
            assertTrue(iterator.hasNext())
            iterator.next()
            expectError {
                iterator.set(
                    Address(
                        NEW_STREET,
                        NEW_CITY
                    )
                )
            }
            assertFalse(wasModified(addresses))
        }

        // Expect listIterator.add to throw
        frame {
            val iterator = addresses.listIterator()
            assertTrue(iterator.hasNext())
            iterator.next()
            expectError {
                iterator.add(
                    Address(
                        NEW_STREET,
                        NEW_CITY
                    )
                )
            }
            assertFalse(wasModified(addresses))
        }
    }

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

        // Expecte asMutable to modify
        validate { addresses.asMutable() }
    }

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
}

fun expectError(block: () -> Unit) {
    var thrown = false
    try {
        block()
    } catch (e: IllegalStateException) {
        thrown = true
    }
    Assert.assertTrue(thrown)
}

// Helpers for the above tests

inline fun <T> frame(crossinline block: ()->T): T {
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

inline fun suspended(crossinline block: ()->Unit): Frame {
    open(false)
    try {
        block()
        return androidx.compose.frames.suspend()
    } catch (e: Exception) {
        abortHandler()
        throw e
    }
}

inline fun <T> restored(frame: Frame, crossinline block: ()->T): T {
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

inline fun aborted(crossinline block: ()->Unit) {
    open(false)
    try {
        block()
    } finally {
        abortHandler()
    }
}

inline fun speculation(crossinline block: ()->Unit) {
    speculate()
    try {
        block()
    } finally {
        abortHandler()
    }
}

inline fun <reified T : Throwable> expectThrow(
    @Suppress("UNUSED_PARAMETER") e: KClass<T>,
    crossinline block: () -> Unit
) {
    var thrown = false
    try {
        block()
    } catch (e: Throwable) {
        Assert.assertTrue(e is T)
        thrown = true
    }
    Assert.assertTrue(thrown)
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
        set(value) {_street = value }
}
