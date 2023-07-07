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

package androidx.compose.ui.layout

import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ScaleFactorTest {

    @Test
    fun testScaleFactorConstructor() {
        val scaleFactor = ScaleFactor(2f, 3f)
        assertEquals(2f, scaleFactor.scaleX)
        assertEquals(3f, scaleFactor.scaleY)
    }

    @Test
    fun testDestructuring() {
        val (scaleX, scaleY) = ScaleFactor(7f, 12f)
        assertEquals(7f, scaleX)
        assertEquals(12f, scaleY)
    }

    @Test
    fun testCopy() {
        val scaleFactor = ScaleFactor(11f, 4f)
        assertEquals(scaleFactor, scaleFactor.copy())
    }

    @Test
    fun testCopyOverwriteScaleX() {
        val scaleFactor = ScaleFactor(7f, 2f)
        assertEquals(ScaleFactor(3f, 2f), scaleFactor.copy(scaleX = 3f))
    }

    @Test
    fun testCopyOverwriteScaleY() {
        val scaleFactor = ScaleFactor(2f, 9f)
        assertEquals(ScaleFactor(2f, 27f), scaleFactor.copy(scaleY = 27f))
    }

    @Test
    fun testScaleFactorMultiplication() {
        assertEquals(ScaleFactor(2f, 8f), ScaleFactor(1f, 4f) * 2f)
    }

    @Test
    fun testScaleFactorDivision() {
        assertEquals(ScaleFactor(1f, 4f), ScaleFactor(2f, 8f) / 2f)
    }

    @Test
    fun testUnspecifiedScaleXQueryThrows() {
        try {
            ScaleFactor.Unspecified.scaleX
            fail("Attempt to access ScaleFactor.Unspecified.scaleX is not allowed")
        } catch (e: Throwable) {
            // no-op
        }
    }

    @Test
    fun testUnspecifiedScaleYQueryThrows() {
        try {
            ScaleFactor.Unspecified.scaleY
            fail("Attempt to access ScaleFactor.Unspecified.scaleY is not allowed")
        } catch (e: Throwable) {
            // no-op
        }
    }

    @Test
    fun testSizeMultiplication() {
        val scaleFactor = ScaleFactor(2f, 3f)
        val size = Size(100f, 200f)
        val expected = Size(200f, 600f)
        // verify commutative property of multiplication
        assertEquals(expected, size * scaleFactor)
        assertEquals(expected, scaleFactor * size)
    }

    @Test
    fun testScaleFactorLerp() {
        val scaleFactor1 = ScaleFactor(1f, 10f)
        val scaleFactor2 = ScaleFactor(3f, 20f)
        assertEquals(ScaleFactor(2f, 15f), lerp(scaleFactor1, scaleFactor2, 0.5f))
    }

    @Test
    fun testSizeDivision() {
        assertEquals(Size(1f, 2f), Size(100f, 300f) / ScaleFactor(100f, 150f))
    }

    @Test
    fun testIsSpecified() {
        assertFalse(ScaleFactor.Unspecified.isSpecified)
        assertTrue(ScaleFactor(1f, 1f).isSpecified)
    }

    @Test
    fun testIsUnspecified() {
        assertTrue(ScaleFactor.Unspecified.isUnspecified)
        assertFalse(ScaleFactor(1f, 1f).isUnspecified)
    }

    @Test
    fun testTakeOrElseTrue() {
        assertTrue(ScaleFactor(1f, 1f).takeOrElse { ScaleFactor.Unspecified }.isSpecified)
    }

    @Test
    fun testTakeOrElseFalse() {
        assertTrue(ScaleFactor.Unspecified.takeOrElse { ScaleFactor(1f, 1f) }.isSpecified)
    }

    @Test
    fun testScaleFactorToString() {
        assertEquals("ScaleFactor(1.2, 1.3)", ScaleFactor(1.234f, 1.25f).toString())
    }
}