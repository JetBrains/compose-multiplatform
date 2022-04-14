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

import android.annotation.SuppressLint
import android.os.Build
import android.view.View
import android.view.ViewOutlineProvider
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.MutableRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.CanvasHolder
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.GraphicLayerInfo
import androidx.compose.ui.node.OwnedLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * View implementation of OwnedLayer.
 */
internal class ViewLayer(
    val ownerView: AndroidComposeView,
    val container: DrawChildContainer,
    drawBlock: (Canvas) -> Unit,
    invalidateParentLayer: () -> Unit
) : View(ownerView.context), OwnedLayer, GraphicLayerInfo {
    private var drawBlock: ((Canvas) -> Unit)? = drawBlock
    private var invalidateParentLayer: (() -> Unit)? = invalidateParentLayer

    private val outlineResolver = OutlineResolver(ownerView.density)
    // Value of the layerModifier's clipToBounds property
    private var clipToBounds = false
    private var clipBoundsCache: android.graphics.Rect? = null
    private val manualClipPath: Path? get() =
        if (!clipToOutline || outlineResolver.outlineClipSupported) {
            null
        } else {
            outlineResolver.clipPath
        }
    var isInvalidated = false
        private set(value) {
            if (value != field) {
                field = value
                ownerView.notifyLayerIsDirty(this, value)
            }
        }
    private var drawnWithZ = false
    private val canvasHolder = CanvasHolder()

    private val matrixCache = LayerMatrixCache(getMatrix)

    /**
     * Local copy of the transform origin as GraphicsLayerModifier can be implemented
     * as a model object. Update this field within [updateLayerProperties] and use it
     * in [resize] or other methods
     */
    private var mTransformOrigin: TransformOrigin = TransformOrigin.Center

    init {
        setWillNotDraw(false) // we WILL draw
        id = generateViewId()
        container.addView(this)
    }

    override val layerId: Long
        get() = id.toLong()

    override val ownerViewId: Long
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            UniqueDrawingIdApi29.getUniqueDrawingId(ownerView)
        } else {
            -1
        }

    @RequiresApi(29)
    private object UniqueDrawingIdApi29 {
        @JvmStatic
        @androidx.annotation.DoNotInline
        fun getUniqueDrawingId(view: View) = view.uniqueDrawingId
    }

    /**
     * Configure the camera distance on the View in pixels. View already has a get/setCameraDistance
     * API however, that operates in Dp values.
     */
    var cameraDistancePx: Float
        get() {
            // View internally converts distance to dp so divide by density here to have
            // consistent usage of pixels with RenderNode that is backing the View
            return cameraDistance / resources.displayMetrics.densityDpi
        }
        set(value) {
            // View internally converts distance to dp so multiply by density here to have
            // consistent usage of pixels with RenderNode that is backing the View
            cameraDistance = value * resources.displayMetrics.densityDpi
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
        this.mTransformOrigin = transformOrigin
        this.scaleX = scaleX
        this.scaleY = scaleY
        this.alpha = alpha
        this.translationX = translationX
        this.translationY = translationY
        this.elevation = shadowElevation
        this.rotation = rotationZ
        this.rotationX = rotationX
        this.rotationY = rotationY
        this.pivotX = mTransformOrigin.pivotFractionX * width
        this.pivotY = mTransformOrigin.pivotFractionY * height
        this.cameraDistancePx = cameraDistance
        this.clipToBounds = clip && shape === RectangleShape
        resetClipBounds()
        val wasClippingManually = manualClipPath != null
        this.clipToOutline = clip && shape !== RectangleShape
        val shapeChanged = outlineResolver.update(
            shape,
            this.alpha,
            this.clipToOutline,
            this.elevation,
            layoutDirection,
            density
        )
        updateOutlineResolver()
        val isClippingManually = manualClipPath != null
        if (wasClippingManually != isClippingManually || (isClippingManually && shapeChanged)) {
            invalidate() // have to redraw the content
        }
        if (!drawnWithZ && elevation > 0) {
            invalidateParentLayer?.invoke()
        }
        matrixCache.invalidate()
        if (Build.VERSION.SDK_INT >= 28) {
            ViewLayerVerificationHelper28.setOutlineAmbientShadowColor(
                this,
                ambientShadowColor.toArgb()
            )
            ViewLayerVerificationHelper28.setOutlineSpotShadowColor(this, spotShadowColor.toArgb())
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ViewLayerVerificationHelper31.setRenderEffect(this, renderEffect)
        }
    }

    override fun isInLayer(position: Offset): Boolean {
        val x = position.x
        val y = position.y
        if (clipToBounds) {
            return 0f <= x && x < width && 0f <= y && y < height
        }

        if (clipToOutline) {
            return outlineResolver.isInOutline(position)
        }

        return true
    }

    private fun updateOutlineResolver() {
        this.outlineProvider = if (outlineResolver.outline != null) {
            OutlineProvider
        } else {
            null
        }
    }

    private fun resetClipBounds() {
        this.clipBounds = if (clipToBounds) {
            if (clipBoundsCache == null) {
                clipBoundsCache = android.graphics.Rect(0, 0, width, height)
            } else {
                clipBoundsCache!!.set(0, 0, width, height)
            }
            clipBoundsCache
        } else {
            null
        }
    }

    override fun resize(size: IntSize) {
        val width = size.width
        val height = size.height
        if (width != this.width || height != this.height) {
            pivotX = mTransformOrigin.pivotFractionX * width
            pivotY = mTransformOrigin.pivotFractionY * height
            outlineResolver.update(Size(width.toFloat(), height.toFloat()))
            updateOutlineResolver()
            layout(left, top, left + width, top + height)
            resetClipBounds()
            matrixCache.invalidate()
        }
    }

    override fun move(position: IntOffset) {
        val left = position.x

        if (left != this.left) {
            offsetLeftAndRight(left - this.left)
            matrixCache.invalidate()
        }
        val top = position.y
        if (top != this.top) {
            offsetTopAndBottom(top - this.top)
            matrixCache.invalidate()
        }
    }

    override fun drawLayer(canvas: Canvas) {
        drawnWithZ = elevation > 0f
        if (drawnWithZ) {
            canvas.enableZ()
        }
        container.drawChild(canvas, this, drawingTime)
        if (drawnWithZ) {
            canvas.disableZ()
        }
    }

    override fun dispatchDraw(canvas: android.graphics.Canvas) {
        isInvalidated = false
        canvasHolder.drawInto(canvas) {
            var didClip = false
            val clipPath = manualClipPath
            if (clipPath != null || !canvas.isHardwareAccelerated) {
                didClip = true
                save()
                outlineResolver.clipToOutline(this)
            }
            drawBlock?.invoke(this)
            if (didClip) {
                restore()
            }
        }
    }

    override fun invalidate() {
        if (!isInvalidated) {
            isInvalidated = true
            super.invalidate()
            ownerView.invalidate()
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    }

    override fun destroy() {
        isInvalidated = false
        ownerView.requestClearInvalidObservations()
        drawBlock = null
        invalidateParentLayer = null

        // L throws during RenderThread when reusing the Views. The stack trace
        // wasn't easy to decode, so this work-around keeps up to 10 Views active
        // only for L. On other versions, it uses the WeakHashMap to retain as many
        // as are convenient.

        val recycle = ownerView.recycle(this@ViewLayer)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M || shouldUseDispatchDraw || !recycle) {
            container.removeViewInLayout(this)
        } else {
            visibility = GONE
        }
    }

    override fun updateDisplayList() {
        if (isInvalidated && !shouldUseDispatchDraw) {
            isInvalidated = false
            updateDisplayList(this)
        }
    }

    override fun forceLayout() {
        // Don't do anything. These Views are treated as RenderNodes, so a forced layout
        // should not do anything. If we keep this, we get more redrawing than is necessary.
    }

    override fun mapOffset(point: Offset, inverse: Boolean): Offset {
        return if (inverse) {
            matrixCache.calculateInverseMatrix(this)?.map(point) ?: Offset.Infinite
        } else {
            matrixCache.calculateMatrix(this).map(point)
        }
    }

    override fun mapBounds(rect: MutableRect, inverse: Boolean) {
        if (inverse) {
            val matrix = matrixCache.calculateInverseMatrix(this)
            if (matrix != null) {
                matrix.map(rect)
            } else {
                rect.set(0f, 0f, 0f, 0f)
            }
        } else {
            matrixCache.calculateMatrix(this).map(rect)
        }
    }

    override fun reuseLayer(drawBlock: (Canvas) -> Unit, invalidateParentLayer: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M || shouldUseDispatchDraw) {
            container.addView(this)
        } else {
            visibility = VISIBLE
        }
        clipToBounds = false
        drawnWithZ = false
        mTransformOrigin = TransformOrigin.Center
        this.drawBlock = drawBlock
        this.invalidateParentLayer = invalidateParentLayer
    }

    companion object {
        private val getMatrix: (View, android.graphics.Matrix) -> Unit = { view, matrix ->
            val newMatrix = view.matrix
            matrix.set(newMatrix)
        }

        val OutlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: android.graphics.Outline) {
                view as ViewLayer
                outline.set(view.outlineResolver.outline!!)
            }
        }
        private var updateDisplayListIfDirtyMethod: Method? = null
        private var recreateDisplayList: Field? = null
        var hasRetrievedMethod = false
            private set

        var shouldUseDispatchDraw = false
            internal set // internal so that tests can use it.

        @SuppressLint("BanUncheckedReflection")
        fun updateDisplayList(view: View) {
            try {
                if (!hasRetrievedMethod) {
                    hasRetrievedMethod = true
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                        updateDisplayListIfDirtyMethod =
                            View::class.java.getDeclaredMethod("updateDisplayListIfDirty")
                        recreateDisplayList =
                            View::class.java.getDeclaredField("mRecreateDisplayList")
                    } else {
                        val getDeclaredMethod = Class::class.java.getDeclaredMethod(
                            "getDeclaredMethod",
                            String::class.java,
                            arrayOf<Class<*>>()::class.java
                        )
                        updateDisplayListIfDirtyMethod = getDeclaredMethod.invoke(
                            View::class.java,
                            "updateDisplayListIfDirty", emptyArray<Class<*>>()
                        ) as Method?
                        val getDeclaredField = Class::class.java.getDeclaredMethod(
                            "getDeclaredField",
                            String::class.java
                        )
                        recreateDisplayList = getDeclaredField.invoke(
                            View::class.java,
                            "mRecreateDisplayList"
                        ) as Field?
                    }
                    updateDisplayListIfDirtyMethod?.isAccessible = true
                    recreateDisplayList?.isAccessible = true
                }
                recreateDisplayList?.setBoolean(view, true)
                updateDisplayListIfDirtyMethod?.invoke(view)
            } catch (_: Throwable) {
                shouldUseDispatchDraw = true
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.S)
private object ViewLayerVerificationHelper31 {

    @androidx.annotation.DoNotInline
    fun setRenderEffect(view: View, target: RenderEffect?) {
        view.setRenderEffect(target?.asAndroidRenderEffect())
    }
}

@RequiresApi(Build.VERSION_CODES.P)
private object ViewLayerVerificationHelper28 {

    @androidx.annotation.DoNotInline
    fun setOutlineAmbientShadowColor(view: View, target: Int) {
        view.outlineAmbientShadowColor = target
    }

    @androidx.annotation.DoNotInline
    fun setOutlineSpotShadowColor(view: View, target: Int) {
        view.outlineSpotShadowColor = target
    }
}
