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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Radius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageAsset
import androidx.compose.ui.graphics.NativePathEffect
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.degrees
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.annotation.FloatRange

/**
 * Simultaneously translate the [DrawScope] coordinate space by [left] and [top] as well as modify
 * the dimensions of the current painting area. This provides a callback to issue more
 * drawing instructions within the modified coordinate space. This method
 * modifies the width of the [DrawScope] to be equivalent to width - (left + right) as well as
 * height to height - (top + bottom). After this method is invoked, the coordinate space is
 * returned to the state before the inset was applied.
 *
 * @param left number of pixels to inset the left drawing bound
 * @param top number of pixels to inset the top drawing bound
 * @param right number of pixels to inset the right drawing bound
 * @param bottom number of pixels to inset the bottom drawing bound
 * @param block lambda that is called to issue drawing commands within the inset coordinate space
 */
inline fun DrawScope.inset(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    block: DrawScope.() -> Unit
) {
    transform.inset(left, top, right, bottom)
    block()
    transform.inset(-left, -top, -right, -bottom)
}

/**
 * Convenience method modifies the [DrawScope] bounds to inset both left, top, right and
 * bottom bounds by [inset]. After this method is invoked,
 * the coordinate space is returned to the state before this inset was applied.
 *
 * @param inset number of pixels to inset left, top, right, and bottom bounds.
 * @param block lambda that is called to issue additional drawing commands within the modified
 * coordinate space
 */
inline fun DrawScope.inset(
    inset: Float,
    block: DrawScope.() -> Unit
) {
    transform.inset(inset, inset, inset, inset)
    block()
    transform.inset(-inset, -inset, -inset, -inset)
}

/**
 * Convenience method modifies the [DrawScope] bounds to inset both left and right bounds by
 * [horizontal] as well as the top and bottom by [vertical]. After this method is invoked,
 * the coordinate space is returned to the state before this inset was applied.
 *
 * @param horizontal number of pixels to inset both left and right bounds. Zero by default
 * @param vertical Optional number of pixels to inset both top and bottom bounds. Zero by
 * default
 * @param block lambda that is called to issue additional drawing commands within the modified
 * coordinate space
 */
inline fun DrawScope.inset(
    horizontal: Float = 0.0f,
    vertical: Float = 0.0f,
    block: DrawScope.() -> Unit
) = inset(horizontal, vertical, horizontal, vertical, block)

/**
 * Translate the coordinate space by the given delta in pixels in both the x and y coordinates
 * respectively
 *
 * @param left Pixels to translate the coordinate space in the x-axis
 * @param top Pixels to translate the coordinate space in the y-axis
 * @param block lambda that is called to issue drawing commands within the
 * translated coordinate space
 */
inline fun DrawScope.translate(
    left: Float = 0.0f,
    top: Float = 0.0f,
    block: DrawScope.() -> Unit
) {
    transform.translate(left, top)
    block()
    transform.translate(-left, -top)
}

/**
 *  Add a rotation (in degrees clockwise) to the current transform at the given pivot point.
 *  The pivot coordinate remains unchanged by the rotation transformation. After the provided
 *  lambda is invoked, the rotation transformation is undone.
 *
 *  @param degrees to rotate clockwise
 *  @param pivotX The x-coordinate for the pivot point, defaults to the center of the
 *  coordinate space horizontally
 *  @param pivotY The y-coordinate for the pivot point, defaults to the center of the
 *  coordinate space vertically
 *  @param block lambda that is called to issue drawing commands within the rotated
 *  coordinate space
 */
@Deprecated(
    "Use rotate(degrees, Offset(pivotX, pivotY)) instead",
    ReplaceWith(
        "rotate(degrees, Offset(pivotX, pivotY))",
        "androidx.compose.ui.graphics.drawscope"
    )
)
inline fun DrawScope.rotate(
    degrees: Float,
    pivotX: Float = center.x,
    pivotY: Float = center.y,
    block: DrawScope.() -> Unit
) = withTransform({ rotate(degrees, Offset(pivotX, pivotY)) }, block)

/**
 *  Add a rotation (in degrees clockwise) to the current transform at the given pivot point.
 *  The pivot coordinate remains unchanged by the rotation transformation. After the provided
 *  lambda is invoked, the rotation transformation is undone.
 *
 *  @param degrees to rotate clockwise
 *  @param pivot The coordinate for the pivot point, defaults to the center of the
 *  coordinate space
 *  @param block lambda that is called to issue drawing commands within the rotated
 *  coordinate space
 */
inline fun DrawScope.rotate(
    degrees: Float,
    pivot: Offset = center,
    block: DrawScope.() -> Unit
) = withTransform({ rotate(degrees, pivot) }, block)

/**
 * Add a rotation (in radians clockwise) to the current transform at the given pivot point.
 * The pivot coordinate remains unchanged by the rotation transformation
 *
 * @param radians to rotate clockwise
 *  @param pivotX The x-coordinate for the pivot point, defaults to the center of the
 *  coordinate space horizontally
 *  @param pivotY The y-coordinate for the pivot point, defaults to the center of the
 *  coordinate space vertically
 * @param block lambda that is called to issue drawing commands within the rotated
 * coordinate space
 */
inline fun DrawScope.rotateRad(
    radians: Float,
    pivotX: Float = center.x,
    pivotY: Float = center.y,
    block: DrawScope.() -> Unit
) {
    withTransform({ rotate(degrees(radians), Offset(pivotX, pivotY)) }, block)
}

/**
 * Add an axis-aligned scale to the current transform, scaling by the first
 * argument in the horizontal direction and the second in the vertical
 * direction at the given pivot coordinate. The pivot coordinate remains
 * unchanged by the scale transformation.
 *
 * If [scaleY] is unspecified, [scaleX] will be used for the scale in both
 * directions.
 *
 * @param scaleX The amount to scale in X
 * @param scaleY The amount to scale in Y
 * @param pivotX The x-coordinate for the pivot point, defaults to the center of the
 * coordinate space horizontally
 * @param pivotY The y-coordinate for the pivot point, defaults to the center of the
 * coordinate space vertically
 * @param block lambda used to issue drawing commands within the scaled coordinate space
 */
@Deprecated(
    "Use scale(scaleX, scaleY, Offset(pivotX, pivotY))",
    ReplaceWith(
        "scale(scaleX, scaleY, Offset(pivotX, pivotY))",
        "androidx.compose.ui.graphics.drawscope"
    )
)
inline fun DrawScope.scale(
    scaleX: Float,
    scaleY: Float = scaleX,
    pivotX: Float = center.x,
    pivotY: Float = center.y,
    block: DrawScope.() -> Unit
) = withTransform({ scale(scaleX, scaleY, Offset(pivotX, pivotY)) }, block)

/**
 * Add an axis-aligned scale to the current transform, scaling by the first
 * argument in the horizontal direction and the second in the vertical
 * direction at the given pivot coordinate. The pivot coordinate remains
 * unchanged by the scale transformation. After this method is invoked, the
 * coordinate space is returned to the state before the scale was applied.
 *
 * @param scaleX The amount to scale in X
 * @param scaleY The amount to scale in Y
 * @param pivot The coordinate for the pivot point, defaults to the center of the
 * coordinate space
 * @param block lambda used to issue drawing commands within the scaled coordinate space
 */
inline fun DrawScope.scale(
    scaleX: Float,
    scaleY: Float,
    pivot: Offset = center,
    block: DrawScope.() -> Unit
) = withTransform({ scale(scaleX, scaleY, pivot) }, block)

/**
 * Add an axis-aligned scale to the current transform, scaling both the horizontal direction and
 * the vertical direction at the given pivot coordinate. The pivot coordinate remains
 * unchanged by the scale transformation. After this method is invoked, the
 * coordinate space is returned to the state before the scale was applied.
 *
 * @param scale The amount to scale uniformly in both directions
 * @param pivot The coordinate for the pivot point, defaults to the center of the
 * coordinate space
 * @param block lambda used to issue drawing commands within the scaled coordinate space
 */
inline fun DrawScope.scale(
    scale: Float,
    pivot: Offset = center,
    block: DrawScope.() -> Unit
) = withTransform({ scale(scale, scale, pivot) }, block)

/**
 * Reduces the clip region to the intersection of the current clip and the
 * given rectangle indicated by the given left, top, right and bottom bounds. This provides
 * a callback to issue drawing commands within the clipped region. After this method is invoked,
 * this clip is no longer applied.
 *
 * Use [ClipOp.Difference] to subtract the provided rectangle from the
 * current clip.
 *
 * @param left Left bound of the rectangle to clip
 * @param top Top bound of the rectangle to clip
 * @param right Right bound of the rectangle to clip
 * @param bottom Bottom bound of the rectangle to clip
 * @param clipOp Clipping operation to conduct on the given bounds, defaults to [ClipOp.Intersect]
 * @param block Lambda callback with this CanvasScope as a receiver scope to issue drawing commands
 * within the provided clip
 */
inline fun DrawScope.clipRect(
    left: Float = 0.0f,
    top: Float = 0.0f,
    right: Float = size.width,
    bottom: Float = size.height,
    clipOp: ClipOp = ClipOp.Intersect,
    block: DrawScope.() -> Unit
) = withTransform({ clipRect(left, top, right, bottom, clipOp) }, block)

/**
 * Reduces the clip region to the intersection of the current clip and the
 * given path. This method provides a callback to issue drawing commands within the region
 * defined by the clipped path. After this method is invoked, this clip is no longer applied.
 *
 * @param path Shape to clip drawing content within
 * @param clipOp Clipping operation to conduct on the given bounds, defaults to [ClipOp.Intersect]
 * @param block Lambda callback with this CanvasScope as a receiver scope to issue drawing commands
 * within the provided clip
 */
inline fun DrawScope.clipPath(
    path: Path,
    clipOp: ClipOp = ClipOp.Intersect,
    block: DrawScope.() -> Unit
) = withTransform({ clipPath(path, clipOp) }, block)

/**
 * Provides access to draw directly with the underlying [Canvas]. This is helpful for situations
 * to re-use alternative drawing logic in combination with [DrawScope]
 *
 * @param block Lambda callback to issue drawing commands on the provided [Canvas]
 */
inline fun DrawScope.drawIntoCanvas(block: (Canvas) -> Unit) = block(canvas)

/**
 * Provides access to draw directly with the underlying [Canvas] along with the current
 * size of the [DrawScope]. This is helpful for situations
 * to re-use alternative drawing logic in combination with [DrawScope]
 *
 * @param block Lambda callback to issue drawing commands on the provided [Canvas] and given size
 */
@Deprecated(
    "Use drawIntoCanvas instead",
    ReplaceWith(
        "drawIntoCanvas { canvas -> }",
        "androidx.compose.ui.graphics.drawscope.drawIntoCanvas"
    )
)
inline fun DrawScope.drawCanvas(block: (Canvas, Size) -> Unit) = block(canvas, size)

/**
 * Perform 1 or more transformations and execute drawing commands with the specified transformations
 * applied. After this call is complete, the transformation before this call was made is restored
 *
 * @sample androidx.compose.ui.graphics.samples.DrawScopeBatchedTransformSample
 *
 * @param transformBlock Callback invoked to issue transformations to be made before the drawing
 * operations are issued
 * @param drawBlock Callback invoked to issue drawing operations after the transformations are
 * applied
 */
inline fun DrawScope.withTransform(
    transformBlock: DrawTransform.() -> Unit,
    drawBlock: DrawScope.() -> Unit
) = canvas.let {
    // Transformation can include inset calls which change the drawing area
    // so cache the previous size before the transformation is done
    // and reset it afterwards
    val previousSize = size
    it.save()
    transformBlock(transform)
    drawBlock()
    it.restore()
    setSize(previousSize)
}

/**
 * Creates a scoped drawing environment with the provided [Canvas]. This provides a
 * declarative, stateless API to draw shapes and paths without requiring
 * consumers to maintain underlying [Canvas] state information.
 * The bounds for drawing within [DrawScope] are provided by the call to
 * [DrawScope.draw] and are always bound to the local translation. That is the left and
 * top coordinates are always the origin and the right and bottom coordinates are always the
 * specified width and height respectively. Drawing content is not clipped,
 * so it is possible to draw outside of the specified bounds.
 *
 * @sample androidx.compose.ui.graphics.samples.DrawScopeSample
 */
@DrawScopeMarker
abstract class DrawScope : Density {

    @PublishedApi internal var canvas: Canvas = EmptyCanvas()

    @PublishedApi internal val transform = object : DrawTransform {

        override val size: Size
            get() = this@DrawScope.size

        override val center: Offset
            get() = this@DrawScope.center

        override fun inset(left: Float, top: Float, right: Float, bottom: Float) {
            this@DrawScope.canvas.let {
                val updatedSize = Size(size.width - (left + right), size.height - (top + bottom))
                require(updatedSize.width >= 0 && updatedSize.height >= 0) {
                    "Width and height must be greater than or equal to zero"
                }
                this@DrawScope.setSize(updatedSize)
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
            this@DrawScope.canvas.clipRect(left, top, right, bottom, clipOp)
        }

        override fun clipPath(path: Path, clipOp: ClipOp) {
            this@DrawScope.canvas.clipPath(path, clipOp)
        }

        override fun translate(left: Float, top: Float) {
            this@DrawScope.canvas.translate(left, top)
        }

        override fun rotate(degrees: Float, pivot: Offset) {
            this@DrawScope.canvas.apply {
                translate(pivot.x, pivot.y)
                rotate(degrees)
                translate(-pivot.x, -pivot.y)
            }
        }

        override fun scale(scaleX: Float, scaleY: Float, pivot: Offset) {
            this@DrawScope.canvas.apply {
                translate(pivot.x, pivot.y)
                scale(scaleX, scaleY)
                translate(-pivot.x, -pivot.y)
            }
        }

        override fun transform(matrix: Matrix) {
            this@DrawScope.canvas.concat(matrix)
        }
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
     * Center of the current bounds of the drawing environment
     */
    val center: Offset
        get() = Offset(size.width / 2, size.height / 2)

    /**
     * Provides the dimensions of the current drawing environment
     */
    var size: Size = Size.Zero
        private set

    /**
     * The layout direction of the layout being drawn in.
     */
    abstract val layoutDirection: LayoutDirection

    /**
     * Draws a line between the given points using the given paint. The line is
     * stroked.
     *
     * @param brush the color or fill to be applied to the line
     * @param start first point of the line to be drawn
     * @param end second point of the line to be drawn
     * @param strokeWidth stroke width to apply to the line
     * @param cap treatment applied to the ends of the line segment
     * @param pathEffect optional effect or pattern to apply to the line
     * @param alpha opacity to be applied to the [brush] from 0.0f to 1.0f representing
     * fully transparent to fully opaque respectively
     * @param colorFilter ColorFilter to apply to the [brush] when drawn into the destination
     * @param blendMode the blending algorithm to apply to the [brush]
     */
    fun drawLine(
        brush: Brush,
        start: Offset,
        end: Offset,
        strokeWidth: Float = Stroke.HairlineWidth,
        cap: StrokeCap = Stroke.DefaultCap,
        pathEffect: NativePathEffect? = null,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = 1.0f,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
    ) = canvas.drawLine(
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
     * Draws a line between the given points using the given paint. The line is
     * stroked.
     *
     * @param color the color to be applied to the line
     * @param start first point of the line to be drawn
     * @param end second point of the line to be drawn
     * @param strokeWidth The stroke width to apply to the line
     * @param cap treatment applied to the ends of the line segment
     * @param pathEffect optional effect or pattern to apply to the line
     * @param alpha opacity to be applied to the [color] from 0.0f to 1.0f representing
     * fully transparent to fully opaque respectively
     * @param colorFilter ColorFilter to apply to the [color] when drawn into the destination
     * @param blendMode the blending algorithm to apply to the [color]
     */
    fun drawLine(
        color: Color,
        start: Offset,
        end: Offset,
        strokeWidth: Float = Stroke.HairlineWidth,
        cap: StrokeCap = Stroke.DefaultCap,
        pathEffect: NativePathEffect? = null,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = 1.0f,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
    ) = canvas.drawLine(
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
     * Draws a rectangle with the given offset and size. If no offset from the top left is provided,
     * it is drawn starting from the origin of the current translation. If no size is provided,
     * the size of the current environment is used.
     *
     * @param brush The color or fill to be applied to the rectangle
     * @param topLeft Offset from the local origin of 0, 0 relative to the current translation
     * @param size Dimensions of the rectangle to draw
     * @param alpha Opacity to be applied to the [brush] from 0.0f to 1.0f representing
     * fully transparent to fully opaque respectively
     * @param style Whether or not the rectangle is stroked or filled in
     * @param colorFilter ColorFilter to apply to the [brush] when drawn into the destination
     * @param blendMode Blending algorithm to apply to destination
     */
    fun drawRect(
        brush: Brush,
        topLeft: Offset = Offset.Zero,
        size: Size = this.size.offsetSize(topLeft),
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
    ) = canvas.drawRect(
        left = topLeft.x,
        top = topLeft.y,
        right = topLeft.x + size.width,
        bottom = topLeft.y + size.height,
        paint = configurePaint(brush, style, alpha, colorFilter, blendMode)
    )

    /**
     * Draws a rectangle with the given offset and size. If no offset from the top left is provided,
     * it is drawn starting from the origin of the current translation. If no size is provided,
     * the size of the current environment is used.
     *
     * @param color The color to be applied to the rectangle
     * @param topLeft Offset from the local origin of 0, 0 relative to the current translation
     * @param size Dimensions of the rectangle to draw
     * @param alpha Opacity to be applied to the [color] from 0.0f to 1.0f representing
     * fully transparent to fully opaque respectively
     * @param style Whether or not the rectangle is stroked or filled in
     * @param colorFilter ColorFilter to apply to the [color] source pixels
     * @param blendMode Blending algorithm to apply to destination
     */
    fun drawRect(
        color: Color,
        topLeft: Offset = Offset.Zero,
        size: Size = this.size.offsetSize(topLeft),
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
    ) = canvas.drawRect(
        left = topLeft.x,
        top = topLeft.y,
        right = topLeft.x + size.width,
        bottom = topLeft.y + size.height,
        paint = configurePaint(color, style, alpha, colorFilter, blendMode)
    )

    /**
     * Draws the given [ImageAsset] into the canvas with its top-left corner at the
     * given [Offset]. The image is composited into the canvas using the given [Paint].
     *
     * @param image The [ImageAsset] to draw
     * @param topLeft Offset from the local origin of 0, 0 relative to the current translation
     * @param alpha Opacity to be applied to [image] from 0.0f to 1.0f representing
     * fully transparent to fully opaque respectively
     * @param style Specifies whether the image is to be drawn filled in or as a rectangular stroke
     * @param colorFilter ColorFilter to apply to the [image] when drawn into the destination
     * @param blendMode Blending algorithm to apply to destination
     */
    fun drawImage(
        image: ImageAsset,
        topLeft: Offset = Offset.Zero,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
    ) = canvas.drawImage(
        image,
        topLeft,
        configurePaint(null, style, alpha, colorFilter, blendMode)
    )

    /**
     * Draws the subset of the given image described by the `src` argument into
     * the canvas in the axis-aligned rectangle given by the `dst` argument.
     *
     * If no src rect is provided, the entire image is scaled into the corresponding destination
     * bounds
     *
     * @param image The source image to draw
     * @param srcOffset Optional offset representing the top left offset of the source image
     * to draw, this defaults to the origin of [image]
     * @param srcSize Optional dimensions of the source image to draw relative to [srcOffset],
     * this defaults the width and height of [image]
     * @param dstOffset Optional offset representing the top left offset of the destination
     * to draw the given image, this defaults to the origin of the current translation
     * tarting top left offset in the destination to draw the image
     * @param dstSize Optional dimensions of the destination to draw, this defaults to [srcSize]
     * @param alpha Opacity to be applied to [image] from 0.0f to 1.0f representing
     * fully transparent to fully opaque respectively
     * @param style Specifies whether the image is to be drawn filled in or as a rectangular stroke
     * @param colorFilter ColorFilter to apply to the [image] when drawn into the destination
     * @param blendMode Blending algorithm to apply to destination
     */
    fun drawImage(
        image: ImageAsset,
        srcOffset: IntOffset = IntOffset.Zero,
        srcSize: IntSize = IntSize(image.width, image.height),
        dstOffset: IntOffset = IntOffset.Zero,
        dstSize: IntSize = srcSize,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
    ) = canvas.drawImageRect(
        image,
        srcOffset,
        srcSize,
        dstOffset,
        dstSize,
        configurePaint(null, style, alpha, colorFilter, blendMode)
    )

    /**
     * Draws a rounded rectangle with the provided size, offset and radii for the x and y axis
     * respectively. This rectangle is drawn with the provided [Brush]
     * parameter and is filled or stroked based on the given [DrawStyle]
     *
     * @param brush The color or fill to be applied to the rounded rectangle
     * @param topLeft Offset from the local origin of 0, 0 relative to the current translation
     * @param size Dimensions of the rectangle to draw
     * @param radius Corner radius of the rounded rectangle
     * @param alpha Opacity to be applied to rounded rectangle from 0.0f to 1.0f representing
     * fully transparent to fully opaque respectively
     * @param style Specifies whether the rounded rectangle is stroked or filled in
     * @param colorFilter ColorFilter to apply to the [brush] when drawn into the destination
     * @param blendMode Blending algorithm to be applied to the brush
     */
    fun drawRoundRect(
        brush: Brush,
        topLeft: Offset = Offset.Zero,
        size: Size = this.size.offsetSize(topLeft),
        radius: Radius = Radius.Zero,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
    ) = canvas.drawRoundRect(
        topLeft.x,
        topLeft.y,
        topLeft.x + size.width,
        topLeft.y + size.height,
        radius.x,
        radius.y,
        configurePaint(brush, style, alpha, colorFilter, blendMode)
    )

    /**
     * Draws a rounded rectangle with the given [Paint]. Whether the rectangle is
     * filled or stroked (or both) is controlled by [Paint.style].
     *
     * @param color The color to be applied to the rounded rectangle
     * @param topLeft Offset from the local origin of 0, 0 relative to the current translation
     * @param size Dimensions of the rectangle to draw
     * @param radius Corner radius of the rounded rectangle
     * @param alpha Opacity to be applied to rounded rectangle from 0.0f to 1.0f representing
     * fully transparent to fully opaque respectively
     * @param style Specifies whether the rounded rectangle is stroked or filled in
     * @param colorFilter ColorFilter to apply to the [color] when drawn into the destination
     * @param blendMode Blending algorithm to be applied to the color
     */
    fun drawRoundRect(
        color: Color,
        topLeft: Offset = Offset.Zero,
        size: Size = this.size.offsetSize(topLeft),
        radius: Radius = Radius.Zero,
        style: DrawStyle = Fill,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = 1.0f,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
    ) = canvas.drawRoundRect(
        topLeft.x,
        topLeft.y,
        topLeft.x + size.width,
        topLeft.y + size.height,
        radius.x,
        radius.y,
        configurePaint(color, style, alpha, colorFilter, blendMode)
    )

    /**
     * Draws a circle at the provided center coordinate and radius. If no center point is provided
     * the center of the bounds is used.
     *
     * @param brush The color or fill to be applied to the circle
     * @param radius The radius of the circle
     * @param center The center coordinate where the circle is to be drawn
     * @param alpha Opacity to be applied to the circle from 0.0f to 1.0f representing
     * fully transparent to fully opaque respectively
     * @param style Whether or not the circle is stroked or filled in
     * @param colorFilter ColorFilter to apply to the [brush] when drawn into the destination
     * @param blendMode Blending algorithm to be applied to the brush
     */
    fun drawCircle(
        brush: Brush,
        radius: Float = size.minDimension / 2.0f,
        center: Offset = this.center,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
    ) = canvas.drawCircle(
        center,
        radius,
        configurePaint(brush, style, alpha, colorFilter, blendMode)
    )

    /**
     * Draws a circle at the provided center coordinate and radius. If no center point is provided
     * the center of the bounds is used.
     *
     * @param color The color or fill to be applied to the circle
     * @param radius The radius of the circle
     * @param center The center coordinate where the circle is to be drawn
     * @param alpha Opacity to be applied to the circle from 0.0f to 1.0f representing
     * fully transparent to fully opaque respectively
     * @param style Whether or not the circle is stroked or filled in
     * @param colorFilter ColorFilter to apply to the [color] when drawn into the destination
     * @param blendMode Blending algorithm to be applied to the brush
     */
    fun drawCircle(
        color: Color,
        radius: Float = size.minDimension / 2.0f,
        center: Offset = this.center,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
    ) = canvas.drawCircle(
        center,
        radius,
        configurePaint(color, style, alpha, colorFilter, blendMode)
    )

    /**
     * Draws an oval with the given offset and size. If no offset from the top left is provided,
     * it is drawn starting from the origin of the current translation. If no size is provided,
     * the size of the current environment is used.
     *
     * @param brush Color or fill to be applied to the oval
     * @param topLeft Offset from the local origin of 0, 0 relative to the current translation
     * @param size Dimensions of the rectangle to draw
     * @param alpha Opacity to be applied to the oval from 0.0f to 1.0f representing
     * fully transparent to fully opaque respectively
     * @param style Whether or not the oval is stroked or filled in
     * @param colorFilter ColorFilter to apply to the [brush] when drawn into the destination
     * @param blendMode Blending algorithm to be applied to the brush
     */
    fun drawOval(
        brush: Brush,
        topLeft: Offset = Offset.Zero,
        size: Size = this.size.offsetSize(topLeft),
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
    ) = canvas.drawOval(
        left = topLeft.x,
        top = topLeft.y,
        right = topLeft.x + size.width,
        bottom = topLeft.y + size.height,
        paint = configurePaint(brush, style, alpha, colorFilter, blendMode)
    )

    /**
     * Draws an oval with the given offset and size. If no offset from the top left is provided,
     * it is drawn starting from the origin of the current translation. If no size is provided,
     * the size of the current environment is used.
     *
     * @param color Color to be applied to the oval
     * @param topLeft Offset from the local origin of 0, 0 relative to the current translation
     * @param size Dimensions of the rectangle to draw
     * @param alpha Opacity to be applied to the oval from 0.0f to 1.0f representing
     * fully transparent to fully opaque respectively
     * @param style Whether or not the oval is stroked or filled in
     * @param colorFilter ColorFilter to apply to the [color] when drawn into the destination
     * @param blendMode Blending algorithm to be applied to the brush
     */
    fun drawOval(
        color: Color,
        topLeft: Offset = Offset.Zero,
        size: Size = this.size.offsetSize(topLeft),
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
    ) = canvas.drawOval(
        left = topLeft.x,
        top = topLeft.y,
        right = topLeft.x + size.width,
        bottom = topLeft.y + size.height,
        paint = configurePaint(color, style, alpha, colorFilter, blendMode)
    )

    /**
     * Draw an arc scaled to fit inside the given rectangle. It starts from
     * startAngle degrees around the oval up to startAngle + sweepAngle
     * degrees around the oval, with zero degrees being the point on
     * the right hand side of the oval that crosses the horizontal line
     * that intersects the center of the rectangle and with positive
     * angles going clockwise around the oval. If useCenter is true, the arc is
     * closed back to the center, forming a circle sector. Otherwise, the arc is
     * not closed, forming a circle segment.
     *
     * @param brush Color or fill to be applied to the arc
     * @param topLeft Offset from the local origin of 0, 0 relative to the current translation
     * @param size Dimensions of the arc to draw
     * @param startAngle Starting angle in degrees. 0 represents 3 o'clock
     * @param sweepAngle Size of the arc in degrees that is drawn clockwise relative to [startAngle]
     * @param useCenter Flag indicating if the arc is to close the center of the bounds
     * @param alpha Opacity to be applied to the arc from 0.0f to 1.0f representing
     * fully transparent to fully opaque respectively
     * @param style Whether or not the arc is stroked or filled in
     * @param colorFilter ColorFilter to apply to the [brush] when drawn into the destination
     * @param blendMode Blending algorithm to be applied to the arc when it is drawn
     */
    fun drawArc(
        brush: Brush,
        startAngle: Float,
        sweepAngle: Float,
        useCenter: Boolean,
        topLeft: Offset = Offset.Zero,
        size: Size = this.size.offsetSize(topLeft),
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
    ) = canvas.drawArc(
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
     * Draw an arc scaled to fit inside the given rectangle. It starts from
     * startAngle degrees around the oval up to startAngle + sweepAngle
     * degrees around the oval, with zero degrees being the point on
     * the right hand side of the oval that crosses the horizontal line
     * that intersects the center of the rectangle and with positive
     * angles going clockwise around the oval. If useCenter is true, the arc is
     * closed back to the center, forming a circle sector. Otherwise, the arc is
     * not closed, forming a circle segment.
     *
     * @param color Color to be applied to the arc
     * @param topLeft Offset from the local origin of 0, 0 relative to the current translation
     * @param size Dimensions of the arc to draw
     * @param startAngle Starting angle in degrees. 0 represents 3 o'clock
     * @param sweepAngle Size of the arc in degrees that is drawn clockwise relative to [startAngle]
     * @param useCenter Flag indicating if the arc is to close the center of the bounds
     * @param alpha Opacity to be applied to the arc from 0.0f to 1.0f representing
     * fully transparent to fully opaque respectively
     * @param style Whether or not the arc is stroked or filled in
     * @param colorFilter ColorFilter to apply to the [color] when drawn into the destination
     * @param blendMode Blending algorithm to be applied to the arc when it is drawn
     */
    fun drawArc(
        color: Color,
        startAngle: Float,
        sweepAngle: Float,
        useCenter: Boolean,
        topLeft: Offset = Offset.Zero,
        size: Size = this.size.offsetSize(topLeft),
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
    ) = canvas.drawArc(
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
     * Draws the given [Path] with the given [Color]. Whether this shape is
     * filled or stroked (or both) is controlled by [DrawStyle]. If the path is
     * filled, then subpaths within it are implicitly closed (see [Path.close]).
     *
     *
     * @param path Path to draw
     * @param color Color to be applied to the path
     * @param alpha Opacity to be applied to the path from 0.0f to 1.0f representing
     * fully transparent to fully opaque respectively
     * @param style Whether or not the path is stroked or filled in
     * @param colorFilter ColorFilter to apply to the [color] when drawn into the destination
     * @param blendMode Blending algorithm to be applied to the path when it is drawn
     */
    fun drawPath(
        path: Path,
        color: Color,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
    ) = canvas.drawPath(path, configurePaint(color, style, alpha, colorFilter, blendMode))

    /**
     * Draws the given [Path] with the given [Color]. Whether this shape is
     * filled or stroked (or both) is controlled by [DrawStyle]. If the path is
     * filled, then subpaths within it are implicitly closed (see [Path.close]).
     *
     * @param path Path to draw
     * @param brush Brush to be applied to the path
     * @param alpha Opacity to be applied to the path from 0.0f to 1.0f representing
     * fully transparent to fully opaque respectively
     * @param style Whether or not the path is stroked or filled in
     * @param colorFilter ColorFilter to apply to the [brush] when drawn into the destination
     * @param blendMode Blending algorithm to be applied to the path when it is drawn
     */
    fun drawPath(
        path: Path,
        brush: Brush,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
    ) = canvas.drawPath(path, configurePaint(brush, style, alpha, colorFilter, blendMode))

    /**
     * Draws a sequence of points according to the given [PointMode].
     *
     * The `points` argument is interpreted as offsets from the origin.
     *
     * @param points List of points to draw with the specified [PointMode]
     * @param pointMode [PointMode] used to indicate how the points are to be drawn
     * @param color Color to be applied to the points
     * @param alpha Opacity to be applied to the path from 0.0f to 1.0f representing
     * fully transparent to fully opaque respectively
     * @param strokeWidth The stroke width to apply to the line
     * @param cap Treatment applied to the ends of the line segment
     * @param pathEffect optional effect or pattern to apply to the point
     * @param colorFilter ColorFilter to apply to the [color] when drawn into the destination
     * @param blendMode Blending algorithm to be applied to the path when it is drawn
     */
    fun drawPoints(
        points: List<Offset>,
        pointMode: PointMode,
        color: Color,
        strokeWidth: Float = Stroke.HairlineWidth,
        cap: StrokeCap = StrokeCap.Butt,
        pathEffect: NativePathEffect? = null,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = 1.0f,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
    ) = canvas.drawPoints(
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
     * Draws a sequence of points according to the given [PointMode].
     *
     * The `points` argument is interpreted as offsets from the origin.
     *
     * @param points List of points to draw with the specified [PointMode]
     * @param pointMode [PointMode] used to indicate how the points are to be drawn
     * @param brush Brush to be applied to the points
     * @param strokeWidth The stroke width to apply to the line
     * @param cap Treatment applied to the ends of the line segment
     * @param pathEffect optional effect or pattern to apply to the points
     * @param alpha Opacity to be applied to the path from 0.0f to 1.0f representing
     * fully transparent to fully opaque respectively
     * @param colorFilter ColorFilter to apply to the [brush] when drawn into the destination
     * @param blendMode Blending algorithm to be applied to the path when it is drawn
     */
    fun drawPoints(
        points: List<Offset>,
        pointMode: PointMode,
        brush: Brush,
        strokeWidth: Float = Stroke.HairlineWidth,
        cap: StrokeCap = StrokeCap.Butt,
        pathEffect: NativePathEffect? = null,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = 1.0f,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
    ) = canvas.drawPoints(
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
    protected fun draw(canvas: Canvas, size: Size, block: DrawScope.() -> Unit) {
        val previousSize = this.size
        // Remember the previous canvas in case we are temporarily re-directing our drawing
        // to a separate Layer/RenderNode only to draw that content back into the original Canvas
        // If there is no previous canvas that was being drawin into, this ends up reseting this
        // parameter back to null defensively
        val previousCanvas = this.canvas
        this.canvas = canvas
        setSize(size)
        canvas.save()
        this.block()
        canvas.restore()
        setSize(previousSize)
        this.canvas = previousCanvas
    }

    /**
     * Internal published APIs used to support inline scoped extension methods
     * on CanvasScope directly, without exposing the underlying stateful APIs
     * to conduct the transformations themselves as inline methods require
     * all methods called within them to be public
     */

    /**
     * Configures the current size of the drawing environment, this is configured as part of
     * the [draw] call
     */
    @PublishedApi
    internal fun setSize(size: Size) {
        this.size = size
    }

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
                        with(drawStyle) {
                            if (strokeWidth != width) strokeWidth = width
                            if (strokeCap != cap) strokeCap = cap
                            if (strokeMiterLimit != miter) strokeMiterLimit = miter
                            if (strokeJoin != join) strokeJoin = join

                            // TODO b/154550525 add PathEffect to Paint if necessary
                            nativePathEffect = pathEffect
                        }
                    }
        }

    /**
     * Helper method to configure the corresponding [Brush] along with other properties
     * on the corresponding paint specified by [DrawStyle]
     */
    private fun configurePaint(
        brush: Brush?,
        style: DrawStyle,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ): Paint = selectPaint(style).apply {
        if (brush != null) {
            brush.applyTo(this, alpha)
        } else if (this.alpha != alpha) {
            this.alpha = alpha
        }
        if (this.colorFilter != colorFilter) this.colorFilter = colorFilter
        if (this.blendMode != blendMode) this.blendMode = blendMode
    }

    /**
     * Helper method to configure the corresponding [Color] along with other properties
     * on the corresponding paint specified by [DrawStyle]
     */
    private fun configurePaint(
        color: Color,
        style: DrawStyle,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ): Paint = selectPaint(style).apply {
        // Modulate the color alpha directly
        // instead of configuring a separate alpha parameter
        val targetColor = color.modulate(alpha)
        if (this.color != targetColor) this.color = targetColor
        if (this.shader != null) this.shader = null
        if (this.colorFilter != colorFilter) this.colorFilter = colorFilter
        if (this.blendMode != blendMode) this.blendMode = blendMode
    }

    private fun configureStrokePaint(
        color: Color,
        strokeWidth: Float,
        miter: Float,
        cap: StrokeCap,
        join: StrokeJoin,
        pathEffect: NativePathEffect?,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
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
            this.nativePathEffect = pathEffect
        }

    private fun configureStrokePaint(
        brush: Brush?,
        strokeWidth: Float,
        miter: Float,
        cap: StrokeCap,
        join: StrokeJoin,
        pathEffect: NativePathEffect?,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) = obtainStrokePaint().apply {
        if (brush != null) {
            brush.applyTo(this, alpha)
        } else if (this.alpha != alpha) {
            this.alpha = alpha
        }
        if (this.colorFilter != colorFilter) this.colorFilter = colorFilter
        if (this.blendMode != blendMode) this.blendMode = blendMode
        if (this.strokeWidth != strokeWidth) this.strokeWidth = strokeWidth
        if (this.strokeMiterLimit != miter) this.strokeMiterLimit = miter
        if (this.strokeCap != cap) this.strokeCap = cap
        if (this.strokeJoin != join) this.strokeJoin = join
        this.nativePathEffect = pathEffect
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
     * Helper method to offset the provided size with the offset in box width and height
     */
    private fun Size.offsetSize(offset: Offset): Size =
        Size(this.width - offset.x, this.height - offset.y)

    companion object {

        /**
         * Default blending mode used for each drawing operation.
         * This ensures that content is drawn on top of the pixels
         * in the destination
         */
        val DefaultBlendMode: BlendMode = BlendMode.SrcOver
    }
}

/**
 * Represents how the shapes should be drawn within a [DrawScope]
 */
sealed class DrawStyle

/**
 * Default [DrawStyle] indicating shapes should be drawn completely filled in with the
 * provided color or pattern
 */
object Fill : DrawStyle()

/**
 * [DrawStyle] that provides information for drawing content with a stroke
 */
data class Stroke(
    /**
     * Configure the width of the stroke in pixels
     */
    val width: Float = 0.0f,

    /**
     * Set the stroke miter value. This is used to control the behavior of miter
     * joins when the joins angle is sharp. This value must be >= 0.
     */
    val miter: Float = DefaultMiter,

    /**
     * Return the paint's Cap, controlling how the start and end of stroked
     * lines and paths are treated. The default is [StrokeCap.Butt]
     */
    val cap: StrokeCap = StrokeCap.Butt,

    /**
     * Set's the treatment where lines and curve segments join on a stroked path.
     * The default is [StrokeJoin.Miter]
     */
    val join: StrokeJoin = StrokeJoin.Miter,

    /**
     * Effect to apply to the stroke, null indicates a solid stroke line is to be drawn
     */
    val pathEffect: NativePathEffect? = null
) : DrawStyle() {
    companion object {

        /**
         * Width to indicate a hairline stroke of 1 pixel
         */
        const val HairlineWidth = 0.0f

        /**
         * Default miter length used in combination with joins
         */
        const val DefaultMiter: Float = 4.0f

        /**
         * Default cap used for line endings
         */
        val DefaultCap = StrokeCap.Butt

        /**
         * Default join style used for connections between line and curve segments
         */
        val DefaultJoin = StrokeJoin.Miter
    }
}