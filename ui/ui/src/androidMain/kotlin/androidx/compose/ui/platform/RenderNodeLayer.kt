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

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.CanvasHolder
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.setFrom
import androidx.compose.ui.node.OwnedLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * RenderNode implementation of OwnedLayer.
 */
@RequiresApi(Build.VERSION_CODES.M)
internal class RenderNodeLayer(
    val ownerView: AndroidComposeView,
    val drawBlock: (Canvas) -> Unit,
    val invalidateParentLayer: () -> Unit
) : OwnedLayer {
    /**
     * True when the RenderNodeLayer has been invalidated and not yet drawn.
     */
    private var isDirty = false
    private val outlineResolver = OutlineResolver(ownerView.density)
    private var isDestroyed = false
    private var androidMatrixCache: android.graphics.Matrix? = null
    private var drawnWithZ = false

    private val canvasHolder = CanvasHolder()

    /**
     * Local copy of the transform origin as GraphicsLayerModifier can be implemented
     * as a model object. Update this field within [updateLayerProperties] and use it
     * in [resize] or other methods
     */
    private var transformOrigin: TransformOrigin = TransformOrigin.Center

    private val renderNode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        RenderNodeApi29(ownerView)
    } else {
        RenderNodeApi23(ownerView)
    }.apply { setHasOverlappingRendering(true) }

    override val layerId: Long
        get() = renderNode.uniqueId

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
        clip: Boolean
    ) {
        this.transformOrigin = transformOrigin
        val wasClippingManually = renderNode.clipToOutline && outlineResolver.clipPath != null
        renderNode.scaleX = scaleX
        renderNode.scaleY = scaleY
        renderNode.alpha = alpha
        renderNode.translationX = translationX
        renderNode.translationY = translationY
        renderNode.elevation = shadowElevation
        renderNode.rotationZ = rotationZ
        renderNode.rotationX = rotationX
        renderNode.rotationY = rotationY
        renderNode.cameraDistance = cameraDistance
        renderNode.pivotX = transformOrigin.pivotFractionX * renderNode.width
        renderNode.pivotY = transformOrigin.pivotFractionY * renderNode.height
        renderNode.clipToOutline = clip && shape !== RectangleShape
        renderNode.clipToBounds = clip && shape === RectangleShape
        val shapeChanged = outlineResolver.update(
            shape,
            renderNode.alpha,
            renderNode.clipToOutline,
            renderNode.elevation
        )
        renderNode.setOutline(outlineResolver.outline)
        val isClippingManually = renderNode.clipToOutline && outlineResolver.clipPath != null
        if (wasClippingManually != isClippingManually || (isClippingManually && shapeChanged)) {
            invalidate()
        } else {
            triggerRepaint()
        }
        if (!drawnWithZ && renderNode.elevation > 0f) {
            invalidateParentLayer()
        }
    }

    override fun resize(size: IntSize) {
        val width = size.width
        val height = size.height
        renderNode.pivotX = transformOrigin.pivotFractionX * width
        renderNode.pivotY = transformOrigin.pivotFractionY * height
        if (renderNode.setPosition(
                renderNode.left,
                renderNode.top,
                renderNode.left + width,
                renderNode.top + height
            )
        ) {
            outlineResolver.update(Size(width.toFloat(), height.toFloat()))
            renderNode.setOutline(outlineResolver.outline)
            invalidate()
        }
    }

    override fun move(position: IntOffset) {
        val oldLeft = renderNode.left
        val oldTop = renderNode.top
        val newLeft = position.x
        val newTop = position.y
        if (oldLeft != newLeft || oldTop != newTop) {
            renderNode.offsetLeftAndRight(newLeft - oldLeft)
            renderNode.offsetTopAndBottom(newTop - oldTop)
            triggerRepaint()
        }
    }

    override fun invalidate() {
        if (!isDirty && !isDestroyed) {
            ownerView.invalidate()
            ownerView.dirtyLayers += this
            isDirty = true
        }
    }

    /**
     * This only triggers the system so that it knows that some kind of painting
     * must happen without actually causing the layer to be invalidated and have
     * to re-record its drawing.
     */
    private fun triggerRepaint() {
        // onDescendantInvalidated is only supported on O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ownerView.parent?.onDescendantInvalidated(ownerView, ownerView)
        } else {
            ownerView.invalidate()
        }
    }

    override fun drawLayer(canvas: Canvas) {
        val androidCanvas = canvas.nativeCanvas
        if (androidCanvas.isHardwareAccelerated) {
            updateDisplayList()
            drawnWithZ = renderNode.elevation > 0f
            if (drawnWithZ) {
                canvas.enableZ()
            }
            renderNode.drawInto(androidCanvas)
            if (drawnWithZ) {
                canvas.disableZ()
            }
        } else {
            drawBlock(canvas)
        }
        isDirty = false
    }

    override fun updateDisplayList() {
        if (isDirty || !renderNode.hasDisplayList) {
            val clipPath = if (renderNode.clipToOutline) outlineResolver.clipPath else null

            renderNode.record(canvasHolder, clipPath, drawBlock)

            isDirty = false
        }
    }

    override fun destroy() {
        isDestroyed = true
        ownerView.dirtyLayers -= this
        ownerView.requestClearInvalidObservations()
    }

    override fun getMatrix(matrix: Matrix) {
        val androidMatrix = androidMatrixCache ?: android.graphics.Matrix().also {
            androidMatrixCache = it
        }
        renderNode.getMatrix(androidMatrix)
        matrix.setFrom(androidMatrix)
    }
}
