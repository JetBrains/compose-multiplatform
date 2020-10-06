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

package androidx.compose.ui.util

/**
 * Iterates through a [List] using the index and calls [action] for each item.
 * This does not allocate an iterator like [Iterable.forEach].
 */
inline fun <T> List<T>.fastForEach(action: (T) -> Unit) {
    for (index in indices) {
        val item = get(index)
        action(item)
    }
}

/**
 * Iterates through a [List] using the index and calls [action] for each item.
 * This does not allocate an iterator like [Iterable.forEachIndexed].
 */
inline fun <T> List<T>.fastForEachIndexed(action: (Int, T) -> Unit) {
    for (index in indices) {
        val item = get(index)
        action(index, item)
    }
}

/**
 * Returns `true` if all elements match the given [predicate].
 */
inline fun <T> List<T>.fastAll(predicate: (T) -> Boolean): Boolean {
    fastForEach { if (!predicate(it)) return false }
    return true
}

/**
 * Returns `true` if at least one element matches the given [predicate].
 */
inline fun <T> List<T>.fastAny(predicate: (T) -> Boolean): Boolean {
    fastForEach { if (predicate(it)) return true }
    return false
}

/**
 * Returns the first value that [predicate] returns `true` for or `null` if nothing matches.
 */
inline fun <T> List<T>.fastFirstOrNull(predicate: (T) -> Boolean): T? {
    fastForEach { if (predicate(it)) return it }
    return null
}

/**
 * Returns the sum of all values produced by [selector] function applied to each element in the
 * list.
 */
inline fun <T> List<T>.fastSumBy(selector: (T) -> Int): Int {
    var sum = 0
    fastForEach { element ->
        sum += selector(element)
    }
    return sum
}

/**
 * Returns a list containing the results of applying the given [transform] function
 * to each element in the original collection.
 */
inline fun <T, R> List<T>.fastMap(transform: (T) -> R): List<R> {
    val target = ArrayList<R>(size)
    fastForEach {
        target += transform(it)
    }
    return target
}

/**
 * Returns the first element yielding the largest value of the given function or `null` if there
 * are no elements.
 */
inline fun <T, R : Comparable<R>> List<T>.fastMaxBy(selector: (T) -> R): T? {
    if (isEmpty()) return null
    var maxElem = get(0)
    var maxValue = selector(maxElem)
    for (i in 1..lastIndex) {
        val e = get(i)
        val v = selector(e)
        if (maxValue < v) {
            maxElem = e
            maxValue = v
        }
    }
    return maxElem
}

/**
 * Applies the given [transform] function to each element of the original collection
 * and appends the results to the given [destination].
 */
inline fun <T, R, C : MutableCollection<in R>> List<T>.fastMapTo(
    destination: C,
    transform: (T) -> R
): C {
    fastForEach { item ->
        destination.add(transform(item))
    }
    return destination
}
