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

package androidx.compose.runtime.snapshots

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Iterates through a [List] using the index and calls [action] for each item.
 * This does not allocate an iterator like [Iterable.forEach].
 */
@OptIn(ExperimentalContracts::class)
internal inline fun <T> List<T>.fastForEach(action: (T) -> Unit) {
    contract { callsInPlace(action) }
    for (index in indices) {
        val item = get(index)
        action(item)
    }
}

/**
 * Returns a [Set] of all elements.
 *
 * The returned set preserves the element iteration order of the original collection.
 */
internal fun <T> List<T>.fastToSet(): Set<T> = HashSet<T>(size).also { set ->
    fastForEach { item -> set.add(item) }
}

/**
 * Iterates through a [List] using the index and calls [action] for each item.
 * This does not allocate an iterator like [Iterable.forEachIndexed].
 */
@OptIn(ExperimentalContracts::class)
internal inline fun <T> List<T>.fastForEachIndexed(action: (Int, T) -> Unit) {
    contract { callsInPlace(action) }
    for (index in indices) {
        val item = get(index)
        action(index, item)
    }
}

/**
 * Returns a list containing the results of applying the given [transform] function
 * to each element in the original collection.
 */
@OptIn(ExperimentalContracts::class)
internal inline fun <T, R> List<T>.fastMap(transform: (T) -> R): List<R> {
    contract { callsInPlace(transform) }
    val target = ArrayList<R>(size)
    fastForEach {
        target += transform(it)
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

/**
 * Returns a list containing the results of applying the given [transform] function
 * to each element in the original collection.
 */
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
 * Returns a list containing only elements matching the given [predicate].
 * @param [predicate] function that takes the index of an element and the element itself
 * and returns the result of predicate evaluation on the element.
 */
@OptIn(ExperimentalContracts::class)
internal inline fun <T> List<T>.fastFilterIndexed(predicate: (index: Int, T) -> Boolean): List<T> {
    contract { callsInPlace(predicate) }
    val target = ArrayList<T>(size)
    fastForEachIndexed { index, e ->
        if (predicate(index, e)) target += e
    }
    return target
}
