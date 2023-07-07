/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:Suppress("NOTHING_TO_INLINE")

package androidx.compose.runtime.external.kotlinx.collections.immutable

import androidx.compose.runtime.external.kotlinx.collections.immutable.implementations.immutableList.persistentVectorOf
import androidx.compose.runtime.external.kotlinx.collections.immutable.implementations.immutableMap.PersistentHashMap
import androidx.compose.runtime.external.kotlinx.collections.immutable.implementations.immutableMap.PersistentHashMapBuilder
import androidx.compose.runtime.external.kotlinx.collections.immutable.implementations.immutableSet.PersistentHashSet
import androidx.compose.runtime.external.kotlinx.collections.immutable.implementations.immutableSet.PersistentHashSetBuilder
import androidx.compose.runtime.external.kotlinx.collections.immutable.implementations.persistentOrderedMap.PersistentOrderedMap
import androidx.compose.runtime.external.kotlinx.collections.immutable.implementations.persistentOrderedMap.PersistentOrderedMapBuilder
import androidx.compose.runtime.external.kotlinx.collections.immutable.implementations.persistentOrderedSet.PersistentOrderedSet
import androidx.compose.runtime.external.kotlinx.collections.immutable.implementations.persistentOrderedSet.PersistentOrderedSetBuilder

//@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
//inline fun <T> @kotlin.internal.Exact ImmutableCollection<T>.mutate(mutator: (MutableCollection<T>) -> Unit): ImmutableCollection<T> = builder().apply(mutator).build()
// it or this?
/**
 * Returns the result of applying the provided modifications on this set.
 *
 * The mutable set passed to the [mutator] closure has the same contents as this persistent set.
 *
 * @return a new persistent set with the provided modifications applied;
 * or this instance if no modifications were made in the result of this operation.
 */
internal inline fun <T> PersistentSet<T>.mutate(mutator: (MutableSet<T>) -> Unit): PersistentSet<T> = builder().apply(mutator).build()

/**
 * Returns the result of applying the provided modifications on this list.
 *
 * The mutable list passed to the [mutator] closure has the same contents as this persistent list.
 *
 * @return a new persistent list with the provided modifications applied;
 * or this instance if no modifications were made in the result of this operation.
 */
internal inline fun <T> PersistentList<T>.mutate(mutator: (MutableList<T>) -> Unit): PersistentList<T> = builder().apply(mutator).build()

/**
 * Returns the result of applying the provided modifications on this map.
 *
 * The mutable map passed to the [mutator] closure has the same contents as this persistent map.
 *
 * @return a new persistent map with the provided modifications applied;
 * or this instance if no modifications were made in the result of this operation.
 */
@Suppress("UNCHECKED_CAST")
internal inline fun <K, V> PersistentMap<out K, V>.mutate(mutator: (MutableMap<K, V>) -> Unit): PersistentMap<K, V> =
        (this as PersistentMap<K, V>).builder().apply(mutator).build()


/**
 * Returns the result of adding the specified [element] to this collection.
 *
 * @returns a new persistent collection with the specified [element] added;
 * or this instance if this collection does not support duplicates and it already contains the element.
 */
internal inline operator fun <E> PersistentCollection<E>.plus(element: E): PersistentCollection<E> = add(element)

/**
 * Returns the result of removing a single appearance of the specified [element] from this collection.
 *
 * @return a new persistent collection with a single appearance of the specified [element] removed;
 * or this instance if there is no such element in this collection.
 */
internal inline operator fun <E> PersistentCollection<E>.minus(element: E): PersistentCollection<E> = remove(element)


/**
 * Returns the result of adding all elements of the specified [elements] collection to this collection.
 *
 * @return a new persistent collection with elements of the specified [elements] collection added;
 * or this instance if no modifications were made in the result of this operation.
 */
internal operator fun <E> PersistentCollection<E>.plus(elements: Iterable<E>): PersistentCollection<E>
        = if (elements is Collection) addAll(elements) else builder().also { it.addAll(elements) }.build()

/**
 * Returns the result of adding all elements of the specified [elements] array to this collection.
 *
 * @return a new persistent collection with elements of the specified [elements] array added;
 * or this instance if no modifications were made in the result of this operation.
 */
internal operator fun <E> PersistentCollection<E>.plus(elements: Array<out E>): PersistentCollection<E>
        = builder().also { it.addAll(elements) }.build()

/**
 * Returns the result of adding all elements of the specified [elements] sequence to this collection.
 *
 * @return a new persistent collection with elements of the specified [elements] sequence added;
 * or this instance if no modifications were made in the result of this operation.
 */
internal operator fun <E> PersistentCollection<E>.plus(elements: Sequence<E>): PersistentCollection<E>
        = builder().also { it.addAll(elements) }.build()


/**
 * Returns the result of removing all elements in this collection that are also
 * contained in the specified [elements] collection.
 *
 * @return a new persistent collection with elements in this collection that are also
 * contained in the specified [elements] collection removed;
 * or this instance if no modifications were made in the result of this operation.
 */
internal operator fun <E> PersistentCollection<E>.minus(elements: Iterable<E>): PersistentCollection<E>
        = if (elements is Collection) removeAll(elements) else builder().also { it.removeAll(elements) }.build()

/**
 * Returns the result of removing all elements in this collection that are also
 * contained in the specified [elements] array.
 *
 * @return a new persistent collection with elements in this collection that are also
 * contained in the specified [elements] array removed;
 * or this instance if no modifications were made in the result of this operation.
 */
internal operator fun <E> PersistentCollection<E>.minus(elements: Array<out E>): PersistentCollection<E>
        = builder().also { it.removeAll(elements) }.build()

/**
 * Returns the result of removing all elements in this collection that are also
 * contained in the specified [elements] sequence.
 *
 * @return a new persistent collection with elements in this collection that are also
 * contained in the specified [elements] sequence removed;
 * or this instance if no modifications were made in the result of this operation.
 */
internal operator fun <E> PersistentCollection<E>.minus(elements: Sequence<E>): PersistentCollection<E>
        =  builder().also { it.removeAll(elements) }.build()


/**
 * Returns a new persistent list with the specified [element] appended.
 */
internal inline operator fun <E> PersistentList<E>.plus(element: E): PersistentList<E> = add(element)

/**
 * Returns the result of removing the first appearance of the specified [element] from this list.
 *
 * @return a new persistent list with the first appearance of the specified [element] removed;
 * or this instance if there is no such element in this list.
 */
internal inline operator fun <E> PersistentList<E>.minus(element: E): PersistentList<E> = remove(element)


/**
 * Returns the result of appending all elements of the specified [elements] collection to this list.
 *
 * The elements are appended in the order they appear in the specified collection.
 *
 * @return a new persistent list with elements of the specified [elements] collection appended;
 * or this instance if the specified collection is empty.
 */
internal operator fun <E> PersistentList<E>.plus(elements: Iterable<E>): PersistentList<E>
        = if (elements is Collection) addAll(elements) else mutate { it.addAll(elements) }

/**
 * Returns the result of appending all elements of the specified [elements] array to this list.
 *
 * The elements are appended in the order they appear in the specified array.
 *
 * @return a new persistent list with elements of the specified [elements] array appended;
 * or this instance if the specified array is empty.
 */
internal operator fun <E> PersistentList<E>.plus(elements: Array<out E>): PersistentList<E>
        = mutate { it.addAll(elements) }

/**
 * Returns the result of appending all elements of the specified [elements] sequence to this list.
 *
 * The elements are appended in the order they appear in the specified sequence.
 *
 * @return a new persistent list with elements of the specified [elements] sequence appended;
 * or this instance if the specified sequence is empty.
 */
internal operator fun <E> PersistentList<E>.plus(elements: Sequence<E>): PersistentList<E>
        = mutate { it.addAll(elements) }


/**
 * Returns the result of removing all elements in this list that are also
 * contained in the specified [elements] collection.
 *
 * @return a new persistent list with elements in this list that are also
 * contained in the specified [elements] collection removed;
 * or this instance if no modifications were made in the result of this operation.
 */
internal operator fun <E> PersistentList<E>.minus(elements: Iterable<E>): PersistentList<E>
        = if (elements is Collection) removeAll(elements) else mutate { it.removeAll(elements) }

/**
 * Returns the result of removing all elements in this list that are also
 * contained in the specified [elements] array.
 *
 * @return a new persistent list with elements in this list that are also
 * contained in the specified [elements] array removed;
 * or this instance if no modifications were made in the result of this operation.
 */
internal operator fun <E> PersistentList<E>.minus(elements: Array<out E>): PersistentList<E>
        = mutate { it.removeAll(elements) }

/**
 * Returns the result of removing all elements in this list that are also
 * contained in the specified [elements] sequence.
 *
 * @return a new persistent list with elements in this list that are also
 * contained in the specified [elements] sequence removed;
 * or this instance if no modifications were made in the result of this operation.
 */
internal operator fun <E> PersistentList<E>.minus(elements: Sequence<E>): PersistentList<E>
        = mutate { it.removeAll(elements) }


/**
 * Returns the result of adding the specified [element] to this set.
 *
 * @return a new persistent set with the specified [element] added;
 * or this instance if it already contains the element.
 */
internal inline operator fun <E> PersistentSet<E>.plus(element: E): PersistentSet<E> = add(element)

/**
 * Returns the result of removing the specified [element] from this set.
 *
 * @return a new persistent set with the specified [element] removed;
 * or this instance if there is no such element in this set.
 */
internal inline operator fun <E> PersistentSet<E>.minus(element: E): PersistentSet<E> = remove(element)


/**
 * Returns the result of adding all elements of the specified [elements] collection to this set.
 *
 * @return a new persistent set with elements of the specified [elements] collection added;
 * or this instance if it already contains every element of the specified collection.
 */
internal operator fun <E> PersistentSet<E>.plus(elements: Iterable<E>): PersistentSet<E>
        = if (elements is Collection) addAll(elements) else mutate { it.addAll(elements) }

/**
 * Returns the result of adding all elements of the specified [elements] array to this set.
 *
 * @return a new persistent set with elements of the specified [elements] array added;
 * or this instance if it already contains every element of the specified array.
 */
internal operator fun <E> PersistentSet<E>.plus(elements: Array<out E>): PersistentSet<E>
        = mutate { it.addAll(elements) }

/**
 * Returns the result of adding all elements of the specified [elements] sequence to this set.
 *
 * @return a new persistent set with elements of the specified [elements] sequence added;
 * or this instance if it already contains every element of the specified sequence.
 */
internal operator fun <E> PersistentSet<E>.plus(elements: Sequence<E>): PersistentSet<E>
        = mutate { it.addAll(elements) }


/**
 * Returns the result of removing all elements in this set that are also
 * contained in the specified [elements] collection.
 *
 * @return a new persistent set with elements in this set that are also
 * contained in the specified [elements] collection removed;
 * or this instance if no modifications were made in the result of this operation.
 */
internal operator fun <E> PersistentSet<E>.minus(elements: Iterable<E>): PersistentSet<E>
        = if (elements is Collection) removeAll(elements) else mutate { it.removeAll(elements) }

/**
 * Returns the result of removing all elements in this set that are also
 * contained in the specified [elements] array.
 *
 * @return a new persistent set with elements in this set that are also
 * contained in the specified [elements] array removed;
 * or this instance if no modifications were made in the result of this operation.
 */
internal operator fun <E> PersistentSet<E>.minus(elements: Array<out E>): PersistentSet<E>
        = mutate { it.removeAll(elements) }

/**
 * Returns the result of removing all elements in this set that are also
 * contained in the specified [elements] sequence.
 *
 * @return a new persistent set with elements in this set that are also
 * contained in the specified [elements] sequence removed;
 * or this instance if no modifications were made in the result of this operation.
 */
internal operator fun <E> PersistentSet<E>.minus(elements: Sequence<E>): PersistentSet<E>
        = mutate { it.removeAll(elements) }

/**
 * Returns all elements in this set that are also
 * contained in the specified [elements] collection.
 *
 * @return a new persistent set with elements in this set that are also
 * contained in the specified [elements] collection;
 * or this instance if no modifications were made in the result of this operation.
 */
internal infix fun <E> PersistentSet<E>.intersect(elements: Iterable<E>): PersistentSet<E>
        = if (elements is Collection) retainAll(elements) else mutate { it.retainAll(elements) }

/**
 * Returns all elements in this collection that are also
 * contained in the specified [elements] collection.
 *
 * @return a new persistent set with elements in this collection that are also
 * contained in the specified [elements] collection
 */
internal infix fun <E> PersistentCollection<E>.intersect(elements: Iterable<E>): PersistentSet<E>
        = this.toPersistentSet().intersect(elements)

/**
 * Returns the result of adding an entry to this map from the specified key-value [pair].
 *
 * If this map already contains a mapping for the key,
 * the old value is replaced by the value from the specified [pair].
 *
 * @return a new persistent map with an entry from the specified key-value [pair] added;
 * or this instance if no modifications were made in the result of this operation.
 */
@Suppress("UNCHECKED_CAST")
internal inline operator fun <K, V> PersistentMap<out K, V>.plus(pair: Pair<K, V>): PersistentMap<K, V>
        = (this as PersistentMap<K, V>).put(pair.first, pair.second)

/**
 * Returns the result of replacing or adding entries to this map from the specified key-value pairs.
 *
 * @return a new persistent map with entries from the specified key-value pairs added;
 * or this instance if no modifications were made in the result of this operation.
 */
internal inline operator fun <K, V> PersistentMap<out K, V>.plus(pairs: Iterable<Pair<K, V>>): PersistentMap<K, V> = putAll(pairs)

/**
 * Returns the result of replacing or adding entries to this map from the specified key-value pairs.
 *
 * @return a new persistent map with entries from the specified key-value pairs added;
 * or this instance if no modifications were made in the result of this operation.
 */
internal inline operator fun <K, V> PersistentMap<out K, V>.plus(pairs: Array<out Pair<K, V>>): PersistentMap<K, V> = putAll(pairs)

/**
 * Returns the result of replacing or adding entries to this map from the specified key-value pairs.
 *
 * @return a new persistent map with entries from the specified key-value pairs added;
 * or this instance if no modifications were made in the result of this operation.
 */
internal inline operator fun <K, V> PersistentMap<out K, V>.plus(pairs: Sequence<Pair<K, V>>): PersistentMap<K, V> = putAll(pairs)

/**
 * Returns the result of merging the specified [map] with this map.
 *
 * The effect of this call is equivalent to that of calling `put(k, v)` once for each
 * mapping from key `k` to value `v` in the specified map.
 *
 * @return a new persistent map with keys and values from the specified [map] associated;
 * or this instance if no modifications were made in the result of this operation.
 */
internal inline operator fun <K, V> PersistentMap<out K, V>.plus(map: Map<out K, V>): PersistentMap<K, V> = putAll(map)


/**
 * Returns the result of merging the specified [map] with this map.
 *
 * The effect of this call is equivalent to that of calling `put(k, v)` once for each
 * mapping from key `k` to value `v` in the specified map.
 *
 * @return a new persistent map with keys and values from the specified [map] associated;
 * or this instance if no modifications were made in the result of this operation.
 */
@Suppress("UNCHECKED_CAST")
internal fun <K, V> PersistentMap<out K, V>.putAll(map: Map<out K, V>): PersistentMap<K, V> =
        (this as PersistentMap<K, V>).putAll(map)

/**
 * Returns the result of replacing or adding entries to this map from the specified key-value pairs.
 *
 * @return a new persistent map with entries from the specified key-value pairs added;
 * or this instance if no modifications were made in the result of this operation.
 */
internal fun <K, V> PersistentMap<out K, V>.putAll(pairs: Iterable<Pair<K, V>>): PersistentMap<K, V>
        = mutate { it.putAll(pairs) }

/**
 * Returns the result of replacing or adding entries to this map from the specified key-value pairs.
 *
 * @return a new persistent map with entries from the specified key-value pairs added;
 * or this instance if no modifications were made in the result of this operation.
 */
internal fun <K, V> PersistentMap<out K, V>.putAll(pairs: Array<out Pair<K, V>>): PersistentMap<K, V>
        = mutate { it.putAll(pairs) }

/**
 * Returns the result of replacing or adding entries to this map from the specified key-value pairs.
 *
 * @return a new persistent map with entries from the specified key-value pairs added;
 * or this instance if no modifications were made in the result of this operation.
 */
internal fun <K, V> PersistentMap<out K, V>.putAll(pairs: Sequence<Pair<K, V>>): PersistentMap<K, V>
        = mutate { it.putAll(pairs) }


/**
 * Returns the result of removing the specified [key] and its corresponding value from this map.
 *
 * @return a new persistent map with the specified [key] and its corresponding value removed;
 * or this instance if it contains no mapping for the key.
 */
@Suppress("UNCHECKED_CAST")
internal operator fun <K, V> PersistentMap<out K, V>.minus(key: K): PersistentMap<K, V>
        = (this as PersistentMap<K, V>).remove(key)

/**
 * Returns the result of removing the specified [keys] and their corresponding values from this map.
 *
 * @return a new persistent map with the specified [keys] and their corresponding values removed;
 * or this instance if no modifications were made in the result of this operation.
 */
internal operator fun <K, V> PersistentMap<out K, V>.minus(keys: Iterable<K>): PersistentMap<K, V>
        = mutate { it.minusAssign(keys) }

/**
 * Returns the result of removing the specified [keys] and their corresponding values from this map.
 *
 * @return a new persistent map with the specified [keys] and their corresponding values removed;
 * or this instance if no modifications were made in the result of this operation.
 */
internal operator fun <K, V> PersistentMap<out K, V>.minus(keys: Array<out K>): PersistentMap<K, V>
        = mutate { it.minusAssign(keys) }

/**
 * Returns the result of removing the specified [keys] and their corresponding values from this map.
 *
 * @return a new persistent map with the specified [keys] and their corresponding values removed;
 * or this instance if no modifications were made in the result of this operation.
 */
internal operator fun <K, V> PersistentMap<out K, V>.minus(keys: Sequence<K>): PersistentMap<K, V>
        = mutate { it.minusAssign(keys) }


/**
 * Returns a new persistent list of the specified elements.
 */
internal fun <E> persistentListOf(vararg elements: E): PersistentList<E> = persistentVectorOf<E>().addAll(elements.asList())

/**
 * Returns an empty persistent list.
 */
internal fun <E> persistentListOf(): PersistentList<E> = persistentVectorOf()


/**
 * Returns a new persistent set with the given elements.
 *
 * Elements of the returned set are iterated in the order they were specified.
 */
internal fun <E> persistentSetOf(vararg elements: E): PersistentSet<E> = PersistentOrderedSet.emptyOf<E>().addAll(elements.asList())

/**
 * Returns an empty persistent set.
 */
internal fun <E> persistentSetOf(): PersistentSet<E> = PersistentOrderedSet.emptyOf<E>()


/**
 * Returns a new persistent set with the given elements.
 *
 * Order of the elements in the returned set is unspecified.
 */
internal fun <E> persistentHashSetOf(vararg elements: E): PersistentSet<E> = PersistentHashSet.emptyOf<E>().addAll(elements.asList())

/**
 * Returns an empty persistent set.
 */
internal fun <E> persistentHashSetOf(): PersistentSet<E> = PersistentHashSet.emptyOf()


/**
 * Returns a new persistent map with the specified contents, given as a list of pairs
 * where the first component is the key and the second is the value.
 *
 * If multiple pairs have the same key, the resulting map will contain the value from the last of those pairs.
 *
 * Entries of the map are iterated in the order they were specified.
 */
internal fun <K, V> persistentMapOf(vararg pairs: Pair<K, V>): PersistentMap<K, V> = PersistentOrderedMap.emptyOf<K,V>().mutate { it += pairs }

/**
 * Returns an empty persistent map.
 */
internal fun <K, V> persistentMapOf(): PersistentMap<K, V> = PersistentOrderedMap.emptyOf()


/**
 * Returns a new persistent map with the specified contents, given as a list of pairs
 * where the first component is the key and the second is the value.
 *
 * If multiple pairs have the same key, the resulting map will contain the value from the last of those pairs.
 *
 * Order of the entries in the returned map is unspecified.
 */
internal fun <K, V> persistentHashMapOf(vararg pairs: Pair<K, V>): PersistentMap<K, V> = PersistentHashMap.emptyOf<K,V>().mutate { it += pairs }

/**
 * Returns an empty persistent map.
 */
internal fun <K, V> persistentHashMapOf(): PersistentMap<K, V> = PersistentHashMap.emptyOf()


/**
 * Returns a new persistent list of the specified elements.
 */
@Deprecated("Use persistentListOf instead.", ReplaceWith("persistentListOf(*elements)"))
internal fun <E> immutableListOf(vararg elements: E): PersistentList<E> = persistentListOf(*elements)

/**
 * Returns an empty persistent list.
 */
@Deprecated("Use persistentListOf instead.", ReplaceWith("persistentListOf()"))
internal fun <E> immutableListOf(): PersistentList<E> = persistentListOf()


/**
 * Returns a new persistent set with the given elements.
 *
 * Elements of the returned set are iterated in the order they were specified.
 */
@Deprecated("Use persistentSetOf instead.", ReplaceWith("persistentSetOf(*elements)"))
internal fun <E> immutableSetOf(vararg elements: E): PersistentSet<E> = persistentSetOf(*elements)

/**
 * Returns an empty persistent set.
 */
@Deprecated("Use persistentSetOf instead.", ReplaceWith("persistentSetOf()"))
internal fun <E> immutableSetOf(): PersistentSet<E> = persistentSetOf()


/**
 * Returns a new persistent set with the given elements.
 *
 * Order of the elements in the returned set is unspecified.
 */
@Deprecated("Use persistentHashSetOf instead.", ReplaceWith("persistentHashSetOf(*elements)"))
internal fun <E> immutableHashSetOf(vararg elements: E): PersistentSet<E> = persistentHashSetOf(*elements)


/**
 * Returns a new persistent map with the specified contents, given as a list of pairs
 * where the first component is the key and the second is the value.
 *
 * If multiple pairs have the same key, the resulting map will contain the value from the last of those pairs.
 *
 * Entries of the map are iterated in the order they were specified.
 */
@Deprecated("Use persistentMapOf instead.", ReplaceWith("persistentMapOf(*pairs)"))
internal fun <K, V> immutableMapOf(vararg pairs: Pair<K, V>): PersistentMap<K, V> = persistentMapOf(*pairs)

/**
 * Returns a new persistent map with the specified contents, given as a list of pairs
 * where the first component is the key and the second is the value.
 *
 * If multiple pairs have the same key, the resulting map will contain the value from the last of those pairs.
 *
 * Order of the entries in the returned map is unspecified.
 */
@Deprecated("Use persistentHashMapOf instead.", ReplaceWith("persistentHashMapOf(*pairs)"))
internal fun <K, V> immutableHashMapOf(vararg pairs: Pair<K, V>): PersistentMap<K, V> = persistentHashMapOf(*pairs)


/**
 * Returns an immutable list containing all elements of this collection.
 *
 * If the receiver is already an immutable list, returns it as is.
 */
internal fun <T> Iterable<T>.toImmutableList(): ImmutableList<T> =
        this as? ImmutableList
        ?: this.toPersistentList()

/**
 * Returns an immutable list containing all elements of this sequence.
 */
internal fun <T> Sequence<T>.toImmutableList(): ImmutableList<T> = toPersistentList()

/**
 * Returns an immutable list containing all characters.
 */
internal fun CharSequence.toImmutableList(): ImmutableList<Char> = toPersistentList()


// fun <T> Array<T>.toImmutableList(): ImmutableList<T> = immutableListOf<T>() + this.asList()


/**
 * Returns a persistent list containing all elements of this collection.
 *
 * If the receiver is already a persistent list, returns it as is.
 * If the receiver is a persistent list builder, calls `build` on it and returns the result.
 */
internal fun <T> Iterable<T>.toPersistentList(): PersistentList<T> =
        this as? PersistentList
        ?: (this as? PersistentList.Builder)?.build()
        ?: persistentListOf<T>() + this

/**
 * Returns a persistent list containing all elements of this sequence.
 */
internal fun <T> Sequence<T>.toPersistentList(): PersistentList<T> = persistentListOf<T>() + this

/**
 * Returns a persistent list containing all characters.
 */
internal fun CharSequence.toPersistentList(): PersistentList<Char> =
    persistentListOf<Char>().mutate { this.toCollection(it) }


/**
 * Returns an immutable set of all elements of this collection.
 *
 * If the receiver is already an immutable set, returns it as is.
 *
 * Elements of the returned set are iterated in the same order as in this collection.
 */
internal fun <T> Iterable<T>.toImmutableSet(): ImmutableSet<T> =
        this as? ImmutableSet<T>
        ?: (this as? PersistentSet.Builder)?.build()
        ?: persistentSetOf<T>() + this

/**
 * Returns an immutable set of all elements of this sequence.
 *
 * Elements of the returned set are iterated in the same order as in this sequence.
 */
internal fun <T> Sequence<T>.toImmutableSet(): ImmutableSet<T> = toPersistentSet()

/**
 * Returns an immutable set of all characters.
 *
 * Elements of the returned set are iterated in the same order as in this char sequence.
 */
internal fun CharSequence.toImmutableSet(): PersistentSet<Char> = toPersistentSet()


/**
 * Returns a persistent set of all elements of this collection.
 *
 * If the receiver is already a persistent set, returns it as is.
 * If the receiver is a persistent set builder, calls `build` on it and returns the result.
 *
 * Elements of the returned set are iterated in the same order as in this collection.
 */
internal fun <T> Iterable<T>.toPersistentSet(): PersistentSet<T> =
        this as? PersistentOrderedSet<T>
        ?: (this as? PersistentOrderedSetBuilder)?.build()
        ?: PersistentOrderedSet.emptyOf<T>() + this

/**
 * Returns a persistent set of all elements of this sequence.
 *
 * Elements of the returned set are iterated in the same order as in this sequence.
 */
internal fun <T> Sequence<T>.toPersistentSet(): PersistentSet<T> = persistentSetOf<T>() + this

/**
 * Returns a persistent set of all characters.
 *
 * Elements of the returned set are iterated in the same order as in this char sequence.
 */
internal fun CharSequence.toPersistentSet(): PersistentSet<Char> =
        persistentSetOf<Char>().mutate { this.toCollection(it) }


/**
 * Returns a persistent set containing all elements from this set.
 *
 * If the receiver is already a persistent hash set, returns it as is.
 * If the receiver is a persistent hash set builder, calls `build` on it and returns the result.
 *
 * Order of the elements in the returned set is unspecified.
 */
internal fun <T> Iterable<T>.toPersistentHashSet(): PersistentSet<T>
    = this as? PersistentHashSet
        ?: (this as? PersistentHashSetBuilder<T>)?.build()
        ?: PersistentHashSet.emptyOf<T>() + this

/**
 * Returns a persistent set of all elements of this sequence.
 *
 * Order of the elements in the returned set is unspecified.
 */
internal fun <T> Sequence<T>.toPersistentHashSet(): PersistentSet<T> = persistentHashSetOf<T>() + this

/**
 * Returns a persistent set of all characters.
 *
 * Order of the elements in the returned set is unspecified.
 */
internal fun CharSequence.toPersistentHashSet(): PersistentSet<Char> =
        persistentHashSetOf<Char>().mutate { this.toCollection(it) }


/**
 * Returns an immutable map containing all entries from this map.
 *
 * If the receiver is already an immutable map, returns it as is.
 *
 * Entries of the returned map are iterated in the same order as in this map.
 */
internal fun <K, V> Map<K, V>.toImmutableMap(): ImmutableMap<K, V>
    = this as? ImmutableMap
        ?: (this as? PersistentMap.Builder)?.build()
        ?: persistentMapOf<K, V>().putAll(this)

/**
 * Returns a persistent map containing all entries from this map.
 *
 * If the receiver is already a persistent map, returns it as is.
 * If the receiver is a persistent map builder, calls `build` on it and returns the result.
 *
 * Entries of the returned map are iterated in the same order as in this map.
 */
internal fun <K, V> Map<K, V>.toPersistentMap(): PersistentMap<K, V>
    = this as? PersistentOrderedMap<K, V>
        ?: (this as? PersistentOrderedMapBuilder<K, V>)?.build()
        ?: PersistentOrderedMap.emptyOf<K, V>().putAll(this)

/**
 * Returns an immutable map containing all entries from this map.
 *
 * If the receiver is already a persistent hash map, returns it as is.
 * If the receiver is a persistent hash map builder, calls `build` on it and returns the result.
 *
 * Order of the entries in the returned map is unspecified.
 */
internal fun <K, V> Map<K, V>.toPersistentHashMap(): PersistentMap<K, V>
        = this as? PersistentHashMap
        ?: (this as? PersistentHashMapBuilder<K, V>)?.build()
        ?: PersistentHashMap.emptyOf<K, V>().putAll(this)
