/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.internal.JvmDefaultWithCompatibility

/**
 * Create a new Canvas instance that targets its drawing commands
 * to the provided [ImageBitmap]
 */
fun Canvas(image: ImageBitmap): Canvas = ActualCanvas(image)

internal expect fun ActualCanvas(image: ImageBitmap): Canvas

expect class NativeCanvas

/**
 * Saves a copy of the current transform and clip on the save stack and executes the
 * provided lambda with the current transform applied. Once the lambda has been executed,
 * the transformation is popped from the stack, undoing the transformation.
 *
 *
 * See also:
 *
 *  [Canvas.saveLayer], which does the same thing but additionally also groups the
 *    commands
 */
/* expect */ inline fun Canvas.withSave(block: () -> Unit) {
    try {
        save()
        block()
    } finally {
        restore()
    }
}

/**
 * Saves a copy of the current transform and clip on the save stack, and then
 * creates a new group which subsequent calls will become a part of. When the
 * lambda is executed and the save stack is popped, the group will be flattened into
 * a layer and have the given `paint`'s [Paint.colorFilter] and [Paint.blendMode]
 * applied.
 *
 * This lets you create composite effects, for example making a group of
 * drawing commands semi-transparent. Without using [Canvas.saveLayer], each part of
 * the group would be painted individually, so where they overlap would be
 * darker than where they do not. By using [Canvas.saveLayer] to group them
 * together, they can be drawn with an opaque color at first, and then the
 * entire group can be made transparent using the [Canvas.saveLayer]'s paint.
 *
 *
 * ## Using saveLayer with clips
 *
 * When a rectangular clip operation (from [Canvas.clipRect]) is not axis-aligned
 * with the raster buffer, or when the clip operation is not rectalinear (e.g.
 * because it is a rounded rectangle clip created by [Canvas.clipPath]), the edge of the
 * clip needs to be anti-aliased.
 *
 * If two draw calls overlap at the edge of such a clipped region, without
 * using [Canvas.saveLayer], the first drawing will be anti-aliased with the
 * background first, and then the second will be anti-aliased with the result
 * of blending the first drawing and the background. On the other hand, if
 * [Canvas.saveLayer] is used immediately after establishing the clip, the second
 * drawing will cover the first in the layer, and thus the second alone will
 * be anti-aliased with the background when the layer is clipped and
 * composited (when lambda is finished executing).
 *
 * ## Performance considerations
 *
 * Generally speaking, [Canvas.saveLayer] is relatively expensive.
 *
 * There are a several different hardware architectures for GPUs (graphics
 * processing units, the hardware that handles graphics), but most of them
 * involve batching commands and reordering them for performance. When layers
 * are used, they cause the rendering pipeline to have to switch render
 * target (from one layer to another). Render target switches can flush the
 * GPU's command buffer, which typically means that optimizations that one
 * could get with larger batching are lost. Render target switches also
 * generate a lot of memory churn because the GPU needs to copy out the
 * current frame buffer contents from the part of memory that's optimized for
 * writing, and then needs to copy it back in once the previous render target
 * (layer) is restored.
 *
 * See also:
 *
 *  * [Canvas.save], which saves the current state, but does not create a new layer
 *    for subsequent commands.
 *  * [BlendMode], which discusses the use of [Paint.blendMode] with
 *    [saveLayer].
 */
@Suppress("DEPRECATION")
inline fun Canvas.withSaveLayer(bounds: Rect, paint: Paint, block: () -> Unit) {
    try {
        saveLayer(bounds, paint)
        block()
    } finally {
        restore()
    }
}

/**
 *  Add a rotation (in degrees clockwise) to the current transform at the given pivot point.
 *  The pivot coordinate remains unchanged by the rotation transformation
 *
 *  @param degrees to rotate clockwise
 *  @param pivotX The x-coord for the pivot point
 *  @param pivotY The y-coord for the pivot point
 */
fun Canvas.rotate(degrees: Float, pivotX: Float, pivotY: Float) {
    if (degrees == 0.0f) return
    translate(pivotX, pivotY)
    rotate(degrees)
    translate(-pivotX, -pivotY)
}

/**
 * Add a rotation (in radians clockwise) to the current transform at the given pivot point.
 * The pivot coordinate remains unchanged by the rotation transformation
 *
 * @param pivotX The x-coord for the pivot point
 * @param pivotY The y-coord for the pivot point
 */
fun Canvas.rotateRad(radians: Float, pivotX: Float = 0.0f, pivotY: Float = 0.0f) {
    rotate(degrees(radians), pivotX, pivotY)
}

/**
 * Add an axis-aligned scale to the current transform, scaling by the first
 * argument in the horizontal direction and the second in the vertical
 * direction at the given pivot coordinate. The pivot coordinate remains
 * unchanged by the scale transformation.
 *
 * If [sy] is unspecified, [sx] will be used for the scale in both
 * directions.
 *
 * @param sx The amount to scale in X
 * @param sy The amount to scale in Y
 * @param pivotX The x-coord for the pivot point
 * @param pivotY The y-coord for the pivot point
 */
fun Canvas.scale(sx: Float, sy: Float = sx, pivotX: Float, pivotY: Float) {
    if (sx == 1.0f && sy == 1.0f) return
    translate(pivotX, pivotY)
    scale(sx, sy)
    translate(-pivotX, -pivotY)
}

/**
 * Return an instance of the native primitive that implements the Canvas interface
 */
expect val Canvas.nativeCanvas: NativeCanvas

@JvmDefaultWithCompatibility
interface Canvas {

    /**
     * Saves a copy of the current transform and clip on the save stack.
     *
     * Call [restore] to pop the save stack.
     *
     * See also:
     *
     *  * [saveLayer], which does the same thing but additionally also groups the
     *    commands done until the matching [restore].
     */
    fun save()

    /**
     * Pops the current save stack, if there is anything to pop.
     * Otherwise, does nothing.
     *
     * Use [save] and [saveLayer] to push state onto the stack.
     *
     * If the state was pushed with with [saveLayer], then this call will also
     * cause the new layer to be composited into the previous layer.
     */
    fun restore()

    /**
     * Saves a copy of the current transform and clip on the save stack, and then
     * creates a new group which subsequent calls will become a part of. When the
     * save stack is later popped, the group will be flattened into a layer and
     * have the given `paint`'s [Paint.colorFilter] and [Paint.blendMode]
     * applied.
     *
     * This lets you create composite effects, for example making a group of
     * drawing commands semi-transparent. Without using [saveLayer], each part of
     * the group would be painted individually, so where they overlap would be
     * darker than where they do not. By using [saveLayer] to group them
     * together, they can be drawn with an opaque color at first, and then the
     * entire group can be made transparent using the [saveLayer]'s paint.
     *
     * Call [restore] to pop the save stack and apply the paint to the group.
     *
     * ## Using saveLayer with clips
     *
     * When a rectangular clip operation (from [clipRect]) is not axis-aligned
     * with the raster buffer, or when the clip operation is not rectalinear (e.g.
     * because it is a rounded rectangle clip created by [clipPath], the edge of the
     * clip needs to be anti-aliased.
     *
     * If two draw calls overlap at the edge of such a clipped region, without
     * using [saveLayer], the first drawing will be anti-aliased with the
     * background first, and then the second will be anti-aliased with the result
     * of blending the first drawing and the background. On the other hand, if
     * [saveLayer] is used immediately after establishing the clip, the second
     * drawing will cover the first in the layer, and thus the second alone will
     * be anti-aliased with the background when the layer is clipped and
     * composited (when [restore] is called).
     *

     *
     * (Incidentally, rather than using [clipPath] with a rounded rectangle defined in a path to
     * draw rounded rectangles like this, prefer the [drawRoundRect] method.
     *
     * ## Performance considerations
     *
     * Generally speaking, [saveLayer] is relatively expensive.
     *
     * There are a several different hardware architectures for GPUs (graphics
     * processing units, the hardware that handles graphics), but most of them
     * involve batching commands and reordering them for performance. When layers
     * are used, they cause the rendering pipeline to have to switch render
     * target (from one layer to another). Render target switches can flush the
     * GPU's command buffer, which typically means that optimizations that one
     * could get with larger batching are lost. Render target switches also
     * generate a lot of memory churn because the GPU needs to copy out the
     * current frame buffer contents from the part of memory that's optimized for
     * writing, and then needs to copy it back in once the previous render target
     * (layer) is restored.
     *
     * See also:
     *
     *  * [save], which saves the current state, but does not create a new layer
     *    for subsequent commands.
     *  * [BlendMode], which discusses the use of [Paint.blendMode] with
     *    [saveLayer].
     */
    fun saveLayer(bounds: Rect, paint: Paint)

    /**
     * Add a translation to the current transform, shifting the coordinate space
     * horizontally by the first argument and vertically by the second argument.
     */
    fun translate(dx: Float, dy: Float)

    /**
     * Add an axis-aligned scale to the current transform, scaling by the first
     * argument in the horizontal direction and the second in the vertical
     * direction.
     *
     * If [sy] is unspecified, [sx] will be used for the scale in both
     * directions.
     *
     * @param sx The amount to scale in X
     * @param sy The amount to scale in Y
     */
    fun scale(sx: Float, sy: Float = sx)

    /**
     *  Add a rotation (in degrees clockwise) to the current transform
     *
     *  @param degrees to rotate clockwise
     */
    fun rotate(degrees: Float)

    /**
     * Add an axis-aligned skew to the current transform, with the first argument
     * being the horizontal skew in degrees clockwise around the origin, and the
     * second argument being the vertical skew in degrees clockwise around the
     * origin.
     */
    fun skew(sx: Float, sy: Float)

    /**
     * Add an axis-aligned skew to the current transform, with the first argument
     * being the horizontal skew in radians clockwise around the origin, and the
     * second argument being the vertical skew in radians clockwise around the
     * origin.
     */
    fun skewRad(sxRad: Float, syRad: Float) {
        skew(degrees(sxRad), degrees(syRad))
    }

    /**
     * Multiply the current transform by the specified 4⨉4 transformation matrix
     * specified as a list of values in column-major order.
     */
    fun concat(matrix: Matrix)

    /**
     * Reduces the clip region to the intersection of the current clip and the
     * given rectangle.
     *
     * Use [ClipOp.Difference] to subtract the provided rectangle from the
     * current clip.
     */
    @Suppress("DEPRECATION")
    fun clipRect(rect: Rect, clipOp: ClipOp = ClipOp.Intersect) =
        clipRect(rect.left, rect.top, rect.right, rect.bottom, clipOp)

    /**
     * Reduces the clip region to the intersection of the current clip and the
     * given bounds.
     *
     * Use [ClipOp.Difference] to subtract the provided rectangle from the
     * current clip.
     *
     * @param left Left bound of the clip region
     * @param top Top bound of the clip region
     * @param right Right bound of the clip region
     * @param bottom Bottom bound of the clip region
     */
    fun clipRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        clipOp: ClipOp = ClipOp.Intersect
    )

    /**
     * Reduces the clip region to the intersection of the current clip and the
     * given [Path].
     */
    fun clipPath(path: Path, clipOp: ClipOp = ClipOp.Intersect)

    /**
     * Draws a line between the given points using the given paint. The line is
     * stroked, the value of the [Paint.style] is ignored for this call.
     *
     * The `p1` and `p2` arguments are interpreted as offsets from the origin.
     */
    fun drawLine(p1: Offset, p2: Offset, paint: Paint)

    /**
     * Draws a rectangle with the given [Paint]. Whether the rectangle is filled
     * or stroked (or both) is controlled by [Paint.style].
     */
    fun drawRect(rect: Rect, paint: Paint) = drawRect(
        left = rect.left,
        top = rect.top,
        right = rect.right,
        bottom = rect.bottom,
        paint = paint
    )

    /**
     * Draws a rectangle with the given [Paint]. Whether the rectangle is filled
     * or stroked (or both) is controlled by [Paint.style].
     *
     * @param left The left bound of the rectangle
     * @param top The top bound of the rectangle
     * @param right The right bound of the rectangle
     * @param bottom The bottom bound of the rectangle
     * @param paint Paint used to color the rectangle with a fill or stroke
     */
    fun drawRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        paint: Paint
    )

    /**
     * Draws a rounded rectangle with the given [Paint]. Whether the rectangle is
     * filled or stroked (or both) is controlled by [Paint.style].
     */
    fun drawRoundRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        radiusX: Float,
        radiusY: Float,
        paint: Paint
    )

    /**
     * Draws an axis-aligned oval that fills the given axis-aligned rectangle
     * with the given [Paint]. Whether the oval is filled or stroked (or both) is
     * controlled by [Paint.style].
     */
    fun drawOval(rect: Rect, paint: Paint) = drawOval(
        left = rect.left,
        top = rect.top,
        right = rect.right,
        bottom = rect.bottom,
        paint = paint
    )

    /**
     * Draws an axis-aligned oval that fills the given bounds provided with the given
     * [Paint]. Whether the rectangle is filled
     * or stroked (or both) is controlled by [Paint.style].
     *
     * @param left The left bound of the rectangle
     * @param top The top bound of the rectangle
     * @param right The right bound of the rectangle
     * @param bottom The bottom bound of the rectangle
     * @param paint Paint used to color the rectangle with a fill or stroke
     */
    fun drawOval(left: Float, top: Float, right: Float, bottom: Float, paint: Paint)

    /**
     * Draws a circle centered at the point given by the first argument and
     * that has the radius given by the second argument, with the [Paint] given in
     * the third argument. Whether the circle is filled or stroked (or both) is
     * controlled by [Paint.style].
     */
    fun drawCircle(center: Offset, radius: Float, paint: Paint)

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
     * This method is optimized for drawing arcs and should be faster than [Path.arcTo].
     */
    fun drawArc(
        rect: Rect,
        startAngle: Float,
        sweepAngle: Float,
        useCenter: Boolean,
        paint: Paint
    ) = drawArc(
        left = rect.left,
        top = rect.top,
        right = rect.right,
        bottom = rect.bottom,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = useCenter,
        paint = paint
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
     * This method is optimized for drawing arcs and should be faster than [Path.arcTo].
     *
     * @param left Left bound of the arc
     * @param top Top bound of the arc
     * @param right Right bound of the arc
     * @param bottom Bottom bound of the arc
     * @param startAngle Starting angle of the arc relative to 3 o'clock
     * @param sweepAngle Sweep angle in degrees clockwise
     * @param useCenter Flag indicating whether or not to include the center of the oval in the
     * arc, and close it if it is being stroked. This will draw a wedge.
     */
    fun drawArc(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startAngle: Float,
        sweepAngle: Float,
        useCenter: Boolean,
        paint: Paint
    )

    /**
     * Draw an arc scaled to fit inside the given rectangle. It starts from
     * startAngle radians around the oval up to startAngle + sweepAngle
     * radians around the oval, with zero radians being the point on
     * the right hand side of the oval that crosses the horizontal line
     * that intersects the center of the rectangle and with positive
     * angles going clockwise around the oval. If useCenter is true, the arc is
     * closed back to the center, forming a circle sector. Otherwise, the arc is
     * not closed, forming a circle segment.
     *
     * This method is optimized for drawing arcs and should be faster than [Path.arcTo].
     */
    fun drawArcRad(
        rect: Rect,
        startAngleRad: Float,
        sweepAngleRad: Float,
        useCenter: Boolean,
        paint: Paint
    ) {
        drawArc(rect, degrees(startAngleRad), degrees(sweepAngleRad), useCenter, paint)
    }

    /**
     * Draws the given [Path] with the given [Paint]. Whether this shape is
     * filled or stroked (or both) is controlled by [Paint.style]. If the path is
     * filled, then subpaths within it are implicitly closed (see [Path.close]).
     */
    fun drawPath(path: Path, paint: Paint)

    /**
     * Draws the given [ImageBitmap] into the canvas with its top-left corner at the
     * given [Offset]. The image is composited into the canvas using the given [Paint].
     */
    fun drawImage(image: ImageBitmap, topLeftOffset: Offset, paint: Paint)

    /**
     * Draws the subset of the given image described by the `src` argument into
     * the canvas in the axis-aligned rectangle given by the `dst` argument.
     *
     * This might sample from outside the `src` rect by up to half the width of
     * an applied filter.
     *
     * @param image ImageBitmap to draw
     * @param srcOffset: Optional offset representing the top left offset of the source image
     * to draw, this defaults to the origin of [image]
     * @param srcSize: Optional dimensions of the source image to draw relative to [srcOffset],
     * this defaults the width and height of [image]
     * @param dstOffset: Offset representing the top left offset of the destination image
     * to draw
     * @param dstSize: Dimensions of the destination to draw
     * @param paint Paint used to composite the [ImageBitmap] pixels into the canvas
     */
    fun drawImageRect(
        image: ImageBitmap,
        srcOffset: IntOffset = IntOffset.Zero,
        srcSize: IntSize = IntSize(image.width, image.height),
        dstOffset: IntOffset = IntOffset.Zero,
        dstSize: IntSize = srcSize,
        paint: Paint
    )

    /**
     * Draws a sequence of points according to the given [PointMode].
     *
     * The `points` argument is interpreted as offsets from the origin.
     *
     * See also:
     *
     *  * [drawRawPoints], which takes `points` as a [FloatArray] rather than a
     *    [List<Offset>].
     */
    fun drawPoints(pointMode: PointMode, points: List<Offset>, paint: Paint)

    /**
     * Draws a sequence of points according to the given [PointMode].
     *
     * The `points` argument is interpreted  as a list of pairs of floating point
     * numbers, where each pair represents an x and y offset from the origin.
     *
     * See also:
     *
     *  * [drawPoints], which takes `points` as a [List<Offset>] rather than a
     *    [List<Float32List>].
     */
    fun drawRawPoints(pointMode: PointMode, points: FloatArray, paint: Paint)

    fun drawVertices(vertices: Vertices, blendMode: BlendMode, paint: Paint)

    /**
     * Enables Z support which defaults to disabled. This allows layers drawn
     * with different elevations to be rearranged based on their elevation. It
     * also enables rendering of shadows.
     * @see disableZ
     */
    fun enableZ()

    /**
     * Disables Z support, preventing any layers drawn after this point from being visually
     * reordered or having shadows rendered. This is not impacted by any [save] or [restore]
     * calls as it is not considered part of the matrix or clip.
     * @see enableZ
     */
    fun disableZ()
}