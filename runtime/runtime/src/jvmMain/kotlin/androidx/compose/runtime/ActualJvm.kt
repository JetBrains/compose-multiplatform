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

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.persistentListOf

actual typealias AtomicReference<V> = java.util.concurrent.atomic.AtomicReference<V>

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
}

internal actual typealias WeakHashMap<K, V> = java.util.WeakHashMap<K, V>

internal actual fun identityHashCode(instance: Any?): Int = System.identityHashCode(instance)

internal actual inline fun <R> synchronized(lock: Any, block: () -> R): R {
    kotlin.synchronized(lock) {
        return block()
    }
}

internal actual typealias Reference<T> = java.lang.ref.Reference<T>

internal actual typealias ReferenceQueue<T> = java.lang.ref.ReferenceQueue<T>

internal actual typealias WeakReference<T> = java.lang.ref.WeakReference<T>

internal actual typealias TestOnly = org.jetbrains.annotations.TestOnly

internal actual class BuildableMapBuilder<K, V>(
    val builder: PersistentMap.Builder<K, V>
) : MutableMap<K, V> by builder {
    actual fun build(): BuildableMap<K, V> {
        return BuildableMap(builder.build())
    }
}

actual data class BuildableMap<K, V>(val map: PersistentMap<K, V>) : Map<K, V> by map {
    internal actual fun builder(): BuildableMapBuilder<K, V> {
        return BuildableMapBuilder(map.builder())
    }
}

private val emptyPersistentMap = persistentHashMapOf<Any, Any>()

@Suppress("UNCHECKED_CAST")
internal actual fun <K, V> buildableMapOf(): BuildableMap<K, V> =
    BuildableMap(emptyPersistentMap as PersistentMap<K, V>)

internal actual class BuildableListBuilder<T>(
    val builder: PersistentList.Builder<T>
) : MutableList<T> by builder {
    actual fun build(): BuildableList<T> {
        return BuildableList(builder.build())
    }
}

internal actual data class BuildableList<T>(val list: PersistentList<T>) : List<T> by list {
    internal actual fun add(element: T) = BuildableList(list.add(element))
    internal actual fun add(index: Int, element: T) = BuildableList(list.add(index, element))
    internal actual fun addAll(elements: Collection<T>) = BuildableList(list.addAll(elements))
    internal actual fun remove(element: T) = BuildableList(list.remove(element))
    internal actual fun removeAll(elements: Collection<T>) = BuildableList(list.removeAll(elements))
    internal actual fun removeAt(index: Int) = BuildableList(list.removeAt(index))
    internal actual fun set(index: Int, element: T) = BuildableList(list.set(index, element))

    internal actual fun builder(): BuildableListBuilder<T> {
        return BuildableListBuilder(list.builder())
    }
}

private val emptyPersistentList = persistentListOf<Any>()
private val emptyBuildableList = BuildableList<Any>(emptyPersistentList)

@Suppress("UNCHECKED_CAST")
internal actual fun <T> buildableListOf(): BuildableList<T> = emptyBuildableList as BuildableList<T>

public actual typealias UnsupportedOperationException = java.lang.UnsupportedOperationException

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun <T> sortArrayWith(
    array: Array<T>,
    comparator: Comparator<T>,
    fromIndex: Int,
    toIndex: Int
) {
    array.sortWith(comparator, fromIndex, toIndex)
}