/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.ui.tooling.preview.rpc.utils

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class RingBufferTest {
    private fun numbers(size: Int): IntArray = IntArray(size) { it }
    private fun testBuffer() = RingBuffer<Int>(4)

    @Test
    fun empty() {
        val it = testBuffer().iterator()
        assertFalse(it.hasNext())
    }

    @Test
    fun addSmall() {
        val buf = testBuffer()
        val expected = numbers(buf.maxSize / 2)
        buf.addAll(expected.asIterable())
        assertArrayEquals(expected, buf.toList().toIntArray())
    }

    @Test
    fun addMedium() {
        val buf = testBuffer()
        val expected = numbers(buf.maxSize + buf.maxSize / 2)
        buf.addAll(expected.asIterable())
        checkAllEquals(expected.takeLast(buf.maxSize), buf.toList())
    }

    @Test
    fun addBig() {
        val buf = testBuffer()
        val expected = numbers(buf.maxSize * 3)
        buf.addAll(expected.asIterable())
        checkAllEquals(expected.takeLast(buf.maxSize), buf.toList())
    }

    @Test
    fun testClear() {
        val buf = testBuffer()
        val expected = numbers(buf.maxSize * 3)
        buf.addAll(expected.asIterable())
        buf.clear()
        checkAllEquals(emptyList(), buf.toList())
    }

    private fun <T> checkAllEquals(expected: Collection<T>, actual: Collection<T>) {
        val expectedString = expected.joinToString(", ", prefix = "[", postfix = "]")
        val actualString = actual.joinToString(", ", prefix = "[", postfix = "]")
        assertEquals(expectedString, actualString)
    }
}