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

package androidx.compose.ui.geometry

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.util.lerp
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

// TODO(mount): Normalize this class. There are many methods that can be extension functions.
/**
 * An immutable, 2D, axis-aligned, floating-point rectangle whose coordinates
 * are relative to a given origin.
 */
@Immutable
data class Rect(
    /**
     * The offset of the left edge of this rectangle from the x axis.
     */
    @Stable
    val left: Float,

    /**
     * The offset of the top edge of this rectangle from the y axis.
     */
    @Stable
    val top: Float,

    /**
     * The offset of the right edge of this rectangle from the x axis.
     */
    @Stable
    val right: Float,

    /**
     * The offset of the bottom edge of this rectangle from the y axis.
     */
    @Stable
    val bottom: Float
) {

    companion object {

        /** A rectangle with left, top, right, and bottom edges all at zero. */
        @Stable
        val Zero: Rect = Rect(0.0f, 0.0f, 0.0f, 0.0f)
    }

    /** The distance between the left and right edges of this rectangle. */
    @Stable
    val width: Float
        get() { return right - left }

    /** The distance between the top and bottom edges of this rectangle. */
    @Stable
    val height: Float
        get() { return bottom - top }

    /**
     * The distance between the upper-left corner and the lower-right corner of
     * this rectangle.
     */
    @Stable
    val size: Size
        get() = Size(width, height)

    /** Whether any of the coordinates of this rectangle are equal to positive infinity. */
    // included for consistency with Offset and Size
    @Stable
    val isInfinite: Boolean
        get() = left >= Float.POSITIVE_INFINITY ||
            top >= Float.POSITIVE_INFINITY ||
            right >= Float.POSITIVE_INFINITY ||
            bottom >= Float.POSITIVE_INFINITY

    /** Whether all coordinates of this rectangle are finite. */
    @Stable
    val isFinite: Boolean
        get() = left.isFinite() &&
            top.isFinite() &&
            right.isFinite() &&
            bottom.isFinite()

    /**
     * Whether this rectangle encloses a non-zero area. Negative areas are
     * considered empty.
     */
    @Stable
    val isEmpty: Boolean
        get() = left >= right || top >= bottom

    /**
     * Returns a new rectangle translated by the given offset.
     *
     * To translate a rectangle by separate x and y components rather than by an
     * [Offset], consider [translate].
     */
    @Stable
    fun translate(offset: Offset): Rect {
        return Rect(left + offset.x, top + offset.y, right + offset.x, bottom + offset.y)
    }

    /**
     * Returns a new rectangle with translateX added to the x components and
     * translateY added to the y components.
     */
    @Stable
    fun translate(translateX: Float, translateY: Float): Rect {
        return Rect(
            left + translateX,
            top + translateY,
            right + translateX,
            bottom + translateY
        )
    }

    /** Returns a new rectangle with edges moved outwards by the given delta. */
    @Stable
    fun inflate(delta: Float): Rect {
        return Rect(left - delta, top - delta, right + delta, bottom + delta)
    }

    /** Returns a new rectangle with edges moved inwards by the given delta. */
    @Stable
    fun deflate(delta: Float): Rect = inflate(-delta)

    /**
     * Returns a new rectangle that is the intersection of the given
     * rectangle and this rectangle. The two rectangles must overlap
     * for this to be meaningful. If the two rectangles do not overlap,
     * then the resulting Rect will have a negative width or height.
     */
    @Stable
    fun intersect(other: Rect): Rect {
        return Rect(
            max(left, other.left),
            max(top, other.top),
            min(right, other.right),
            min(bottom, other.bottom)
        )
    }

    /** Whether `other` has a nonzero area of overlap with this rectangle. */
    fun overlaps(other: Rect): Boolean {
        if (right <= other.left || other.right <= left)
            return false
        if (bottom <= other.top || other.bottom <= top)
            return false
        return true
    }

    /**
     * The lesser of the magnitudes of the [width] and the [height] of this
     * rectangle.
     */
    val minDimension: Float
        get() = min(width.absoluteValue, height.absoluteValue)

    /**
     * The greater of the magnitudes of the [width] and the [height] of this
     * rectangle.
     */
    val maxDimension: Float
        get() = max(width.absoluteValue, height.absoluteValue)

    /**
     * The offset to the intersection of the top and left edges of this rectangle.
     */
    val topLeft: Offset
        get() = Offset(left, top)

    /**
     * The offset to the center of the top edge of this rectangle.
     */
    val topCenter: Offset
        get() = Offset(left + width / 2.0f, top)

    /**
     * The offset to the intersection of the top and right edges of this rectangle.
     */
    val topRight: Offset
        get() = Offset(right, top)

    /**
     * The offset to the center of the left edge of this rectangle.
     */
    val centerLeft: Offset
        get() = Offset(left, top + height / 2.0f)

    /**
     * The offset to the point halfway between the left and right and the top and
     * bottom edges of this rectangle.
     *
     * See also [Size.center].
     */
    val center: Offset
        get() = Offset(left + width / 2.0f, top + height / 2.0f)

    /**
     * The offset to the center of the right edge of this rectangle.
     */
    val centerRight: Offset
        get() = Offset(right, top + height / 2.0f)

    /**
     * The offset to the intersection of the bottom and left edges of this rectangle.
     */
    val bottomLeft: Offset
        get() = Offset(left, bottom)

    /**
     * The offset to the center of the bottom edge of this rectangle.
     */
    val bottomCenter: Offset
        get() { return Offset(left + width / 2.0f, bottom) }

    /**
     * The offset to the intersection of the bottom and right edges of this rectangle.
     */
    val bottomRight: Offset
        get() { return Offset(right, bottom) }

    /**
     * Whether the point specified by the given offset (which is assumed to be
     * relative to the origin) lies between the left and right and the top and
     * bottom edges of this rectangle.
     *
     * Rectangles include their top and left edges but exclude their bottom and
     * right edges.
     */
    operator fun contains(offset: Offset): Boolean {
        return offset.x >= left && offset.x < right && offset.y >= top && offset.y < bottom
    }

    override fun toString() = "Rect.fromLTRB(" +
        "${left.toStringAsFixed(1)}, " +
        "${top.toStringAsFixed(1)}, " +
        "${right.toStringAsFixed(1)}, " +
        "${bottom.toStringAsFixed(1)})"
}

/**
 * Construct a rectangle from its left and top edges as well as its width and height.
 * @param offset Offset to represent the top and left parameters of the Rect
 * @param size Size to determine the width and height of this [Rect].
 * @return Rect with [Rect.left] and [Rect.top] configured to [Offset.x] and [Offset.y] as
 * [Rect.right] and [Rect.bottom] to [Offset.x] + [Size.width] and [Offset.y] + [Size.height]
 * respectively
 */
@Stable
fun Rect(offset: Offset, size: Size): Rect =
    Rect(
        offset.x,
        offset.y,
        offset.x + size.width,
        offset.y + size.height
    )

/**
 * Construct the smallest rectangle that encloses the given offsets, treating
 * them as vectors from the origin.
 * @param topLeft Offset representing the left and top edges of the rectangle
 * @param bottomRight Offset representing the bottom and right edges of the rectangle
 */
@Stable
fun Rect(topLeft: Offset, bottomRight: Offset): Rect =
    Rect(
        topLeft.x,
        topLeft.y,
        bottomRight.x,
        bottomRight.y
    )

/**
 * Construct a rectangle that bounds the given circle
 * @param center Offset that represents the center of the circle
 * @param radius Radius of the circle to enclose
 */
@Stable
fun Rect(center: Offset, radius: Float): Rect =
    Rect(
        center.x - radius,
        center.y - radius,
        center.x + radius,
        center.y + radius
    )

/**
 * Linearly interpolate between two rectangles.
 *
 * The [fraction] argument represents position on the timeline, with 0.0 meaning
 * that the interpolation has not started, returning [start] (or something
 * equivalent to [start]), 1.0 meaning that the interpolation has finished,
 * returning [stop] (or something equivalent to [stop]), and values in between
 * meaning that the interpolation is at the relevant point on the timeline
 * between [start] and [stop]. The interpolation can be extrapolated beyond 0.0 and
 * 1.0, so negative values and values greater than 1.0 are valid (and can
 * easily be generated by curves).
 *
 * Values for [fraction] are usually obtained from an [Animation<Float>], such as
 * an `AnimationController`.
 */
@Stable
fun lerp(start: Rect, stop: Rect, fraction: Float): Rect {
    return Rect(
        lerp(start.left, stop.left, fraction),
        lerp(start.top, stop.top, fraction),
        lerp(start.right, stop.right, fraction),
        lerp(start.bottom, stop.bottom, fraction)
    )
}