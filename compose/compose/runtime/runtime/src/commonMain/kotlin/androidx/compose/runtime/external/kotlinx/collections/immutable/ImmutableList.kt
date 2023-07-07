/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package androidx.compose.runtime.external.kotlinx.collections.immutable

import androidx.compose.runtime.external.kotlinx.collections.immutable.internal.ListImplementation

/**
 * A generic immutable ordered collection of elements. Methods in this interface support only read-only access to the immutable list.
 *
 * Modification operations are supported through the [PersistentList] interface.
 *
 * Implementors of this interface take responsibility to be immutable.
 * Once constructed they must contain the same elements in the same order.
 *
 * @param E the type of elements contained in the list. The immutable list is covariant on its element type.
 */
internal interface ImmutableList<out E> : List<E>, ImmutableCollection<E> {

    /**
     * Returns a view of the portion of this list between the specified [fromIndex] (inclusive) and [toIndex] (exclusive).
     *
     * The returned list is backed by this list.
     *
     * @throws IndexOutOfBoundsException if [fromIndex] is less than zero or [toIndex] is greater than the size of this list.
     * @throws IllegalArgumentException if [fromIndex] is greater than [toIndex].
     */
    override fun subList(fromIndex: Int, toIndex: Int): ImmutableList<E> = SubList(this, fromIndex, toIndex)

    private class SubList<E>(private val source: ImmutableList<E>, private val fromIndex: Int, private val toIndex: Int) : ImmutableList<E>, AbstractList<E>() {
        private var _size: Int = 0

        init {
            ListImplementation.checkRangeIndexes(fromIndex, toIndex, source.size)
            this._size = toIndex - fromIndex
        }

        override fun get(index: Int): E {
            ListImplementation.checkElementIndex(index, _size)

            return source[fromIndex + index]
        }

        override val size: Int get() = _size

        override fun subList(fromIndex: Int, toIndex: Int): ImmutableList<E> {
            ListImplementation.checkRangeIndexes(fromIndex, toIndex, this._size)
            return SubList(source, this.fromIndex + fromIndex, this.fromIndex + toIndex)
        }
    }
}

/**
 * A generic persistent ordered collection of elements that supports adding and removing elements.
 *
 * Modification operations return new instances of the persistent list with the modification applied.
 *
 * @param E the type of elements contained in the list. The persistent list is covariant on its element type.
 */
internal interface PersistentList<out E> : ImmutableList<E>, PersistentCollection<E> {
    /**
     * Returns a new persistent list with the specified [element] appended.
     */
    override fun add(element: @UnsafeVariance E): PersistentList<E>

    /**
     * Returns the result of appending all elements of the specified [elements] collection to this list.
     *
     * The elements are appended in the order they appear in the specified collection.
     *
     * @return a new persistent list with elements of the specified [elements] collection appended;
     * or this instance if the specified collection is empty.
     */
    override fun addAll(elements: Collection<@UnsafeVariance E>): PersistentList<E> // = super<ImmutableCollection>.addAll(elements) as ImmutableList

    /**
     * Returns the result of removing the first appearance of the specified [element] from this list.
     *
     * @return a new persistent list with the first appearance of the specified [element] removed;
     * or this instance if there is no such element in this list.
     */
    override fun remove(element: @UnsafeVariance E): PersistentList<E>

    /**
     * Returns the result of removing all elements in this list that are also
     * contained in the specified [elements] collection.
     *
     * @return a new persistent list with elements in this list that are also
     * contained in the specified [elements] collection removed;
     * or this instance if no modifications were made in the result of this operation.
     */
    override fun removeAll(elements: Collection<@UnsafeVariance E>): PersistentList<E>

    /**
     * Returns the result of removing all elements in this list that match the specified [predicate].
     *
     * @return a new persistent list with elements matching the specified [predicate] removed;
     * or this instance if no elements match the predicate.
     */
    override fun removeAll(predicate: (E) -> Boolean): PersistentList<E>

    /**
     * Returns all elements in this list that are also
     * contained in the specified [elements] collection.
     *
     * @return a new persistent list with elements in this list that are also
     * contained in the specified [elements] collection;
     * or this instance if no modifications were made in the result of this operation.
     */
    override fun retainAll(elements: Collection<@UnsafeVariance E>): PersistentList<E>

    /**
     * Returns an empty persistent list.
     */
    override fun clear(): PersistentList<E>


    /**
     * Returns the result of inserting the specified [c] collection at the specified [index].
     *
     * @return a new persistent list with the specified [c] collection inserted at the specified [index];
     * or this instance if the specified collection is empty.
     *
     * @throws IndexOutOfBoundsException if [index] is out of bounds of this list.
     */
    fun addAll(index: Int, c: Collection<@UnsafeVariance E>): PersistentList<E> // = builder().apply { addAll(index, c.toList()) }.build()

    /**
     * Returns a new persistent list with the element at the specified [index] replaced with the specified [element].
     *
     * @throws IndexOutOfBoundsException if [index] is out of bounds of this list.
     */
    fun set(index: Int, element: @UnsafeVariance E): PersistentList<E>

    /**
     * Returns a new persistent list with the specified [element] inserted at the specified [index].
     *
     * @throws IndexOutOfBoundsException if [index] is out of bounds of this list.
     */
    fun add(index: Int, element: @UnsafeVariance E): PersistentList<E>

    /**
     * Returns a new persistent list with the element at the specified [index] removed.
     *
     * @throws IndexOutOfBoundsException if [index] is out of bounds of this list.
     */
    fun removeAt(index: Int): PersistentList<E>

    /**
     * A generic builder of the persistent list. Builder exposes its modification operations through the [MutableList] interface.
     *
     * Builders are reusable, that is [build] method can be called multiple times with modifications between these calls.
     * However, modifications applied do not affect previously built persistent list instances.
     *
     * Builder is backed by the same underlying data structure as the persistent list it was created from.
     * Thus, [builder] and [build] methods take constant time consisting of passing the backing storage to the
     * new builder and persistent list instances, respectively.
     *
     * The builder tracks which nodes in the structure are shared with the persistent list,
     * and which are owned by it exclusively. It owns the nodes it copied during modification
     * operations and avoids copying them on subsequent modifications.
     *
     * When [build] is called the builder forgets about all owned nodes it had created.
     */
    interface Builder<E>: MutableList<E>, PersistentCollection.Builder<E> {
        override fun build(): PersistentList<E>
    }

    override fun builder(): Builder<@UnsafeVariance E>
}