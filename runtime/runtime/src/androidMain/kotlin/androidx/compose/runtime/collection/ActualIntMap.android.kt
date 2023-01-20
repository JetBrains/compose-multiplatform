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

import android.util.SparseArray

internal actual class IntMap<E> private constructor(
    private val sparseArray: android.util.SparseArray<E>
) {
    constructor(initialCapacity: Int = 10) : this(SparseArray(initialCapacity))

    /**
     * True if this map contains key
     */
    actual operator fun contains(key: Int): Boolean = sparseArray.indexOfKey(key) >= 0

    /**
     * Get [key] or null
     */
    actual operator fun get(key: Int): E? = sparseArray[key]

    /**
     * Get [key] or [valueIfAbsent]
     */
    actual fun get(key: Int, valueIfAbsent: E): E = sparseArray.get(key, valueIfAbsent)

    /**
     * Set [key] to [value]
     */
    actual operator fun set(key: Int, value: E) = sparseArray.put(key, value)

    /**
     * Remove key, if it exists
     *
     * Otherwise no op
     */
    actual fun remove(key: Int) = sparseArray.remove(key)

    /**
     * Clear this map
     */
    actual fun clear() = sparseArray.clear()

    /**
     * Current count of values
     */
    actual val size: Int
        get() = sparseArray.size()
}

// filename is not allowed if only a class is declared
private val allowFilename = 0