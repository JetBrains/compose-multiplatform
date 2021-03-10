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

// TODO(aelias): Mark the typealiases internal when https://youtrack.jetbrains.com/issue/KT-36695 is fixed.
// Currently, they behave as internal because the actual is internal, even though the expect is public.

internal expect open class ThreadLocal<T>(initialValue: () -> T) {
    fun get(): T
    fun set(value: T)
}

internal fun <T> ThreadLocal() = ThreadLocal<T?> { null }

/**
 * This is similar to a [ThreadLocal] but has lower overhead because it avoids a weak reference.
 * This should only be used when the writes are delimited by a try...finally call that will clean
 * up the reference such as [androidx.compose.runtime.snapshots.Snapshot.enter] else the reference
 * could get pinned by the thread local causing a leak.
 *
 * [ThreadLocal] can be used to implement the actual for platforms that do not exhibit the same
 * overhead for thread locals as the JVM and ART.
 */
internal expect class SnapshotThreadLocal<T>() {
    fun get(): T?
    fun set(value: T?)
}

internal expect fun identityHashCode(instance: Any?): Int

@PublishedApi
internal expect inline fun <R> synchronized(lock: Any, block: () -> R): R

expect class AtomicReference<V>(value: V) {
    fun get(): V
    fun set(value: V)
    fun getAndSet(value: V): V
    fun compareAndSet(expect: V, newValue: V): Boolean
}

@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
expect annotation class TestOnly()

@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
expect annotation class CheckResult(
    val suggest: String
)

/**
 * The [MonotonicFrameClock] used by [withFrameNanos] and [withFrameMillis] if one is not present
 * in the calling [kotlin.coroutines.CoroutineContext].
 */
// Implementor's note:
// This frame clock implementation should try to synchronize with the vsync rate of the device's
// default display. Without this synchronization, any usage of this default clock will result
// in inconsistent animation frame timing and associated visual artifacts.
expect val DefaultMonotonicFrameClock: MonotonicFrameClock
