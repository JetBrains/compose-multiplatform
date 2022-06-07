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

import androidx.compose.ui.geometry.MutableRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.DefaultShadowColor
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.graphics.asSkiaPath
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toSkiaRRect
import androidx.compose.ui.graphics.toSkiaRect
import androidx.compose.ui.node.OwnedLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import org.jetbrains.skia.ClipMode
import org.jetbrains.skia.Picture
import org.jetbrains.skia.PictureRecorder
import org.jetbrains.skia.Point3
import org.jetbrains.skia.ShadowUtils

internal class SkiaLayer(
    private var density: Density,
    private val invalidateParentLayer: () -> Unit,
    private val drawBlock: (Canvas) -> Unit,
    private val onDestroy: () -> Unit = {}
) : OwnedLayer {
    private var size = IntSize.Zero
    private var position = IntOffset.Zero
    private var outlineCache =
        OutlineCache(density, size, RectangleShape, LayoutDirection.Ltr)
    // Internal for testing
    internal val matrix = Matrix()
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
    private var renderEffect: RenderEffect? = null
    private var shadowElevation: Float = 0f
    private var ambientShadowColor: Color = DefaultShadowColor
    private var spotShadowColor: Color = DefaultShadowColor

    override fun destroy() {
        picture?.close()
        pictureRecorder.close()
        isDestroyed = true
        onDestroy()
    }

    override fun reuseLayer(drawBlock: (Canvas) -> Unit, invalidateParentLayer: () -> Unit) {
        // TODO: in destroy, call recycle, and reconfigure this layer to be ready to use here.
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

    override fun mapOffset(point: Offset, inverse: Boolean): Offset {
        return getMatrix(inverse).map(point)
    }

    override fun mapBounds(rect: MutableRect, inverse: Boolean) {
        getMatrix(inverse).map(rect)
    }

    override fun isInLayer(position: Offset): Boolean {
        if (!clip) {
            return true
        }

        val x = position.x
        val y = position.y
        if (outlineCache.shape === RectangleShape) {
            return 0f <= x && x < size.width && 0f <= y && y < size.height
        }

        return isInOutline(outlineCache.outline, x, y)
    }

    private fun getMatrix(inverse: Boolean): Matrix {
        return if (inverse) {
            Matrix().apply {
                setFrom(matrix)
                invert()
            }
        } else {
            matrix
        }
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
        renderEffect: RenderEffect?,
        ambientShadowColor: Color,
        spotShadowColor: Color,
        layoutDirection: LayoutDirection,
        density: Density
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
        this.density = density
        this.renderEffect = renderEffect
        this.ambientShadowColor = ambientShadowColor
        this.spotShadowColor = spotShadowColor
        outlineCache.shape = shape
        outlineCache.layoutDirection = layoutDirection
        outlineCache.density = density
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
        if (picture == null) {
            val bounds = size.toSize().toRect()
            val pictureCanvas = pictureRecorder.beginRecording(bounds.toSkiaRect())
            performDrawLayer(pictureCanvas.asComposeCanvas(), bounds)
            picture = pictureRecorder.finishRecordingAsPicture()
        }

        canvas.save()
        canvas.concat(matrix)
        canvas.translate(position.x.toFloat(), position.y.toFloat())
        canvas.nativeCanvas.drawPicture(picture!!, null, null)
        canvas.restore()
    }

    private fun performDrawLayer(canvas: Canvas, bounds: Rect) {
        if (alpha > 0) {
            if (shadowElevation > 0) {
                drawShadow(canvas)
            }

            if (clip) {
                canvas.save()
                when (val outline = outlineCache.outline) {
                    is Outline.Rectangle -> canvas.clipRect(outline.rect)
                    is Outline.Rounded -> canvas.clipRoundRect(outline.roundRect)
                    is Outline.Generic -> canvas.clipPath(outline.path)
                }
            }

            val currentRenderEffect = renderEffect
            if (alpha < 1 || currentRenderEffect != null) {
                canvas.saveLayer(
                    bounds,
                    Paint().apply {
                        alpha = this@SkiaLayer.alpha
                        asFrameworkPaint().imageFilter = currentRenderEffect?.asSkiaImageFilter()
                    }
                )
            } else {
                canvas.save()
            }

            drawBlock(canvas)
            canvas.restore()
            if (clip) {
                canvas.restore()
            }
        }
    }

    private fun Canvas.clipRoundRect(rect: RoundRect, clipOp: ClipOp = ClipOp.Intersect) {
        val antiAlias = true
        nativeCanvas.clipRRect(rect.toSkiaRRect(), clipOp.toSkia(), antiAlias)
    }

    private fun ClipOp.toSkia() = when (this) {
        ClipOp.Difference -> ClipMode.DIFFERENCE
        ClipOp.Intersect -> ClipMode.INTERSECT
        else -> ClipMode.INTERSECT
    }

    override fun updateDisplayList() = Unit

    fun drawShadow(canvas: Canvas) = with(density) {
        val path = when (val outline = outlineCache.outline) {
            is Outline.Rectangle -> Path().apply { addRect(outline.rect) }
            is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
            is Outline.Generic -> outline.path
            else -> return
        }

        // TODO: perspective?
        val zParams = Point3(0f, 0f, shadowElevation)

        // TODO: configurable?
        val lightPos = Point3(0f, -300.dp.toPx(), 600.dp.toPx())
        val lightRad = 800.dp.toPx()

        val ambientAlpha = 0.039f * alpha
        val spotAlpha = 0.19f * alpha
        val ambientColor = ambientShadowColor.copy(alpha = ambientAlpha)
        val spotColor = spotShadowColor.copy(alpha = spotAlpha)

        ShadowUtils.drawShadow(
            canvas.nativeCanvas, path.asSkiaPath(), zParams, lightPos,
            lightRad,
            ambientColor.toArgb(),
            spotColor.toArgb(), alpha < 1f, false
        )
    }
}
