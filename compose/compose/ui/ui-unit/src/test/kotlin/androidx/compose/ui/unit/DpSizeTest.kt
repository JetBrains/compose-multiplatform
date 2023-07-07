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

class DpSizeTest {
    @Test
    fun constructor() {
        val size = DpSize(width = 5.dp, height = 10.dp)
        assertEquals(5.dp, size.width)
        assertEquals(10.dp, size.height)
    }

    @Test
    fun dpSizeTimesInt() {
        assertEquals(DpSize(10.dp, 10.dp), DpSize(2.dp, 2.dp) * 5)
        assertEquals(DpSize(10.dp, 10.dp), 5 * DpSize(2.dp, 2.dp))
    }

    @Test
    fun dpSizeTimesFloat() {
        assertEquals(DpSize(10.dp, 10.dp), DpSize(2.dp, 2.dp) * 5f)
        assertEquals(DpSize(10.dp, 10.dp), 5f * DpSize(2.dp, 2.dp))
    }

    @Test
    fun dpSizeDivInt() {
        assertEquals(DpSize(10.dp, 10.dp), DpSize(40.dp, 40.dp) / 4)
    }

    @Test
    fun dpSizeDivFloat() {
        assertEquals(DpSize(10.dp, 10.dp), DpSize(40.dp, 40.dp) / 4f)
    }

    @Test
    fun sizeCenter() {
        val size = DpSize(width = 10.dp, height = 20.dp)
        assertEquals(DpOffset(5.dp, 10.dp), size.center)
    }

    @Test
    fun components() {
        val size = DpSize(width = 10.dp, height = 20.dp)
        val (w, h) = size
        assertEquals(10.dp, w)
        assertEquals(20.dp, h)
    }

    @Test
    fun lerp() {
        assertEquals(DpSize(1.dp, 2.dp), lerp(DpSize(1.dp, 2.dp), DpSize(10.dp, 5.dp), 0f))
        assertEquals(DpSize(10.dp, 5.dp), lerp(DpSize(1.dp, 2.dp), DpSize(10.dp, 5.dp), 1f))
        assertEquals(DpSize(0.dp, 0.dp), lerp(DpSize(-10.dp, -5.dp), DpSize(10.dp, 5.dp), 0.5f))
    }

    @Test
    fun dpRectSize() {
        assertEquals(DpSize(10.dp, 5.dp), DpRect(2.dp, 3.dp, 12.dp, 8.dp).size)
    }

    @Test
    fun testIsSpecified() {
        assertFalse(DpSize.Unspecified.isSpecified)
        assertTrue(DpSize(1.dp, 1.dp).isSpecified)
    }

    @Test
    fun testIsUnspecified() {
        assertTrue(DpSize.Unspecified.isUnspecified)
        assertFalse(DpSize(1.dp, 1.dp).isUnspecified)
    }

    @Test
    fun testTakeOrElseTrue() {
        assertTrue(DpSize(1.dp, 1.dp).takeOrElse { DpSize.Unspecified }.isSpecified)
    }

    @Test
    fun testTakeOrElseFalse() {
        assertTrue(DpSize.Unspecified.takeOrElse { DpSize(1.dp, 1.dp) }.isSpecified)
    }

    @Test
    fun testToString() {
        assertEquals("1.0.dp x 1.0.dp", DpSize(1.dp, 1.dp).toString())
    }

    @Test
    fun testUnspecifiedToString() {
        assertEquals("DpSize.Unspecified", DpSize.Unspecified.toString())
    }
}