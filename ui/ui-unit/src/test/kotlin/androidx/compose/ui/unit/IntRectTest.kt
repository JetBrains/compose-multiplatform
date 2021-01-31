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

package androidx.compose.ui.unit

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class IntRectTest {
    @Test
    fun `rect created by width and height`() {
        val r = IntRect(IntOffset(1, 3), IntSize(5, 7))
        Assert.assertEquals(1, r.left)
        Assert.assertEquals(3, r.top)
        Assert.assertEquals(6, r.right)
        Assert.assertEquals(10, r.bottom)
    }

    @Test
    fun `rect intersection`() {
        val r1 = IntRect(0, 0, 100, 100)
        val r2 = IntRect(50, 50, 200, 200)
        val r3 = r1.intersect(r2)
        Assert.assertEquals(50, r3.left)
        Assert.assertEquals(50, r3.top)
        Assert.assertEquals(100, r3.right)
        Assert.assertEquals(100, r3.bottom)
        val r4 = r2.intersect(r1)
        Assert.assertEquals(r3, r4)
    }

    @Test
    fun `rect width`() {
        Assert.assertEquals(210, IntRect(70, 10, 280, 300).width)
    }

    @Test
    fun `rect height`() {
        Assert.assertEquals(290, IntRect(70, 10, 280, 300).height)
    }

    @Test
    fun `rect size`() {
        Assert.assertEquals(
            IntSize(210, 290),
            IntRect(70, 10, 280, 300).size
        )
    }

    @Test
    fun `rect isEmpty`() {
        Assert.assertTrue(IntRect(0, 0, 0, 10).isEmpty)
        Assert.assertTrue(IntRect(1, 0, 0, 10).isEmpty)
        Assert.assertTrue(IntRect(0, 1, 10, 0).isEmpty)
        Assert.assertTrue(IntRect(0, 1, 10, 1).isEmpty)

        Assert.assertFalse(IntRect(0, 1, 2, 3).isEmpty)
    }

    @Test
    fun `rect translate IntOffset`() {
        val shifted = IntRect(0, 5, 10, 15).translate(IntOffset(10, 15))
        Assert.assertEquals(IntRect(10, 20, 20, 30), shifted)
    }

    @Test
    fun `rect translate`() {
        val translated = IntRect(0, 5, 10, 15).translate(10, 15)
        Assert.assertEquals(IntRect(10, 20, 20, 30), translated)
    }

    @Test
    fun `rect inflate`() {
        val inflated = IntRect(5, 10, 10, 20).inflate(5)
        Assert.assertEquals(IntRect(0, 5, 15, 25), inflated)
    }

    @Test
    fun `rect deflate`() {
        val deflated = IntRect(0, 5, 15, 25).deflate(5)
        Assert.assertEquals(IntRect(5, 10, 10, 20), deflated)
    }

    @Test
    fun `rect intersect`() {
        val intersected = IntRect(0, 0, 20, 20).intersect(
            IntRect(10, 10, 30, 30)
        )
        Assert.assertEquals(IntRect(10, 10, 20, 20), intersected)
    }

    @Test
    fun `rect overlap`() {
        val rect1 = IntRect(0, 5, 10, 15)
        val rect2 = IntRect(5, 10, 15, 20)
        Assert.assertTrue(rect1.overlaps(rect2))
        Assert.assertTrue(rect2.overlaps(rect1))
    }

    @Test
    fun `rect does not overlap`() {
        val rect1 = IntRect(0, 5, 10, 15)
        val rect2 = IntRect(10, 5, 20, 15)
        Assert.assertFalse(rect1.overlaps(rect2))
        Assert.assertFalse(rect2.overlaps(rect1))
    }

    @Test
    fun `rect minDimension`() {
        val rect = IntRect(0, 5, 100, 25)
        Assert.assertEquals(20, rect.minDimension)
    }

    @Test
    fun `rect maxDimension`() {
        val rect = IntRect(0, 5, 100, 25)
        Assert.assertEquals(100, rect.maxDimension)
    }

    @Test
    fun `rect topLeft`() {
        val rect = IntRect(27, 38, 100, 200)
        Assert.assertEquals(IntOffset(27, 38), rect.topLeft)
    }

    @Test
    fun `rect topCenter`() {
        val rect = IntRect(100, 15, 200, 300)
        Assert.assertEquals(IntOffset(150, 15), rect.topCenter)
    }

    @Test
    fun `rect topRight`() {
        val rect = IntRect(100, 15, 200, 300)
        Assert.assertEquals(IntOffset(200, 15), rect.topRight)
    }

    @Test
    fun `rect centerLeft`() {
        val rect = IntRect(100, 10, 200, 300)
        Assert.assertEquals(IntOffset(100, 155), rect.centerLeft)
    }

    @Test
    fun `rect center`() {
        val rect = IntRect(100, 10, 200, 300)
        Assert.assertEquals(IntOffset(150, 155), rect.center)
    }

    @Test
    fun `rect centerRight`() {
        val rect = IntRect(100, 10, 200, 300)
        Assert.assertEquals(IntOffset(200, 155), rect.centerRight)
    }

    @Test
    fun `rect bottomLeft`() {
        val rect = IntRect(100, 10, 200, 300)
        Assert.assertEquals(IntOffset(100, 300), rect.bottomLeft)
    }

    @Test
    fun `rect bottomCenter`() {
        val rect = IntRect(100, 10, 200, 300)
        Assert.assertEquals(IntOffset(150, 300), rect.bottomCenter)
    }

    @Test
    fun `rect bottomRight`() {
        val rect = IntRect(100, 10, 200, 300)
        Assert.assertEquals(IntOffset(200, 300), rect.bottomRight)
    }

    @Test
    fun `rect contains`() {
        val rect = IntRect(100, 10, 200, 300)
        val IntOffset = IntOffset(177, 288)
        Assert.assertTrue(rect.contains(IntOffset))
    }

    @Test
    fun `rect does not contain`() {
        val rect = IntRect(100, 10, 200, 300)
        val IntOffset1 = IntOffset(201, 150)
        Assert.assertFalse(rect.contains(IntOffset1))

        val IntOffset2 = IntOffset(200, 301)
        Assert.assertFalse(rect.contains(IntOffset2))
    }

    @Test
    fun `rect from IntOffset and size`() {
        val offset = IntOffset(220, 300)
        val size = IntSize(80, 200)
        Assert.assertEquals(IntRect(220, 300, 300, 500), IntRect(offset, size))
    }

    @Test
    fun `rect from topleft and bottomRight`() {
        val offset1 = IntOffset(27, 38)
        val offset2 = IntOffset(130, 280)
        Assert.assertEquals(IntRect(27, 38, 130, 280), IntRect(offset1, offset2))
    }

    @Test
    fun `rect from center and radius`() {
        val offset = IntOffset(100, 50)
        val radius = 25
        Assert.assertEquals(IntRect(75, 25, 125, 75), IntRect(offset, radius))
    }

    @Test
    fun `rect lerp`() {
        val rect1 = IntRect(0, 0, 100, 100)
        val rect2 = IntRect(50, 50, 200, 200)

        Assert.assertEquals(
            IntRect(25, 25, 150, 150),
            lerp(rect1, rect2, 0.5f)
        )
    }
}