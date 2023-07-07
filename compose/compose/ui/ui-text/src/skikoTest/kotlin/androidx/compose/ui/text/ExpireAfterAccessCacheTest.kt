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

package androidx.compose.ui.text

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ExpireAfterAccessCacheTest {
    private var time: Long = 0
    private val cache = ExpireAfterAccessCache<String, String>(
        expireAfterNanos = 1.secondsToNanos(),
        currentNanos = { time }
    )

    @BeforeTest
    fun before() {
        time = 0
    }

    @Test
    fun singleKey() {
        assertEquals("v1_1", cache.get("k1") { "v1_1" })
        assertEquals(setOf("k1"), cache.map.keys)
        assertEquals(setOf("k1"), cache.accessTime.keys)
        assertEquals(0, cache.accessTime["k1"])

        time += 10.millisToNanos()

        assertEquals("v1_1", cache.get("k1") { "v1_2" })
        assertEquals(setOf("k1"), cache.map.keys)
        assertEquals(setOf("k1"), cache.accessTime.keys)
        assertEquals(time, cache.accessTime["k1"])

        time += 2.secondsToNanos()

        assertEquals("v1_1", cache.get("k1") { "v1_3" })
        assertEquals(setOf("k1"), cache.map.keys)
        assertEquals(setOf("k1"), cache.accessTime.keys)
        assertEquals(time, cache.accessTime["k1"])
    }

    @Test
    fun manyKeys() {
        assertEquals("v1_1", cache.get("k1") { "v1_1" })
        assertEquals(setOf("k1"), cache.map.keys)
        assertEquals(cache.map.keys, cache.accessTime.keys)
        assertEquals(0, cache.accessTime["k1"])

        time += 10.millisToNanos()

        assertEquals("v2_1", cache.get("k2") { "v2_1" })
        assertEquals(setOf("k1", "k2"), cache.map.keys)
        assertEquals(cache.map.keys, cache.accessTime.keys)
        assertEquals(0, cache.accessTime["k1"])
        assertEquals(time, cache.accessTime["k2"])

        time += 10.millisToNanos()

        assertEquals("v2_1", cache.get("k2") { "v2_2" })
        assertEquals("v3_1", cache.get("k3") { "v3_1" })
        assertEquals(setOf("k1", "k2", "k3"), cache.map.keys)
        assertEquals(cache.map.keys, cache.accessTime.keys)
        assertEquals(0, cache.accessTime["k1"])
        assertEquals(time, cache.accessTime["k2"])
        assertEquals(time, cache.accessTime["k3"])

        time += 2.secondsToNanos()

        assertEquals("v2_1", cache.get("k2") { "v2_3" })
        assertEquals(setOf("k2"), cache.map.keys)
        assertEquals(setOf("k2"), cache.accessTime.keys)
        assertEquals(time, cache.accessTime["k2"])
    }
}

private const val nanosPerMillis = 1_000_000L
private const val nanosPerSecond = 1_000_000_000L
private fun Int.millisToNanos(): Long = this * nanosPerMillis
private fun Int.secondsToNanos(): Long = this * nanosPerSecond
