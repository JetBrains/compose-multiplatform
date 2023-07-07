/*
 * Copyright 2023 The Android Open Source Project
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
import kotlinx.browser.window
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

internal actual fun getCurrentThreadId(): Long = 0

internal actual class AtomicInt actual constructor(private var value: Int) {
    actual fun get(): Int = value

    actual fun set(value: Int) {
        this.value = value
    }
    actual fun add(amount: Int): Int {
        this.value += amount
        return this.value
    }
}

actual class AtomicReference<V> actual constructor(private var value: V) {
    actual fun get(): V = value

    actual fun set(value: V) {
        this.value = value
    }

    actual fun getAndSet(value: V): V {
        val oldValue = this.value
        this.value = value
        return oldValue
    }

    actual fun compareAndSet(expect: V, newValue: V): Boolean =
        if (expect == value) {
            value = newValue
            true
        } else {
            false
        }
}

@JsFun("(obj, index) => obj[index]")
private external fun dynamicGetInt(obj: JsAny, index: String): Int?

@JsFun("(obj) => typeof obj")
private external fun jsTypeOf(a: JsAny?): String

internal actual fun identityHashCode(instance: Any?): Int {
    if (instance == null) {
        return 0
    }
    return instance.hashCode()
}

actual annotation class CompositionContextLocal {}

@JsFun("""
(() => {
const memoizeIdentityHashCodeMap = new WeakMap();
let nextHash = 1;
return (obj) => {
    const res = memoizeIdentityHashCodeMap.get(obj);
    if (res === undefined) {
        const value = nextHash++;
        memoizeIdentityHashCodeMap.set(obj, value);
        return value;
    }
    return res;
}
})()"""
)
private external fun getIdentityHashCode(instance: JsAny): Int

actual annotation class TestOnly

actual val DefaultMonotonicFrameClock: MonotonicFrameClock = MonotonicClockImpl()

@OptIn(ExperimentalTime::class)
private class MonotonicClockImpl : MonotonicFrameClock {
    override suspend fun <R> withFrameNanos(
        onFrame: (Long) -> R
    ): R = suspendCoroutine { continuation ->
        window.requestAnimationFrame {
            val duration = it.toDuration(DurationUnit.MILLISECONDS)
            val result = onFrame(duration.inWholeNanoseconds)
            continuation.resume(result)
        }
    }
}

internal actual fun <T> createSnapshotMutableState(
    value: T,
    policy: SnapshotMutationPolicy<T>
): SnapshotMutableState<T> = SnapshotMutableStateImpl(value, policy)

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