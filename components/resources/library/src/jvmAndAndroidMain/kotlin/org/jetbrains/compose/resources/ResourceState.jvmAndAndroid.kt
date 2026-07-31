package org.jetbrains.compose.resources

import java.util.concurrent.locks.LockSupport
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.startCoroutineUninterceptedOrReturn

/**
 * Starts the [block] on the calling thread. A block which doesn't suspend is completely executed
 * here and its result is returned right away - without any waiting and, unlike `runBlocking`,
 * without a single check of the interruption flag.
 *
 * If the [block] does suspend, the calling thread is parked by a [ResourceAwaiter]
 * until another thread completes the block.
 */
internal actual fun <T> runResourceBlocking(block: suspend () -> T): T {
    val awaiter = ResourceAwaiter<T>(Thread.currentThread())
    val result = block.startCoroutineUninterceptedOrReturn(awaiter)
    if (result !== COROUTINE_SUSPENDED) {
        @Suppress("UNCHECKED_CAST")
        return result as T
    } else {
        return awaiter.await()
    }
}

/**
 * A completion of a resource loading coroutine which parks the [thread] until a result arrives.
 *
 * It waits in [LockSupport.park] because, unlike `Object.wait` and the `runBlocking` loop,
 * it never throws an `InterruptedException`. But `park` returns immediately while the interruption
 * flag is set, so the flag is taken away for the time of waiting - otherwise the waiting turns
 * into a busy spin - and it is given back to its real addressee at the end.
 */
private class ResourceAwaiter<T>(
    private val thread: Thread
) : Continuation<T> {
    override val context: CoroutineContext get() = EmptyCoroutineContext

    @Volatile
    private var result: Result<T>? = null

    override fun resumeWith(result: Result<T>) {
        this.result = result
        LockSupport.unpark(thread)
    }

    fun await(): T {
        var interrupted = Thread.interrupted()
        try {
            while (true) {
                result?.let { return it.getOrThrow() }
                LockSupport.park(this)
                if (Thread.interrupted()) interrupted = true
            }
        } finally {
            if (interrupted) thread.interrupt()
        }
    }
}