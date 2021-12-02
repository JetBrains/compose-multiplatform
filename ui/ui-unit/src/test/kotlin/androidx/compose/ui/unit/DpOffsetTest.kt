/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.unit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DpOffsetTest {
    @Test
    fun constructor() {
        val size = DpOffset(x = 5.dp, y = 10.dp)
        assertEquals(5.dp, size.x)
        assertEquals(10.dp, size.y)
    }

    @Test
    fun copy() {
        val position = DpOffset(12.dp, 27.dp)
        assertEquals(position, position.copy())
    }

    @Test
    fun copyOverwritesX() {
        val position = DpOffset(15.dp, 32.dp)
        val copy = position.copy(x = 59.dp)
        assertEquals(59.dp, copy.x)
        assertEquals(32.dp, copy.y)
    }

    @Test
    fun copyOverwritesY() {
        val position = DpOffset(19.dp, 42.dp)
        val copy = position.copy(y = 67.dp)
        assertEquals(19.dp, copy.x)
        assertEquals(67.dp, copy.y)
    }

    @Test
    fun dpOffsetPlusDpOffset() {
        val a = DpOffset(3.dp, 10.dp)
        val b = DpOffset(5.dp, 8.dp)
        assertEquals(DpOffset(8.dp, 18.dp), a + b)
        assertEquals(DpOffset(8.dp, 18.dp), b + a)
    }

    @Test
    fun dpOffsetMinusDpOffset() {
        val a = DpOffset(3.dp, 10.dp)
        val b = DpOffset(5.dp, 8.dp)
        assertEquals(DpOffset((-2).dp, 2.dp), a - b)
        assertEquals(DpOffset(2.dp, (-2).dp), b - a)
    }

    @Test
    fun lerp() {
        val a = DpOffset(3.dp, 10.dp)
        val b = DpOffset(5.dp, 8.dp)
        assertEquals(DpOffset(4.dp, 9.dp), lerp(a, b, 0.5f))
        assertEquals(DpOffset(3.dp, 10.dp), lerp(a, b, 0f))
        assertEquals(DpOffset(5.dp, 8.dp), lerp(a, b, 1f))
    }

    @Test
    fun testIsSpecified() {
        assertFalse(DpOffset.Unspecified.isSpecified)
        assertTrue(DpOffset(1.dp, 1.dp).isSpecified)
    }

    @Test
    fun testIsUnspecified() {
        assertTrue(DpOffset.Unspecified.isUnspecified)
        assertFalse(DpOffset(1.dp, 1.dp).isUnspecified)
    }

    @Test
    fun testTakeOrElseTrue() {
        assertTrue(DpOffset(1.dp, 1.dp).takeOrElse { DpOffset.Unspecified }.isSpecified)
    }

    @Test
    fun testTakeOrElseFalse() {
        assertTrue(DpOffset.Unspecified.takeOrElse { DpOffset(1.dp, 1.dp) }.isSpecified)
    }

    @Test
    fun testToString() {
        assertEquals("(1.0.dp, 1.0.dp)", DpOffset(1.dp, 1.dp).toString())
    }

    @Test
    fun testUnspecifiedToString() {
        assertEquals("DpOffset.Unspecified", DpOffset.Unspecified.toString())
    }
}