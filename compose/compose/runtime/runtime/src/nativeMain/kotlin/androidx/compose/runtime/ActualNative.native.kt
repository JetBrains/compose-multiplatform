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

package androidx.compose.runtime

import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotContextElement
import androidx.compose.runtime.snapshots.SnapshotMutableState
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.yield
import kotlin.native.identityHashCode
import kotlin.system.getTimeNanos
import kotlin.time.ExperimentalTime
import kotlin.native.concurrent.isFrozen
import kotlin.native.concurrent.freeze
import kotlin.native.concurrent.ensureNeverFrozen

private val threadCounter = kotlin.native.concurrent.AtomicLong(0)

@kotlin.native.concurrent.ThreadLocal
private var threadId: Long = threadCounter.addAndGet(1)

internal actual fun getCurrentThreadId(): Long = threadId

/**
 * AtomicReference implementation suitable for both single and multi-threaded context.
 */
actual class AtomicReference<V> actual constructor(value: V) {
    private val delegate = kotlin.native.concurrent.AtomicReference(value)

    actual fun get(): V = delegate.value

    actual fun set(value: V) {
        delegate.value = value
    }

    actual fun getAndSet(value: V): V {
        var old = delegate.value
        while (!delegate.compareAndSet(old, value)) { old = delegate.value }
        return old
    }

    actual fun compareAndSet(expect: V, newValue: V): Boolean {
        return delegate.compareAndSet(expect, newValue)
    }
}

internal actual class AtomicInt actual constructor(value: Int) {
    private val delegate = kotlin.native.concurrent.AtomicInt(value)
    actual fun get(): Int = delegate.value
    actual fun set(value: Int) {
        delegate.value = value
    }
    actual fun add(amount: Int): Int = delegate.addAndGet(amount)
}

internal actual fun identityHashCode(instance: Any?): Int =
    instance.identityHashCode()

actual annotation class TestOnly

actual typealias CompositionContextLocal = kotlin.native.concurrent.ThreadLocal

actual val DefaultMonotonicFrameClock: MonotonicFrameClock = MonotonicClockImpl()

@OptIn(ExperimentalTime::class)
private class MonotonicClockImpl : MonotonicFrameClock {
    override suspend fun <R> withFrameNanos(
        onFrame: (Long) -> R
    ): R {
        yield()
        return onFrame(getTimeNanos())
    }
}

internal actual object Trace {
    actual fun beginSection(name: String): Any? {
        return null
    }

    actual fun endSection(token: Any?) {
    }
}

actual annotation class CheckResult actual constructor(actual val suggest: String)

internal actual fun <T> createSnapshotMutableState(
    value: T,
    policy: SnapshotMutationPolicy<T>
): SnapshotMutableState<T> =
    SnapshotMutableStateImpl(value, policy)

@ExperimentalComposeApi
internal actual class SnapshotContextElementImpl actual constructor(
    private val snapshot: Snapshot
) : SnapshotContextElement {

    init {
        error("provide SnapshotContextElementImpl when coroutines lib has necessary APIs")
    }

    override val key: CoroutineContext.Key<*>
        get() = SnapshotContextElement
}

internal actual fun logError(message: String, e: Throwable) {
    println(message)
    e.printStackTrace()
}