/*
 * Copyright 2020-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.resources

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AsyncCacheTest {

    private lateinit var cache: AsyncCache<String, String>

    @BeforeTest
    fun setup() {
        cache = AsyncCache()
    }

    @Test
    fun `test cache stores and retrieves value`() = runTest {
        val key = "testKey"
        val expectedValue = "Hello, World!"

        val value = cache.getOrLoad(key) { expectedValue }

        assertEquals(expectedValue, value)
    }

    @Test
    fun `test cache returns same instance for same key`() = runTest {
        val key = "testKey"
        var loadCount = 0

        val firstLoad = cache.getOrLoad(key) {
            loadCount++
            "Hello"
        }
        val secondLoad = cache.getOrLoad(key) { "NewValue" }

        assertEquals("Hello", firstLoad)
        assertEquals("Hello", secondLoad)
        assertEquals(1, loadCount) // Ensures the load function runs only once
    }

    @Test
    fun `test concurrent access to cache`() = runTest {
        val key = "testKey"
        var loadCount = 0

        coroutineScope {
            repeat(10) {
                launch {
                    cache.getOrLoad(key) {
                        delay(100) // Simulate work
                        loadCount++
                        "Concurrent Value"
                    }
                }
            }
        }

        assertEquals(1, loadCount) // Ensures only one load operation happened
    }

    @Test
    fun `test cache invalidation on clear`() = runTest {
        val key = "testKey"

        cache.getOrLoad(key) { "InitialValue" }
        cache.clear()

        val newValue = cache.getOrLoad(key) { "NewValue" }

        assertEquals("NewValue", newValue)
    }
}