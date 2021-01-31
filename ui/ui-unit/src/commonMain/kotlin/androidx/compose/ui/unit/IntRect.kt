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

@file:Suppress("NOTHING_TO_INLINE")

package androidx.compose.ui.unit

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.translate
import androidx.compose.ui.util.lerp
import kotlin.math.absoluteValue

/**
 * An immutable, 2D, axis-aligned, integer bounds rectangle whose coordinates are relative
 * to a given origin.
 */
@Immutable
data class IntRect(
    /**
     * The offset of the left edge of this rectangle from the x axis.
     */
    @Stable
    val left: Int,

    /**
     * The offset of the top edge of this rectangle from the y axis.
     */
    @Stable
    val top: Int,

    /**
     * The offset of the right edge of this rectangle from the x axis.
     */
    @Stable
    val right: Int,

    /**
     * The offset of the bottom edge of this rectangle from the y axis.
     */
    @Stable
    val bottom: Int
) {
    companion object {

        /** A rectangle with left, top, right, and bottom edges all at zero. */
        @Stable
        val Zero: IntRect = IntRect(0, 0, 0, 0)
    }

    /** The distance between the left and right edges of this rectangle. */
    @Stable
    val width: Int
        get() { return right - left }

    /** The distance between the top and bottom edges of this rectangle. */
    @Stable
    val height: Int
        get() { return bottom - top }

    /**
     * The distance between the upper-left corner and the lower-right corner of
     * this rectangle.
     */
    @Stable
    val size: IntSize
        get() = IntSize(width, height)

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
    fun translate(offset: IntOffset): IntRect {
        return IntRect(left + offset.x, top + offset.y, right + offset.x, bottom + offset.y)
    }

    /**
     * Returns a new rectangle with translateX added to the x components and
     * translateY added to the y components.
     */
    @Stable
    fun translate(translateX: Int, translateY: Int): IntRect {
        return IntRect(
            left + translateX,
            top + translateY,
            right + translateX,
            bottom + translateY
        )
    }

    /** Returns a new rectangle with edges moved outwards by the given delta. */
    @Stable
    fun inflate(delta: Int): IntRect {
        return IntRect(left - delta, top - delta, right + delta, bottom + delta)
    }

    /** Returns a new rectangle with edges moved inwards by the given delta. */
    @Stable
    fun deflate(delta: Int): IntRect = inflate(-delta)

    /**
     * Returns a new rectangle that is the intersection of the given
     * rectangle and this rectangle. The two rectangles must overlap
     * for this to be meaningful. If the two rectangles do not overlap,
     * then the resulting IntRect will have a negative width or height.
     */
    @Stable
    fun intersect(other: IntRect): IntRect {
        return IntRect(
            kotlin.math.max(left, other.left),
            kotlin.math.max(top, other.top),
            kotlin.math.min(right, other.right),
            kotlin.math.min(bottom, other.bottom)
        )
    }

    /** Whether `other` has a nonzero area of overlap with this rectangle. */
    fun overlaps(other: IntRect): Boolean {
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
    val minDimension: Int
        get() = kotlin.math.min(width.absoluteValue, height.absoluteValue)

    /**
     * The greater of the magnitudes of the [width] and the [height] of this
     * rectangle.
     */
    val maxDimension: Int
        get() = kotlin.math.max(width.absoluteValue, height.absoluteValue)

    /**
     * The offset to the intersection of the top and left edges of this rectangle.
     */
    val topLeft: IntOffset
        get() = IntOffset(left, top)

    /**
     * The offset to the center of the top edge of this rectangle.
     */
    val topCenter: IntOffset
        get() = IntOffset(left + width / 2, top)

    /**
     * The offset to the intersection of the top and right edges of this rectangle.
     */
    val topRight: IntOffset
        get() = IntOffset(right, top)

    /**
     * The offset to the center of the left edge of this rectangle.
     */
    val centerLeft: IntOffset
        get() = IntOffset(left, top + height / 2)

    /**
     * The offset to the point halfway between the left and right and the top and
     * bottom edges of this rectangle.
     *
     * See also [IntSize.center].
     */
    val center: IntOffset
        get() = IntOffset(left + width / 2, top + height / 2)

    /**
     * The offset to the center of the right edge of this rectangle.
     */
    val centerRight: IntOffset
        get() = IntOffset(right, top + height / 2)

    /**
     * The offset to the intersection of the bottom and left edges of this rectangle.
     */
    val bottomLeft: IntOffset
        get() = IntOffset(left, bottom)

    /**
     * The offset to the center of the bottom edge of this rectangle.
     */
    val bottomCenter: IntOffset
        get() { return IntOffset(left + width / 2, bottom) }

    /**
     * The offset to the intersection of the bottom and right edges of this rectangle.
     */
    val bottomRight: IntOffset
        get() { return IntOffset(right, bottom) }

    /**
     * Whether the point specified by the given offset (which is assumed to be
     * relative to the origin) lies between the left and right and the top and
     * bottom edges of this rectangle.
     *
     * Rectangles include their top and left edges but exclude their bottom and
     * right edges.
     */
    fun contains(offset: IntOffset): Boolean {
        return offset.x >= left && offset.x < right && offset.y >= top && offset.y < bottom
    }

    override fun toString() = "IntRect.fromLTRB(" +
        "$left, " +
        "$top, " +
        "$right, " +
        "$bottom)"
}

/**
 * Construct a rectangle from its left and top edges as well as its width and height.
 * @param offset Offset to represent the top and left parameters of the Rect
 * @param size Size to determine the width and height of this [IntRect].
 * @return Rect with [IntRect.left] and [IntRect.top] configured to [IntOffset.x] and
 * [IntOffset.y] as [IntRect.right] and [IntRect.bottom] to [IntOffset.x] + [IntSize.width] and
 * [IntOffset.y] + [IntSize.height] respectively
 */
@Stable
fun IntRect(offset: IntOffset, size: IntSize) =
    IntRect(
        left = offset.x,
        top = offset.y,
        right = offset.x + size.width,
        bottom = offset.y + size.height
    )

/**
 * Construct the smallest rectangle that encloses the given offsets, treating
 * them as vectors from the origin.
 * @param topLeft Offset representing the left and top edges of the rectangle
 * @param bottomRight Offset representing the bottom and right edges of the rectangle
 */
@Stable
fun IntRect(topLeft: IntOffset, bottomRight: IntOffset): IntRect =
    IntRect(
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
fun IntRect(center: IntOffset, radius: Int): IntRect =
    IntRect(
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
fun lerp(start: IntRect, stop: IntRect, fraction: Float): IntRect {
    return IntRect(
        lerp(start.left, stop.left, fraction),
        lerp(start.top, stop.top, fraction),
        lerp(start.right, stop.right, fraction),
        lerp(start.bottom, stop.bottom, fraction)
    )
}
