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

package androidx.compose

expect class BitSet() {
    fun set(bitIndex: Int)
    fun or(set: BitSet)
    fun clear(bitIndex: Int)
    operator fun get(bitIndex: Int): Boolean
}

expect open class ThreadLocal<T>(initialValue: () -> T) {
    fun get(): T
    fun set(value: T)
}

fun <T> ThreadLocal() = ThreadLocal<T?> { null }

expect class WeakHashMap<K, V>() : MutableMap<K, V>

expect fun identityHashCode(instance: Any?): Int

expect interface ViewParent

expect open class View {
    fun getTag(key: Int): Any
    fun setTag(key: Int, tag: Any?)
}
expect val View.parent: ViewParent
expect val View.context: Context

expect abstract class ViewGroup : View {
    fun removeAllViews()
}

expect abstract class Context

expect class FrameLayout(context: Context)

expect inline fun <R> synchronized(lock: Any, block: () -> R): R

expect open class WeakReference<T> : Reference<T> {
    constructor(referent: T)
    constructor(referent: T, q: ReferenceQueue<in T>?)
}

expect abstract class Reference<T> {
    open fun get(): T?
}

expect open class ReferenceQueue<T>() {
    open fun poll(): Reference<out T>?
}

expect class Looper

expect fun isMainThread(): Boolean

expect object LooperWrapper {
    fun getMainLooper(): Looper
}

expect class Handler(looper: Looper) {
    fun postAtFrontOfQueue(block: () -> Unit): Boolean
}

expect interface ChoreographerFrameCallback {
    fun doFrame(frameTimeNanos: Long)
}

/**
 * A forwarding interface to a persistent map used internally to represent ambient scopes.
 */
interface BuildableMap<K, V> : Map<K, V> {
    interface Builder<K, V> : MutableMap<K, V> {
        fun build(): BuildableMap<K, V>
    }
    fun builder(): Builder<K, V>
}

@Suppress("UNCHECKED_CAST")
internal inline fun <K, V> BuildableMap<K, V>.mutate(
    mutator: (MutableMap<K, V>) -> Unit
): BuildableMap<K, V> = builder().apply(mutator).build()

internal expect fun <K, V> buildableMapOf(): BuildableMap<K, V>

expect object Choreographer {
    fun postFrameCallback(callback: ChoreographerFrameCallback)
    fun postFrameCallbackDelayed(delayMillis: Long, callback: ChoreographerFrameCallback)
    fun removeFrameCallback(callback: ChoreographerFrameCallback)
}

@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.CONSTRUCTOR
)
expect annotation class MainThread()

@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.CONSTRUCTOR
)
expect annotation class TestOnly()

@Target(AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
expect annotation class CheckResult(
    val suggest: String
)
