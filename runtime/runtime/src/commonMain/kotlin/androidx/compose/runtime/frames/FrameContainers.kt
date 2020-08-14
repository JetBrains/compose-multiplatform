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

@file:Suppress("DEPRECATION", "UNUSED_PARAMETER")

package androidx.compose.runtime.frames

import kotlin.jvm.JvmField

@Deprecated(
    "Frames have been replaced by snapshots",
    ReplaceWith(
        "StableMutableList<T>",
        "androidx.compose.runtime.StableMutableList"
    )
)
class ModelList<T>
        @Deprecated(
            "Frames have been replaced by snapshots",
            ReplaceWith(
                "mutableStateListOf()",
                "androidx.compose.runtime.mutableStateListOf"
            )
        )
        constructor() : MutableList<T>, Framed {
    private var myFirst: Record =
        ArrayContainer<T>()
    override val firstFrameRecord: Record get() = myFirst

    override fun prependFrameRecord(value: Record) {
        value.next = myFirst
        myFirst = value
    }

    @Suppress("UNCHECKED_CAST") private val readable: ArrayContainer<T>
        get() = deprecated()
    @Suppress("UNCHECKED_CAST") private val writable: ArrayContainer<T>
        get() = deprecated()

    override val size: Int get() = readable.list.size
    override fun add(element: T): Boolean = writable.list.add(element)
    override fun add(index: Int, element: T) = writable.list.add(index, element)
    override fun addAll(index: Int, elements: Collection<T>): Boolean =
        writable.list.addAll(index, elements)
    override fun addAll(elements: Collection<T>): Boolean = writable.list.addAll(elements)
    override fun clear() = writable.list.clear()
    override fun contains(element: T): Boolean = readable.list.contains(element)
    override fun containsAll(elements: Collection<T>): Boolean = readable.list.containsAll(elements)
    override fun get(index: Int): T = readable.list.get(index)
    override fun indexOf(element: T): Int = readable.list.indexOf(element)
    override fun isEmpty(): Boolean = readable.list.isEmpty()
    override fun iterator(): MutableIterator<T> = ModelListIterator(this, 0)
    override fun lastIndexOf(element: T): Int = readable.list.lastIndexOf(element)
    override fun listIterator(): MutableListIterator<T> = ModelListIterator(this, 0)
    override fun listIterator(index: Int): MutableListIterator<T> = ModelListIterator(this, index)
    override fun remove(element: T): Boolean = writable.list.remove(element)
    override fun removeAll(elements: Collection<T>): Boolean = writable.list.removeAll(elements)
    override fun removeAt(index: Int): T = writable.list.removeAt(index)
    override fun retainAll(elements: Collection<T>): Boolean = writable.list.retainAll(elements)
    override fun set(index: Int, element: T): T = writable.list.set(index, element)
    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> =
        writable.list.subList(fromIndex, toIndex)

    private class ArrayContainer<T> : AbstractRecord() {
        @JvmField
        var list: ArrayList<T> = arrayListOf<T>()

        override fun assign(value: Record) {
            @Suppress("UNCHECKED_CAST")
            (value as? ArrayContainer<T>)?.let {
                this.list = it.list.toMutableList() as ArrayList<T>
            }
        }

        override fun create() = ArrayContainer<T>()
    }

    private class ModelListIterator<T>(val modelList: ModelList<T>, val index: Int) :
        MutableIterator<T>,
        MutableListIterator<T> {

        private var nextCount = 0
        private var readId: Int = deprecated()
        private var currentIterator = modelList.readable.list.listIterator(index)

        private fun ensureMutable(): ModelListIterator<T> = deprecated()

        override fun hasNext(): Boolean = currentIterator.hasNext()
        override fun next(): T = currentIterator.next().also { nextCount++ }
        override fun remove() = ensureMutable().currentIterator.remove()
        override fun hasPrevious(): Boolean = currentIterator.hasPrevious()
        override fun nextIndex(): Int = currentIterator.nextIndex().also { nextCount++ }
        override fun previous(): T = currentIterator.previous().also { nextCount-- }
        override fun previousIndex(): Int = currentIterator.previousIndex().also { nextCount-- }
        override fun add(element: T) = ensureMutable().currentIterator.add(element)
        override fun set(element: T) = ensureMutable().currentIterator.set(element)
    }
}

@Deprecated(
    "Frames have been replaced by snapshots",
    ReplaceWith(
        "mutableStateListOf()",
        "androidx.compose.runtime.mutableStateListOf"
    ),
    DeprecationLevel.ERROR
)
fun <T> modelListOf(): ModelList<T> = deprecated()

@Deprecated(
    "Frames have been replaced by snapshots",
    ReplaceWith(
        "mutableStateListOf(element)",
        "androidx.compose.runtime.mutableStateListOf"
    ),
    DeprecationLevel.ERROR
)
fun <T> modelListOf(element: T) = ModelList<T>().apply { add(element) }

@Deprecated(
    "Frames have been replaced by snapshots",
    ReplaceWith(
        "mutableStateListOf(*elements)",
        "androidx.compose.runtime.mutableStateListOf"
    ),
    DeprecationLevel.ERROR
)
fun <T> modelListOf(vararg elements: T) = ModelList<T>().apply { addAll(elements) }

@Deprecated(
    "Frames have been replaced by snapshots",
    ReplaceWith(
        "StableMutableMap<K, V>",
        "androidx.compose.runtime.StableMutableMap"
    )
)
class ModelMap<K, V>
    @Deprecated(
        "Frames have been replaced by snapshots",
        ReplaceWith(
            "mutableStateMapOf()",
            "androidx.compose.runtime.StableMutableMap"
        )
    )
    constructor() : MutableMap<K, V>, Framed {
    private var myFirst: Record =
        MapContainer<K, V>()
    override val firstFrameRecord: Record get() = myFirst

    override fun prependFrameRecord(value: Record) {
        value.next = myFirst
        myFirst = value
    }

    @Suppress("UNCHECKED_CAST")
    private val readable: MapContainer<K, V>
        get() = deprecated()
    @Suppress("UNCHECKED_CAST")
    private val writable: MapContainer<K, V>
        get() = deprecated()

    override val size: Int get() = readable.map.size
    override fun containsKey(key: K): Boolean = readable.map.containsKey(key)
    override fun containsValue(value: V): Boolean = readable.map.containsValue(value)
    override fun get(key: K): V? = readable.map.get(key)
    override fun isEmpty(): Boolean = readable.map.isEmpty()

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = immutableSet(readable.map.entries)
    override val keys: MutableSet<K>
        get() = immutableSet(readable.map.keys)
    override val values: MutableCollection<V>
        get() = immutableCollection(readable.map.values)

    override fun clear() = writable.map.clear()

    override fun put(key: K, value: V): V? = writable.map.put(key, value)
    override fun putAll(from: Map<out K, V>) = writable.map.putAll(from)
    override fun remove(key: K): V? = writable.map.remove(key)

    private class MapContainer<K, V> : AbstractRecord() {
        @JvmField var map = mutableMapOf<K, V>()

        override fun assign(value: Record) {
            @Suppress("UNCHECKED_CAST")
            (value as? MapContainer<K, V>)?.let {
                this.map = LinkedHashMap<K, V>(it.map)
            }
        }

        override fun create() = MapContainer<K, V>()
    }
}

@Deprecated(
    "Frames have been replaced by snapshots",
    ReplaceWith(
        "mutableStateMapOf()",
        "androidx.compose.runtime.mutableStateMapOf"
    ),
    DeprecationLevel.ERROR
)
fun <K, V> modelMapOf(): ModelMap<K, V> = deprecated()

@Deprecated(
    "Frames have been replaced by snapshots",
    ReplaceWith(
        "mutableStateMapOf(*pairs)",
        "androidx.compose.runtime.mutableStateMapOf"
    ),
    DeprecationLevel.ERROR
)
fun <K, V> modelMapOf(vararg pairs: Pair<K, V>): ModelMap<K, V> = deprecated()

private fun error(): Nothing =
    error("Model sub-collection, iterators, lists and sets are immutable, use asMutable() first")

private class ImmutableSetImpl<T>(
    private val set: MutableSet<T>
) : MutableSet<T> by set {
    override fun add(element: T): Boolean = error()
    override fun addAll(elements: Collection<T>): Boolean = error()
    override fun clear() = error()
    override fun iterator(): MutableIterator<T> =
        immutableIterator(set.iterator())
    override fun remove(element: T): Boolean = error()
    override fun removeAll(elements: Collection<T>): Boolean = error()
    override fun retainAll(elements: Collection<T>): Boolean = error()
}

private fun <T> immutableSet(set: MutableSet<T>): MutableSet<T> = ImmutableSetImpl(set)

private class ImmutableIteratorImpl<T>(
    private val iterator: MutableIterator<T>
) : MutableIterator<T> by iterator {
    override fun remove() = error()
}

// TODO delete the explicit type after https://youtrack.jetbrains.com/issue/KT-20996
private fun <T> immutableIterator(
    iterator: MutableIterator<T>
): MutableIterator<T> = ImmutableIteratorImpl(iterator)

private class ImmutableListIteratorImpl<T>(
    private val iterator: MutableListIterator<T>
) : MutableListIterator<T> by iterator {
    override fun add(element: T) = error()
    override fun remove() = error()
    override fun set(element: T) = error()
}

// TODO delete the explicit type after https://youtrack.jetbrains.com/issue/KT-20996
private fun <T> immutableListIterator(
    iterator: MutableListIterator<T>
): MutableListIterator<T> = ImmutableListIteratorImpl(iterator)

private class ImmutableCollectionImpl<T>(
    private val collection: MutableCollection<T>
) : MutableCollection<T> by collection {
    override fun add(element: T): Boolean = error()
    override fun addAll(elements: Collection<T>): Boolean = error()
    override fun clear() = error()
    override fun iterator(): MutableIterator<T> =
        immutableIterator(collection.iterator())
    override fun remove(element: T): Boolean = error()
    override fun removeAll(elements: Collection<T>): Boolean = error()
    override fun retainAll(elements: Collection<T>): Boolean = error()
}

// TODO delete the explicit type after https://youtrack.jetbrains.com/issue/KT-20996
private fun <T> immutableCollection(
    collection: MutableCollection<T>
): MutableCollection<T> = ImmutableCollectionImpl(collection)
