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

package androidx.compose.ui.text.caches

import androidx.compose.ui.text.createSynchronizedObject
import androidx.compose.ui.text.synchronized

import kotlin.jvm.JvmName

/**
 * Copy from collection2 until that library can be added as a dependency
 */
internal open class LruCache<K, V> {

    private val monitor = createSynchronizedObject()
    private val map: HashMap<K, V>
    private val keySet: LinkedHashSet<K>

    /** Size of this cache in units. Not necessarily the number of elements. */
    @get:JvmName("size")
    public var size: Int = 0
        /**
         * For caches that do not override [sizeOf], this returns the number
         * of entries in the cache. For all other caches, this returns the sum of
         * the sizes of the entries in this cache.
         */
        get() = synchronizedValue { return field }
        private set

    private var maxSize = 0

    private var putCount = 0
    private var createCount = 0
    private var evictionCount = 0
    private var hitCount = 0
    private var missCount = 0

    /**
     * @param maxSize for caches that do not override [sizeOf], this is
     *     the maximum number of entries in the cache. For all other caches,
     *     this is the maximum sum of the sizes of the entries in this cache.
     */
        constructor(maxSize: Int) {
        require(maxSize > 0) { "maxSize <= 0" }
        this.maxSize = maxSize
        map = HashMap<K, V>(0, 0.75f)
        keySet = LinkedHashSet<K>()
    }

    /**
     * Sets the size of the cache.
     *
     * @param maxSize The new maximum size.
     */
    open fun resize(maxSize: Int) {
        require(maxSize > 0) { "maxSize <= 0" }

        synchronized(monitor) {
            this.maxSize = maxSize
        }
        trimToSize(maxSize)
    }

    /**
     * Returns the value for [key] if it exists in the cache or can be
     * created by [create]. If a value was returned, it is moved to the
     * head of the queue. This returns `null` if a value is not cached and cannot
     * be created.
     */
        fun get(key: K): V? {
        var mapValue: V? = null

        synchronized(monitor) {
            mapValue = map.get(key)
            if (mapValue != null) {
                // Push the key to the end of the keySet as the cached entry gets hit.
                keySet.remove(key)
                keySet.add(key)
                hitCount++
                return mapValue
            } else {
                missCount++
            }
        }

        val createdValue: V? = create(key)
        if (createdValue == null) {
            return null
        }

        synchronized(monitor) {
            createCount++
            val previousValue: V? = map.put(key, createdValue)
            // Push the key to the end of the keySet as the cached entry gets hit.
            keySet.remove(key)
            keySet.add(key)
            if (previousValue != null) {
                // There was a conflict so undo that last put
                map.put(key, previousValue)
                mapValue = previousValue
            } else {
                size += safeSizeOf(key, createdValue)
            }
        }

        if (mapValue != null) {
            entryRemoved(false, key, createdValue, mapValue)
            return mapValue
        } else {
            trimToSize(maxSize)
            return createdValue
        }
    }

    /**
     * Caches [value] for [key]. The value is moved to the head of
     * the queue.
     *
     * @return the previous value mapped by [key].
     * @throws NullPointerException if [key] or [value] is null
     */
        fun put(key: K, value: V): V? {
        // Must throw NPE for JVM interop contract.
        if (key == null || value == null) {
            throw NullPointerException()
        }

        var previous: V? = null
        synchronized(monitor) {
            putCount++
            size += safeSizeOf(key, value)
            previous = map.put(key, value)
            if (previous != null) {
                size -= safeSizeOf(key, previous!!)
            }
            if (keySet.contains(key)) {
                keySet.remove(key)
            }
            keySet.add(key)
        }

        if (previous != null) {
            entryRemoved(false, key, previous!!, value)
        }

        trimToSize(maxSize)
        return previous
    }

    /**
     * Remove the eldest entries until the total of remaining entries is at or
     * below the requested size.
     *
     * @param maxSize the maximum size of the cache before returning. May be -1
     *            to evict even 0-sized elements.
     * @throws IllegalStateException
     */
    open fun trimToSize(maxSize: Int) {
        while (true) {
            var key: K? = null
            var value: V? = null

            synchronized(monitor) {
                if (size < 0 ||
                    (map.isEmpty() && size != 0) ||
                    (map.isEmpty() != keySet.isEmpty())
                ) {
                    throw IllegalStateException("map/keySet size inconsistency")
                }

                if (size > maxSize && !map.isEmpty()) {
                    key = keySet.first()
                    value = map.get(key) ?: throw IllegalStateException(
                        "inconsistent " +
                            "state"
                    )
                    map.remove(key)
                    keySet.remove(key)
                    size -= safeSizeOf(key!!, value!!)
                    evictionCount++
                }
            }

            if (key == null && value == null) {
                break
            } else {
                entryRemoved(true, key!!, value!!, null)
            }
        }
    }

    /**
     * Removes the entry for [key] if it exists.
     *
     * @return the previous value mapped by [key].
     * @throws NullPointerException if [key] is null from a JVM caller.
     */
    fun remove(key: K): V? {
        // Must throw NPE for JVM interop contract.
        if (key == null) {
            throw NullPointerException()
        }

        var previous: V? = null
        synchronized(monitor) {
            previous = map.remove(key)
            keySet.remove(key)
            if (previous != null) {
                size -= safeSizeOf(key, previous!!)
            }
        }

        if (previous != null) {
            entryRemoved(false, key, previous!!, null)
        }

        return previous
    }

    /**
     * Called for entries that have been evicted or removed. This method is
     * invoked when a value is evicted to make space, removed by a call to
     * [remove], or replaced by a call to [put]. The default
     * implementation does nothing.
     *
     * The method is called without synchronization: other threads may
     * access the cache while this method is executing.
     *
     * @param evicted `true` if the entry is being removed to make space, `false`
     *     if the removal was caused by a [put] or [remove].
     * @param newValue the new value for [key], if it exists. If non-null,
     *     this removal was caused by a [put]. Otherwise it was caused by
     *     an eviction or a [remove].
     */
    protected open fun entryRemoved(evicted: Boolean, key: K, oldValue: V, newValue: V?) {
    }

    /**
     * Called after a cache miss to compute a value for the corresponding key.
     * Returns the computed value or null if no value can be computed. The
     * default implementation returns null.
     *
     * The method is called without synchronization: other threads may
     * access the cache while this method is executing.
     *
     * If a value for [key] exists in the cache when this method
     * returns, the created value will be released with [entryRemoved]
     * and discarded. This can occur when multiple threads request the same key
     * at the same time (causing multiple values to be created), or when one
     * thread calls [put] while another is creating a value for the same
     * key.
     */
        protected open fun create(key: K): V? = null

    private fun safeSizeOf(key: K, value: V): Int {
        val result = sizeOf(key, value)
        check(result >= 0) { "Negative size: $key=$value" }
        return result
    }

    /**
     * Returns the size of the entry for [key] and [value] in
     * user-defined units.  The default implementation returns 1 so that size
     * is the number of entries and max size is the maximum number of entries.
     *
     * An entry's size must not change while it is in the cache.
     */
    protected open fun sizeOf(key: K, value: V) = 1

    /**
     * Clear the cache, calling [entryRemoved] on each removed entry.
     */
    fun evictAll() {
        trimToSize(-1) // -1 will evict 0-sized elements
    }

    /**
     * For caches that do not override [sizeOf], this returns the maximum
     * number of entries in the cache. For all other caches, this returns the
     * maximum sum of the sizes of the entries in this cache.
     */
    fun maxSize(): Int = synchronizedValue { maxSize }

    /**
     * Returns the number of times [get] returned a value that was
     * already present in the cache.
     */
    fun hitCount(): Int = synchronizedValue { hitCount }

    /**
     * Returns the number of times [get] returned null or required a new
     * value to be created.
     */
    fun missCount(): Int = synchronizedValue { missCount }

    /**
     * Returns the number of times [create] returned a value.
     */
    fun createCount(): Int = synchronizedValue { createCount }

    /**
     * Returns the number of times [put] was called.
     */
    fun putCount(): Int = synchronizedValue { putCount }

    /**
     * Returns the number of values that have been evicted.
     */
    fun evictionCount(): Int = synchronizedValue { evictionCount }

    /**
     * Returns a copy of the current contents of the cache, ordered from least
     * recently accessed to most recently accessed.
     */
    fun snapshot(): Map<K, V> {
        synchronized(monitor) {
            val linkedHashMap = LinkedHashMap<K, V>()
            for (key in keySet) {
                linkedHashMap.put(key, map.get(key)!!)
            }
            return linkedHashMap
        }
    }

    override fun toString(): String {
        synchronized(monitor) {
            val accesses = hitCount + missCount
            val hitPercent = if (accesses != 0) 100 * hitCount / accesses else 0
            return "LruCache[maxSize=$maxSize,hits=$hitCount,misses=$missCount," +
                "hitRate=$hitPercent%]"
        }
    }

    internal inline fun <R> synchronizedValue(block: () -> R): R {
        return synchronized(monitor, block)
    }
}
