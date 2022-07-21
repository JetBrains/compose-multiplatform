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
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.test.assertTrue

@RunWith(JUnit4::class)
class RectTest {

    companion object {
        private const val DELTA = 0.01f
    }

    @Test
    fun `rect accessors`() {
        val r = Rect(1.0f, 3.0f, 5.0f, 7.0f)
        assertEquals(1.0f, r.left, DELTA)
        assertEquals(3.0f, r.top, DELTA)
        assertEquals(5.0f, r.right, DELTA)
        assertEquals(7.0f, r.bottom, DELTA)
    }

    @Test
    fun `rect created by width and height`() {
        val r = Rect(Offset(1.0f, 3.0f), Size(5.0f, 7.0f))
        assertEquals(1.0f, r.left, DELTA)
        assertEquals(3.0f, r.top, DELTA)
        assertEquals(6.0f, r.right, DELTA)
        assertEquals(10.0f, r.bottom, DELTA)
    }

    @Test
    fun `rect intersection`() {
        val r1 = Rect(0.0f, 0.0f, 100.0f, 100.0f)
        val r2 = Rect(50.0f, 50.0f, 200.0f, 200.0f)
        val r3 = r1.intersect(r2)
        assertEquals(50.0f, r3.left, DELTA)
        assertEquals(50.0f, r3.top, DELTA)
        assertEquals(100.0f, r3.right, DELTA)
        assertEquals(100.0f, r3.bottom, DELTA)
        val r4 = r2.intersect(r1)
        assertEquals(r3, r4)
    }

    @Test
    fun `rect width`() {
        assertEquals(210f, Rect(70f, 10f, 280f, 300f).width)
    }

    @Test
    fun `rect height`() {
        assertEquals(290f, Rect(70f, 10f, 280f, 300f).height)
    }

    @Test
    fun `rect size`() {
        assertEquals(
            Size(210f, 290f),
            Rect(70f, 10f, 280f, 300f).size
        )
    }

    @Test
    fun `rect infinite`() {
        assertTrue(Rect(Float.POSITIVE_INFINITY, 10f, 200f, 500f).isInfinite)
        assertTrue(Rect(10f, Float.POSITIVE_INFINITY, 200f, 500f).isInfinite)
        assertTrue(Rect(10f, 200f, Float.POSITIVE_INFINITY, 500f).isInfinite)
        assertTrue(Rect(10f, 200f, 500f, Float.POSITIVE_INFINITY).isInfinite)

        assertFalse(Rect(0f, 1f, 2f, 3f).isInfinite)
    }

    @Test
    fun `rect finite`() {
        assertTrue(Rect(0f, 1f, 2f, 3f).isFinite)
        assertFalse(Rect(0f, 1f, 2f, Float.POSITIVE_INFINITY).isFinite)
    }

    @Test
    fun `rect isEmpty`() {
        assertTrue(Rect(0f, 0f, 0f, 10f).isEmpty)
        assertTrue(Rect(1f, 0f, 0f, 10f).isEmpty)
        assertTrue(Rect(0f, 1f, 10f, 0f).isEmpty)
        assertTrue(Rect(0f, 1f, 10f, 1f).isEmpty)

        assertFalse(Rect(0f, 1f, 2f, 3f).isEmpty)
    }

    @Test
    fun `rect translate offset`() {
        val shifted = Rect(0f, 5f, 10f, 15f).translate(Offset(10f, 15f))
        assertEquals(Rect(10f, 20f, 20f, 30f), shifted)
    }

    @Test
    fun `rect translate`() {
        val translated = Rect(0f, 5f, 10f, 15f).translate(10f, 15f)
        assertEquals(Rect(10f, 20f, 20f, 30f), translated)
    }

    @Test
    fun `rect inflate`() {
        val inflated = Rect(5f, 10f, 10f, 20f).inflate(5f)
        assertEquals(Rect(0f, 5f, 15f, 25f), inflated)
    }

    @Test
    fun `rect deflate`() {
        val deflated = Rect(0f, 5f, 15f, 25f).deflate(5f)
        assertEquals(Rect(5f, 10f, 10f, 20f), deflated)
    }

    @Test
    fun `rect intersect`() {
        val intersected = Rect(0f, 0f, 20f, 20f).intersect(
            Rect(10f, 10f, 30f, 30f)
        )
        assertEquals(Rect(10f, 10f, 20f, 20f), intersected)
    }

    @Test
    fun `rect overlap`() {
        val rect1 = Rect(0f, 5f, 10f, 15f)
        val rect2 = Rect(5f, 10f, 15f, 20f)
        assertTrue(rect1.overlaps(rect2))
        assertTrue(rect2.overlaps(rect1))
    }

    @Test
    fun `rect does not overlap`() {
        val rect1 = Rect(0f, 5f, 10f, 15f)
        val rect2 = Rect(10f, 5f, 20f, 15f)
        assertFalse(rect1.overlaps(rect2))
        assertFalse(rect2.overlaps(rect1))
    }

    @Test
    fun `rect minDimension`() {
        val rect = Rect(0f, 5f, 100f, 25f)
        assertEquals(20f, rect.minDimension)
    }

    @Test
    fun `rect maxDimension`() {
        val rect = Rect(0f, 5f, 100f, 25f)
        assertEquals(100f, rect.maxDimension)
    }

    @Test
    fun `rect topLeft`() {
        val rect = Rect(27f, 38f, 100f, 200f)
        assertEquals(Offset(27f, 38f), rect.topLeft)
    }

    @Test
    fun `rect topCenter`() {
        val rect = Rect(100f, 15f, 200f, 300f)
        assertEquals(Offset(150f, 15f), rect.topCenter)
    }

    @Test
    fun `rect topRight`() {
        val rect = Rect(100f, 15f, 200f, 300f)
        assertEquals(Offset(200f, 15f), rect.topRight)
    }

    @Test
    fun `rect centerLeft`() {
        val rect = Rect(100f, 10f, 200f, 300f)
        assertEquals(Offset(100f, 155f), rect.centerLeft)
    }

    @Test
    fun `rect center`() {
        val rect = Rect(100f, 10f, 200f, 300f)
        assertEquals(Offset(150f, 155f), rect.center)
    }

    @Test
    fun `rect centerRight`() {
        val rect = Rect(100f, 10f, 200f, 300f)
        assertEquals(Offset(200f, 155f), rect.centerRight)
    }

    @Test
    fun `rect bottomLeft`() {
        val rect = Rect(100f, 10f, 200f, 300f)
        assertEquals(Offset(100f, 300f), rect.bottomLeft)
    }

    @Test
    fun `rect bottomCenter`() {
        val rect = Rect(100f, 10f, 200f, 300f)
        assertEquals(Offset(150f, 300f), rect.bottomCenter)
    }

    @Test
    fun `rect bottomRight`() {
        val rect = Rect(100f, 10f, 200f, 300f)
        assertEquals(Offset(200f, 300f), rect.bottomRight)
    }

    @Test
    fun `rect contains`() {
        val rect = Rect(100f, 10f, 200f, 300f)
        val offset = Offset(177f, 288f)
        assertTrue(offset in rect)
    }

    @Test
    fun `rect does not contain`() {
        val rect = Rect(100f, 10f, 200f, 300f)
        val offset1 = Offset(201f, 150f)
        assertFalse(offset1 in rect)

        val offset2 = Offset(200f, 301f)
        assertFalse(offset2 in rect)
    }

    @Test
    fun `rect from offset and size`() {
        val offset = Offset(220f, 300f)
        val size = Size(80f, 200f)
        assertEquals(Rect(220f, 300f, 300f, 500f), Rect(offset, size))
    }

    @Test
    fun `rect from topleft and bottomRight`() {
        val offset1 = Offset(27f, 38f)
        val offset2 = Offset(130f, 280f)
        assertEquals(Rect(27f, 38f, 130f, 280f), Rect(offset1, offset2))
    }

    @Test
    fun `rect from center and radius`() {
        val offset = Offset(100f, 50f)
        val radius = 25f
        assertEquals(Rect(75f, 25f, 125f, 75f), Rect(offset, radius))
    }

    @Test
    fun `rect lerp`() {
        val rect1 = Rect(0f, 0f, 100f, 100f)
        val rect2 = Rect(50f, 50f, 200f, 200f)

        assertEquals(Rect(25f, 25f, 150f, 150f), lerp(rect1, rect2, 0.5f))
    }
}