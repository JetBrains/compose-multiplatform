import com.google.r4a.frames.FrameAborted
import com.google.r4a.frames.FrameData
import com.google.r4a.frames.Record
import com.google.r4a.frames.abortHandler
import com.google.r4a.frames.commit
import com.google.r4a.frames.commitHandler
import com.google.r4a.frames.open
import com.google.r4a.frames.restore
import com.google.r4a.frames.suspend
import junit.framework.TestCase
import org.junit.Assert
import java.util.ArrayDeque
import kotlin.reflect.KClass

const val OLD_STREET = "123 Any Street"
const val OLD_CITY = "AnyTown"
const val NEW_STREET = "456 New Street"

class FrameTest: TestCase() {

    fun testCreatingAddress() {
        val address = frame {
            val address = Address(OLD_STREET, OLD_CITY)
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
        val address = frame { Address(OLD_STREET, OLD_CITY) }
        frame {
            address.street = NEW_STREET
        }
        frame {
            Assert.assertEquals(NEW_STREET, address.street)
            Assert.assertEquals(OLD_CITY, address.city)
        }
    }

    fun testIsolation() {
        val address = frame { Address(OLD_STREET, OLD_CITY) }
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
        val address = frame { Address(OLD_STREET, OLD_CITY) }
        Assert.assertEquals(1, address.first.length)
        frame { address.street = NEW_STREET }
        Assert.assertEquals(2, address.first.length)
        frame { address.street = "other street" }
        Assert.assertEquals(2, address.first.length)
    }

    fun testAborted() {
        val address = frame {Address(OLD_STREET, OLD_CITY)}
        aborted {
            address.street = NEW_STREET
            Assert.assertEquals(NEW_STREET, address.street)
        }
        frame {
            Assert.assertEquals(OLD_STREET, address.street)
        }
    }

    fun testCommitAbortInteraction() {
        val address = frame {Address(OLD_STREET, OLD_CITY)}
        val frame1 = suspended {
            address.street = "From frame1"
        }
        val frame2 = suspended {
            address.street = "From frame2"
        }

        // New frames should see the old value
        frame { Assert.assertEquals(OLD_STREET, address.street) }

        // Aborting frame2 and committing frame1 should result in frame1
        abortHandler(frame2)

        // New frames should still see the old value
        frame { Assert.assertEquals(OLD_STREET, address.street) }

        // Commit frame1, new frames should see frame1's value
        commit(frame1)
        frame { Assert.assertEquals("From frame1", address.street) }
    }

    fun testCollisionAB() {
        val address = frame { Address(OLD_STREET, OLD_CITY) }
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
        val address = frame { Address(OLD_STREET, OLD_CITY) }
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
        val addresses = frame { (0..changeCount).map { Address(OLD_STREET, OLD_CITY) } }
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

    fun testManySimultaniousFrames() {
        val frameCount = 1000
        val frames = ArrayDeque<FrameData>()
        val addresses = frame { (0..frameCount).map { Address(OLD_STREET, OLD_CITY) } }
        for (i in 0..frameCount) {
            frames.push(suspended { addresses[i].street = "From index $i" })
        }
        for (i in 0..frameCount) {
            commit(frames.remove())
        }
        for (i in 0..frameCount) {
            frame { Assert.assertEquals("From index $i", addresses[i].street) }
        }
    }

    fun testRaw() {
        val count = 1000
        val addresses = (0..count).map { AddressRaw(OLD_STREET, OLD_CITY)}
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
        val addresses = (0..count).map { AddressProp(OLD_STREET, OLD_CITY)}
        for (i in 0..count) {
            addresses[i].street = "From index $i"
            Assert.assertEquals("From index $i", addresses[i].street)
        }
        for (i in 0..count) {
            Assert.assertEquals("From index $i", addresses[i].street)
        }

    }
}

// Helpers for the above tests

inline fun <T> frame(crossinline block: ()->T): T {
    open(false, false)
    try {
        return block()
    } catch (e: Exception) {
        abortHandler()
        throw e
    } finally {
        commitHandler()
    }
}

inline fun suspended(crossinline block: ()->Unit): FrameData {
    open(false, false)
    try {
        block()
        return suspend()
    } catch(e: Exception) {
        abortHandler()
        throw e
    }
}

inline fun <T> restored(frame: FrameData, crossinline block: ()->T): T {
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
    open(false, false)
    try {
        block()
    } finally {
        abortHandler()
    }
}

inline fun <reified T: Throwable> expectThrow(@Suppress("UNUSED_PARAMETER") e: KClass<T>, crossinline block: () -> Unit) {
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


class AddressRaw(var street: String, var city: String)

class AddressProp(streetValue: String, cityValue: String) {
    var _street = streetValue
    var _city = cityValue

    var street: String
        get() = _street
        set(value: String) {_street = value }

    var city: String
        get() = _city
        set(value: String) { _city = value }
}
