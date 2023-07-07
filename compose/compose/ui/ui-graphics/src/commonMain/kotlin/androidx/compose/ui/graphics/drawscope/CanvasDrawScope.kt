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

package androidx.compose.ui.graphics.drawscope

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope.Companion.DefaultFilterQuality
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.center

/**
 * Default density value that is used as a stub to provide a non-null
 * density parameter within CanvasDrawScope.
 * Density is provided as a parameter as part of the draw call to
 * issue drawing commands into a target canvas so this Density value is never consumed
 */
private val DefaultDensity = Density(1.0f, 1.0f)

/**
 * Implementation of [DrawScope] that issues drawing commands
 * into the specified canvas and bounds via [CanvasDrawScope.draw]
 */
class CanvasDrawScope : DrawScope {

    @PublishedApi internal val drawParams = DrawParams()

    override val layoutDirection: LayoutDirection
        get() = drawParams.layoutDirection

    override val density: Float
        get() = drawParams.density.density

    override val fontScale: Float
        get() = drawParams.density.fontScale

    override val drawContext = object : DrawContext {
        override val canvas: Canvas
            get() = drawParams.canvas

        override var size: Size
            get() = drawParams.size
            set(value) {
                drawParams.size = value
            }

        override val transform: DrawTransform = asDrawTransform()
    }

    /**
     * Internal [Paint] used only for drawing filled in shapes with a color or gradient
     * This is lazily allocated on the first drawing command that uses the [Fill] [DrawStyle]
     * and re-used across subsequent calls
     */
    private var fillPaint: Paint? = null

    /**
     * Internal [Paint] used only for drawing stroked shapes with a color or gradient
     * This is lazily allocated on the first drawing command that uses the [Stroke] [DrawStyle]
     * and re-used across subsequent calls
     */
    private var strokePaint: Paint? = null

    /**
     * @see [DrawScope.drawLine]
     */
    override fun drawLine(
        brush: Brush,
        start: Offset,
        end: Offset,
        strokeWidth: Float,
        cap: StrokeCap,
        pathEffect: PathEffect?,
        /*FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) = drawParams.canvas.drawLine(
        start,
        end,
        configureStrokePaint(
            brush,
            strokeWidth,
            Stroke.DefaultMiter,
            cap,
            StrokeJoin.Miter,
            pathEffect,
            alpha,
            colorFilter,
            blendMode
        )
    )

    /**
     * @see [DrawScope.drawLine]
     */
    override fun drawLine(
        color: Color,
        start: Offset,
        end: Offset,
        strokeWidth: Float,
        cap: StrokeCap,
        pathEffect: PathEffect?,
        /*FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) = drawParams.canvas.drawLine(
        start,
        end,
        configureStrokePaint(
            color,
            strokeWidth,
            Stroke.DefaultMiter,
            cap,
            StrokeJoin.Miter,
            pathEffect,
            alpha,
            colorFilter,
            blendMode
        )
    )

    /**
     * @see [DrawScope.drawRect]
     */
    override fun drawRect(
        brush: Brush,
        topLeft: Offset,
        size: Size,
        /*FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) = drawParams.canvas.drawRect(
        left = topLeft.x,
        top = topLeft.y,
        right = topLeft.x + size.width,
        bottom = topLeft.y + size.height,
        paint = configurePaint(brush, style, alpha, colorFilter, blendMode)
    )

    /**
     * @see [DrawScope.drawRect]
     */
    override fun drawRect(
        color: Color,
        topLeft: Offset,
        size: Size,
        /*FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) = drawParams.canvas.drawRect(
        left = topLeft.x,
        top = topLeft.y,
        right = topLeft.x + size.width,
        bottom = topLeft.y + size.height,
        paint = configurePaint(color, style, alpha, colorFilter, blendMode)
    )

    /**
     * @see [DrawScope.drawImage]
     */
    override fun drawImage(
        image: ImageBitmap,
        topLeft: Offset,
        /*FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) = drawParams.canvas.drawImage(
        image,
        topLeft,
        configurePaint(null, style, alpha, colorFilter, blendMode)
    )

    /**
     * @see [DrawScope.drawImage]
     */
    @Deprecated(
        "Prefer usage of drawImage that consumes an optional FilterQuality parameter",
        replaceWith = ReplaceWith(
            "drawImage(image, srcOffset, srcSize, dstOffset, dstSize, alpha, style," +
                " colorFilter, blendMode, FilterQuality.Low)",
            "androidx.compose.ui.graphics.drawscope",
            "androidx.compose.ui.graphics.FilterQuality"
        ),
        level = DeprecationLevel.HIDDEN
    )
    override fun drawImage(
        image: ImageBitmap,
        srcOffset: IntOffset,
        srcSize: IntSize,
        dstOffset: IntOffset,
        dstSize: IntSize,
        /*FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) = drawParams.canvas.drawImageRect(
        image,
        srcOffset,
        srcSize,
        dstOffset,
        dstSize,
        configurePaint(null, style, alpha, colorFilter, blendMode)
    )

    /**
     * @see [DrawScope.drawImage]
     */
    override fun drawImage(
        image: ImageBitmap,
        srcOffset: IntOffset,
        srcSize: IntSize,
        dstOffset: IntOffset,
        dstSize: IntSize,
        /*FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
        filterQuality: FilterQuality
    ) = drawParams.canvas.drawImageRect(
        image,
        srcOffset,
        srcSize,
        dstOffset,
        dstSize,
        configurePaint(null, style, alpha, colorFilter, blendMode, filterQuality)
    )

    /**
     * @see [DrawScope.drawRoundRect]
     */
    override fun drawRoundRect(
        brush: Brush,
        topLeft: Offset,
        size: Size,
        cornerRadius: CornerRadius,
        /*FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) = drawParams.canvas.drawRoundRect(
        topLeft.x,
        topLeft.y,
        topLeft.x + size.width,
        topLeft.y + size.height,
        cornerRadius.x,
        cornerRadius.y,
        configurePaint(brush, style, alpha, colorFilter, blendMode)
    )

    /**
     * @see [DrawScope.drawRoundRect]
     */
    override fun drawRoundRect(
        color: Color,
        topLeft: Offset,
        size: Size,
        cornerRadius: CornerRadius,
        style: DrawStyle,
        /*FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) = drawParams.canvas.drawRoundRect(
        topLeft.x,
        topLeft.y,
        topLeft.x + size.width,
        topLeft.y + size.height,
        cornerRadius.x,
        cornerRadius.y,
        configurePaint(color, style, alpha, colorFilter, blendMode)
    )

    /**
     * @see [DrawScope.drawCircle]
     */
    override fun drawCircle(
        brush: Brush,
        radius: Float,
        center: Offset,
        /*FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) = drawParams.canvas.drawCircle(
        center,
        radius,
        configurePaint(brush, style, alpha, colorFilter, blendMode)
    )

    /**
     * @see [DrawScope.drawCircle]
     */
    override fun drawCircle(
        color: Color,
        radius: Float,
        center: Offset,
        /*FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) = drawParams.canvas.drawCircle(
        center,
        radius,
        configurePaint(color, style, alpha, colorFilter, blendMode)
    )

    /**
     * @see [DrawScope.drawOval]
     */
    override fun drawOval(
        brush: Brush,
        topLeft: Offset,
        size: Size,
        /*FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) = drawParams.canvas.drawOval(
        left = topLeft.x,
        top = topLeft.y,
        right = topLeft.x + size.width,
        bottom = topLeft.y + size.height,
        paint = configurePaint(brush, style, alpha, colorFilter, blendMode)
    )

    /**
     * @see [DrawScope.drawOval]
     */
    override fun drawOval(
        color: Color,
        topLeft: Offset,
        size: Size,
        /*FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) = drawParams.canvas.drawOval(
        left = topLeft.x,
        top = topLeft.y,
        right = topLeft.x + size.width,
        bottom = topLeft.y + size.height,
        paint = configurePaint(color, style, alpha, colorFilter, blendMode)
    )

    /**
     * @see [DrawScope.drawArc]
     */
    override fun drawArc(
        brush: Brush,
        startAngle: Float,
        sweepAngle: Float,
        useCenter: Boolean,
        topLeft: Offset,
        size: Size,
        /*FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) = drawParams.canvas.drawArc(
        left = topLeft.x,
        top = topLeft.y,
        right = topLeft.x + size.width,
        bottom = topLeft.y + size.height,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = useCenter,
        paint = configurePaint(brush, style, alpha, colorFilter, blendMode)
    )

    /**
     * @see [DrawScope.drawArc]
     */
    override fun drawArc(
        color: Color,
        startAngle: Float,
        sweepAngle: Float,
        useCenter: Boolean,
        topLeft: Offset,
        size: Size,
        /*FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) = drawParams.canvas.drawArc(
        left = topLeft.x,
        top = topLeft.y,
        right = topLeft.x + size.width,
        bottom = topLeft.y + size.height,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = useCenter,
        paint = configurePaint(color, style, alpha, colorFilter, blendMode)
    )

    /**
     * @see [DrawScope.drawPath]
     */
    override fun drawPath(
        path: Path,
        color: Color,
        /*FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) = drawParams.canvas.drawPath(
        path,
        configurePaint(color, style, alpha, colorFilter, blendMode)
    )

    /**
     * @see [DrawScope.drawPath]
     */
    override fun drawPath(
        path: Path,
        brush: Brush,
        /*FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) = drawParams.canvas.drawPath(
        path,
        configurePaint(brush, style, alpha, colorFilter, blendMode)
    )

    /**
     * @see [DrawScope.drawPoints]
     */
    override fun drawPoints(
        points: List<Offset>,
        pointMode: PointMode,
        color: Color,
        strokeWidth: Float,
        cap: StrokeCap,
        pathEffect: PathEffect?,
        /*FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) = drawParams.canvas.drawPoints(
        pointMode,
        points,
        configureStrokePaint(
            color,
            strokeWidth,
            Stroke.DefaultMiter,
            cap,
            StrokeJoin.Miter,
            pathEffect,
            alpha,
            colorFilter,
            blendMode
        )
    )

    /**
     * @see [DrawScope.drawPoints]
     */
    override fun drawPoints(
        points: List<Offset>,
        pointMode: PointMode,
        brush: Brush,
        strokeWidth: Float,
        cap: StrokeCap,
        pathEffect: PathEffect?,
        /*FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) = drawParams.canvas.drawPoints(
        pointMode,
        points,
        configureStrokePaint(
            brush,
            strokeWidth,
            Stroke.DefaultMiter,
            cap,
            StrokeJoin.Miter,
            pathEffect,
            alpha,
            colorFilter,
            blendMode
        )
    )

    /**
     * Draws into the provided [Canvas] with the commands specified in the lambda with this
     * [DrawScope] as a receiver
     *
     * @param canvas target canvas to render into
     * @param size bounds relative to the current canvas translation in which the [DrawScope]
     * should draw within
     * @param block lambda that is called to issue drawing commands on this [DrawScope]
     */
    inline fun draw(
        density: Density,
        layoutDirection: LayoutDirection,
        canvas: Canvas,
        size: Size,
        block: DrawScope.() -> Unit
    ) {
        // Remember the previous drawing parameters in case we are temporarily re-directing our
        // drawing to a separate Layer/RenderNode only to draw that content back into the original
        // Canvas. If there is no previous canvas that was being drawing into, this ends up
        // resetting these parameters back to defaults defensively
        val (prevDensity, prevLayoutDirection, prevCanvas, prevSize) = drawParams
        drawParams.apply {
            this.density = density
            this.layoutDirection = layoutDirection
            this.canvas = canvas
            this.size = size
        }
        canvas.save()
        this.block()
        canvas.restore()
        drawParams.apply {
            this.density = prevDensity
            this.layoutDirection = prevLayoutDirection
            this.canvas = prevCanvas
            this.size = prevSize
        }
    }

    /**
     * Internal published APIs used to support inline scoped extension methods
     * on DrawScope directly, without exposing the underlying stateful APIs
     * to conduct the transformations themselves as inline methods require
     * all methods called within them to be public
     */

    /**
     * Helper method to instantiate the paint object on first usage otherwise
     * return the previously allocated Paint used for drawing filled regions
     */
    private fun obtainFillPaint(): Paint =
        fillPaint ?: Paint().apply { style = PaintingStyle.Fill }.also {
            fillPaint = it
        }

    /**
     * Helper method to instantiate the paint object on first usage otherwise
     * return the previously allocated Paint used for drawing strokes
     */
    private fun obtainStrokePaint(): Paint =
        strokePaint ?: Paint().apply { style = PaintingStyle.Stroke }.also {
            strokePaint = it
        }

    /**
     * Selects the appropriate [Paint] object based on the style
     * and applies the underlying [DrawStyle] parameters
     */
    private fun selectPaint(drawStyle: DrawStyle): Paint =
        when (drawStyle) {
            Fill -> obtainFillPaint()
            is Stroke ->
                obtainStrokePaint()
                    .apply {
                        if (strokeWidth != drawStyle.width) strokeWidth = drawStyle.width
                        if (strokeCap != drawStyle.cap) strokeCap = drawStyle.cap
                        if (strokeMiterLimit != drawStyle.miter) strokeMiterLimit = drawStyle.miter
                        if (strokeJoin != drawStyle.join) strokeJoin = drawStyle.join
                        if (pathEffect != drawStyle.pathEffect) pathEffect = drawStyle.pathEffect
                    }
        }

    /**
     * Helper method to configure the corresponding [Brush] along with other properties
     * on the corresponding paint specified by [DrawStyle]
     */
    private fun configurePaint(
        brush: Brush?,
        style: DrawStyle,
        /*FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
        filterQuality: FilterQuality = DefaultFilterQuality
    ): Paint = selectPaint(style).apply {
        if (brush != null) {
            brush.applyTo(size, this, alpha)
        } else if (this.alpha != alpha) {
            this.alpha = alpha
        }
        if (this.colorFilter != colorFilter) this.colorFilter = colorFilter
        if (this.blendMode != blendMode) this.blendMode = blendMode
        if (this.filterQuality != filterQuality) this.filterQuality = filterQuality
    }

    /**
     * Helper method to configure the corresponding [Color] along with other properties
     * on the corresponding paint specified by [DrawStyle]
     */
    private fun configurePaint(
        color: Color,
        style: DrawStyle,
        /*FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
        filterQuality: FilterQuality = DefaultFilterQuality
    ): Paint = selectPaint(style).apply {
        // Modulate the color alpha directly
        // instead of configuring a separate alpha parameter
        val targetColor = color.modulate(alpha)
        if (this.color != targetColor) this.color = targetColor
        if (this.shader != null) this.shader = null
        if (this.colorFilter != colorFilter) this.colorFilter = colorFilter
        if (this.blendMode != blendMode) this.blendMode = blendMode
        if (this.filterQuality != filterQuality) this.filterQuality = filterQuality
    }

    private fun configureStrokePaint(
        color: Color,
        strokeWidth: Float,
        miter: Float,
        cap: StrokeCap,
        join: StrokeJoin,
        pathEffect: PathEffect?,
        /*FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
        filterQuality: FilterQuality = DefaultFilterQuality
    ) =
        obtainStrokePaint().apply {
            // Modulate the color alpha directly
            // instead of configuring a separate alpha parameter
            val targetColor = color.modulate(alpha)
            if (this.color != targetColor) this.color = targetColor
            if (this.shader != null) this.shader = null
            if (this.colorFilter != colorFilter) this.colorFilter = colorFilter
            if (this.blendMode != blendMode) this.blendMode = blendMode
            if (this.strokeWidth != strokeWidth) this.strokeWidth = strokeWidth
            if (this.strokeMiterLimit != miter) this.strokeMiterLimit = miter
            if (this.strokeCap != cap) this.strokeCap = cap
            if (this.strokeJoin != join) this.strokeJoin = join
            if (this.pathEffect != pathEffect) this.pathEffect = pathEffect
            if (this.filterQuality != filterQuality) this.filterQuality = filterQuality
        }

    private fun configureStrokePaint(
        brush: Brush?,
        strokeWidth: Float,
        miter: Float,
        cap: StrokeCap,
        join: StrokeJoin,
        pathEffect: PathEffect?,
        /*FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
        filterQuality: FilterQuality = DefaultFilterQuality
    ) = obtainStrokePaint().apply {
        if (brush != null) {
            brush.applyTo(size, this, alpha)
        } else if (this.alpha != alpha) {
            this.alpha = alpha
        }
        if (this.colorFilter != colorFilter) this.colorFilter = colorFilter
        if (this.blendMode != blendMode) this.blendMode = blendMode
        if (this.strokeWidth != strokeWidth) this.strokeWidth = strokeWidth
        if (this.strokeMiterLimit != miter) this.strokeMiterLimit = miter
        if (this.strokeCap != cap) this.strokeCap = cap
        if (this.strokeJoin != join) this.strokeJoin = join
        if (this.pathEffect != pathEffect) this.pathEffect = pathEffect
        if (this.filterQuality != filterQuality) this.filterQuality = filterQuality
    }

    /**
     * Returns a [Color] modulated with the given alpha value
     */
    private fun Color.modulate(alpha: Float): Color =
        if (alpha != 1.0f) {
            copy(alpha = this.alpha * alpha)
        } else {
            this
        }

    /**
     * Internal parameters to represent the current CanvasDrawScope
     * used to reduce the size of the inline draw call to avoid
     * bloat of additional assignment calls for each parameter
     * individually
     */
    @PublishedApi internal data class DrawParams(
        var density: Density = DefaultDensity,
        var layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        var canvas: Canvas = EmptyCanvas(),
        var size: Size = Size.Zero
    )
}

/**
 * Convenience method for creating a [DrawTransform] from the current [DrawContext]
 */
private fun DrawContext.asDrawTransform(): DrawTransform = object : DrawTransform {
    override val size: Size
        get() = this@asDrawTransform.size

    override val center: Offset
        get() = size.center

    override fun inset(left: Float, top: Float, right: Float, bottom: Float) {
        this@asDrawTransform.canvas.let {
            val updatedSize = Size(size.width - (left + right), size.height - (top + bottom))
            require(updatedSize.width >= 0 && updatedSize.height >= 0) {
                "Width and height must be greater than or equal to zero"
            }
            this@asDrawTransform.size = updatedSize
            it.translate(left, top)
        }
    }

    override fun clipRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        clipOp: ClipOp
    ) {
        this@asDrawTransform.canvas.clipRect(left, top, right, bottom, clipOp)
    }

    override fun clipPath(path: Path, clipOp: ClipOp) {
        this@asDrawTransform.canvas.clipPath(path, clipOp)
    }

    override fun translate(left: Float, top: Float) {
        this@asDrawTransform.canvas.translate(left, top)
    }

    override fun rotate(degrees: Float, pivot: Offset) {
        this@asDrawTransform.canvas.apply {
            translate(pivot.x, pivot.y)
            rotate(degrees)
            translate(-pivot.x, -pivot.y)
        }
    }

    override fun scale(scaleX: Float, scaleY: Float, pivot: Offset) {
        this@asDrawTransform.canvas.apply {
            translate(pivot.x, pivot.y)
            scale(scaleX, scaleY)
            translate(-pivot.x, -pivot.y)
        }
    }

    override fun transform(matrix: Matrix) {
        this@asDrawTransform.canvas.concat(matrix)
    }
}