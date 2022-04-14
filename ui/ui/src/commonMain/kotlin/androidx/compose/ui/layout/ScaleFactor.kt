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

package androidx.compose.ui.layout

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.util.packFloats
import androidx.compose.ui.util.unpackFloat1
import androidx.compose.ui.util.unpackFloat2

/**
 * Constructs a [ScaleFactor] from the given x and y scale values
 */
@Stable
fun ScaleFactor(scaleX: Float, scaleY: Float) = ScaleFactor(packFloats(scaleX, scaleY))

/**
 * Holds 2 dimensional scaling factors for horizontal and vertical axes
 */
@Immutable
@kotlin.jvm.JvmInline
value class ScaleFactor internal constructor(@PublishedApi internal val packedValue: Long) {

    /**
     * Returns the scale factor to apply along the horizontal axis
     */
    @Stable
    val scaleX: Float
        get() {
            // Explicitly compare against packed values to avoid
            // auto-boxing of ScaleFactor.Unspecified
            check(this.packedValue != ScaleFactor.Unspecified.packedValue) {
                "ScaleFactor is unspecified"
            }
            return unpackFloat1(packedValue)
        }

    /**
     * Returns the scale factor to apply along the vertical axis
     */
    @Stable
    val scaleY: Float
        get() {
            // Explicitly compare against packed values to avoid
            // auto-boxing of Size.Unspecified
            check(this.packedValue != ScaleFactor.Unspecified.packedValue) {
                "ScaleFactor is unspecified"
            }
            return unpackFloat2(packedValue)
        }

    @Suppress("NOTHING_TO_INLINE")
    @Stable
    inline operator fun component1(): Float = scaleX

    @Suppress("NOTHING_TO_INLINE")
    @Stable
    inline operator fun component2(): Float = scaleY

    /**
     * Returns a copy of this ScaleFactor instance optionally overriding the
     * scaleX or scaleY parameters
     */
    fun copy(scaleX: Float = this.scaleX, scaleY: Float = this.scaleY) = ScaleFactor(scaleX, scaleY)

    /**
     * Multiplication operator.
     *
     * Returns a [ScaleFactor] with scale x and y values multiplied by the operand
     */
    @Stable
    operator fun times(operand: Float) = ScaleFactor(scaleX * operand, scaleY * operand)

    /**
     * Division operator.
     *
     * Returns a [ScaleFactor] with scale x and y values divided by the operand
     */
    @Stable
    operator fun div(operand: Float) = ScaleFactor(scaleX / operand, scaleY / operand)

    override fun toString() = "ScaleFactor(${scaleX.roundToTenths()}, ${scaleY.roundToTenths()})"

    companion object {

        /**
         * A ScaleFactor whose [scaleX] and [scaleY] parameters are unspecified. This is a sentinel
         * value used to initialize a non-null parameter.
         * Access to scaleX or scaleY on an unspecified size is not allowed
         */
        @Stable
        val Unspecified = ScaleFactor(Float.NaN, Float.NaN)
    }
}

private fun Float.roundToTenths(): Float {
    val shifted = this * 10
    val decimal = shifted - shifted.toInt()
    // Kotlin's round operator rounds 0.5f down to 0. Manually compare against
    // 0.5f and round up if necessary
    val roundedShifted = if (decimal >= 0.5f) {
        shifted.toInt() + 1
    } else {
        shifted.toInt()
    }
    return roundedShifted.toFloat() / 10
}

/**
 * `false` when this is [ScaleFactor.Unspecified].
 */
@Stable
inline val ScaleFactor.isSpecified: Boolean
    get() = packedValue != ScaleFactor.Unspecified.packedValue

/**
 * `true` when this is [ScaleFactor.Unspecified].
 */
@Stable
inline val ScaleFactor.isUnspecified: Boolean
    get() = packedValue == ScaleFactor.Unspecified.packedValue

/**
 * If this [ScaleFactor] [isSpecified] then this is returned, otherwise [block] is executed
 * and its result is returned.
 */
inline fun ScaleFactor.takeOrElse(block: () -> ScaleFactor): ScaleFactor =
    if (isSpecified) this else block()

/**
 * Multiplication operator with [Size].
 *
 * Return a new [Size] with the width and height multiplied by the [ScaleFactor.scaleX] and
 * [ScaleFactor.scaleY] respectively
 */
@Stable
operator fun Size.times(scaleFactor: ScaleFactor): Size =
    Size(this.width * scaleFactor.scaleX, this.height * scaleFactor.scaleY)

/**
 * Multiplication operator with [Size] with reverse parameter types to maintain
 * commutative properties of multiplication
 *
 * Return a new [Size] with the width and height multiplied by the [ScaleFactor.scaleX] and
 * [ScaleFactor.scaleY] respectively
 */
@Stable
operator fun ScaleFactor.times(size: Size): Size = size * this

/**
 * Division operator with [Size]
 *
 * Return a new [Size] with the width and height divided by [ScaleFactor.scaleX] and
 * [ScaleFactor.scaleY] respectively
 */
@Stable
operator fun Size.div(scaleFactor: ScaleFactor): Size =
    Size(width / scaleFactor.scaleX, height / scaleFactor.scaleY)

/**
 * Linearly interpolate between two [ScaleFactor] parameters
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
fun lerp(start: ScaleFactor, stop: ScaleFactor, fraction: Float): ScaleFactor {
    return ScaleFactor(
        androidx.compose.ui.util.lerp(start.scaleX, stop.scaleX, fraction),
        androidx.compose.ui.util.lerp(start.scaleY, stop.scaleY, fraction)
    )
}