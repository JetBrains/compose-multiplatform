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

package androidx.compose.ui.graphics

import androidx.compose.runtime.Immutable

/**
 * Effect applied to the geometry of a drawing primitive. For example, this can be used
 * to draw shapes as a dashed or shaped pattern, or apply a treatment around line segment
 * intersections.
 */
interface PathEffect {
    companion object {

        /**
         * Replaces sharp angles between line segments into rounded angles of the specified radius
         *
         * @param radius Rounded corner radius to apply for each angle of the drawn shape
         */
        fun cornerPathEffect(radius: Float): PathEffect = actualCornerPathEffect(radius)

        /**
         * Draws a shape as a series of dashes with the given intervals and offset into the specified
         * interval array. The intervals must contain an even number of entries (>=2). The even indices
         * specify "on" intervals and the odd indices represent "off" intervals. The phase parameter
         * is the pixel offset into the intervals array (mod the sum of all of the intervals).
         *
         * For example: if `intervals[] = {10, 20}`, and phase = 25, this will set up a dashed
         * path like so: 5 pixels off 10 pixels on 20 pixels off 10 pixels on 20 pixels off
         *
         * The phase parameter is
         * an offset into the intervals array. The intervals array
         * controls the length of the dashes. This is only applied for stroked shapes
         * (ex. [PaintingStyle.Stroke] and is ignored for filled in shapes (ex. [PaintingStyle.Fill]
         *
         * @param intervals Array of "on" and "off" distances for the dashed line segments
         * @param phase Pixel offset into the intervals array
         */
        fun dashPathEffect(intervals: FloatArray, phase: Float = 0f): PathEffect =
            actualDashPathEffect(intervals, phase)

        /**
         * Create a PathEffect that applies the inner effect to the path, and then applies the outer
         * effect to the result of the inner effect. (e.g. outer(inner(path)).
         */
        fun chainPathEffect(outer: PathEffect, inner: PathEffect): PathEffect =
            actualChainPathEffect(outer, inner)

        /**
         * Dash the drawn path by stamping it with the specified shape represented as a [Path].
         * This is only applied to stroke shapes and will be ignored with filled shapes.
         * The stroke width used with this [PathEffect] is ignored as well.
         *
         * @param shape Path to stamp along
         * @param advance Spacing between each stamped shape
         * @param phase Amount to offset before the first shape is stamped
         * @param style How to transform the shape at each position as it is stamped
         */
        fun stampedPathEffect(
            shape: Path,
            advance: Float,
            phase: Float,
            style: StampedPathEffectStyle
        ): PathEffect = actualStampedPathEffect(shape, advance, phase, style)
    }
}

internal expect fun actualCornerPathEffect(radius: Float): PathEffect

internal expect fun actualDashPathEffect(intervals: FloatArray, phase: Float): PathEffect

internal expect fun actualChainPathEffect(outer: PathEffect, inner: PathEffect): PathEffect

internal expect fun actualStampedPathEffect(
    shape: Path,
    advance: Float,
    phase: Float,
    style: StampedPathEffectStyle
): PathEffect

/**
 * Strategy for transforming each point of the shape along the drawn path
 *
 * @sample androidx.compose.ui.graphics.samples.StampedPathEffectSample
 */
@Immutable
@kotlin.jvm.JvmInline
value class StampedPathEffectStyle internal constructor(
    @Suppress("unused") private val value: Int
) {

    companion object {
        /**
         * Translate the path shape into the specified location aligning the top left of the path with
         * the drawn geometry. This does not modify the path itself.
         *
         * For example, a circle drawn with a square path and [Translate] will draw the square path
         * repeatedly with the top left corner of each stamped square along the curvature of the circle.
         */
        val Translate = StampedPathEffectStyle(0)

        /**
         * Rotates the path shape its center along the curvature of the drawn geometry. This does not
         * modify the path itself.
         *
         * For example, a circle drawn with a square path and [Rotate] will draw the square path
         * repeatedly with the center of each stamped square along the curvature of the circle as well
         * as each square being rotated along the circumference.
         */
        val Rotate = StampedPathEffectStyle(1)

        /**
         * Modifies the points within the path such that they fit within the drawn geometry. This will
         * turn straight lines into curves.
         *
         * For example, a circle drawn with a square path and [Morph] will modify the straight lines
         * of the square paths to be curves such that each stamped square is rendered as an arc around
         * the curvature of the circle.
         */
        val Morph = StampedPathEffectStyle(2)
    }

    override fun toString() = when (this) {
        Translate -> "Translate"
        Rotate -> "Rotate"
        Morph -> "Morph"
        else -> "Unknown"
    }
}