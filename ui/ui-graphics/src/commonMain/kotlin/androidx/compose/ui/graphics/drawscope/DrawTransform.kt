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
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.degrees
import kotlin.jvm.JvmDefaultWithCompatibility

/**
 * Convenience method modifies the [DrawTransform] bounds to inset both left and right bounds by
 * [horizontal] as well as the top and bottom by [vertical]. After this method is
 * invoked, the coordinate space is returned to the state before the inset was applied
 *
 * @param horizontal number of pixels to inset both left and right bounds. Zero by default.
 * @param vertical number of pixels to inset both top and bottom bounds. Zero by default.
 */
@Suppress("NOTHING_TO_INLINE")
inline fun DrawTransform.inset(horizontal: Float = 0.0f, vertical: Float = 0.0f) =
    inset(horizontal, vertical, horizontal, vertical)

/**
 * Convenience method modifies the [DrawScope] bounds to inset both left, top, right and
 * bottom bounds by [inset]. After this method is invoked,
 * the coordinate space is returned to the state before this inset was applied.
 *
 * @param inset number of pixels to inset left, top, right, and bottom bounds.
 */
@Suppress("NOTHING_TO_INLINE")
inline fun DrawTransform.inset(
    inset: Float
) = inset(inset, inset)

/**
 * Add a rotation (in radians clockwise) to the current transform at the given pivot point.
 * The pivot coordinate remains unchanged by the rotation transformation
 *
 * @param radians to rotate clockwise
 * @param pivot The coordinate for the pivot point, defaults to the center of the
 * coordinate space
 */
@Suppress("NOTHING_TO_INLINE")
inline fun DrawTransform.rotateRad(
    radians: Float,
    pivot: Offset = center
) = rotate(degrees(radians), pivot)

/**
 * Add an axis-aligned scale to the current transform, scaling uniformly in both directions
 * by the provided scale factor at the pivot coordinate. The pivot coordinate remains
 * unchanged by the scale transformation.
 *
 * @param scale The amount to scale
 * @param pivot The coordinate for the pivot point, defaults to the center of the
 * coordinate space
 */
@Suppress("NOTHING_TO_INLINE")
inline fun DrawTransform.scale(scale: Float, pivot: Offset = center) =
    scale(scale, scale, pivot)

/**
 * Defines transformations that can be applied to a drawing environment
 */
@DrawScopeMarker
@JvmDefaultWithCompatibility
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
     * given rectangle indicated by the given left, top, right and bottom bounds.
     * After this method is invoked, this clip is no longer applied.
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
     * given rounded rectangle. After this method is invoked, this clip is no longer applied
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
     *  @param pivot The coordinates for the pivot point, defaults to the center of the
     *  coordinate space
     */
    fun rotate(degrees: Float, pivot: Offset = center)

    /**
     * Add an axis-aligned scale to the current transform, scaling by the first
     * argument in the horizontal direction and the second in the vertical
     * direction at the given pivot coordinate. The pivot coordinate remains
     * unchanged by the scale transformation.
     *
     * @param scaleX The amount to scale in X
     * @param scaleY The amount to scale in Y
     * @param pivot The coordinate for the pivot point, defaults to the center of the
     * coordinate space
     */
    fun scale(scaleX: Float, scaleY: Float, pivot: Offset = center)

    /**
     * Transform the drawing environment by the given matrix
     * @param matrix transformation matrix used to transform the drawing environment
     */
    fun transform(matrix: Matrix)
}