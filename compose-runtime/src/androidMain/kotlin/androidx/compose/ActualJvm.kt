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

actual typealias BitSet = java.util.BitSet

actual open class ThreadLocal<T> actual constructor() : java.lang.ThreadLocal<T>() {
    actual override fun get(): T? {
        return super.get()
    }

    actual override fun set(value: T?) {
        super.set(value)
    }

    actual override fun initialValue(): T? {
        return super.initialValue()
    }
}

actual typealias WeakHashMap<K, V> = java.util.WeakHashMap<K, V>

actual fun identityHashCode(instance: Any?): Int = System.identityHashCode(instance)

actual inline fun <R> synchronized(lock: Any, block: () -> R): R {
    kotlin.synchronized(lock) {
        return block()
    }
}

actual typealias WeakReference<T> = java.lang.ref.WeakReference<T>

actual typealias MainThread = androidx.annotation.MainThread

actual typealias TestOnly = org.jetbrains.annotations.TestOnly

actual typealias CheckResult = androidx.annotation.CheckResult
