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
import androidx.compose.ui.util.unpackFloat1
import androidx.compose.ui.util.unpackFloat2
import kotlin.math.sqrt

/**
 * Constructs an Offset from the given relative x and y offsets
 */
@Stable
fun Offset(x: Float, y: Float) = Offset(packFloats(x, y))

/**
 * An immutable 2D floating-point offset.
 *
 * Generally speaking, Offsets can be interpreted in two ways:
 *
 * 1. As representing a point in Cartesian space a specified distance from a
 *    separately-maintained origin. For example, the top-left position of
 *    children in the [RenderBox] protocol is typically represented as an
 *    [Offset] from the top left of the parent box.
 *
 * 2. As a vector that can be applied to coordinates. For example, when
 *    painting a [RenderObject], the parent is passed an [Offset] from the
 *    screen's origin which it can add to the offsets of its children to find
 *    the [Offset] from the screen's origin to each of the children.
 *
 * Because a particular [Offset] can be interpreted as one sense at one time
 * then as the other sense at a later time, the same class is used for both
 * senses.
 *
 * See also:
 *
 *  * [Size], which represents a vector describing the size of a rectangle.
 *
 * Creates an offset. The first argument sets [x], the horizontal component,
 * and the second sets [y], the vertical component.
 */
@Immutable
@kotlin.jvm.JvmInline
value class Offset internal constructor(internal val packedValue: Long) {

    @Stable
    val x: Float
        get() {
            // Explicitly compare against packed values to avoid auto-boxing of Size.Unspecified
            check(this.packedValue != Unspecified.packedValue) {
                "Offset is unspecified"
            }
            return unpackFloat1(packedValue)
        }

    @Stable
    val y: Float
        get() {
            // Explicitly compare against packed values to avoid auto-boxing of Size.Unspecified
            check(this.packedValue != Unspecified.packedValue) {
                "Offset is unspecified"
            }
            return unpackFloat2(packedValue)
        }

    @Stable
    operator fun component1(): Float = x

    @Stable
    operator fun component2(): Float = y

    /**
     * Returns a copy of this Offset instance optionally overriding the
     * x or y parameter
     */
    fun copy(x: Float = this.x, y: Float = this.y) = Offset(x, y)

    companion object {
        /**
         * An offset with zero magnitude.
         *
         * This can be used to represent the origin of a coordinate space.
         */
        @Stable
        val Zero = Offset(0.0f, 0.0f)

        /**
         * An offset with infinite x and y components.
         *
         * See also:
         *
         *  * [isInfinite], which checks whether either component is infinite.
         *  * [isFinite], which checks whether both components are finite.
         */
        // This is included for completeness, because [Size.infinite] exists.
        @Stable
        val Infinite = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)

        /**
         * Represents an unspecified [Offset] value, usually a replacement for `null`
         * when a primitive value is desired.
         */
        @Stable
        val Unspecified = Offset(Float.NaN, Float.NaN)
    }

    @Stable
    fun isValid(): Boolean {
        check(!x.isNaN() && !y.isNaN()) {
            "Offset argument contained a NaN value."
        }
        return true
    }

    /**
     * The magnitude of the offset.
     *
     * If you need this value to compare it to another [Offset]'s distance,
     * consider using [getDistanceSquared] instead, since it is cheaper to compute.
     */
    @Stable
    fun getDistance() = sqrt(x * x + y * y)

    /**
     * The square of the magnitude of the offset.
     *
     * This is cheaper than computing the [getDistance] itself.
     */
    @Stable
    fun getDistanceSquared() = x * x + y * y

    /**
     * Unary negation operator.
     *
     * Returns an offset with the coordinates negated.
     *
     * If the [Offset] represents an arrow on a plane, this operator returns the
     * same arrow but pointing in the reverse direction.
     */
    @Stable
    operator fun unaryMinus(): Offset = Offset(-x, -y)

    /**
     * Binary subtraction operator.
     *
     * Returns an offset whose [x] value is the left-hand-side operand's [x]
     * minus the right-hand-side operand's [x] and whose [y] value is the
     * left-hand-side operand's [y] minus the right-hand-side operand's [y].
     */
    @Stable
    operator fun minus(other: Offset): Offset = Offset(x - other.x, y - other.y)

    /**
     * Binary addition operator.
     *
     * Returns an offset whose [x] value is the sum of the [x] values of the
     * two operands, and whose [y] value is the sum of the [y] values of the
     * two operands.
     */
    @Stable
    operator fun plus(other: Offset): Offset = Offset(x + other.x, y + other.y)

    /**
     * Multiplication operator.
     *
     * Returns an offset whose coordinates are the coordinates of the
     * left-hand-side operand (an Offset) multiplied by the scalar
     * right-hand-side operand (a Float).
     */
    @Stable
    operator fun times(operand: Float): Offset = Offset(x * operand, y * operand)

    /**
     * Division operator.
     *
     * Returns an offset whose coordinates are the coordinates of the
     * left-hand-side operand (an Offset) divided by the scalar right-hand-side
     * operand (a Float).
     */
    @Stable
    operator fun div(operand: Float): Offset = Offset(x / operand, y / operand)

    /**
     * Modulo (remainder) operator.
     *
     * Returns an offset whose coordinates are the remainder of dividing the
     * coordinates of the left-hand-side operand (an Offset) by the scalar
     * right-hand-side operand (a Float).
     */
    @Stable
    operator fun rem(operand: Float) = Offset(x % operand, y % operand)

    override fun toString() = if (isSpecified) {
        "Offset(${x.toStringAsFixed(1)}, ${y.toStringAsFixed(1)})"
    } else {
        // In this case reading the x or y properties will throw, and they don't contain meaningful
        // values as strings anyway.
        "Offset.Unspecified"
    }
}

/**
 * Linearly interpolate between two offsets.
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
fun lerp(start: Offset, stop: Offset, fraction: Float): Offset {
    return Offset(
        lerp(start.x, stop.x, fraction),
        lerp(start.y, stop.y, fraction)
    )
}

/**
 * True if both x and y values of the [Offset] are finite
 */
@Stable
val Offset.isFinite: Boolean get() = x.isFinite() && y.isFinite()

/**
 * `false` when this is [Offset.Unspecified].
 */
@Stable
val Offset.isSpecified: Boolean get() = packedValue != Offset.Unspecified.packedValue

/**
 * `true` when this is [Offset.Unspecified].
 */
@Stable
val Offset.isUnspecified: Boolean get() = packedValue == Offset.Unspecified.packedValue

/**
 * If this [Offset]&nbsp;[isSpecified] then this is returned, otherwise [block] is executed
 * and its result is returned.
 */
inline fun Offset.takeOrElse(block: () -> Offset): Offset =
    if (isSpecified) this else block()
