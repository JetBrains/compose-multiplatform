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

import androidx.compose.runtime.snapshots.SnapshotMutableState
import kotlinx.coroutines.yield
import kotlin.native.identityHashCode
import kotlin.system.getTimeNanos
import kotlin.time.ExperimentalTime
import kotlin.native.concurrent.isFrozen
import kotlin.native.concurrent.freeze
import kotlin.native.concurrent.ensureNeverFrozen

@kotlin.native.concurrent.ThreadLocal
private val threadLocalStorage = mutableMapOf<Any, Any?>()

// TODO:
internal actual open class ThreadLocal<T> actual constructor(
    initialValue: () -> T
) {
    // TODO: not exact semantics as on JVM, initialize initial value only once, not per thread,
    // as otherwise we have to share create factory.
    private val initial = initialValue()

    private var value: T
        get() = threadLocalStorage.getOrPut(this, { initial }) as T
        set(value) {
            threadLocalStorage[this] = value
        }

    actual fun get(): T = value

    actual fun set(value: T) {
        this.value = value
    }

    actual fun remove() {
        threadLocalStorage.remove(this)
    }
}

/**
 * AtomicReference implementation suitable for both single and multi-threaded context.
 */
actual class AtomicReference<V> actual constructor(value: V) {
    private val delegate = kotlin.native.concurrent.FreezableAtomicReference(value)

    actual fun get(): V = delegate.value

    actual fun set(value: V) {
        if (delegate.isFrozen)
            value.freeze()
        delegate.value = value
    }

    actual fun getAndSet(value: V): V {
        if (delegate.isFrozen)
            value.freeze()
        var old = delegate.value
        while (!delegate.compareAndSet(old, value)) { old = delegate.value }
        return old
    }

    actual fun compareAndSet(expect: V, newValue: V): Boolean {
        if (delegate.isFrozen)
            newValue.freeze()
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

internal actual fun ensureMutable(it: Any) {
    it.ensureNeverFrozen()
}

internal actual fun identityHashCode(instance: Any?): Int =
    instance.identityHashCode()

actual annotation class TestOnly

actual typealias CompositionContextLocal = kotlin.native.concurrent.ThreadLocal

actual inline fun <R> synchronized(lock: Any, block: () -> R): R =
    block()

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

// fixme: not actually thread local
internal actual class SnapshotThreadLocal<T> actual constructor() {
    private var value: T? = null

    actual fun get(): T? = value
    actual fun set(value: T?) {
        this.value = value
    }
}