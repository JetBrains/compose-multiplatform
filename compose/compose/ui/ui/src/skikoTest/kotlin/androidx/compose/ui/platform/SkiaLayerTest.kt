/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SkiaLayerTest {

    private val layer = TestSkiaLayer()
    private val cos45 = cos(PI / 4).toFloat()

    private val matrix get() = layer.matrix
    private val inverseMatrix get() = Matrix().apply {
        matrix.invertTo(this)
    }


    @Test
    fun initial() {
        assertMapping(
            from = Offset(0f, 0f),
            to = Offset(0f, 0f)
        )
        assertMapping(
            from = Offset(100f, 10f),
            to = Offset(100f, 10f)
        )
    }

    @Test
    fun move() {
        layer.move(IntOffset(10, 20))

        assertMapping(
            from = Offset(0f, 0f),
            to = Offset(0f, 0f)
        )
        assertMapping(
            from = Offset(100f, 10f),
            to = Offset(100f, 10f)
        )
    }

    @Test
    fun resize() {
        layer.resize(IntSize(100, 10))

        assertMapping(
            from = Offset(0f, 0f),
            to = Offset(0f, 0f)
        )
        assertMapping(
            from = Offset(100f, 10f),
            to = Offset(100f, 10f)
        )
    }

    @Test
    fun resize_and_move() {
        layer.resize(IntSize(100, 10))
        layer.move(IntOffset(10, 20))

        assertMapping(
            from = Offset(0f, 0f),
            to = Offset(0f, 0f)
        )
        assertMapping(
            from = Offset(100f, 10f),
            to = Offset(100f, 10f)
        )
    }

    @Test
    fun translation_left_top_origin() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            translationX = 10f,
            translationY = 20f,
            transformOrigin = TransformOrigin(0f, 0f)
        )

        assertMapping(
            from = Offset(10f, 20f),
            to = Offset(0f, 0f)
        )
        assertMapping(
            from = Offset(110f, 30f),
            to = Offset(100f, 10f)
        )
    }

    @Test
    fun translation_bottom_right_origin() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            translationX = 10f,
            translationY = 20f,
            transformOrigin = TransformOrigin(1f, 1f)
        )

        assertMapping(
            from = Offset(10f, 20f),
            to = Offset(0f, 0f)
        )
        assertMapping(
            from = Offset(110f, 30f),
            to = Offset(100f, 10f)
        )
    }

    @Test
    fun scale_left_top_origin() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            scaleX = 2f,
            scaleY = 4f,
            transformOrigin = TransformOrigin(0f, 0f)
        )

        assertMapping(
            from = Offset(0f, 0f),
            to = Offset(0f, 0f)
        )
        assertMapping(
            from = Offset(200f, 40f),
            to = Offset(100f, 10f)
        )
    }

    @Test
    fun scale_bottom_right_origin() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            scaleX = 2f,
            scaleY = 4f,
            transformOrigin = TransformOrigin(1f, 1f)
        )

        assertMapping(
            from = Offset(-100f, -30f),
            to = Offset(0f, 0f)
        )
        assertMapping(
            from = Offset(100f, 10f),
            to = Offset(100f, 10f)
        )
    }

    @Test
    fun rotationX_left_top_origin() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            rotationX = 45f,
            transformOrigin = TransformOrigin(0f, 0f),
            cameraDistance = Float.MAX_VALUE
        )

        val y = 10 * cos45
        assertMapping(
            from = Offset(0f, 0f),
            to = Offset(0f, 0f)
        )
        assertMapping(
            from = Offset(100f, y),
            to = Offset(100f, 10f)
        )
    }

    @Test
    fun rotationX_bottom_right_origin() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            rotationX = 45f,
            transformOrigin = TransformOrigin(1f, 1f),
            cameraDistance = Float.MAX_VALUE
        )

        val y = 10 * (1 - cos45)
        assertMapping(
            from = Offset(0f, y),
            to = Offset(0f, 0f)
        )
        assertMapping(
            from = Offset(100f, 10f),
            to = Offset(100f, 10f)
        )
    }

    @Test
    fun rotationY_left_top_origin() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            rotationY = 45f,
            transformOrigin = TransformOrigin(0f, 0f),
            cameraDistance = Float.MAX_VALUE
        )

        val x = 100 * cos45
        assertMapping(
            from = Offset(0f, 0f),
            to = Offset(0f, 0f)
        )
        assertMapping(
            from = Offset(x, 10f),
            to = Offset(100f, 10f)
        )
    }

    @Test
    fun rotationY_bottom_right_origin() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            rotationY = 45f,
            transformOrigin = TransformOrigin(1f, 1f),
            cameraDistance = Float.MAX_VALUE
        )

        val x = 100 * (1 - cos45)
        assertMapping(
            from = Offset(x, 0f),
            to = Offset(0f, 0f)
        )
        assertMapping(
            from = Offset(100f, 10f),
            to = Offset(100f, 10f)
        )
    }

    @Test
    fun rotationZ_left_top_origin() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            rotationZ = 90f,
            transformOrigin = TransformOrigin(0f, 0f)
        )

        assertMapping(
            from = Offset(0f, 0f),
            to = Offset(0f, 0f)
        )
        assertMapping(
            from = Offset(-10f, 100f),
            to = Offset(100f, 10f)
        )
    }

    @Test
    fun rotationZ_bottom_right_origin() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            rotationZ = 90f,
            transformOrigin = TransformOrigin(1f, 1f)
        )

        assertMapping(
            from = Offset(110f, -90f),
            to = Offset(0f, 0f)
        )
        assertMapping(
            from = Offset(100f, 10f),
            to = Offset(100f, 10f)
        )
    }

    @Test
    fun translation_scale_left_top_origin() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            translationX = 60f,
            translationY = 7f,
            scaleX = 2f,
            scaleY = 4f,
            transformOrigin = TransformOrigin(0f, 0f)
        )

        assertMapping(
            from = Offset(60f, 7f),
            to = Offset(0f, 0f)
        )
        assertMapping(
            from = Offset(260f, 47f),
            to = Offset(100f, 10f)
        )
    }

    @Test
    fun translation_rotationZ_left_top_origin() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            translationX = 60f,
            translationY = 7f,
            rotationZ = 90f,
            transformOrigin = TransformOrigin(0f, 0f)
        )

        assertMapping(
            from = Offset(60f, 7f),
            to = Offset(0f, 0f)
        )
        assertMapping(
            from = Offset(50f, 107f),
            to = Offset(100f, 10f)
        )
    }

    @Test
    fun translation_rotationX_left_top_origin() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            translationX = 60f,
            translationY = 7f,
            rotationX = 45f,
            transformOrigin = TransformOrigin(0f, 0f),
            cameraDistance = Float.MAX_VALUE
        )

        val y = 10 * cos45
        assertMapping(
            from = Offset(60f, 7f),
            to = Offset(0f, 0f)
        )
        assertMapping(
            from = Offset(160f, 7f + y),
            to = Offset(100f, 10f)
        )
    }

    @Test
    fun translation_rotationY_left_top_origin() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            translationX = 60f,
            translationY = 7f,
            rotationY = 45f,
            transformOrigin = TransformOrigin(0f, 0f),
            cameraDistance = Float.MAX_VALUE
        )

        val x = 100 * cos45
        assertMapping(
            from = Offset(60f, 7f),
            to = Offset(0f, 0f)
        )
        assertMapping(
            from = Offset(x + 60f, 17f),
            to = Offset(100f, 10f)
        )
    }

    @Test
    fun scale_rotationZ_left_top_origin() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            scaleX = 2f,
            scaleY = 4f,
            rotationZ = 90f,
            transformOrigin = TransformOrigin(0f, 0f)
        )

        assertMapping(
            from = Offset(0f, 0f),
            to = Offset(0f, 0f)
        )
        assertMapping(
            from = Offset(-40f, 200f),
            to = Offset(100f, 10f)
        )
    }

    @Test
    fun translation_scale_rotationZ_left_top_origin() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            translationX = 60f,
            translationY = 7f,
            scaleX = 2f,
            scaleY = 4f,
            rotationZ = 90f,
            transformOrigin = TransformOrigin(0f, 0f)
        )

        assertMapping(
            from = Offset(60f, 7f),
            to = Offset(0f, 0f)
        )
        assertMapping(
            from = Offset(20f, 207f),
            to = Offset(100f, 10f)
        )
    }

    @Test
    fun is_in_layer() {
        layer.resize(IntSize(0, 0))
        layer.updateProperties(
            clip = false
        )

        assertTrue(layer.isInLayer(Offset(-1f, -1f)))
        assertTrue(layer.isInLayer(Offset(0f, 0f)))
        assertTrue(layer.isInLayer(Offset(1f, 1f)))

        layer.resize(IntSize(0, 0))
        layer.updateProperties(
            clip = true
        )

        assertFalse(layer.isInLayer(Offset(-1f, -1f)))
        assertFalse(layer.isInLayer(Offset(0f, 0f)))
        assertFalse(layer.isInLayer(Offset(1f, 1f)))

        layer.resize(IntSize(0, 0))
        layer.updateProperties(
            clip = true,
            shape = CircleShape
        )

        assertFalse(layer.isInLayer(Offset(-1f, -1f)))
        assertFalse(layer.isInLayer(Offset(0f, 0f)))
        assertFalse(layer.isInLayer(Offset(1f, 1f)))

        layer.resize(IntSize(1, 2))
        layer.updateProperties(
            clip = true
        )

        assertFalse(layer.isInLayer(Offset(-1f, -1f)))
        assertTrue(layer.isInLayer(Offset(0f, 0f)))
        assertTrue(layer.isInLayer(Offset(0f, 1f)))
        assertFalse(layer.isInLayer(Offset(0f, 2f)))
        assertFalse(layer.isInLayer(Offset(1f, 0f)))

        layer.resize(IntSize(100, 200))
        layer.updateProperties(
            clip = true,
            shape = CircleShape
        )

        assertFalse(layer.isInLayer(Offset(5f, 5f)))
        assertFalse(layer.isInLayer(Offset(95f, 195f)))
        assertTrue(layer.isInLayer(Offset(50f, 100f)))
    }

    private fun TestSkiaLayer() = SkiaLayer(
        Density(1f, 1f),
        invalidateParentLayer = {},
        drawBlock = {}
    )

    private fun assertMapping(from: Offset, to: Offset) {
        assertEquals(from, matrix.map(to), 0.001f)
        assertEquals(to, inverseMatrix.map(from), 0.001f)
    }

    private fun assertEquals(expected: Offset, actual: Offset, absoluteTolerance: Float) {
        val message = "Expected <$expected>, actual <$actual>."
        assertEquals(expected.x, actual.x, absoluteTolerance, message)
        assertEquals(expected.y, actual.y, absoluteTolerance, message)
    }

    private fun SkiaLayer.updateProperties(
        scaleX: Float = 1f,
        scaleY: Float = 1f,
        alpha: Float = 1f,
        translationX: Float = 0f,
        translationY: Float = 0f,
        shadowElevation: Float = 0f,
        ambientShadowColor: Color = DefaultShadowColor,
        spotShadowColor: Color = DefaultShadowColor,
        rotationX: Float = 0f,
        rotationY: Float = 0f,
        rotationZ: Float = 0f,
        cameraDistance: Float = DefaultCameraDistance,
        transformOrigin: TransformOrigin = TransformOrigin.Center,
        shape: Shape = RectangleShape,
        clip: Boolean = false,
        renderEffect: RenderEffect? = null,
        compositingStrategy: CompositingStrategy = CompositingStrategy.Auto
    ) {
        updateLayerProperties(
            scaleX, scaleY, alpha, translationX, translationY, shadowElevation, rotationX,
            rotationY, rotationZ, cameraDistance, transformOrigin, shape, clip, renderEffect,
            ambientShadowColor, spotShadowColor, compositingStrategy, LayoutDirection.Ltr,
            Density(1f, 1f)
        )
    }
}
