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

package androidx.compose.ui.graphics

import androidx.compose.ui.geometry.MutableRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.math.abs
import kotlin.math.sqrt

class MatrixTest {
    @Test
    fun identity() {
        val matrix = Matrix()
        assertTrue(matrix.isIdentity())
        for (row in 0..3) {
            for (column in 0..3) {
                val expectedVal = if (row == column) 1f else 0f
                val newVal = expectedVal + 0.1f

                assertEquals(expectedVal, matrix[row, column], 0f)
                matrix[row, column] = newVal
                assertFalse(matrix.isIdentity())
                matrix[row, column] = expectedVal
            }
        }
    }

    @Test
    fun reset() {
        val matrix = Matrix()
        matrix.translate(5f)
        matrix.reset()
        assertTrue(matrix.isIdentity())
    }

    @Test
    fun mapPoint() {
        val matrix = Matrix()
        matrix.rotateZ(45f)
        val zPoint = matrix.map(Offset(10f, 0f))
        assertEquals(7.071f, zPoint.x, 0.01f)
        assertEquals(7.071f, zPoint.y, 0.01f)

        matrix.reset()
        matrix.rotateX(45f)
        val xPoint = matrix.map(Offset(0f, 10f))
        assertEquals(0f, xPoint.x, 0.01f)
        assertEquals(7.071f, xPoint.y, 0.01f)

        matrix.reset()
        matrix.rotateY(45f)
        val yPoint = matrix.map(Offset(10f, 0f))
        assertEquals(7.071f, yPoint.x, 0.01f)
        assertEquals(0f, yPoint.y, 0.01f)
    }

    @Test
    fun mapPointInfinite() {
        val matrix = Matrix()
        matrix[3, 3] = 0f

        assertEquals(Offset.Zero, matrix.map(Offset(1f, 1f)))
        matrix[3, 3] = Float.MIN_VALUE

        assertEquals(Offset.Zero, matrix.map(Offset(1f, 1f)))
    }

    @Test
    fun mapRect() {
        val matrix = Matrix()

        assertEquals(Rect(0f, 0f, 10f, 10f), matrix.map(Rect(0f, 0f, 10f, 10f)))

        matrix.rotateZ(90f)
        matrix.scale(2f, 2f)

        val rect90 = matrix.map(Rect(0f, 0f, 10f, 10f))
        assertEquals(-20f, rect90.left, 0.0001f)
        assertEquals(0f, rect90.top, 0.0001f)
        assertEquals(0f, rect90.right, 0.0001f)
        assertEquals(20f, rect90.bottom, 0.0001f)

        matrix.reset()
        matrix.rotateZ(45f)

        val rect45 = matrix.map(Rect(0f, 0f, 10f, 10f))
        val sqrt2Times10 = 10f * sqrt(2f)
        assertEquals(-sqrt2Times10 / 2f, rect45.left, 0.0001f)
        assertEquals(0f, rect45.top, 0.0001f)
        assertEquals(sqrt2Times10 / 2f, rect45.right, 0.0001f)
        assertEquals(sqrt2Times10, rect45.bottom, 0.0001f)
    }

    @Test
    fun mapMutableRect() {
        val matrix = Matrix()

        val rect = MutableRect(0f, 0f, 10f, 10f)
        matrix.map(rect)
        assertEquals(MutableRect(0f, 0f, 10f, 10f), rect)

        matrix.rotateZ(90f)
        matrix.scale(2f, 2f)

        matrix.map(rect)
        assertEquals(MutableRect(-20f, 0f, 0f, 20f), rect)

        matrix.reset()
        matrix.rotateZ(45f)

        rect.set(0f, 0f, 10f, 10f)
        matrix.map(rect)
        val sqrt2Times10 = 10f * sqrt(2f)
        assertEquals(MutableRect(-sqrt2Times10 / 2f, 0f, sqrt2Times10 / 2f, sqrt2Times10), rect)
    }

    @Test
    fun rotateZ() {
        val matrix = Matrix()

        // no rotation
        assertEquals(Offset(0f, 0f), matrix.map(Offset(0f, 0f)))
        assertEquals(Offset(10f, 10f), matrix.map(Offset(10f, 10f)))

        matrix.rotateZ(90f)
        assertEquals(Offset(0f, 0f), matrix.map(Offset(0f, 0f)))
        assertEquals(Offset(0f, 10f), matrix.map(Offset(10f, 0f)))
        assertEquals(Offset(-10f, 10f), matrix.map(Offset(10f, 10f)))
    }

    @Test
    fun rotateX() {
        val matrix = Matrix()

        matrix.rotateX(90f)
        assertEquals(Offset(0f, 0f), matrix.map(Offset(0f, 0f)))
        assertEquals(Offset(10f, 0f), matrix.map(Offset(10f, 0f)))
        assertEquals(Offset(0f, 0f), matrix.map(Offset(0f, 10f)))
        assertEquals(Offset(10f, 0f), matrix.map(Offset(10f, 10f)))
    }

    @Test
    fun rotateY() {
        val matrix = Matrix()

        matrix.rotateY(90f)
        assertEquals(Offset(0f, 0f), matrix.map(Offset(0f, 0f)))
        assertEquals(Offset(0f, 0f), matrix.map(Offset(10f, 0f)))
        assertEquals(Offset(0f, 10f), matrix.map(Offset(0f, 10f)))
        assertEquals(Offset(0f, 10f), matrix.map(Offset(10f, 10f)))
    }

    @Test
    fun scale() {
        val matrix = Matrix()

        matrix.scale(2f, 3f, 4f)
        assertEquals(Offset(0f, 0f), matrix.map(Offset(0f, 0f)))
        assertEquals(Offset(0f, 30f), matrix.map(Offset(0f, 10f)))
        assertEquals(Offset(20f, 0f), matrix.map(Offset(10f, 0f)))
        assertEquals(Offset(20f, 30f), matrix.map(Offset(10f, 10f)))
    }

    @Test
    fun translate() {
        val matrix = Matrix()

        matrix.translate(5f, 20f, 30f)
        assertEquals(Offset(5f, 20f), matrix.map(Offset(0f, 0f)))
        assertEquals(Offset(5f, 30f), matrix.map(Offset(0f, 10f)))
        assertEquals(Offset(15f, 20f), matrix.map(Offset(10f, 0f)))
        assertEquals(Offset(15f, 30f), matrix.map(Offset(10f, 10f)))
    }

    @Test
    fun invert() {
        val m1 = Matrix()
        m1.invert()
        assertTrue(m1.isIdentity())

        m1.rotateZ(12f)
        val m2 = Matrix()
        m2.setFrom(m1)
        m2.invert()
        m2 *= m1
        assertTrue(m2.isNearIdentity())

        m1.reset()
        m1.translate(1f, 200f, 3f)
        m1.rotateZ(30f)
        m1.rotateX(18f)
        m1.scale(1.2f, 1.5f, 1.1f)
        m1.rotateY(1f)
        m2.setFrom(m1)
        m2.invert()
        m2 *= m1
        assertTrue(m2.isNearIdentity())
    }

    @Test
    fun combineScaleTranslate() {
        val matrix = Matrix()
        matrix.scale(2f, 3f)
        matrix.translate(10f, 10f)

        assertEquals(Offset(20f, 30f), matrix.map(Offset(0f, 0f)))

        matrix.reset()
        matrix.translate(10f, 10f)
        matrix.scale(2f, 3f)

        assertEquals(Offset(10f, 10f), matrix.map(Offset(0f, 0f)))
        assertEquals(Offset(30f, 40f), matrix.map(Offset(10f, 10f)))
    }

    @Test
    fun combineRotateTranslate() {
        val matrix = Matrix()
        matrix.rotateZ(90f)
        matrix.translate(10f, 10f)

        assertEquals(Offset(-10f, 10f), matrix.map(Offset(0f, 0f)))

        matrix.reset()
        matrix.translate(10f, 10f)
        matrix.rotateZ(90f)

        assertEquals(Offset(10f, 10f), matrix.map(Offset(0f, 0f)))
        assertEquals(Offset(3f, 17f), matrix.map(Offset(7f, 7f)))
    }

    companion object {
        private fun assertEquals(expected: Offset, actual: Offset) {
            assertEquals(expected.x, actual.x, 0.0001f)
            assertEquals(expected.y, actual.y, 0.0001f)
        }

        private fun assertEquals(expected: MutableRect, actual: MutableRect) {
            assertEquals(expected.left, actual.left, 0.0001f)
            assertEquals(expected.top, actual.top, 0.0001f)
            assertEquals(expected.right, actual.right, 0.0001f)
            assertEquals(expected.bottom, actual.bottom, 0.0001f)
        }

        private fun Matrix.isNearIdentity(): Boolean {
            for (row in 0..3) {
                for (column in 0..3) {
                    val expected = if (row == column) 1f else 0f
                    val delta = this[row, column] - expected
                    if (abs(delta) > 0.0001f) {
                        return false
                    }
                }
            }
            return true
        }
    }
}