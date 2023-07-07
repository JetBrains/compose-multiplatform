/*
 * Copyright 2020 The Android Source Project
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

package androidx.compose.ui.text.caches

private const val DEBUG = false
private const val TAG = "SimpleArrayMap"

/**
 * Attempt to spot concurrent modifications to this data structure.
 *
 * It's best-effort, but any time we can throw something more diagnostic than an
 * ArrayIndexOutOfBoundsException deep in the ArrayMap internals it's going to
 * save a lot of development time.
 *
 * Good times to look for CME include after any array allocations/copyOf calls
 * and at the end of functions that change size (put/remove/clear).
 */
private const val CONCURRENT_MODIFICATION_EXCEPTIONS = true

/**
 * The minimum amount by which the capacity of a ArrayMap will increase.
 * This is tuned to be relatively space-efficient.
 */
private const val BASE_SIZE = 4

/**
 * Copy of SimpleArrayMap from collection2 until dependency can be added correctly
 */
internal class SimpleArrayMap<K, V> {

    private var hashes: IntArray
    private var keyValues: Array<Any?>
    protected var _size = 0

    // Suppression necessary, see KT-43542.
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:kotlin.jvm.JvmName("size")
    val size: Int get() = _size

    protected fun indexOf(key: Any, hash: Int): Int {
        val N = _size

        // Important fast case: if nothing is in here, nothing to look for.
        if (N == 0) {
            return 0.inv()
        }
        val index: Int = hashes.binarySearchInternal(N, hash)

        // If the hash code wasn't found, then we have no entry for this key.
        if (index < 0) {
            return index
        }

        // If the key at the returned index matches, that's what we want.
        if (key == keyValues[index shl 1]) {
            return index
        }

        // Search for a matching key after the index.
        var end: Int
        end = index + 1
        while (end < N && hashes[end] == hash) {
            if (key == keyValues[end shl 1]) return end
            end++
        }

        // Search for a matching key before the index.
        var i = index - 1
        while (i >= 0 && hashes[i] == hash) {
            if (key == keyValues[i shl 1]) return i
            i--
        }

        // Key not found -- return negative value indicating where a
        // new entry for this key should go.  We use the end of the
        // hash chain to reduce the number of array entries that will
        // need to be copied when inserting.
        return end.inv()
    }

    protected fun indexOfNull(): Int {
        val N = _size

        // Important fast case: if nothing is in here, nothing to look for.
        if (N == 0) {
            return 0.inv()
        }
        val index: Int = hashes.binarySearchInternal(N, 0)

        // If the hash code wasn't found, then we have no entry for this key.
        if (index < 0) {
            return index
        }

        // If the key at the returned index matches, that's what we want.
        if (null == keyValues[index shl 1]) {
            return index
        }

        // Search for a matching key after the index.
        var end: Int
        end = index + 1
        while (end < N && hashes[end] == 0) {
            if (null == keyValues[end shl 1]) return end
            end++
        }

        // Search for a matching key before the index.
        var i = index - 1
        while (i >= 0 && hashes[i] == 0) {
            if (null == keyValues[i shl 1]) return i
            i--
        }

        // Key not found -- return negative value indicating where a
        // new entry for this key should go.  We use the end of the
        // hash chain to reduce the number of array entries that will
        // need to be copied when inserting.
        return end.inv()
    }

    /**
     * Create a new ArrayMap with a given initial capacity.
     */
    @kotlin.jvm.JvmOverloads
    constructor(capacity: Int = 0) {
        if (capacity == 0) {
            hashes = EMPTY_INTS
            keyValues = EMPTY_OBJECTS
        } else {
            hashes = IntArray(capacity)
            keyValues = arrayOfNulls<Any?>(capacity shl 1)
        }
        _size = 0
    }

    /**
     * Create a new ArrayMap with the mappings from the given ArrayMap.
     */
    constructor(map: SimpleArrayMap<K, V>?) : this() {
        if (map != null) {
            putAll(map)
        }
    }

    /**
     * Make the array map empty.  All storage is released.
     *
     * @throws ConcurrentModificationException if the map has been concurrently modified.
     */
    fun clear() {
        if (_size > 0) {
            hashes = EMPTY_INTS
            keyValues = EMPTY_OBJECTS
            _size = 0
        }
        if (CONCURRENT_MODIFICATION_EXCEPTIONS && _size > 0) {
            throw ConcurrentModificationException()
        }
    }

    /**
     * Ensure the array map can hold at least <var>minimumCapacity</var>
     * items.
     *
     * @throws ConcurrentModificationException if the map has been concurrently modified.
     */
    fun ensureCapacity(minimumCapacity: Int) {
        val osize = _size
        if (hashes.size < minimumCapacity) {
            hashes = hashes.copyOf(minimumCapacity)
            keyValues = keyValues.copyOf(minimumCapacity shl 1)
        }
        if (CONCURRENT_MODIFICATION_EXCEPTIONS && _size != osize) {
            throw ConcurrentModificationException()
        }
    }

    /**
     * Check whether a key exists in the array.
     *
     * @param key The key to search for.
     * @return Returns true if the key exists, else false.
     */
    fun containsKey(key: K): Boolean = indexOfKey(key) >= 0

    /**
     * Returns the index of a key in the set.
     *
     * @param key The key to search for.
     * @return Returns the index of the key if it exists, else a negative integer.
     */
    fun indexOfKey(key: Any?): Int =
        if (key == null) indexOfNull() else indexOf(key, key.hashCode())

    internal fun indexOfValue(value: V): Int {
        val N = _size shl 1
        val array = keyValues
        if (value == null) {
            var i = 1
            while (i < N) {
                if (array[i] == null) {
                    return i shr 1
                }
                i += 2
            }
        } else {
            var i = 1
            while (i < N) {
                if (value == array[i]) {
                    return i shr 1
                }
                i += 2
            }
        }
        return -1
    }

    /**
     * Check whether a value exists in the array.  This requires a linear search
     * through the entire array.
     *
     * @param value The value to search for.
     * @return Returns true if the value exists, else false.
     */
    fun containsValue(value: V): Boolean = indexOfValue(value) >= 0

    /**
     * Retrieve a value from the array.
     * @param key The key of the value to retrieve.
     * @return Returns the value associated with the given key,
     * or null if there is no such key.
     */
    @Suppress("UNCHECKED_CAST")
    operator fun get(key: K): V? {
        // TODO: Explain why re-impl instead of using getOrDefault()
        val index = indexOfKey(key)
        return if (index >= 0) keyValues[(index shl 1) + 1] as V else null
    }

    /**
     * Retrieve a value from the array, or [defaultValue] if there is no mapping for the key.
     * @param key The key of the value to retrieve.
     * @param defaultValue The default mapping of the key
     * @return Returns the value associated with the given key,
     * or [defaultValue] if there is no mapping for the key.
     */
    @Suppress("UNCHECKED_CAST")
    fun getOrDefault(key: K, defaultValue: V): V {
        val index = indexOfKey(key)
        return if (index >= 0) keyValues[(index shl 1) + 1] as V else defaultValue
    }

    /**
     * Return the key at the given index in the array.
     * @param index The desired index, must be between 0 and [size]-1.
     * @return Returns the key stored at the given index.
     */
    @Suppress("UNCHECKED_CAST")
    fun keyAt(index: Int): K = keyValues[index shl 1] as K

    /**
     * Return the value at the given index in the array.
     * @param index The desired index, must be between 0 and [size]-1.
     * @return Returns the value stored at the given index.
     */
    @Suppress("UNCHECKED_CAST")
    fun valueAt(index: Int): V = keyValues[(index shl 1) + 1] as V

    /**
     * Set the value at a given index in the array.
     * @param index The desired index, must be between 0 and [size]-1.
     * @param value The new value to store at this index.
     * @return Returns the previous value at the given index.
     */
    @Suppress("UNCHECKED_CAST")
    fun setValueAt(index: Int, value: V): V {
        val actualIndex = (index shl 1) + 1
        val old = keyValues[actualIndex] as V
        keyValues[actualIndex] = value
        return old
    }

    /**
     * Return true if the array map contains no items.
     */
    fun isEmpty(): Boolean = _size <= 0

    /**
     * Add a new value to the array map.
     * @param key The key under which to store the value.  <b>Must not be null.</b>  If
     * this key already exists in the array, its value will be replaced.
     * @param value The value to store for the given key.
     * @return Returns the old value that was stored for the given key, or null if there
     * was no such key.
     * @throws ConcurrentModificationException if the map has been concurrently modified.
     */
    @Suppress("UNCHECKED_CAST")
    fun put(key: K, value: V): V? {
        val osize = _size
        val hash: Int
        var index: Int

        if (key == null) {
            hash = 0
            index = indexOfNull()
        } else {
            hash = key.hashCode()
            index = indexOf(key, hash)
        }
        if (index >= 0) {
            index = (index shl 1) + 1
            val old = keyValues[index] as V
            keyValues[index] = value
            return old
        }

        index = index.inv()
        if (osize >= hashes.size) {
            val n = when {
                osize >= BASE_SIZE * 2 -> osize + (osize shr 1)
                osize >= BASE_SIZE -> BASE_SIZE * 2
                else -> BASE_SIZE
            }
            if (DEBUG) {
                println("$TAG put: grow from ${hashes.size} to $n")
            }
            hashes = hashes.copyOf(n)
            keyValues = keyValues.copyOf(n shl 1)

            if (CONCURRENT_MODIFICATION_EXCEPTIONS && osize != _size) {
                throw ConcurrentModificationException()
            }
        }

        if (index < osize) {
            if (DEBUG) {
                println("$TAG put: move $index-${osize - index} to ${index + 1}")
            }
            hashes.copyInto(hashes, index + 1, index, osize)
            keyValues.copyInto(keyValues, (index + 1) shl 1, index shl 1, _size shl 1)
        }

        if (CONCURRENT_MODIFICATION_EXCEPTIONS) {
            if (osize != _size || index >= hashes.size) {
                throw ConcurrentModificationException()
            }
        }

        hashes[index] = hash
        keyValues[index shl 1] = key
        keyValues[(index shl 1) + 1] = value
        _size++
        return null
    }

    /**
     * Perform a [put] of all key/value pairs in <var>array</var>
     * @param array The array whose contents are to be retrieved.
     */
    fun putAll(array: SimpleArrayMap<out K, out V>) {
        val N = array._size
        ensureCapacity(_size + N)
        if (_size == 0) {
            if (N > 0) {
                array.hashes.copyInto(hashes, 0, 0, N)
                array.keyValues.copyInto(keyValues, 0, 0, N shl 1)
                _size = N
            }
        } else {
            for (i in 0 until N) {
                put(array.keyAt(i), array.valueAt(i))
            }
        }
    }

    /**
     * Add a new value to the array map only if the key does not already have a value or it is
     * mapped to `null`.
     * @param key The key under which to store the value.
     * @param value The value to store for the given key.
     * @return Returns the value that was stored for the given key, or null if there
     * was no such key.
     */
    fun putIfAbsent(key: K, value: V): V? {
        var mapValue = get(key)
        if (mapValue == null) {
            mapValue = put(key, value)
        }
        return mapValue
    }

    /**
     * Remove an existing key from the array map.
     * @param key The key of the mapping to remove.
     * @return Returns the value that was stored under the key, or null if there
     * was no such key.
     */
    fun remove(key: K): V? {
        val index = indexOfKey(key)
        return if (index >= 0) removeAt(index) else null
    }

    /**
     * Remove an existing key from the array map only if it is currently mapped to [value].
     * @param key The key of the mapping to remove.
     * @param value The value expected to be mapped to the key.
     * @return Returns true if the mapping was removed.
     */
    fun remove(key: K, value: V): Boolean {
        val index = indexOfKey(key)
        if (index >= 0) {
            val mapValue = valueAt(index)
            if (value == mapValue) {
                removeAt(index)
                return true
            }
        }
        return false
    }

    /**
     * Remove the key/value mapping at the given index.
     * @param index The desired index, must be between 0 and [size]-1.
     * @return Returns the value that was stored at this index.
     * @throws ConcurrentModificationException if the map has been concurrently modified.
     */
    @Suppress("UNCHECKED_CAST")
    fun removeAt(index: Int): V? {
        val old = keyValues[(index shl 1) + 1]
        val osize = _size
        if (osize <= 1) {
            // Now empty.
            if (DEBUG) {
                println("$TAG remove: shrink from $hashes.size to 0")
            }
            clear()
        } else {
            val nsize = osize - 1
            if (hashes.size > BASE_SIZE * 2 && osize < hashes.size / 3) {
                // Shrunk enough to reduce size of arrays.  We don't allow it to
                // shrink smaller than (BASE_SIZE*2) to avoid flapping between
                // that and BASE_SIZE.
                val n =
                    if (osize > BASE_SIZE * 2) osize + (osize shr 1) else BASE_SIZE * 2
                if (DEBUG) {
                    println("$TAG remove: shrink from $hashes.size to $n")
                }
                val ohashes = hashes
                val oarray: Array<Any?> = keyValues

                hashes = IntArray(n)
                keyValues = arrayOfNulls<Any?>(n shl 1)

                if (CONCURRENT_MODIFICATION_EXCEPTIONS && osize != _size) {
                    throw ConcurrentModificationException()
                }
                if (index > 0) {
                    if (DEBUG) {
                        println("$TAG remove: copy from 0-$index to 0")
                    }
                    ohashes.copyInto(hashes, 0, 0, index)
                    oarray.copyInto(keyValues, 0, 0, index shl 1)
                }
                if (index < nsize) {
                    if (DEBUG) {
                        println("$TAG remove: copy from ${index + 1}-$nsize to $index")
                    }
                    ohashes.copyInto(hashes, index, index + 1, nsize + 1)
                    oarray.copyInto(keyValues, index shl 1, (index + 1) shl 1, (nsize + 1) shl 1)
                }
            } else {
                if (index < nsize) {
                    if (DEBUG) println("$TAG remove: move ${index + 1}-$nsize to $index")
                    hashes.copyInto(hashes, index, index + 1, nsize + 1)
                    keyValues.copyInto(keyValues, index shl 1, (index + 1) shl 1, (nsize + 1) shl 1)
                }
                keyValues[nsize shl 1] = null
                keyValues[(nsize shl 1) + 1] = null
            }
            if (CONCURRENT_MODIFICATION_EXCEPTIONS && osize != _size) {
                throw ConcurrentModificationException()
            }
            _size = nsize
        }
        return old as V
    }

    /**
     * Replace the mapping for [key] only if it is already mapped to a value.
     * @param key The key of the mapping to replace.
     * @param value The value to store for the given key.
     * @return Returns the previous mapped value or null.
     */
    fun replace(key: K, value: V): V? {
        val index = indexOfKey(key)
        return if (index >= 0) setValueAt(index, value) else null
    }

    /**
     * Replace the mapping for [key] only if it is already mapped to a value.
     *
     * @param key The key of the mapping to replace.
     * @param oldValue The value expected to be mapped to the key.
     * @param newValue The value to store for the given key.
     * @return Returns true if the value was replaced.
     */
    fun replace(key: K, oldValue: V, newValue: V): Boolean {
        val index = indexOfKey(key)
        if (index >= 0) {
            val mapValue = valueAt(index)
            if (mapValue === oldValue) {
                setValueAt(index, newValue)
                return true
            }
        }
        return false
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation returns false if the object is not a Map or
     * SimpleArrayMap, or if the maps have different sizes. Otherwise, for each
     * key in this map, values of both maps are compared. If the values for any
     * key are not equal, the method returns false, otherwise it returns true.
     */
    @Suppress("UNCHECKED_CAST")
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        try {
            if (other is SimpleArrayMap<*, *>) {
                val map = other as SimpleArrayMap<in Any?, in Any?>
                if (_size != map._size) {
                    return false
                }

                for (i in 0 until _size) {
                    val key = keyAt(i)
                    val mine: V? = valueAt(i)
                    // TODO use index-based ops for this
                    val theirs = map.get(key)
                    if (mine == null) {
                        if (theirs != null || !map.containsKey(key)) {
                            return false
                        }
                    } else if (mine != theirs) {
                        return false
                    }
                }
                return true
            } else if (other is Map<*, *>) {
                val map = other
                if (_size != map.size) {
                    return false
                }
                for (i in 0 until _size) {
                    val key = keyAt(i)
                    val mine: V? = valueAt(i)
                    val theirs = map[key]
                    if (mine == null) {
                        if (theirs != null || !map.containsKey(key)) {
                            return false
                        }
                    } else if (mine != theirs) {
                        return false
                    }
                }
                return true
            }
        } catch (ignored: NullPointerException) {
        } catch (ignored: ClassCastException) {
        }
        return false
    }

    /**
     * {@inheritDoc}
     */
    override fun hashCode(): Int {
        val hashes = hashes
        val array: Array<Any?> = keyValues
        var result = 0
        var i = 0
        var v = 1
        val s = _size
        while (i < s) {
            val value = array[v]
            result += hashes[i] xor (value?.hashCode() ?: 0)
            i++
            v += 2
        }
        return result
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation composes a string by iterating over its mappings. If
     * this map contains itself as a key or a value, the string "(this Map)"
     * will appear in its place.
     */
    override fun toString(): String {
        if (isEmpty()) {
            return "{}"
        }

        val buffer = StringBuilder(_size * 28)
        buffer.append('{')
        for (i in 0 until _size) {
            if (i > 0) {
                buffer.append(", ")
            }
            val key = keyAt(i)
            if (key !== this) {
                buffer.append(key)
            } else {
                buffer.append("(this Map)")
            }
            buffer.append('=')
            val value = valueAt(i)
            if (value !== this) {
                buffer.append(value)
            } else {
                buffer.append("(this Map)")
            }
        }
        buffer.append('}')
        return buffer.toString()
    }
}
