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

package androidx.compose.runtime

import androidx.compose.runtime.internal.ThreadMap
import androidx.compose.runtime.internal.emptyThreadMap

internal actual typealias AtomicReference<V> = java.util.concurrent.atomic.AtomicReference<V>

internal actual open class ThreadLocal<T> actual constructor(
    private val initialValue: () -> T
) : java.lang.ThreadLocal<T>() {
    @Suppress("UNCHECKED_CAST")
    actual override fun get(): T {
        return super.get() as T
    }

    actual override fun set(value: T) {
        super.set(value)
    }

    override fun initialValue(): T? {
        return initialValue.invoke()
    }

    actual override fun remove() {
        super.remove()
    }
}

internal actual class SnapshotThreadLocal<T> {
    private val map = AtomicReference<ThreadMap>(emptyThreadMap)
    private val writeMutex = Any()

    @Suppress("UNCHECKED_CAST")
    actual fun get(): T? = map.get().get(Thread.currentThread().id) as T?

    actual fun set(value: T?) {
        val key = Thread.currentThread().id
        synchronized(writeMutex) {
            val current = map.get()
            if (current.trySet(key, value)) return
            map.set(current.newWith(key, value))
        }
    }
}

internal actual fun identityHashCode(instance: Any?): Int = System.identityHashCode(instance)

@PublishedApi
internal actual inline fun <R> synchronized(lock: Any, block: () -> R): R {
    return kotlin.synchronized(lock, block)
}

internal actual typealias TestOnly = org.jetbrains.annotations.TestOnly

internal actual fun invokeComposable(composer: Composer, composable: @Composable () -> Unit) {
    @Suppress("UNCHECKED_CAST")
    val realFn = composable as Function2<Composer, Int, Unit>
    realFn(composer, 1)
}

internal actual fun <T> invokeComposableForResult(
    composer: Composer,
    composable: @Composable () -> T
): T {
    @Suppress("UNCHECKED_CAST")
    val realFn = composable as Function2<Composer, Int, T>
    return realFn(composer, 1)
}

internal actual class AtomicInt actual constructor(value: Int) {
    val delegate = java.util.concurrent.atomic.AtomicInteger(value)
    actual fun get(): Int = delegate.get()
    actual fun set(value: Int) = delegate.set(value)
    actual fun add(amount: Int): Int = delegate.addAndGet(amount)
}

internal actual fun ensureMutable(it: Any) { /* NOTHING */ }
