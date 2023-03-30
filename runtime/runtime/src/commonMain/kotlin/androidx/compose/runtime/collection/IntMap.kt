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

/**
 * Map of (int) -> Element that attempts to avoid boxing.
 */
internal expect class IntMap<E>() {
    /**
     * True if this map contains key
     */
    operator fun contains(key: Int): Boolean

    /**
     * Get [key] or null
     */
    operator fun get(key: Int): E?

    /**
     * Get [key] or [valueIfAbsent]
     */
    fun get(key: Int, valueIfAbsent: E): E

    /**
     * Sets [key] to [value]
     */
    operator fun set(key: Int, value: E)

    /**
     * Remove [key], if it exists
     *
     * Otherwise no op
     */
    fun remove(key: Int)

    /**
     * Clear this map
     */
    fun clear()

    /**
     * Current count of key value pairs.
     */
    val size: Int
}