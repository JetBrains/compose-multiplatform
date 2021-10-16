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
package androidx.compose.ui.text.platform

internal actual class WeakHashMap<K, V> :  MutableMap<K, V> {
    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = TODO()
    override val values: MutableSet<V>
        get() = TODO()
    override val keys: MutableSet<K>
        get() = TODO()
    override val size: Int 
        get() = TODO()
    override  fun containsKey(key: K): Boolean = TODO()
    override  fun containsValue(key: V): Boolean = TODO()
    override fun clear() = TODO()
    override fun get(key: K): V? = TODO()
    override fun put(key: K, value: V): V? = TODO()
    override fun putAll(from: Map<out K, V>): Unit = TODO()
    override fun remove(key: K): V? = TODO()
    override fun isEmpty(): Boolean = TODO()
}

internal fun <K, V> WeakHashMap<K, V>.getOrPut(key: K, default: (K) -> V) = default(key)

