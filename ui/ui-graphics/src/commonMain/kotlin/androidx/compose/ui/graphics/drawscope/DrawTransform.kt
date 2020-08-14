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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vectormath.Matrix4
import androidx.compose.ui.graphics.vectormath.degrees

/**
 * Convenience method modifies the [DrawTransform] bounds to inset both left and right bounds by
 * [dx] as well as the top and bottom by [dy]. If only [dx] is provided, the same inset is applied
 * to all 4 bounds
 *
 * @param dx number of pixels to inset both left and right bounds
 * @param dy Optional number of pixels to inset both top and bottom bounds, by default this also
 * insets the top and bottom by [dx] pixels
 */
@Suppress("NOTHING_TO_INLINE")
inline fun DrawTransform.inset(dx: Float = 0.0f, dy: Float = 0.0f) = inset(dx, dy, dx, dy)

/**
 * Add a rotation (in radians clockwise) to the current transform at the given pivot point.
 * The pivot coordinate remains unchanged by the rotation transformation
 *
 * @param radians to rotate clockwise
 * @param pivotX The x-coordinate for the pivot point, defaults to the center of the
 * coordinate space horizontally
 * @param pivotY The y-coordinate for the pivot point, defaults to the center of the
 * coordinate space vertically
 */
@Suppress("NOTHING_TO_INLINE")
inline fun DrawTransform.rotateRad(
    radians: Float,
    pivotX: Float = center.x,
    pivotY: Float = center.y
) = rotate(degrees(radians), pivotX, pivotY)

/**
 * Defines transformations that can be applied to a drawing environment
 */
@DrawScopeMarker
interface DrawTransform {

    /**
     * Get the current size of the CanvasTransform
     */
    val size: Size

    /**
     * Convenience method to obtain the current position of the current transformation
     */
    val center: Offset
        get() = Offset(size.width / 2, size.height / 2)

    /**
     * Simultaneously translate the coordinate space by [left] and [top] as well
     * as modify the dimensions of the current painting area. This provides a callback to issue more
     * drawing instructions within the modified coordinate space. This method
     * modifies the width to be equivalent to width - (left + right) as well as
     * height to height - (top + bottom)
     *
     * @param left number of pixels to inset the left drawing bound
     * @param top number of pixels to inset the top drawing bound
     * @param right number of pixels to inset the right drawing bound
     * @param bottom number of pixels to inset the bottom drawing bound
     */
    fun inset(left: Float, top: Float, right: Float, bottom: Float)

    /**
     * Reduces the clip region to the intersection of the current clip and the
     * given rectangle indicated by the given left, top, right and bottom bounds
     *
     * Use [ClipOp.Difference] to subtract the provided rectangle from the
     * current clip.
     *
     * @param left Left bound of the rectangle to clip
     * @param top Top bound of the rectangle to clip
     * @param right Right bound ofthe rectangle to clip
     * @param bottom Bottom bound of the rectangle to clip
     * @param clipOp Clipping operation to perform on the given bounds
     */
    fun clipRect(
        left: Float = 0.0f,
        top: Float = 0.0f,
        right: Float = size.width,
        bottom: Float = size.height,
        clipOp: ClipOp = ClipOp.Intersect
    )

    /**
     * Reduces the clip region to the intersection of the current clip and the
     * given rounded rectangle.
     *
     * @param path Shape to clip drawing content within
     * @param clipOp Clipping operation to conduct on the given bounds, defaults to [ClipOp.Intersect]
     */
    fun clipPath(path: Path, clipOp: ClipOp = ClipOp.Intersect)

    /**
     * Translate the coordinate space by the given delta in pixels in both the x and y coordinates
     * respectively
     *
     * @param left Pixels to translate the coordinate space in the x-axis
     * @param top Pixels to translate the coordinate space in the y-axis
     */
    fun translate(left: Float = 0.0f, top: Float = 0.0f)

    /**
     *  Add a rotation (in degrees clockwise) to the current transform at the given pivot point.
     *  The pivot coordinate remains unchanged by the rotation transformation.
     *
     *  @param degrees to rotate clockwise
     *  @param pivotX The x-coordinate for the pivot point, defaults to the center of the
     *  coordinate space horizontally
     *  @param pivotY The y-coordinate for the pivot point, defaults to the center of the
     *  coordinate space vertically
     */
    fun rotate(degrees: Float, pivotX: Float = center.x, pivotY: Float = center.y)

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
     */
    fun scale(
        scaleX: Float,
        scaleY: Float = scaleX,
        pivotX: Float = center.x,
        pivotY: Float = center.y
    )

    /**
     * Transform the drawing environment by the given matrix
     * @param matrix transformation matrix used to transform the drawing environment
     */
    fun transform(matrix: Matrix4)
}