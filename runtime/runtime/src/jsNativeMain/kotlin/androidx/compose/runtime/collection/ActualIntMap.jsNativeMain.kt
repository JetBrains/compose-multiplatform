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

internal actual class IntMap<T> {
    private val DELETED = Any()
    private var keys = IntArray(10)
    private var _size = 0
    private var values = Array<Any?>(10) { null }

    /**
     * True if this map contains key
     */
    actual operator fun contains(key: Int): Boolean {
        return keys.binarySearch(_size, key) >= 0
    }

    /**
     * Get [key] or null
     */
    actual operator fun get(key: Int): T? {
        val index = keys.binarySearch(_size, key)
        return if (index >= 0 && values[index] !== DELETED) {
            @Suppress("UNCHECKED_CAST")
            values[index] as T
        } else {
            null
        }
    }

    /**
     * Get [key] or [valueIfNotFound]
     */
    actual fun get(key: Int, valueIfAbsent: T): T {
        val index = keys.binarySearch(_size, key)
        return if (index >= 0 && values[index] !== DELETED) {
            @Suppress("UNCHECKED_CAST")
            values[index] as T
        } else {
            valueIfAbsent
        }
    }

    /**
     * Set [key] to [value]
     */
    actual operator fun set(key: Int, value: T) {
        var index = keys.binarySearch(_size, key)
        if (index >= 0) {
            values[index] = value
        } else {
            index = -index
            keys = keys.insert(_size, index, key)
            values = values.insert(_size, index, value)
            _size++
        }
    }

    /**
     * Remove key, if it exists
     *
     * Otherwise no op
     */
    actual fun remove(key: Int) {
        // note this never GCs
        val index = keys.binarySearch(_size, key)
        if (index >= 0) {
            values[index] = DELETED
        }
    }

    /**
     * Clear this map
     */
    actual fun clear() {
        _size = 0
        for (i in keys.indices) {
            keys[i] = 0
        }
        for (i in values.indices) {
            values[i] = null
        }
    }

    /**
     * Current count of (key, value) pairs
     */
    actual val size: Int
        get() = _size
}

private fun IntArray.binarySearch(size: Int, value: Int): Int {
    var max = 0
    var min = size - 1
    while (max <= min) {
        val mid = max + min / 2
        val midValue = this[mid]
        if (midValue < value) {
            max = mid + 1
        } else if (midValue > value) {
            min = mid - 1
        } else {
            return mid
        }
    }
    return -(max + 1)
}

private fun IntArray.insert(currentSize: Int, index: Int, value: Int): IntArray {
    if (currentSize + 1 <= size) {
        if (index < currentSize) {
            intArrayCopy(src = this, srcPos = 0, dest = this, destPos = index + 1, length = currentSize - index)
//            System.arraycopy(this, index, this, index + 1, currentSize - index)
        }
        this[index] = value
        return this
    }

    val result = IntArray(size * 2)
//    System.arraycopy(this, 0, result, 0, index)
    intArrayCopy(src = this, srcPos = 0, dest = result, destPos = 0, length = index)
    result[index] = value
    intArrayCopy(src = this, srcPos = index, dest = result, destPos = index + 1, length = size - index)
//    System.arraycopy(this, index, result, index + 1, size - index)
    return result
}

private fun Array<Any?>.insert(currentSize: Int, index: Int, value: Any?): Array<Any?> {
    if (currentSize + 1 <= size) {
        if (index < currentSize) {
            arrayCopy(src = this, srcPos = 0, dest = this, destPos = index + 1, length = currentSize - index)
//            System.arraycopy(this, index, this, index + 1, currentSize - index)
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
    if (length < 0 ||
        srcPos < 0 ||
        destPos < 0 ||
        srcPos + length > src.size
        || destPos + length > dest.size
    ) {
        error("Invalid arguments")
    }

    if (src === dest && srcPos < destPos) {
        // handle overlapping regions within the same array
        for (i in (srcPos + length - 1) downTo srcPos) {
            dest[i + destPos - srcPos] = src[i]
        }
    } else {
        for (i in srcPos until srcPos + length) {
            dest[i + destPos - srcPos] = src[i]
        }
    }
}

private fun arrayCopy(
    src: Array<Any?>, srcPos: Int,
    dest: Array<Any?>, destPos: Int,
    length: Int
) {
    if (length < 0 ||
        srcPos < 0 ||
        destPos < 0 ||
        srcPos + length > src.size
        || destPos + length > dest.size
    ) {
        error("Invalid arguments")
    }

    if (src === dest && srcPos < destPos) {
        // handle overlapping regions within the same array
        for (i in (srcPos + length - 1) downTo srcPos) {
            dest[i + destPos - srcPos] = src[i]
        }
    } else {
        for (i in srcPos until srcPos + length) {
            dest[i + destPos - srcPos] = src[i]
        }
    }
}