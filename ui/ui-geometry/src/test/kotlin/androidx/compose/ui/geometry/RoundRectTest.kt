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
class RoundRectTest {

    @Test
    fun testRoundRectContains() {
        val roundRect = RoundRect(
            Rect(1.0f, 1.0f, 2.0f, 2.0f),
            topLeft = Radius(0.5f),
            topRight = Radius(0.25f),
            bottomRight = Radius(0.25f, 0.75f),
            bottomLeft = Radius.Zero
        )

        assertFalse(roundRect.contains(Offset(1.0f, 1.0f)))
        assertFalse(roundRect.contains(Offset(1.1f, 1.1f)))
        assertTrue(roundRect.contains(Offset(1.15f, 1.15f)))
        assertFalse(roundRect.contains(Offset(2.0f, 1.0f)))
        assertFalse(roundRect.contains(Offset(1.93f, 1.07f)))
        assertFalse(roundRect.contains(Offset(1.97f, 1.7f)))
        assertTrue(roundRect.contains(Offset(1.7f, 1.97f)))
        assertTrue(roundRect.contains(Offset(1.0f, 1.99f)))
    }

    @Test
    fun testRoundRectContainsLargeRadii() {
        val roundRect = RoundRect(
            Rect(1.0f, 1.0f, 2.0f, 2.0f),
            topLeft = Radius(5000.0f),
            topRight = Radius(2500.0f),
            bottomRight = Radius(2500.0f, 7500.0f),
            bottomLeft = Radius.Zero
        )

        assertFalse(roundRect.contains(Offset(1.0f, 1.0f)))
        assertFalse(roundRect.contains(Offset(1.1f, 1.1f)))
        assertTrue(roundRect.contains(Offset(1.15f, 1.15f)))
        assertFalse(roundRect.contains(Offset(2.0f, 1.0f)))
        assertFalse(roundRect.contains(Offset(1.93f, 1.07f)))
        assertFalse(roundRect.contains(Offset(1.97f, 1.7f)))
        assertTrue(roundRect.contains(Offset(1.7f, 1.97f)))
        assertTrue(roundRect.contains(Offset(1.0f, 1.99f)))
    }

    @Test
    fun testRoundRectWidthComputation() {
        val left = 10f
        val right = 30f
        val roundRect = RoundRect(
            10f,
            20f,
            30f,
            40f,
            Radius(5f, 10f),
            Radius(15f, 20f),
            Radius(25f, 30f),
            Radius(35f, 40f)
        )
        assertEquals(right - left, roundRect.width)
    }

    @Test
    fun testRoundRectHeightComputation() {
        val top = 20f
        val bottom = 40f
        val roundRect = RoundRect(
            10f,
            top,
            30f,
            bottom,
            Radius(5f, 10f),
            Radius(15f, 20f),
            Radius(25f, 30f),
            Radius(35f, 40f)
        )
        assertEquals(bottom - top, roundRect.height)
    }

    @Test
    fun testRoundRectMatchingFloatConstructor() {
        val roundRect1 = RoundRect(10f, 15f, 20f, 25f, 30f, 30f)
        val roundRect2 = RoundRect(
            10f,
            15f,
            20f,
            25f,
            Radius(30f, 30f),
            Radius(30f, 30f),
            Radius(30f, 30f),
            Radius(30f, 30f)
        )
        assertEquals(roundRect1, roundRect2)
    }

    @Test
    fun testRoundRectRadiusConstructor() {
        val roundRect1 = RoundRect(10f, 15f, 20f, 25f, Radius(30f))
        val roundRect2 = RoundRect(
            10f,
            15f,
            20f,
            25f,
            Radius(30f, 30f),
            Radius(30f, 30f),
            Radius(30f, 30f),
            Radius(30f, 30f)
        )
        assertEquals(roundRect1, roundRect2)
    }

    @Test
    fun testRoundRectWithRectAndRadiusConstructor() {
        val roundRect1 = RoundRect(Rect(10f, 15f, 20f, 25f), Radius(30f))
        val roundRect2 = RoundRect(
            10f,
            15f,
            20f,
            25f,
            Radius(30f, 30f),
            Radius(30f, 30f),
            Radius(30f, 30f),
            Radius(30f, 30f)
        )
        assertEquals(roundRect1, roundRect2)
    }

    @Test
    fun testRoundRectWithRectAndSeparateRadii() {
        val roundRect1 = RoundRect(
            Rect(10f, 15f, 20f, 25f),
            Radius(1f, 2f),
            Radius(3f, 4f),
            Radius(5f, 6f),
            Radius(7f, 8f)
        )
        val roundRect2 = RoundRect(
            10f,
            15f,
            20f,
            25f,
            Radius(1f, 2f),
            Radius(3f, 4f),
            Radius(5f, 6f),
            Radius(7f, 8f)
        )
        assertEquals(roundRect1, roundRect2)
    }

    @Test
    fun testRadiusProperties() {
        val rr = RoundRect(
            0f,
            0f,
            10f,
            10f,
            Radius(10f, 15f),
            Radius(17f, 20f),
            Radius (25f, 30f),
            Radius(35f, 40f)
        )
        assertEquals(Radius(10f, 15f), rr.topLeftRadius)
        assertEquals(Radius(17f, 20f), rr.topRightRadius)
        assertEquals(Radius(25f, 30f), rr.bottomRightRadius)
        assertEquals(Radius(35f, 40f), rr.bottomLeftRadius)
    }

    @Test
    fun testMinDimension() {
        val rr = RoundRect(0f, 0f, 100f, 50f, Radius(7f, 8f))
        assertEquals(50f, rr.minDimension)
    }

    @Test
    fun testMaxDimension() {
        val rr = RoundRect(0f, 0f, 300f, 100f, Radius(5f, 10f))
        assertEquals(300f, rr.maxDimension)
    }

    @Test
    fun testCenter() {
        val rr = RoundRect(0f, 0f, 200f, 100f, Radius.Zero)
        assertEquals(Offset(100f, 50f), rr.center)
    }

    @Test
    fun testSafeInnerRect() {
        val insetFactor = 0.29289321881f // 1-cos(pi/4)
        val rr = RoundRect(
            left = 0f, top = 0f, right = 100f, bottom = 100f,
            topLeftRadius = Radius(0f, 5f),
            topRightRadius = Radius(5f, 10f),
            bottomRightRadius = Radius(10f, 15f),
            bottomLeftRadius = Radius(15f, 20f)
        )
        assertEquals(
            Rect(
                15f * insetFactor,
                10f * insetFactor,
                100f - 10f * insetFactor,
                100f - 20f * insetFactor
            ),
            rr.safeInnerRect
        )
    }

    @Test
    fun testBoundingRect() {
        val rr = RoundRect(1f, 2f, 3f, 4f, Radius(15f, 10f))
        assertEquals(Rect(1f, 2f, 3f, 4f), rr.boundingRect)
    }
}
