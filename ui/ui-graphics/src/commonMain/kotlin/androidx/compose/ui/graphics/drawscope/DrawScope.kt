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
import androidx.compose.ui.geometry.CornerRadius
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
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.degrees
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.center
import androidx.compose.ui.graphics.internal.JvmDefaultWithCompatibility

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
    drawContext.transform.inset(left, top, right, bottom)
    block()
    drawContext.transform.inset(-left, -top, -right, -bottom)
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
    drawContext.transform.inset(inset, inset, inset, inset)
    block()
    drawContext.transform.inset(-inset, -inset, -inset, -inset)
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
    drawContext.transform.translate(left, top)
    block()
    drawContext.transform.translate(-left, -top)
}

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
 * @param pivot The coordinate for the pivot point, defaults to the center of the
 *  coordinate space
 * @param block lambda that is called to issue drawing commands within the rotated
 * coordinate space
 */
inline fun DrawScope.rotateRad(
    radians: Float,
    pivot: Offset = center,
    block: DrawScope.() -> Unit
) {
    withTransform({ rotate(degrees(radians), pivot) }, block)
}

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
inline fun DrawScope.drawIntoCanvas(block: (Canvas) -> Unit) = block(drawContext.canvas)

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
) = with(drawContext) {
    // Transformation can include inset calls which change the drawing area
    // so cache the previous size before the transformation is done
    // and reset it afterwards
    val previousSize = size
    canvas.save()
    transformBlock(transform)
    drawBlock()
    canvas.restore()
    size = previousSize
}

/**
 * Creates a scoped drawing environment with the provided [Canvas]. This provides a
 * declarative, stateless API to draw shapes and paths without requiring
 * consumers to maintain underlying [Canvas] state information.
 * [DrawScope] implementations are also provided sizing information and transformations
 * are done relative to the local translation. That is left and top coordinates are always the
 * origin and the right and bottom coordinates are always the specified width and height
 * respectively. Drawing content is not clipped, so it is possible to draw outside of the
 * specified bounds.
 *
 * @sample androidx.compose.ui.graphics.samples.DrawScopeSample
 */
@DrawScopeMarker
@JvmDefaultWithCompatibility
interface DrawScope : Density {

    /**
     * The current [DrawContext] that contains the dependencies
     * needed to create the drawing environment
     */
    val drawContext: DrawContext

    /**
     * Center of the current bounds of the drawing environment
     */
    val center: Offset
        get() = drawContext.size.center

    /**
     * Provides the dimensions of the current drawing environment
     */
    val size: Size
        get() = drawContext.size

    /**
     * The layout direction of the layout being drawn in.
     */
    val layoutDirection: LayoutDirection

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
        pathEffect: PathEffect? = null,
        /*FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float = 1.0f,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
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
        pathEffect: PathEffect? = null,
        /*FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float = 1.0f,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
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
        /*FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
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
        /*@FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
    )

    /**
     * Draws the given [ImageBitmap] into the canvas with its top-left corner at the
     * given [Offset]. The image is composited into the canvas using the given [Paint].
     *
     * @param image The [ImageBitmap] to draw
     * @param topLeft Offset from the local origin of 0, 0 relative to the current translation
     * @param alpha Opacity to be applied to [image] from 0.0f to 1.0f representing
     * fully transparent to fully opaque respectively
     * @param style Specifies whether the image is to be drawn filled in or as a rectangular stroke
     * @param colorFilter ColorFilter to apply to the [image] when drawn into the destination
     * @param blendMode Blending algorithm to apply to destination
     */
    fun drawImage(
        image: ImageBitmap,
        topLeft: Offset = Offset.Zero,
        /*@FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
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
    @Deprecated(
        "Prefer usage of drawImage that consumes an optional FilterQuality parameter",
        level = DeprecationLevel.HIDDEN,
        replaceWith = ReplaceWith(
            "drawImage(image, srcOffset, srcSize, dstOffset, dstSize, alpha, style, " +
                "colorFilter, blendMode, FilterQuality.Low)",
            "androidx.compose.ui.graphics.drawscope",
            "androidx.compose.ui.graphics.FilterQuality"
        )
    ) // Binary API compatibility.
    fun drawImage(
        image: ImageBitmap,
        srcOffset: IntOffset = IntOffset.Zero,
        srcSize: IntSize = IntSize(image.width, image.height),
        dstOffset: IntOffset = IntOffset.Zero,
        dstSize: IntSize = srcSize,
        /*@FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
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
     * @param filterQuality Sampling algorithm applied to the [image] when it is scaled and drawn
     * into the destination. The default is [FilterQuality.Low] which scales using a bilinear
     * sampling algorithm
     */
    fun drawImage(
        image: ImageBitmap,
        srcOffset: IntOffset = IntOffset.Zero,
        srcSize: IntSize = IntSize(image.width, image.height),
        dstOffset: IntOffset = IntOffset.Zero,
        dstSize: IntSize = srcSize,
        /*@FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode,
        filterQuality: FilterQuality = DefaultFilterQuality
    ) {
        drawImage(
            image = image,
            srcOffset = srcOffset,
            srcSize = srcSize,
            dstOffset = dstOffset,
            dstSize = dstSize,
            alpha = alpha,
            style = style,
            colorFilter = colorFilter,
            blendMode = blendMode
        )
    }

    /**
     * Draws a rounded rectangle with the provided size, offset and radii for the x and y axis
     * respectively. This rectangle is drawn with the provided [Brush]
     * parameter and is filled or stroked based on the given [DrawStyle]
     *
     * @param brush The color or fill to be applied to the rounded rectangle
     * @param topLeft Offset from the local origin of 0, 0 relative to the current translation
     * @param size Dimensions of the rectangle to draw
     * @param cornerRadius Corner radius of the rounded rectangle, negative radii values are clamped to 0
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
        cornerRadius: CornerRadius = CornerRadius.Zero,
        /*@FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
    )

    /**
     * Draws a rounded rectangle with the given [Paint]. Whether the rectangle is
     * filled or stroked (or both) is controlled by [Paint.style].
     *
     * @param color The color to be applied to the rounded rectangle
     * @param topLeft Offset from the local origin of 0, 0 relative to the current translation
     * @param size Dimensions of the rectangle to draw
     * @param cornerRadius Corner radius of the rounded rectangle, negative radii values are clamped to 0
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
        cornerRadius: CornerRadius = CornerRadius.Zero,
        style: DrawStyle = Fill,
        /*@FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float = 1.0f,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
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
        /*@FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
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
        /*@FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
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
     *
     * @sample androidx.compose.ui.graphics.samples.DrawScopeOvalBrushSample
     */
    fun drawOval(
        brush: Brush,
        topLeft: Offset = Offset.Zero,
        size: Size = this.size.offsetSize(topLeft),
        /*@FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
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
     *
     * @sample androidx.compose.ui.graphics.samples.DrawScopeOvalColorSample
     */
    fun drawOval(
        color: Color,
        topLeft: Offset = Offset.Zero,
        size: Size = this.size.offsetSize(topLeft),
        /*@FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
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
        /*@FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
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
        /*@FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
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
        /*@FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
    )

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
        /*@FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float = 1.0f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
    )

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
        pathEffect: PathEffect? = null,
        /*@FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float = 1.0f,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
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
     * fully transparent to fully opaque respectively.
     * @param colorFilter ColorFilter to apply to the [brush] when drawn into the destination
     * @param blendMode Blending algorithm to be applied to the path when it is drawn
     */
    fun drawPoints(
        points: List<Offset>,
        pointMode: PointMode,
        brush: Brush,
        strokeWidth: Float = Stroke.HairlineWidth,
        cap: StrokeCap = StrokeCap.Butt,
        pathEffect: PathEffect? = null,
        /*@FloatRange(from = 0.0, to = 1.0)*/
        alpha: Float = 1.0f,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode
    )

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

        /**
         * Default FilterQuality used for determining the filtering algorithm
         * to apply when scaling [ImageBitmap] objects. Maps to the default
         * behavior of bilinear filtering
         */
        val DefaultFilterQuality: FilterQuality = FilterQuality.Low
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
class Stroke(
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
    val pathEffect: PathEffect? = null
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Stroke) return false

        if (width != other.width) return false
        if (miter != other.miter) return false
        if (cap != other.cap) return false
        if (join != other.join) return false
        if (pathEffect != other.pathEffect) return false

        return true
    }

    override fun hashCode(): Int {
        var result = width.hashCode()
        result = 31 * result + miter.hashCode()
        result = 31 * result + cap.hashCode()
        result = 31 * result + join.hashCode()
        result = 31 * result + (pathEffect?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "Stroke(width=$width, miter=$miter, cap=$cap, join=$join, pathEffect=$pathEffect)"
    }
}