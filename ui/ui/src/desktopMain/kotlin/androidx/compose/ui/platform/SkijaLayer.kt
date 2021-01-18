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

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.DesktopCanvas
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asDesktopPath
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toSkijaRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.node.OwnedLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toBounds
import androidx.compose.ui.unit.toRect
import org.jetbrains.skija.Picture
import org.jetbrains.skija.PictureRecorder
import org.jetbrains.skija.Point3
import org.jetbrains.skija.ShadowUtils

class SkijaLayer(
    private val owner: DesktopOwner,
    private val invalidateParentLayer: () -> Unit,
    private val drawBlock: (Canvas) -> Unit
) : OwnedLayer {
    private var size = IntSize.Zero
    private var position = IntOffset.Zero
    private var outlineCache =
        OutlineCache(owner.density, size, RectangleShape, LayoutDirection.Ltr)
    private val matrix = Matrix()
    private val pictureRecorder = PictureRecorder()
    private var picture: Picture? = null
    private var isDestroyed = false

    private var transformOrigin: TransformOrigin = TransformOrigin.Center
    private var translationX: Float = 0f
    private var translationY: Float = 0f
    private var rotationX: Float = 0f
    private var rotationY: Float = 0f
    private var rotationZ: Float = 0f
    private var scaleX: Float = 1f
    private var scaleY: Float = 1f
    private var alpha: Float = 1f
    private var clip: Boolean = false
    private var shadowElevation: Float = 0f

    override val layerId = lastId++

    override fun destroy() {
        picture?.close()
        pictureRecorder.close()
        isDestroyed = true
    }

    override fun resize(size: IntSize) {
        if (size != this.size) {
            this.size = size
            outlineCache.size = size
            updateMatrix()
            invalidate()
        }
    }

    override fun move(position: IntOffset) {
        if (position != this.position) {
            this.position = position
            invalidateParentLayer()
        }
    }

    override fun getMatrix(matrix: Matrix) {
        matrix.setFrom(this.matrix)
    }

    override fun updateLayerProperties(
        scaleX: Float,
        scaleY: Float,
        alpha: Float,
        translationX: Float,
        translationY: Float,
        shadowElevation: Float,
        rotationX: Float,
        rotationY: Float,
        rotationZ: Float,
        cameraDistance: Float,
        transformOrigin: TransformOrigin,
        shape: Shape,
        clip: Boolean,
        layoutDirection: LayoutDirection
    ) {
        this.transformOrigin = transformOrigin
        this.translationX = translationX
        this.translationY = translationY
        this.rotationX = rotationX
        this.rotationY = rotationY
        this.rotationZ = rotationZ
        this.scaleX = scaleX
        this.scaleY = scaleY
        this.alpha = alpha
        this.clip = clip
        this.shadowElevation = shadowElevation
        outlineCache.shape = shape
        outlineCache.layoutDirection = layoutDirection
        updateMatrix()
        invalidate()
    }

    // TODO(demin): support perspective projection for rotationX/rotationY (as in Android)
    // TODO(njawad) Add camera distance leveraging Sk3DView along with rotationX/rotationY
    // see https://cs.android.com/search?q=RenderProperties.cpp&sq= updateMatrix method
    // for how 3d transformations along with camera distance are applied. b/173402019
    private fun updateMatrix() {
        val pivotX = transformOrigin.pivotFractionX * size.width
        val pivotY = transformOrigin.pivotFractionY * size.height

        matrix.reset()
        matrix *= Matrix().apply {
            translate(x = -pivotX, y = -pivotY)
        }
        matrix *= Matrix().apply {
            translate(translationX, translationY)
            rotateX(rotationX)
            rotateY(rotationY)
            rotateZ(rotationZ)
            scale(scaleX, scaleY)
        }
        matrix *= Matrix().apply {
            translate(x = pivotX, y = pivotY)
        }
    }

    override fun invalidate() {
        if (!isDestroyed && picture != null) {
            picture?.close()
            picture = null
            invalidateParentLayer()
        }
    }

    override fun drawLayer(canvas: Canvas) {
        outlineCache.density = owner.density
        if (picture == null) {
            val bounds = size.toBounds().toRect()
            val pictureCanvas = pictureRecorder.beginRecording(bounds.toSkijaRect())
            performDrawLayer(DesktopCanvas(pictureCanvas), bounds)
            picture = pictureRecorder.finishRecordingAsPicture()
        }

        canvas.save()
        canvas.concat(matrix)
        canvas.translate(position.x.toFloat(), position.y.toFloat())
        canvas.nativeCanvas.drawPicture(picture!!, null, null)
        canvas.restore()
    }

    private fun performDrawLayer(canvas: DesktopCanvas, bounds: Rect) {
        if (alpha > 0) {
            if (shadowElevation > 0) {
                drawShadow(canvas)
            }

            if (alpha < 1) {
                canvas.saveLayer(
                    bounds,
                    Paint().apply { alpha = this@SkijaLayer.alpha }
                )
            } else {
                canvas.save()
            }

            if (clip) {
                when (val outline = outlineCache.outline) {
                    is Outline.Rectangle -> canvas.clipRect(outline.rect)
                    is Outline.Rounded -> canvas.clipRoundRect(outline.roundRect)
                    is Outline.Generic -> canvas.clipPath(outline.path)
                }
            }

            drawBlock(canvas)
            canvas.restore()
        }
    }

    override fun updateDisplayList() = Unit

    @OptIn(ExperimentalUnsignedTypes::class)
    fun drawShadow(canvas: DesktopCanvas) = with(owner.density) {
        val path = when (val outline = outlineCache.outline) {
            is Outline.Rectangle -> Path().apply { addRect(outline.rect) }
            is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
            is Outline.Generic -> outline.path
            else -> return
        }

        // TODO: perspective?
        val zParams = Point3(0f, 0f, shadowElevation)

        // TODO: configurable?
        val lightPos = Point3(0f, 0f, 600.dp.toPx())
        val lightRad = 800.dp.toPx()

        val ambientAlpha = 0.039f * alpha
        val spotAlpha = 0.19f * alpha
        val ambientColor = Color.Black.copy(alpha = ambientAlpha)
        val spotColor = Color.Black.copy(alpha = spotAlpha)

        ShadowUtils.drawShadow(
            canvas.nativeCanvas, path.asDesktopPath(), zParams, lightPos,
            lightRad,
            ambientColor.toArgb(),
            spotColor.toArgb(), alpha < 1f, false
        )
    }

    companion object {
        private var lastId = 0L
    }
}
