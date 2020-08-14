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
import androidx.compose.ui.util.packFloats
import androidx.compose.ui.util.toStringAsFixed
import androidx.compose.ui.util.unpackFloat1
import androidx.compose.ui.util.unpackFloat2
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.truncate

/**
 * Constructs a [Size] from the given width and height
 */
@Stable
fun Size(width: Float, height: Float) = Size(packFloats(width, height))

/**
 * Holds a 2D floating-point size.
 *
 * You can think of this as an [Offset] from the origin.
 */
@Immutable
inline class Size(@PublishedApi internal val packedValue: Long) {

    @Stable
    val width: Float
        get() = unpackFloat1(packedValue)

    @Stable
    val height: Float
        get() = unpackFloat2(packedValue)

    @Suppress("NOTHING_TO_INLINE")
    @Stable
    inline operator fun component1(): Float = width

    @Suppress("NOTHING_TO_INLINE")
    @Stable
    inline operator fun component2(): Float = height

    /**
     * Returns a copy of this Size instance optionally overriding the
     * width or height parameter
     */
    fun copy(width: Float = this.width, height: Float = this.height) = Size(width, height)

    companion object {
        /**
         * Creates an instance of [Size] that has the same values as another.
         */
        // Used by the rendering library's _DebugSize hack.
        @Deprecated("Use copy(width, height) on the desired Size instance instead",
            ReplaceWith("source.copy()"))
        fun copy(source: Size): Size {
            return Size(source.width, source.height)
        }

        /**
         * An empty size, one with a zero width and a zero height.
         */
        @Stable
        val Zero = Size(0.0f, 0.0f)

        /**
         * A size whose [width] and [height] are infinite.
         *
         * See also:
         *
         *  * [isInfinite], which checks whether either dimension is infinite.
         *  * [isFinite], which checks whether both dimensions are finite.
         */
        @Stable
        val Unspecified = Size(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)

        /**
         * A size whose [width] and [height] are infinite.
         *
         * See also:
         *
         *  * [isInfinite], which checks whether either dimension is infinite.
         *  * [isFinite], which checks whether both dimensions are finite.
         */
        @Deprecated("Use Unspecified instead",
            ReplaceWith("Unspecified", "androidx.compose.ui.geometry"))
        @Stable
        val UnspecifiedSize = Unspecified
    }

    /**
     * Whether this size encloses a non-zero area.
     *
     * Negative areas are considered empty.
     */
    @Stable
    fun isEmpty() = width <= 0.0f || height <= 0.0f

    /**
     * Binary subtraction operator for [Size].
     *
     * Subtracting a [Size] from a [Size] returns the [Offset] that describes how
     * much bigger the left-hand-side operand is than the right-hand-side
     * operand. Adding that resulting [Offset] to the [Size] that was the
     * right-hand-side operand would return a [Size] equal to the [Size] that was
     * the left-hand-side operand. (i.e. if `sizeA - sizeB -> offsetA`, then
     * `offsetA + sizeB -> sizeA`)
     *
     * Subtracting an [Offset] from a [Size] returns the [Size] that is smaller than
     * the [Size] operand by the difference given by the [Offset] operand. In other
     * words, the returned [Size] has a [width] consisting of the [width] of the
     * left-hand-side operand minus the [Offset.x] dimension of the
     * right-hand-side operand, and a [height] consisting of the [height] of the
     * left-hand-side operand minus the [Offset.y] dimension of the
     * right-hand-side operand.
     */
    @Stable
    operator fun minus(other: Offset): Size {
        return Size(width - other.x, height - other.y)
    }

    @Stable
    operator fun minus(other: Size): Offset {
        return Offset(width - other.width, height - other.height)
    }

    /**
     * Binary addition operator for adding an [Offset] to a [Size].
     *
     * Returns a [Size] whose [width] is the sum of the [width] of the
     * left-hand-side operand, a [Size], and the [Offset.x] dimension of the
     * right-hand-side operand, an [Offset], and whose [height] is the sum of the
     * [height] of the left-hand-side operand and the [Offset.y] dimension of
     * the right-hand-side operand.
     */
    @Stable
    operator fun plus(other: Offset) = Size(width + other.x, height + other.y)

    /**
     * Multiplication operator.
     *
     * Returns a [Size] whose dimensions are the dimensions of the left-hand-side
     * operand (a [Size]) multiplied by the scalar right-hand-side operand (a
     * [Float]).
     */
    @Stable
    operator fun times(operand: Float) = Size(width * operand, height * operand)

    /**
     * Division operator.
     *
     * Returns a [Size] whose dimensions are the dimensions of the left-hand-side
     * operand (a [Size]) divided by the scalar right-hand-side operand (a
     * [Float]).
     */
    @Stable
    operator fun div(operand: Float) = Size(width / operand, height / operand)

    /**
     * Integer (truncating) division operator.
     *
     * Returns a [Size] whose dimensions are the dimensions of the left-hand-side
     * operand (a [Size]) divided by the scalar right-hand-side operand (a
     * [Float]), rounded towards zero.
     */
    @Stable
    fun truncDiv(operand: Float) =
        Size(truncate(width / operand), truncate(height / operand))

    /**
     * Modulo (remainder) operator.
     *
     * Returns a [Size] whose dimensions are the remainder of dividing the
     * left-hand-side operand (a [Size]) by the scalar right-hand-side operand (a
     * [Float]).
     */
    @Stable
    operator fun rem(operand: Float) = Size(width % operand, height % operand)

    /**
     * The lesser of the magnitudes of the [width] and the [height].
     */
    @Stable
    val minDimension: Float
        get() = min(width.absoluteValue, height.absoluteValue)

    /**
     * The greater of the magnitudes of the [width] and the [height].
     */
    @Stable
    val maxDimension: Float
        get() = max(width.absoluteValue, height.absoluteValue)

    // Convenience methods that do the equivalent of calling the similarly named
    // methods on a Rect constructed from the given origin and this size.

    /**
     * The offset to the intersection of the top and left edges of the rectangle
     * described by the given [Offset] (which is interpreted as the top-left corner)
     * and this [Size].
     *
     * See also [Rect.topLeft].
     */
    @Stable
    fun topLeft(origin: Offset): Offset = origin

    /**
     * The offset to the center of the top edge of the rectangle described by the
     * given offset (which is interpreted as the top-left corner) and this size.
     *
     * See also [Rect.topCenter].
     */
    @Stable
    fun topCenter(origin: Offset): Offset = Offset(origin.x + width / 2.0f, origin.y)

    /**
     * The offset to the intersection of the top and right edges of the rectangle
     * described by the given offset (which is interpreted as the top-left corner)
     * and this size.
     *
     * See also [Rect.topRight].
     */
    @Stable
    fun topRight(origin: Offset): Offset = Offset(origin.x + width, origin.y)

    /**
     * The offset to the center of the left edge of the rectangle described by the
     * given offset (which is interpreted as the top-left corner) and this size.
     *
     * See also [Rect.centerLeft].
     */
    @Stable
    fun centerLeft(origin: Offset): Offset = Offset(origin.x, origin.y + height / 2.0f)

    /**
     * The offset to the point halfway between the left and right and the top and
     * bottom edges of the rectangle described by the given offset (which is
     * interpreted as the top-left corner) and this size.
     *
     * See also [Rect.center].
     */
    @Stable
    fun center(origin: Offset = Offset.Zero): Offset = Offset(origin.x + width / 2.0f, origin.y +
            height / 2.0f)

    /**
     * The offset to the center of the right edge of the rectangle described by the
     * given offset (which is interpreted as the top-left corner) and this size.
     *
     * See also [Rect.centerLeft].
     */
    @Stable
    fun centerRight(origin: Offset): Offset = Offset(origin.x + width, origin.y + height / 2.0f)

    /**
     * The offset to the intersection of the bottom and left edges of the
     * rectangle described by the given offset (which is interpreted as the
     * top-left corner) and this size.
     *
     * See also [Rect.bottomLeft].
     */
    @Stable
    fun bottomLeft(origin: Offset): Offset = Offset(origin.x, origin.y + height)

    /**
     * The offset to the center of the bottom edge of the rectangle described by
     * the given offset (which is interpreted as the top-left corner) and this
     * size.
     *
     * See also [Rect.bottomLeft].
     */
    @Stable
    fun bottomCenter(origin: Offset): Offset = Offset(origin.x + width / 2.0f, origin.y + height)

    /**
     * The offset to the intersection of the bottom and right edges of the
     * rectangle described by the given offset (which is interpreted as the
     * top-left corner) and this size.
     *
     * See also [Rect.bottomRight].
     */
    @Stable
    fun bottomRight(origin: Offset): Offset = Offset(origin.x + width, origin.y + height)

    /**
     * Whether the point specified by the given offset (which is assumed to be
     * relative to the top left of the size) lies between the left and right and
     * the top and bottom edges of a rectangle of this size.
     *
     * Rectangles include their top and left edges but exclude their bottom and
     * right edges.
     */
    @Stable
    fun contains(offset: Offset): Boolean {
        return offset.x >= 0.0f && offset.x < width && offset.y >= 0.0f && offset.y < height
    }

    /**
     * A [Size] with the [width] and [height] swapped.
     */
    @Stable
    fun getFlipped() = Size(height, width)

    override fun toString() = "Size(${width.toStringAsFixed(1)}, ${height.toStringAsFixed(1)})"
}

/**
 * Linearly interpolate between two sizes
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
fun lerp(start: Size, stop: Size, fraction: Float): Size? {
    return Size(
        lerp(start.width, stop.width, fraction),
        lerp(start.height, stop.height, fraction)
    )
}

/**
 * Returns a [Size] with [size]'s [Size.width] and [Size.height] multiplied by [this]
 */
@Suppress("NOTHING_TO_INLINE")
@Stable
inline operator fun Int.times(size: Size) = size * this.toFloat()

/**
 * Returns a [Size] with [size]'s [Size.width] and [Size.height] multiplied by [this]
 */
@Suppress("NOTHING_TO_INLINE")
@Stable
inline operator fun Double.times(size: Size) = size * this.toFloat()

/**
 * Convert a [Size] to a [Rect].
 */
@Stable
fun Size.toRect(): Rect {
    return Rect(Offset.Zero, this)
}

/**
 * Returns a [Size] with [size]'s [Size.width] and [Size.height] multiplied by [this]
 */
@Suppress("NOTHING_TO_INLINE")
@Stable
inline operator fun Float.times(size: Size) = size * this