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
            topLeft = CornerRadius(0.5f),
            topRight = CornerRadius(0.25f),
            bottomRight = CornerRadius(0.25f, 0.75f),
            bottomLeft = CornerRadius.Zero
        )

        assertFalse(Offset(1.0f, 1.0f) in roundRect)
        assertFalse(Offset(1.1f, 1.1f) in roundRect)
        assertTrue(Offset(1.15f, 1.15f) in roundRect)
        assertFalse(Offset(2.0f, 1.0f) in roundRect)
        assertFalse(Offset(1.93f, 1.07f) in roundRect)
        assertFalse(Offset(1.97f, 1.7f) in roundRect)
        assertTrue(Offset(1.7f, 1.97f) in roundRect)
        assertTrue(Offset(1.0f, 1.99f) in roundRect)
    }

    @Test
    fun testRoundRectContainsLargeRadii() {
        val roundRect = RoundRect(
            Rect(1.0f, 1.0f, 2.0f, 2.0f),
            topLeft = CornerRadius(5000.0f),
            topRight = CornerRadius(2500.0f),
            bottomRight = CornerRadius(2500.0f, 7500.0f),
            bottomLeft = CornerRadius.Zero
        )

        assertFalse(Offset(1.0f, 1.0f) in roundRect)
        assertFalse(Offset(1.1f, 1.1f) in roundRect)
        assertTrue(Offset(1.15f, 1.15f) in roundRect)
        assertFalse(Offset(2.0f, 1.0f) in roundRect)
        assertFalse(Offset(1.93f, 1.07f) in roundRect)
        assertFalse(Offset(1.97f, 1.7f) in roundRect)
        assertTrue(Offset(1.7f, 1.97f) in roundRect)
        assertTrue(Offset(1.0f, 1.99f) in roundRect)
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
            CornerRadius(5f, 10f),
            CornerRadius(15f, 20f),
            CornerRadius(25f, 30f),
            CornerRadius(35f, 40f)
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
            CornerRadius(5f, 10f),
            CornerRadius(15f, 20f),
            CornerRadius(25f, 30f),
            CornerRadius(35f, 40f)
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
            CornerRadius(30f, 30f),
            CornerRadius(30f, 30f),
            CornerRadius(30f, 30f),
            CornerRadius(30f, 30f)
        )
        assertEquals(roundRect1, roundRect2)
    }

    @Test
    fun testRoundRectRadiusConstructor() {
        val roundRect1 = RoundRect(10f, 15f, 20f, 25f, CornerRadius(30f))
        val roundRect2 = RoundRect(
            10f,
            15f,
            20f,
            25f,
            CornerRadius(30f, 30f),
            CornerRadius(30f, 30f),
            CornerRadius(30f, 30f),
            CornerRadius(30f, 30f)
        )
        assertEquals(roundRect1, roundRect2)
    }

    @Test
    fun testRoundRectWithRectAndRadiusConstructor() {
        val roundRect1 = RoundRect(Rect(10f, 15f, 20f, 25f), CornerRadius(30f))
        val roundRect2 = RoundRect(
            10f,
            15f,
            20f,
            25f,
            CornerRadius(30f, 30f),
            CornerRadius(30f, 30f),
            CornerRadius(30f, 30f),
            CornerRadius(30f, 30f)
        )
        assertEquals(roundRect1, roundRect2)
    }

    @Test
    fun testRoundRectWithRectAndSeparateRadii() {
        val roundRect1 = RoundRect(
            Rect(10f, 15f, 20f, 25f),
            CornerRadius(1f, 2f),
            CornerRadius(3f, 4f),
            CornerRadius(5f, 6f),
            CornerRadius(7f, 8f)
        )
        val roundRect2 = RoundRect(
            10f,
            15f,
            20f,
            25f,
            CornerRadius(1f, 2f),
            CornerRadius(3f, 4f),
            CornerRadius(5f, 6f),
            CornerRadius(7f, 8f)
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
            CornerRadius(10f, 15f),
            CornerRadius(17f, 20f),
            CornerRadius(25f, 30f),
            CornerRadius(35f, 40f)
        )
        assertEquals(CornerRadius(10f, 15f), rr.topLeftCornerRadius)
        assertEquals(CornerRadius(17f, 20f), rr.topRightCornerRadius)
        assertEquals(CornerRadius(25f, 30f), rr.bottomRightCornerRadius)
        assertEquals(CornerRadius(35f, 40f), rr.bottomLeftCornerRadius)
    }

    @Test
    fun testMinDimension() {
        val rr = RoundRect(0f, 0f, 100f, 50f, CornerRadius(7f, 8f))
        assertEquals(50f, rr.minDimension)
    }

    @Test
    fun testMaxDimension() {
        val rr = RoundRect(0f, 0f, 300f, 100f, CornerRadius(5f, 10f))
        assertEquals(300f, rr.maxDimension)
    }

    @Test
    fun testCenter() {
        val rr = RoundRect(0f, 0f, 200f, 100f, CornerRadius.Zero)
        assertEquals(Offset(100f, 50f), rr.center)
    }

    @Test
    fun testSafeInnerRect() {
        val insetFactor = 0.29289321881f // 1-cos(pi/4)
        val rr = RoundRect(
            left = 0f,
            top = 0f,
            right = 100f,
            bottom = 100f,
            topLeftCornerRadius = CornerRadius(0f, 5f),
            topRightCornerRadius = CornerRadius(5f, 10f),
            bottomRightCornerRadius = CornerRadius(10f, 15f),
            bottomLeftCornerRadius = CornerRadius(15f, 20f)
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
        val rr = RoundRect(1f, 2f, 3f, 4f, CornerRadius(15f, 10f))
        assertEquals(Rect(1f, 2f, 3f, 4f), rr.boundingRect)
    }
}
