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

package androidx.compose.ui.platform

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.round
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt

class SkijaLayerTest {
    @get:Rule
    val rule = createComposeRule()

    private val layer = TestSkijaLayer()
    private val matrix = Matrix()
    private val cos45 = cos(PI / 4)

    @Test
    fun initial() {
        layer.getMatrix(matrix)

        assertEquals(IntOffset(0, 0), matrix.map(Offset(0f, 0f)).round())
        assertEquals(IntOffset(100, 10), matrix.map(Offset(100f, 10f)).round())
    }

    @Test
    fun move() {
        layer.move(IntOffset(10, 20))
        layer.getMatrix(matrix)

        assertEquals(IntOffset(0, 0), matrix.map(Offset(0f, 0f)).round())
        assertEquals(IntOffset(100, 10), matrix.map(Offset(100f, 10f)).round())
    }

    @Test
    fun resize() {
        layer.resize(IntSize(100, 10))
        layer.getMatrix(matrix)

        assertEquals(IntOffset(0, 0), matrix.map(Offset(0f, 0f)).round())
        assertEquals(IntOffset(100, 10), matrix.map(Offset(100f, 10f)).round())
    }

    @Test
    fun `resize and move`() {
        layer.resize(IntSize(100, 10))
        layer.move(IntOffset(10, 20))
        layer.getMatrix(matrix)

        assertEquals(IntOffset(0, 0), matrix.map(Offset(0f, 0f)).round())
        assertEquals(IntOffset(100, 10), matrix.map(Offset(100f, 10f)).round())
    }

    @Test
    fun `translation, left-top origin`() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            translationX = 10f,
            translationY = 20f,
            transformOrigin = TransformOrigin(0f, 0f)
        )
        layer.getMatrix(matrix)

        assertEquals(IntOffset(10, 20), matrix.map(Offset(0f, 0f)).round())
        assertEquals(IntOffset(110, 30), matrix.map(Offset(100f, 10f)).round())
    }

    @Test
    fun `translation, bottom-right origin`() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            translationX = 10f,
            translationY = 20f,
            transformOrigin = TransformOrigin(1f, 1f)
        )
        layer.getMatrix(matrix)

        assertEquals(IntOffset(10, 20), matrix.map(Offset(0f, 0f)).round())
        assertEquals(IntOffset(110, 30), matrix.map(Offset(100f, 10f)).round())
    }

    @Test
    fun `scale, left-top origin`() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            scaleX = 2f,
            scaleY = 4f,
            transformOrigin = TransformOrigin(0f, 0f)
        )
        layer.getMatrix(matrix)

        assertEquals(IntOffset(0, 0), matrix.map(Offset(0f, 0f)).round())
        assertEquals(IntOffset(200, 40), matrix.map(Offset(100f, 10f)).round())
    }

    @Test
    fun `scale, bottom-right origin`() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            scaleX = 2f,
            scaleY = 4f,
            transformOrigin = TransformOrigin(1f, 1f)
        )
        layer.getMatrix(matrix)

        assertEquals(IntOffset(-100, -30), matrix.map(Offset(0f, 0f)).round())
        assertEquals(IntOffset(100, 10), matrix.map(Offset(100f, 10f)).round())
    }

    @Test
    fun `rotationX, left-top origin`() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            rotationX = 45f,
            transformOrigin = TransformOrigin(0f, 0f)
        )
        layer.getMatrix(matrix)

        val y = (10 * cos45).roundToInt()
        assertEquals(IntOffset(0, 0), matrix.map(Offset(0f, 0f)).round())
        assertEquals(IntOffset(100, y), matrix.map(Offset(100f, 10f)).round())
    }

    @Test
    fun `rotationX, bottom-right origin`() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            rotationX = 45f,
            transformOrigin = TransformOrigin(1f, 1f)
        )
        layer.getMatrix(matrix)

        val y = 10 * (1 - cos45.toFloat())
        println(matrix.map(Offset(0f, 0f)))
        println(matrix.map(Offset(100f, 10f)))
        assertEquals(Offset(0f, y), matrix.map(Offset(0f, 0f)))
        assertEquals(Offset(100f, 10f), matrix.map(Offset(100f, 10f)))
    }

    @Test
    fun `rotationY, left-top origin`() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            rotationY = 45f,
            transformOrigin = TransformOrigin(0f, 0f)
        )
        layer.getMatrix(matrix)

        val x = (100 * cos45).roundToInt()
        assertEquals(IntOffset(0, 0), matrix.map(Offset(0f, 0f)).round())
        assertEquals(IntOffset(x, 10), matrix.map(Offset(100f, 10f)).round())
    }

    @Test
    fun `rotationY, bottom-right origin`() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            rotationY = 45f,
            transformOrigin = TransformOrigin(1f, 1f)
        )
        layer.getMatrix(matrix)

        val x = (100 * (1 - cos45)).roundToInt()
        assertEquals(IntOffset(x, 0), matrix.map(Offset(0f, 0f)).round())
        assertEquals(IntOffset(100, 10), matrix.map(Offset(100f, 10f)).round())
    }

    @Test
    fun `rotationZ, left-top origin`() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            rotationZ = 90f,
            transformOrigin = TransformOrigin(0f, 0f)
        )
        layer.getMatrix(matrix)

        assertEquals(IntOffset(0, 0), matrix.map(Offset(0f, 0f)).round())
        assertEquals(IntOffset(-10, 100), matrix.map(Offset(100f, 10f)).round())
    }

    @Test
    fun `rotationZ, bottom-right origin`() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            rotationZ = 90f,
            transformOrigin = TransformOrigin(1f, 1f)
        )
        layer.getMatrix(matrix)

        assertEquals(IntOffset(110, -90), matrix.map(Offset(0f, 0f)).round())
        assertEquals(IntOffset(100, 10), matrix.map(Offset(100f, 10f)).round())
    }

    @Test
    fun `translation, scale, left-top origin`() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            translationX = 60f,
            translationY = 7f,
            scaleX = 2f,
            scaleY = 4f,
            transformOrigin = TransformOrigin(0f, 0f)
        )
        layer.getMatrix(matrix)

        assertEquals(IntOffset(0 + 60, 0 + 7), matrix.map(Offset(0f, 0f)).round())
        assertEquals(IntOffset(100 * 2 + 60, 10 * 4 + 7), matrix.map(Offset(100f, 10f)).round())
    }

    @Test
    fun `translation, rotationZ, left-top origin`() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            translationX = 60f,
            translationY = 7f,
            rotationZ = 90f,
            transformOrigin = TransformOrigin(0f, 0f)
        )
        layer.getMatrix(matrix)

        assertEquals(IntOffset(0 + 60, 0 + 7), matrix.map(Offset(0f, 0f)).round())
        assertEquals(IntOffset(-10 + 60, 100 + 7), matrix.map(Offset(100f, 10f)).round())
    }

    @Test
    fun `translation, rotationX, left-top origin`() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            translationX = 60f,
            translationY = 7f,
            rotationX = 45f,
            transformOrigin = TransformOrigin(0f, 0f)
        )
        layer.getMatrix(matrix)

        val y = (10 * cos45).roundToInt()
        val translationY = (7 * cos45).roundToInt()
        assertEquals(IntOffset(0 + 60, 0 + translationY), matrix.map(Offset(0f, 0f)).round())
        assertEquals(IntOffset(100 + 60, y + translationY), matrix.map(Offset(100f, 10f)).round())
    }

    @Test
    fun `translation, rotationY, left-top origin`() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            translationX = 60f,
            translationY = 7f,
            rotationY = 45f,
            transformOrigin = TransformOrigin(0f, 0f)
        )
        layer.getMatrix(matrix)

        val x = (100 * cos45).roundToInt()
        val translationX = (60 * cos45).roundToInt()
        assertEquals(IntOffset(0 + translationX, 0 + 7), matrix.map(Offset(0f, 0f)).round())
        assertEquals(IntOffset(x + translationX, 10 + 7), matrix.map(Offset(100f, 10f)).round())
    }

    @Test
    fun `scale, rotationZ, left-top origin`() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            scaleX = 2f,
            scaleY = 4f,
            rotationZ = 90f,
            transformOrigin = TransformOrigin(0f, 0f)
        )
        layer.getMatrix(matrix)

        assertEquals(IntOffset(0, 0), matrix.map(Offset(0f, 0f)).round())
        assertEquals(IntOffset(-10 * 4, 100 * 2), matrix.map(Offset(100f, 10f)).round())
    }

    @Test
    fun `translation, scale, rotationZ, left-top origin`() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            translationX = 60f,
            translationY = 7f,
            scaleX = 2f,
            scaleY = 4f,
            rotationZ = 90f,
            transformOrigin = TransformOrigin(0f, 0f)
        )
        layer.getMatrix(matrix)

        assertEquals(IntOffset(0 + 60, 0 + 7), matrix.map(Offset(0f, 0f)).round())
        assertEquals(IntOffset(-10 * 4 + 60, 100 * 2 + 7), matrix.map(Offset(100f, 10f)).round())
    }

    private fun TestSkijaLayer() = SkijaLayer(
        { Density(1f, 1f) },
        invalidateParentLayer = {},
        drawBlock = {}
    )

    private fun SkijaLayer.updateProperties(
        scaleX: Float = 1f,
        scaleY: Float = 1f,
        alpha: Float = 1f,
        translationX: Float = 0f,
        translationY: Float = 0f,
        shadowElevation: Float = 0f,
        rotationX: Float = 0f,
        rotationY: Float = 0f,
        rotationZ: Float = 0f,
        cameraDistance: Float = 0f,
        transformOrigin: TransformOrigin = TransformOrigin.Center,
        shape: Shape = RectangleShape,
        clip: Boolean = false
    ) {
        updateLayerProperties(
            scaleX, scaleY, alpha, translationX, translationY, shadowElevation, rotationX,
            rotationY, rotationZ, cameraDistance, transformOrigin, shape, clip, LayoutDirection.Ltr
        )
    }
}
