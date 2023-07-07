/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package androidx.compose.runtime.external.kotlinx.collections.immutable

/**
 * A generic immutable collection of elements. Methods in this interface support only read-only access to the collection.
 *
 * Modification operations are supported through the [PersistentCollection] interface.
 *
 * Implementors of this interface take responsibility to be immutable.
 * Once constructed they must contain the same elements in the same order.
 *
 * @param E the type of elements contained in the collection. The immutable collection is covariant on its element type.
 */
internal interface ImmutableCollection<out E>: Collection<E>

/**
 * A generic persistent collection of elements that supports adding and removing elements.
 *
 * Modification operations return new instances of the persistent collection with the modification applied.
 *
 * @param E the type of elements contained in the collection. The persistent collection is covariant on its element type.
 */
internal interface PersistentCollection<out E> : ImmutableCollection<E> {
    /**
     * Returns the result of adding the specified [element] to this collection.
     *
     * @returns a new persistent collection with the specified [element] added;
     * or this instance if this collection does not support duplicates and it already contains the element.
     */
    fun add(element: @UnsafeVariance E): PersistentCollection<E>

    /**
     * Returns the result of adding all elements of the specified [elements] collection to this collection.
     *
     * @return a new persistent collection with elements of the specified [elements] collection added;
     * or this instance if no modifications were made in the result of this operation.
     */
    fun addAll(elements: Collection<@UnsafeVariance E>): PersistentCollection<E>

    /**
     * Returns the result of removing a single appearance of the specified [element] from this collection.
     *
     * @return a new persistent collection with a single appearance of the specified [element] removed;
     * or this instance if there is no such element in this collection.
     */
    fun remove(element: @UnsafeVariance E): PersistentCollection<E>

    /**
     * Returns the result of removing all elements in this collection that are also
     * contained in the specified [elements] collection.
     *
     * @return a new persistent collection with elements in this collection that are also
     * contained in the specified [elements] collection removed;
     * or this instance if no modifications were made in the result of this operation.
     */
    fun removeAll(elements: Collection<@UnsafeVariance E>): PersistentCollection<E>

    /**
     * Returns the result of removing all elements in this collection that match the specified [predicate].
     *
     * @return a new persistent collection with elements matching the specified [predicate] removed;
     * or this instance if no elements match the predicate.
     */
    fun removeAll(predicate: (E) -> Boolean): PersistentCollection<E>

    /**
     * Returns all elements in this collection that are also
     * contained in the specified [elements] collection.
     *
     * @return a new persistent set with elements in this set that are also
     * contained in the specified [elements] collection;
     * or this instance if no modifications were made in the result of this operation.
     */
    fun retainAll(elements: Collection<@UnsafeVariance E>): PersistentCollection<E>

    /**
     * Returns an empty persistent collection.
     */
    fun clear(): PersistentCollection<E>

    /**
     * A generic builder of the persistent collection. Builder exposes its modification operations through the [MutableCollection] interface.
     *
     * Builders are reusable, that is [build] method can be called multiple times with modifications between these calls.
     * However, modifications applied do not affect previously built persistent collection instances.
     *
     * Builder is backed by the same underlying data structure as the persistent collection it was created from.
     * Thus, [builder] and [build] methods take constant time consisting of passing the backing storage to the
     * new builder and persistent collection instances, respectively.
     *
     * The builder tracks which nodes in the structure are shared with the persistent collection,
     * and which are owned by it exclusively. It owns the nodes it copied during modification
     * operations and avoids copying them on subsequent modifications.
     *
     * When [build] is called the builder forgets about all owned nodes it had created.
     */
    interface Builder<E>: MutableCollection<E> {
        /**
         * Returns a persistent collection with the same contents as this builder.
         *
         * This method can be called multiple times.
         *
         * If operations applied on this builder have caused no modifications:
         * - on the first call it returns the same persistent collection instance this builder was obtained from.
         * - on subsequent calls it returns the same previously returned persistent collection instance.
         */
        fun build(): PersistentCollection<E>
    }

    /**
     * Returns a new builder with the same contents as this collection.
     *
     * The builder can be used to efficiently perform multiple modification operations.
     */
    fun builder(): Builder<@UnsafeVariance E>
}
