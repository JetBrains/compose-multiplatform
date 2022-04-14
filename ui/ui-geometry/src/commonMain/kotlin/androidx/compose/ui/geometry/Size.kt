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
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

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
@kotlin.jvm.JvmInline
value class Size internal constructor(@PublishedApi internal val packedValue: Long) {

    @Stable
    val width: Float
        get() {
            // Explicitly compare against packed values to avoid auto-boxing of Size.Unspecified
            check(this.packedValue != Unspecified.packedValue) {
                "Size is unspecified"
            }
            return unpackFloat1(packedValue)
        }

    @Stable
    val height: Float
        get() {
            // Explicitly compare against packed values to avoid auto-boxing of Size.Unspecified
            check(this.packedValue != Unspecified.packedValue) {
                "Size is unspecified"
            }
            return unpackFloat2(packedValue)
        }

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
         * An empty size, one with a zero width and a zero height.
         */
        @Stable
        val Zero = Size(0.0f, 0.0f)

        /**
         * A size whose [width] and [height] are unspecified. This is a sentinel
         * value used to initialize a non-null parameter.
         * Access to width or height on an unspecified size is not allowed.
         */
        @Stable
        val Unspecified = Size(Float.NaN, Float.NaN)
    }

    /**
     * Whether this size encloses a non-zero area.
     *
     * Negative areas are considered empty.
     */
    @Stable
    fun isEmpty() = width <= 0.0f || height <= 0.0f

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

    override fun toString() =
        if (isSpecified) {
            "Size(${width.toStringAsFixed(1)}, ${height.toStringAsFixed(1)})"
        } else {
            // In this case reading the width or height properties will throw, and they don't
            // contain meaningful values as strings anyway.
            "Size.Unspecified"
        }
}

/**
 * `false` when this is [Size.Unspecified].
 */
@Stable
inline val Size.isSpecified: Boolean
    get() = packedValue != Size.Unspecified.packedValue

/**
 * `true` when this is [Size.Unspecified].
 */
@Stable
inline val Size.isUnspecified: Boolean
    get() = packedValue == Size.Unspecified.packedValue

/**
 * If this [Size]&nbsp;[isSpecified] then this is returned, otherwise [block] is executed
 * and its result is returned.
 */
inline fun Size.takeOrElse(block: () -> Size): Size =
    if (isSpecified) this else block()

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
fun lerp(start: Size, stop: Size, fraction: Float): Size {
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

/**
 * Returns the [Offset] of the center of the rect from the point of [0, 0]
 * with this [Size].
 */
@Stable
val Size.center: Offset get() = Offset(width / 2f, height / 2f)