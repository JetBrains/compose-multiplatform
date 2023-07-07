/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.compose.ui.geometry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MutableRectTest {
    @Test
    fun accessors() {
        val r = MutableRect(1f, 3f, 5f, 9f)
        assertEquals(1f, r.left, 0f)
        assertEquals(3f, r.top, 0f)
        assertEquals(5f, r.right, 0f)
        assertEquals(9f, r.bottom, 0f)
        assertEquals(4f, r.width, 0f)
        assertEquals(6f, r.height, 0f)
        assertEquals(Size(4f, 6f), r.size)
    }

    @Test
    fun empty() {
        val r = MutableRect(1f, 3f, 5f, 9f)
        assertFalse(r.isEmpty)
        r.left = 5f
        assertTrue(r.isEmpty)
        r.left = 1f
        r.bottom = 3f
        assertTrue(r.isEmpty)
    }

    @Test
    fun contains() {
        val r = MutableRect(1f, 3f, 5f, 9f)
        assertTrue(Offset(1f, 3f) in r)
        assertTrue(Offset(3f, 3f) in r)
        assertFalse(Offset(5f, 3f) in r)
        assertTrue(Offset(1f, 6f) in r)
        assertTrue(Offset(3f, 6f) in r)
        assertFalse(Offset(5f, 6f) in r)
        assertFalse(Offset(1f, 9f) in r)
        assertFalse(Offset(3f, 9f) in r)
        assertFalse(Offset(5f, 9f) in r)
        assertFalse(Offset(0f, 0f) in r)
        assertFalse(Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY) in r)
        assertFalse(Offset(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY) in r)
    }

    @Test
    fun intersect() {
        val r = MutableRect(0f, 0f, 100f, 100f)
        r.intersect(50f, 50f, 200f, 200f)
        assertEquals(50f, r.left, 0f)
        assertEquals(50f, r.top, 0f)
        assertEquals(100f, r.right, 0f)
        assertEquals(100f, r.bottom, 0f)

        val r2 = MutableRect(50f, 50f, 200f, 200f)
        r2.intersect(0f, 0f, 100f, 100f)
        assertEquals(50f, r2.left, 0f)
        assertEquals(50f, r2.top, 0f)
        assertEquals(100f, r2.right, 0f)
        assertEquals(100f, r2.bottom, 0f)
    }

    @Test
    fun set() {
        val r = MutableRect(0f, 0f, 100f, 100f)
        r.set(10f, 3f, 20f, 6f)
        assertEquals(10f, r.left, 0f)
        assertEquals(3f, r.top, 0f)
        assertEquals(20f, r.right, 0f)
        assertEquals(6f, r.bottom, 0f)
    }
}