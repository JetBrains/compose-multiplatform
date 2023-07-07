/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.runtime.collection

internal actual class IntMap<E> actual constructor() {

    // TODO(o.k.): IntMap is supposed to avoid Integer boxing!
    // but after merging 1.4 changes we faced a crash in some iOS samples.
    // For reproducer, see dima.avdeev/reproduce-lazy-column-crash
    // The initial implementation (supplied from upstream) was not tested
    // and `binarySearch` and `set` functions require more attention.
    // For now, we rely on kotlin's MutableMap (with Integer boxing).
    private val backingMap = mutableMapOf<Int, E>()

    /**
     * True if this map contains key
     */
    actual operator fun contains(key: Int): Boolean {
        return backingMap.containsKey(key)
    }

    /**
     * Get [key] or null
     */
    actual operator fun get(key: Int): E? {
        return backingMap[key]
    }

    /**
     * Get [key] or [valueIfNotFound]
     */
    actual fun get(key: Int, valueIfAbsent: E): E {
        return backingMap.getOrElse(key) { valueIfAbsent }
    }

    /**
     * Set [key] to [value]
     */
    actual operator fun set(key: Int, value: E) {
        backingMap[key] = value
    }

    /**
     * Remove key, if it exists
     *
     * Otherwise no op
     */
    actual fun remove(key: Int) {
        backingMap.remove(key)
    }

    /**
     * Clear this map
     */
    actual fun clear() {
        backingMap.clear()
    }

    /**
     * Current count of (key, value) pairs
     */
    actual val size: Int
        get() = backingMap.size
}

private fun IntArray.binarySearch(size: Int, value: Int): Int {
    var min = 0
    var max = size - 1
    while (min <= max) {
        val mid = (min + max) / 2
        val midValue = this[mid]
        if (midValue < value) {
            min = mid + 1
        } else if (midValue > value) {
            max = mid - 1
        } else {
            return mid
        }
    }
    return -(min + 1)
}

private fun IntArray.insert(currentSize: Int, index: Int, value: Int): IntArray {
    if (currentSize + 1 <= size) {
        if (index < currentSize) {
            intArrayCopy(src = this, srcPos = index, dest = this, destPos = index + 1, length = currentSize - index)
            // System.arraycopy(this, index, this, index + 1, currentSize - index)
        }
        this[index] = value
        return this
    }

    val result = IntArray(size * 2)
    // System.arraycopy(this, 0, result, 0, index)
    intArrayCopy(src = this, srcPos = 0, dest = result, destPos = 0, length = index)
    result[index] = value
    intArrayCopy(src = this, srcPos = index, dest = result, destPos = index + 1, length = size - index)
    // System.arraycopy(this, index, result, index + 1, size - index)
    return result
}

private fun Array<Any?>.insert(currentSize: Int, index: Int, value: Any?): Array<Any?> {
    if (currentSize + 1 <= size) {
        if (index < currentSize) {
            arrayCopy(src = this, srcPos = index, dest = this, destPos = index + 1, length = currentSize - index)
            // System.arraycopy(this, index, this, index + 1, currentSize - index)
        }
        this[index] = value
        return this
    }

    val result = Array<Any?>(size * 2) { null }
    arrayCopy(src = this, srcPos = 0, dest = result, destPos = 0, length = index)
    //System.arraycopy(this, 0, result, 0, index)
    result[index] = value
    arrayCopy(src = this, srcPos = index, dest = result, destPos = index + 1, length = size - index)
    //System.arraycopy(this, index, result, index + 1, size - index)
    return result
}


private fun intArrayCopy(
    src: IntArray, srcPos: Int,
    dest: IntArray, destPos: Int,
    length: Int
) {
    src.copyInto(dest, destinationOffset = destPos, startIndex = srcPos, endIndex = srcPos + length)
}

private fun arrayCopy(
    src: Array<Any?>, srcPos: Int,
    dest: Array<Any?>, destPos: Int,
    length: Int
) {
    src.copyInto(dest, destinationOffset = destPos, startIndex = srcPos, endIndex = srcPos + length)
}
