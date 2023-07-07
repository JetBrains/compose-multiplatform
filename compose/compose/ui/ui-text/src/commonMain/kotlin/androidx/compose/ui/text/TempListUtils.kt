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

package androidx.compose.ui.text

import androidx.compose.ui.util.fastForEach
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

// TODO: remove these when we can add new APIs to ui-util outside of beta cycle

/**
 * Returns a list containing only elements matching the given [predicate].
 *
 * **Do not use for collections that come from public APIs**, since they may not support random
 * access in an efficient way, and this method may actually be a lot slower. Only use for
 * collections that are created by code we control and are known to support random access.
 */
@Suppress("BanInlineOptIn") // Treat Kotlin Contracts as non-experimental.
@OptIn(ExperimentalContracts::class)
internal inline fun <T> List<T>.fastFilter(predicate: (T) -> Boolean): List<T> {
    contract { callsInPlace(predicate) }
    val target = ArrayList<T>(size)
    fastForEach {
        if (predicate(it)) target += (it)
    }
    return target
}

/**
 * Returns a list containing only elements from the given collection
 * having distinct keys returned by the given [selector] function.
 *
 * The elements in the resulting list are in the same order as they were in the source collection.
 *
 * **Do not use for collections that come from public APIs**, since they may not support random
 * access in an efficient way, and this method may actually be a lot slower. Only use for
 * collections that are created by code we control and are known to support random access.
 */
@Suppress("BanInlineOptIn") // Treat Kotlin Contracts as non-experimental.
@OptIn(ExperimentalContracts::class)
internal inline fun <T, K> List<T>.fastDistinctBy(selector: (T) -> K): List<T> {
    contract { callsInPlace(selector) }
    val set = HashSet<K>(size)
    val target = ArrayList<T>(size)
    fastForEach { e ->
        val key = selector(e)
        if (set.add(key)) target += e
    }
    return target
}

/**
 * Returns the first element yielding the largest value of the given function or `null` if there
 * are no elements.
 *
 * **Do not use for collections that come from public APIs**, since they may not support random
 * access in an efficient way, and this method may actually be a lot slower. Only use for
 * collections that are created by code we control and are known to support random access.
 */
@Suppress("BanInlineOptIn") // Treat Kotlin Contracts as non-experimental.
@OptIn(ExperimentalContracts::class)
internal inline fun <T, R : Comparable<R>> List<T>.fastMinByOrNull(selector: (T) -> R): T? {
    contract { callsInPlace(selector) }
    if (isEmpty()) return null
    var minElem = get(0)
    var minValue = selector(minElem)
    for (i in 1..lastIndex) {
        val e = get(i)
        val v = selector(e)
        if (minValue > v) {
            minElem = e
            minValue = v
        }
    }
    return minElem
}

/**
 * Accumulates value starting with [initial] value and applying [operation] from left to right
 * to current accumulator value and each element.
 *
 * Returns the specified [initial] value if the collection is empty.
 *
 * **Do not use for collections that come from public APIs**, since they may not support random
 * access in an efficient way, and this method may actually be a lot slower. Only use for
 * collections that are created by code we control and are known to support random access.
 *
 * @param [operation] function that takes current accumulator value and an element, and calculates
 * the next accumulator value.
 */
@Suppress("BanInlineOptIn") // Treat Kotlin Contracts as non-experimental.
@OptIn(ExperimentalContracts::class)
internal inline fun <T, R> List<T>.fastFold(initial: R, operation: (acc: R, T) -> R): R {
    contract { callsInPlace(operation) }
    var accumulator = initial
    fastForEach { e ->
        accumulator = operation(accumulator, e)
    }
    return accumulator
}

/**
 * Returns a single list of all elements yielded from results of [transform] function being invoked
 * on each element of original collection.
 *
 * **Do not use for collections that come from public APIs**, since they may not support random
 * access in an efficient way, and this method may actually be a lot slower. Only use for
 * collections that are created by code we control and are known to support random access.
 */
@Suppress("BanInlineOptIn") // Treat Kotlin Contracts as non-experimental.
@OptIn(ExperimentalContracts::class)
internal inline fun <T, R> List<T>.fastFlatMap(transform: (T) -> Iterable<R>): List<R> {
    contract { callsInPlace(transform) }
    val target = ArrayList<R>(size)
    fastForEach { e ->
        val list = transform(e)
        target.addAll(list)
    }
    return target
}

/**
 * Returns a list containing all elements not matching the given [predicate].
 *
 * **Do not use for collections that come from public APIs**, since they may not support random
 * access in an efficient way, and this method may actually be a lot slower. Only use for
 * collections that are created by code we control and are known to support random access.
 */
@Suppress("BanInlineOptIn") // Treat Kotlin Contracts as non-experimental.
@OptIn(ExperimentalContracts::class)
internal inline fun <T> List<T>.fastFilterNot(predicate: (T) -> Boolean): List<T> {
    contract { callsInPlace(predicate) }
    val target = ArrayList<T>(size)
    fastForEach {
        if (!predicate(it)) target += (it)
    }
    return target
}

/**
 * Returns a list containing all elements that are not null
 *
 * **Do not use for collections that come from public APIs**, since they may not support random
 * access in an efficient way, and this method may actually be a lot slower. Only use for
 * collections that are created by code we control and are known to support random access.
 */
@OptIn(ExperimentalContracts::class)
internal fun <T : Any> List<T?>.fastFilterNotNull(): List<T> {
    val target = ArrayList<T>(size)
    fastForEach {
        if ((it) != null) target += (it)
    }
    return target
}

/**
 * Returns a list containing the first elements satisfying the given [predicate].
 *
 * **Do not use for collections that come from public APIs**, since they may not support random
 * access in an efficient way, and this method may actually be a lot slower. Only use for
 * collections that are created by code we control and are known to support random access.
 */
@Suppress("BanInlineOptIn") // Treat Kotlin Contracts as non-experimental.
@OptIn(ExperimentalContracts::class)
internal inline fun <T> List<T>.fastTakeWhile(predicate: (T) -> Boolean): List<T> {
    contract { callsInPlace(predicate) }
    val target = ArrayList<T>(size)
    for (i in indices) {
        val item = get(i)
        if (!predicate(item))
            break
        target += item
    }
    return target
}

/**
 * Returns a list containing all elements except first [n] elements.
 *
 * **Do not use for collections that come from public APIs**, since they may not support random
 * access in an efficient way, and this method may actually be a lot slower. Only use for
 * collections that are created by code we control and are known to support random access.
 *
 * @throws IllegalArgumentException if [n] is negative.
 */
internal fun <T> List<T>.fastDrop(n: Int): List<T> {
    require(n >= 0) { "Requested element count $n is less than zero." }
    if (n == 0) {
        return this
    }
    val resultSize = size - n
    if (resultSize <= 0) {
        return emptyList()
    }
    if (resultSize == 1) {
        return listOf(last())
    }
    val target = ArrayList<T>(resultSize)
    for (index in n until size) {
        target += get(index)
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
