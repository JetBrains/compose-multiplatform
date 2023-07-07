/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui

import androidx.compose.ui.util.fastForEach
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

// TODO: remove these when we can add new APIs to ui-util outside of beta cycle

/**
 * Returns a list containing the results of applying the given [transform] function
 * to each pair of two adjacent elements in this collection.
 *
 * The returned list is empty if this collection contains less than two elements.
 *
 * **Do not use for collections that come from public APIs**, since they may not support random
 * access in an efficient way, and this method may actually be a lot slower. Only use for
 * collections that are created by code we control and are known to support random access.
 */
@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
internal inline fun <T, R> List<T>.fastZipWithNext(transform: (T, T) -> R): List<R> {
    contract { callsInPlace(transform) }
    if (size == 0 || size == 1) return emptyList()
    val result = mutableListOf<R>()
    var current = get(0)
    // `until` as we don't want to invoke this for the last element, since that won't have a `next`
    for (i in 0 until lastIndex) {
        val next = get(i + 1)
        result.add(transform(current, next))
        current = next
    }
    return result
}

/**
 * Accumulates value starting with the first element and applying [operation] from left to right
 * to current accumulator value and each element.
 *
 * Throws an exception if this collection is empty. If the collection can be empty in an expected
 * way, please use [reduceOrNull] instead. It returns `null` when its receiver is empty.
 *
 * **Do not use for collections that come from public APIs**, since they may not support random
 * access in an efficient way, and this method may actually be a lot slower. Only use for
 * collections that are created by code we control and are known to support random access.
 *
 * @param [operation] function that takes current accumulator value and an element,
 * and calculates the next accumulator value.
 */
@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
internal inline fun <S, T : S> List<T>.fastReduce(operation: (acc: S, T) -> S): S {
    contract { callsInPlace(operation) }
    if (isEmpty()) throw UnsupportedOperationException("Empty collection can't be reduced.")
    var accumulator: S = first()
    for (i in 1..lastIndex) {
        accumulator = operation(accumulator, get(i))
    }
    return accumulator
}

/**
 * Returns a [Map] containing key-value pairs provided by [transform] function
 * applied to elements of the given collection.
 *
 * If any of two pairs would have the same key the last one gets added to the map.
 *
 * The returned map preserves the entry iteration order of the original collection.
 *
 * **Do not use for collections that come from public APIs**, since they may not support random
 * access in an efficient way, and this method may actually be a lot slower. Only use for
 * collections that are created by code we control and are known to support random access.
 */
@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
internal inline fun <T, K, V> List<T>.fastAssociate(transform: (T) -> Pair<K, V>): Map<K, V> {
    contract { callsInPlace(transform) }
    val target = LinkedHashMap<K, V>(size)
    fastForEach { e ->
        target += transform(e)
    }
    return target
}

/**
 * Returns a list of values built from the elements of `this` collection and the [other] collection with the same index
 * using the provided [transform] function applied to each pair of elements.
 * The returned list has length of the shortest collection.
 *
 * **Do not use for collections that come from public APIs**, since they may not support random
 * access in an efficient way, and this method may actually be a lot slower. Only use for
 * collections that are created by code we control and are known to support random access.
 */
@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
internal inline fun <T, R, V> List<T>.fastZip(
    other: List<R>,
    transform: (a: T, b: R) -> V
): List<V> {
    contract { callsInPlace(transform) }
    val minSize = minOf(size, other.size)
    val target = ArrayList<V>(minSize)
    for (i in 0 until minSize) {
        target += (transform(get(i), other[i]))
    }
    return target
}

/**
 * Returns a list containing the results of applying the given [transform] function
 * to each element in the original collection.
 *
 * **Do not use for collections that come from public APIs**, since they may not support random
 * access in an efficient way, and this method may actually be a lot slower. Only use for
 * collections that are created by code we control and are known to support random access.
 */
@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
internal inline fun <T, R> List<T>.fastMapNotNull(transform: (T) -> R?): List<R> {
    contract { callsInPlace(transform) }
    val target = ArrayList<R>(size)
    fastForEach { e ->
        transform(e)?.let { target += it }
    }
    return target
}

/**
 * Creates a string from all the elements separated using [separator] and using the given [prefix]
 * and [postfix] if supplied.
 *
 * If the collection could be huge, you can specify a non-negative value of [limit], in which case
 * only the first [limit] elements will be appended, followed by the [truncated] string (which
 * defaults to "...").
 *
 * **Do not use for collections that come from public APIs**, since they may not support random
 * access in an efficient way, and this method may actually be a lot slower. Only use for
 * collections that are created by code we control and are known to support random access.
 */
internal fun <T> List<T>.fastJoinToString(
    separator: CharSequence = ", ",
    prefix: CharSequence = "",
    postfix: CharSequence = "",
    limit: Int = -1,
    truncated: CharSequence = "...",
    transform: ((T) -> CharSequence)? = null
): String {
    return fastJoinTo(StringBuilder(), separator, prefix, postfix, limit, truncated, transform)
        .toString()
}

/**
 * Appends the string from all the elements separated using [separator] and using the given
 * [prefix] and [postfix] if supplied.
 *
 * If the collection could be huge, you can specify a non-negative value of [limit], in which
 * case only the first [limit] elements will be appended, followed by the [truncated] string
 * (which defaults to "...").
 *
 * **Do not use for collections that come from public APIs**, since they may not support random
 * access in an efficient way, and this method may actually be a lot slower. Only use for
 * collections that are created by code we control and are known to support random access.
 */
private fun <T, A : Appendable> List<T>.fastJoinTo(
    buffer: A,
    separator: CharSequence = ", ",
    prefix: CharSequence = "",
    postfix: CharSequence = "",
    limit: Int = -1,
    truncated: CharSequence = "...",
    transform: ((T) -> CharSequence)? = null
): A {
    buffer.append(prefix)
    var count = 0
    for (index in indices) {
        val element = get(index)
        if (++count > 1) buffer.append(separator)
        if (limit < 0 || count <= limit) {
            buffer.appendElement(element, transform)
        } else break
    }
    if (limit >= 0 && count > limit) buffer.append(truncated)
    buffer.append(postfix)
    return buffer
}

/**
 * Copied from Appendable.kt
 */
private fun <T> Appendable.appendElement(element: T, transform: ((T) -> CharSequence)?) {
    when {
        transform != null -> append(transform(element))
        element is CharSequence? -> append(element)
        element is Char -> append(element)
        else -> append(element.toString())
    }
}
