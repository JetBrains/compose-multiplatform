/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package androidx.compose.runtime.external.kotlinx.collections.immutable.adapters

import androidx.compose.runtime.external.kotlinx.collections.immutable.*


/*
 These classes allow to expose read-only collection as immutable, if it's actually immutable one
 Use with caution: wrapping mutable collection as immutable is a contract violation of the latter.
 */

internal open class ImmutableCollectionAdapter<E>(private val impl: Collection<E>) : ImmutableCollection<E>, Collection<E> by impl {
    override fun equals(other: Any?): Boolean = impl.equals(other)
    override fun hashCode(): Int = impl.hashCode()
    override fun toString(): String = impl.toString()
}


internal class ImmutableListAdapter<E>(private val impl: List<E>) : ImmutableList<E>, List<E> by impl {

    override fun subList(fromIndex: Int, toIndex: Int): ImmutableList<E> = ImmutableListAdapter(impl.subList(fromIndex, toIndex))

    override fun equals(other: Any?): Boolean = impl.equals(other)
    override fun hashCode(): Int = impl.hashCode()
    override fun toString(): String = impl.toString()
}


internal class ImmutableSetAdapter<E>(impl: Set<E>) : ImmutableSet<E>, ImmutableCollectionAdapter<E>(impl)


internal class ImmutableMapAdapter<K, out V>(private val impl: Map<K, V>) : ImmutableMap<K, V>, Map<K, V> by impl {
    // TODO: Lazy initialize these properties?
    override val keys: ImmutableSet<K> = ImmutableSetAdapter(impl.keys)
    override val values: ImmutableCollection<V> = ImmutableCollectionAdapter(impl.values)
    override val entries: ImmutableSet<Map.Entry<K, V>> = ImmutableSetAdapter(impl.entries)

    override fun equals(other: Any?): Boolean = impl.equals(other)
    override fun hashCode(): Int = impl.hashCode()
    override fun toString(): String = impl.toString()
}