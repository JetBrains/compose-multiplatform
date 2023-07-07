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

package androidx.compose.ui.graphics

import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.Test
import kotlin.test.assertTrue

class ColorMatrixTest {

    private val tolerance = 0.0000001f

    private val source = floatArrayOf(
        0f,
        1f,
        2f,
        3f,
        4f,
        5f,
        6f,
        7f,
        8f,
        9f,
        10f,
        11f,
        12f,
        13f,
        14f,
        15f,
        16f,
        17f,
        18f,
        19f
    )

    private var colorMatrix = ColorMatrix(source)

    @BeforeTest
    fun setup() {
        colorMatrix = ColorMatrix(source)
    }

    @Test
    fun testGetOperator() {
        var i = 0
        for (row in 0 until 4) {
            for (col in 0 until 5) {
                assertEquals(colorMatrix[row, col], source[i++])
            }
        }
    }

    @Test
    fun testSetOperator() {
        val matrix = ColorMatrix(source.copyOf())
        var i = 0
        for (row in 0 until 4) {
            for (col in 0 until 5) {
                matrix[row, col]++
                assertEquals(matrix[row, col], source[i++] + 1)
            }
        }
    }

    @Test
    fun testColorMatrix() {
        ColorMatrix()
        val cM1 = ColorMatrix(source)
        val fA1: FloatArray = cM1.values
        assertTrue(source.contentEquals(fA1))
        assertTrue(source.contentEquals(fA1))
        val cM2 = ColorMatrix(cM1.values)
        val fA2: FloatArray = cM2.values
        assertTrue(fA1.contentEquals(fA2))
    }

    @Test
    fun testReset() {
        var ret: FloatArray = colorMatrix.values
        preCompare(ret)
        colorMatrix.reset()
        ret = colorMatrix.values
        assertEquals(20, ret.size)
        for (i in 0..19) {
            if (0 == i % 6) {
                assertEquals(1.0f, ret[i], 0.0f)
                continue
            }
            assertEquals(0.0f, ret[i], 0.0f)
        }
    }

    @Test
    fun testSet1() {
        var ret: FloatArray = colorMatrix.values
        preCompare(ret)
        val fArray = floatArrayOf(
            19f,
            18f,
            17f,
            16f,
            15f,
            14f,
            13f,
            12f,
            11f,
            10f,
            9f,
            8f,
            7f,
            6f,
            5f,
            4f,
            3f,
            2f,
            1f,
            0f
        )
        colorMatrix.set(ColorMatrix(fArray))
        ret = colorMatrix.values
        assertTrue(fArray.contentEquals(ret))
    }

    @Test
    fun testSet2() {
        var ret: FloatArray = colorMatrix.values
        preCompare(ret)
        val fArray = floatArrayOf(
            19f,
            18f,
            17f,
            16f,
            15f,
            14f,
            13f,
            12f,
            11f,
            10f,
            9f,
            8f,
            7f,
            6f,
            5f,
            4f,
            3f,
            2f,
            1f,
            0f
        )
        colorMatrix.set(ColorMatrix(fArray))
        ret = colorMatrix.values
        assertTrue(fArray.contentEquals(ret))
    }

    @Test
    fun testSetRotate() {
        val cm1 = ColorMatrix().apply { setToRotateRed(180f) }
        assertEquals(-1.0f, cm1.values[6], tolerance)
        assertEquals(-1.0f, cm1.values[12], tolerance)
        assertEquals(0f, cm1.values[7], tolerance)
        assertEquals(0f, cm1.values[11], tolerance)
        val cm2 = ColorMatrix().apply { setToRotateGreen(180f) }
        assertEquals(-1.0f, cm2.values[0], tolerance)
        assertEquals(-1.0f, cm2.values[12], tolerance)
        assertEquals(0f, cm2.values[2], tolerance)
        assertEquals(0f, cm2.values[10], tolerance)
        val cm3 = ColorMatrix().apply { setToRotateBlue(180f) }
        assertEquals(-1.0f, cm3.values[0], tolerance)
        assertEquals(-1.0f, cm3.values[6], tolerance)
        assertEquals(0f, cm3.values[1], tolerance)
        assertEquals(0f, cm3.values[5], tolerance)
    }

    @Test
    fun testSetSaturation() {
        colorMatrix.setToSaturation(0.5f)
        val ret: FloatArray = colorMatrix.values
        val expected = floatArrayOf(
            0.6065f, 0.3575f, 0.036f, 0.0f, 0.0f,
            0.1065f, 0.85749996f, 0.036f, 0.0f, 0.0f,
            0.1065f, 0.3575f, 0.536f, 0.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f, 0.0f
        )
        for (i in ret.indices) {
            assertEquals(
                expected[i],
                ret[i],
                "Expected: $expected[i] at index: $i received: $ret[i]"
            )
        }
    }

    @Test
    fun testSetScale() {
        val values = ColorMatrix().apply {
            setToScale(
                redScale = 2f,
                greenScale = 3f,
                blueScale = 4f,
                alphaScale = 5f
            )
        }.values
        assertEquals(20, values.size)
        assertEquals(2.0f, values[0], 0.0f)
        assertEquals(3.0f, values[6], 0.0f)
        assertEquals(4.0f, values[12], 0.0f)
        assertEquals(5.0f, values[18], 0.0f)
        for (i in 1..19) {
            if (0 == i % 6) {
                continue
            }
            assertEquals(0.0f, values[i], 0.0f)
        }
    }

    @Test
    fun testSetRGB2YUV() {
        colorMatrix.convertRgbToYuv()
        assertTrue(
            floatArrayOf(
                0.299f, 0.587f, 0.114f, 0.0f, 0.0f,
                -0.16874f, -0.33126f, 0.5f, 0.0f, 0.0f,
                0.5f, -0.41869f, -0.08131f, 0.0f, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f, 0.0f
            ).contentEquals(colorMatrix.values)
        )
    }

    @Test
    fun testSetYUV2RGB() {
        colorMatrix.convertYuvToRgb()
        assertTrue(
            floatArrayOf(
                1.0f, 0.0f, 1.402f, 0.0f, 0.0f,
                1.0f, -0.34414f, -0.71414f, 0.0f, 0.0f,
                1.0f, 1.772f, 0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f, 0.0f
            ).contentEquals(colorMatrix.values)
        )
    }

    @Test
    fun testTimesAssignPostConcat() {
        colorMatrix *= ColorMatrix()
        val ret: FloatArray = colorMatrix.values
        for (i in 0..19) {
            assertEquals(i.toFloat(), ret[i], 0.0f)
        }
    }

    @Test
    fun testTimesAssignPreConcat() {
        val target = ColorMatrix()
        target *= colorMatrix
        val ret: FloatArray = colorMatrix.values
        for (i in 0..19) {
            assertEquals(i.toFloat(), ret[i], 0.0f)
        }
    }

    @Test
    fun testTimesAssign() {
        val floatA = floatArrayOf(
            0f,
            1f,
            2f,
            3f,
            4f,
            5f,
            6f,
            7f,
            8f,
            9f,
            9f,
            8f,
            7f,
            6f,
            5f,
            4f,
            3f,
            2f,
            1f,
            0f
        )
        val floatB = floatArrayOf(
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f
        )
        val matrix = ColorMatrix(floatA)
        matrix *= ColorMatrix(floatB)
        val ret: FloatArray = matrix.values
        assertTrue(
            floatArrayOf(
                6.0f, 6.0f, 6.0f, 6.0f, 10f,
                26.0f, 26.0f, 26.0f, 26.0f, 35.0f,
                30.0f, 30.0f, 30.0f, 30.0f, 35.0f,
                10.0f, 10.0f, 10.0f, 10.0f, 10.0f
            ).contentEquals(ret)
        )
    }

    private fun preCompare(ret: FloatArray) {
        assertEquals(20, ret.size)
        for (i in 0..19) {
            assertEquals(i.toFloat(), ret[i], 0.0f)
        }
    }
}