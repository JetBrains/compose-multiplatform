package org.jetbrains.compose.resources

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.concurrent.thread
import kotlin.test.*

class RunResourceBlockingTest {

    @AfterTest
    fun tearDown() {
        //don't leak the interruption flag into other tests
        Thread.interrupted()
    }

    @Test
    fun testNullResultOfBlockWithoutSuspension() {
        assertEquals(null, runResourceBlocking<String?> { null })
    }

    @Test
    fun testInterruptedThreadCanReadWithoutSuspension() {
        Thread.currentThread().interrupt()

        assertEquals("a value", runResourceBlocking { "a value" })

        assertTrue(Thread.interrupted(), "The interruption flag must be kept for its real addressee")
    }

    @Test
    fun testInterruptionDuringWaitingIsNotLost() {
        val callerThread = Thread.currentThread()
        val handoff = ValueHandoff("a value")
        //the thread is interrupted when it is already parked inside `runResourceBlocking`
        handoff.completeWhenCallerIsParked(callerThread, beforeComplete = { callerThread.interrupt() })

        assertEquals("a value", runResourceBlocking { handoff.await() })

        assertTrue(Thread.interrupted(), "The interruption flag must be kept for its real addressee")
    }

    @Test
    fun testWithContextInsideTheBlock() {
        assertEquals("a value", runResourceBlocking { withContext(Dispatchers.Default) { "a value" } })
    }

    @Test
    fun testDelayInsideTheBlock() {
        assertEquals("a value", runResourceBlocking { delay(100); "a value" })
    }

    @Test
    fun testExceptionWithoutSuspensionIsRethrown() {
        val error = assertFailsWith<MissingResourceException> {
            runResourceBlocking { throw MissingResourceException("1.png") }
        }
        assertTrue(error.message.orEmpty().contains("1.png"))
    }

    @Test
    fun testExceptionAfterSuspensionIsRethrown() {
        val handoff = ValueHandoff("a value")
        handoff.completeWhenCallerIsParked(Thread.currentThread())

        val error = assertFailsWith<MissingResourceException> {
            runResourceBlocking {
                handoff.await()
                throw MissingResourceException("1.png")
            }
        }
        assertTrue(error.message.orEmpty().contains("1.png"))
    }
}

/**
 * Provides a [value] from another thread strictly after the calling thread is parked,
 * so `runResourceBlocking` is guaranteed to go through its waiting path.
 */
private class ValueHandoff<T>(private val value: T) {
    private val deferred = CompletableDeferred<T>()

    fun completeWhenCallerIsParked(caller: Thread, beforeComplete: () -> Unit = {}) {
        thread(name = "resource-value-handoff", isDaemon = true) {
            val deadline = System.nanoTime() + 10L * 1_000_000_000
            while (caller.state !in PARKED_STATES && System.nanoTime() < deadline) {
                Thread.yield()
            }
            beforeComplete()
            deferred.complete(value)
        }
    }

    suspend fun await(): T = deferred.await()

    private companion object {
        val PARKED_STATES = setOf(Thread.State.WAITING, Thread.State.TIMED_WAITING)
    }
}
