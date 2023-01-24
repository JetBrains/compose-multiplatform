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
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotContextElement
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.ThreadContextElement

internal actual typealias AtomicReference<V> = java.util.concurrent.atomic.AtomicReference<V>

internal actual fun getCurrentThreadId(): Long = Thread.currentThread().id

internal actual fun identityHashCode(instance: Any?): Int = System.identityHashCode(instance)

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

actual annotation class CompositionContextLocal {}

internal actual class AtomicInt actual constructor(value: Int) {
    val delegate = java.util.concurrent.atomic.AtomicInteger(value)
    actual fun get(): Int = delegate.get()
    actual fun set(value: Int) = delegate.set(value)
    actual fun add(amount: Int): Int = delegate.addAndGet(amount)
}

/**
 * Implementation of [SnapshotContextElement] that enters a single given snapshot when updating
 * the thread context of a resumed coroutine.
 */
@ExperimentalComposeApi
internal actual class SnapshotContextElementImpl actual constructor(
    private val snapshot: Snapshot
) : SnapshotContextElement, ThreadContextElement<Snapshot?> {
    override val key: CoroutineContext.Key<*>
        get() = SnapshotContextElement

    override fun updateThreadContext(context: CoroutineContext): Snapshot? =
        snapshot.unsafeEnter()

    override fun restoreThreadContext(context: CoroutineContext, oldState: Snapshot?) {
        snapshot.unsafeLeave(oldState)
    }
}
