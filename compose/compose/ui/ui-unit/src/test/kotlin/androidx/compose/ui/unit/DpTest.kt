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

package androidx.compose.ui.unit

import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DpTest {

    @Test
    fun constructor() {
        val dim1 = Dp(value = 5f)
        assertEquals(5f, dim1.value, 0f)

        val dim2 = Dp(value = Float.POSITIVE_INFINITY)
        assertEquals(Float.POSITIVE_INFINITY, dim2.value, 0f)

        val dim3 = Dp(value = Float.NaN)
        assertEquals(Float.NaN, dim3.value, 0f)
    }

    @Test
    fun dpIntegerConstruction() {
        val dim = 10.dp
        assertEquals(10f, dim.value, 0f)
    }

    @Test
    fun dpFloatConstruction() {
        val dim = 10f.dp
        assertEquals(10f, dim.value, 0f)
    }

    @Test
    fun dpDoubleConstruction() {
        val dim = 10.0.dp
        assertEquals(10f, dim.value, 0f)
    }

    @Test
    fun subtractOperator() {
        assertEquals(-1f, (3.dp - 4.dp).value)
        assertEquals(1f, (10.dp - 9.dp).value, 0f)
    }

    @Test
    fun addOperator() {
        assertEquals(2f, (1.dp + 1.dp).value, 0f)
        assertEquals(10f, (6.dp + 4.dp).value, 0f)
    }

    @Test
    fun multiplyOperator() {
        assertEquals(0f, (1.dp * 0f).value, 0f)
        assertEquals(10f, (1.dp * 10f).value, 0f)
    }

    @Test
    fun multiplyOperatorScalar() {
        assertEquals(10f, 10f * 1.dp.value, 0f)
        assertEquals(10f, 10 * 1.dp.value, 0f)
        assertEquals(10f, (10.0 * 1.dp).value, 0f)
    }

    @Test
    fun divideOperator() {
        assertEquals(10f, 100.dp / 10f.dp, 0f)
        assertEquals(0f, 0.dp / 10f.dp, 0f)
    }

    @Test
    fun divideToScalar() {
        assertEquals(1f, 1.dp / 1.dp, 0f)
    }

    @Test
    fun hairline() {
        assertEquals(0f, Dp.Hairline.value, 0f)
    }

    @Test
    fun infinite() {
        assertEquals(Float.POSITIVE_INFINITY, Dp.Infinity.value, 0f)
    }

    @Suppress("DIVISION_BY_ZERO")
    @Test
    fun compare() {
        assertTrue(0.dp < Float.MIN_VALUE.dp)
        assertTrue(1.dp < 3.dp)
        assertEquals(0, 1.dp.compareTo(1.dp))
        assertTrue(1.dp > 0.dp)
        assertTrue(Float.NEGATIVE_INFINITY.dp < Dp.Infinity)
        assertTrue(Float.NEGATIVE_INFINITY.dp < 0.dp)
        assertTrue(Dp.Infinity > Float.MAX_VALUE.dp)

        val zeroNaN = 0f / 0f
        val infNaN = Float.POSITIVE_INFINITY / Float.NEGATIVE_INFINITY
        assertEquals(0, zeroNaN.dp.compareTo(zeroNaN.dp))
        assertEquals(0, infNaN.dp.compareTo(infNaN.dp))
    }

    @Test
    fun minTest() {
        assertEquals(10f, min(10.dp, 20.dp).value, 0f)
        assertEquals(10f, min(20.dp, 10.dp).value, 0f)
        assertEquals(10f, min(10.dp, 10.dp).value, 0f)
    }

    @Test
    fun maxTest() {
        assertEquals(20f, max(10.dp, 20.dp).value, 0f)
        assertEquals(20f, max(20.dp, 10.dp).value, 0f)
        assertEquals(20f, max(20.dp, 20.dp).value, 0f)
    }

    @Test
    fun coerceIn() {
        assertEquals(10f, 10.dp.coerceIn(0.dp, 20.dp).value, 0f)
        assertEquals(10f, 20.dp.coerceIn(0.dp, 10.dp).value, 0f)
        assertEquals(10f, 0.dp.coerceIn(10.dp, 20.dp).value, 0f)
        try {
            10.dp.coerceIn(20.dp, 10.dp)
            fail("Expected an exception here")
        } catch (e: IllegalArgumentException) {
            // success!
        }
    }

    @Test
    fun coerceAtLeast() {
        assertEquals(10f, 0.dp.coerceAtLeast(10.dp).value, 0f)
        assertEquals(10f, 10.dp.coerceAtLeast(5.dp).value, 0f)
        assertEquals(10f, 10.dp.coerceAtLeast(10.dp).value, 0f)
    }

    @Test
    fun coerceAtMost() {
        assertEquals(10f, 100.dp.coerceAtMost(10.dp).value, 0f)
        assertEquals(10f, 10.dp.coerceAtMost(20.dp).value, 0f)
        assertEquals(10f, 10.dp.coerceAtMost(10.dp).value, 0f)
    }

    @Test
    fun dpRectConstructor() {
        assertEquals(
            DpRect(10.dp, 5.dp, 25.dp, 15.dp),
            DpRect(DpOffset(10.dp, 5.dp), DpSize(15.dp, 10.dp))
        )
    }

    @Test
    fun dpRectWidth() {
        val dpRect = DpRect(10.dp, 5.dp, 25.dp, 15.dp)
        assertEquals(15.dp, dpRect.width)
    }

    @Test
    fun dpRectHeight() {
        val dpRect = DpRect(10.dp, 5.dp, 25.dp, 15.dp)
        assertEquals(10.dp, dpRect.height)
    }

    @Test
    fun testIsSpecified() {
        Assert.assertFalse(Dp.Unspecified.isSpecified)
        assertTrue(Dp(1f).isSpecified)
    }

    @Test
    fun testIsUnspecified() {
        assertTrue(Dp.Unspecified.isUnspecified)
        Assert.assertFalse(Dp(1f).isUnspecified)
    }

    @Test
    fun testTakeOrElseTrue() {
        assertTrue(Dp(1f).takeOrElse { Dp.Unspecified }.isSpecified)
    }

    @Test
    fun testTakeOrElseFalse() {
        assertTrue(Dp.Unspecified.takeOrElse { Dp(1f) }.isSpecified)
    }
}