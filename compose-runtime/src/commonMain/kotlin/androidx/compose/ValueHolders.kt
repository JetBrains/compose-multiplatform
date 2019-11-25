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

/**
 * A ValueHolder can produce a single value. How that value is produced depends on which of the
 * decedent classes are used.
 */
sealed class ValueHolder<out T> {
    abstract val value: T
}

/**
 * A StaticValueHolder holds a value that will never change.
 */
internal data class StaticValueHolder<out T>(override val value: T) : ValueHolder<T>()

/**
 * A DynamicValueHolder holds a value that can be modified but it the value is backed by a State<T>
 * allowing the changes to be observed during composition.
 */
internal class DynamicValueHolder<T>(initialValue: T) : ValueHolder<T>() {
    private val current = mutableStateOf(initialValue)
    override var value: T
        get() = current.value
        set(value: T) { current.value = value }
}

/**
 * A lazy value holder is static value holder for which the value is produced by the valueProducer
 * parameter which is called once and the result is remembered for the life of LazyValueHolder.
 */
internal class LazyValueHolder<out T>(valueProducer: (() -> T)?) : ValueHolder<T>() {
    @Suppress("UNCHECKED_CAST")
    private val current by lazy {
        val fn = valueProducer
        if (fn == null) null as T
        else fn()
    }

    override val value: T get() = current
}
