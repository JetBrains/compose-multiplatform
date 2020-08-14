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
@file:Suppress("NOTHING_TO_INLINE")

package androidx.compose.ui.unit

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import kotlin.math.roundToInt

/**
 * Holds a unit of squared dimensions, such as `1.value * 2.px`. [PxSquared], [PxCubed],
 * and [PxInverse] are used primarily for pixel calculations to ensure resulting
 * units are as expected. Many times, pixel calculations use scalars to determine the final
 * dimension during calculation:
 *     val width = oldWidth * stretchAmount
 * Other times, it is useful to do intermediate calculations with Dimensions directly:
 *     val width = oldWidth * newTotalWidth / oldTotalWidth
 */
@Suppress("EXPERIMENTAL_FEATURE_WARNING")
@Immutable
inline class PxSquared(val value: Float) : Comparable<PxSquared> {
    /**
     * Add two DimensionSquares together.
     */
    @Stable
    inline operator fun plus(other: PxSquared) =
        PxSquared(value = value + other.value)

    /**
     * Subtract a DimensionSquare from another one.
     */
    @Stable
    inline operator fun minus(other: PxSquared) =
        PxSquared(value = value - other.value)

    /**
     * Divide a DimensionSquare by a scalar.
     */
    @Stable
    inline operator fun div(other: Float): PxSquared =
        PxSquared(value = value / other)

    /**
     * Divide by a PxSquared to get a scalar result.
     */
    @Stable
    inline operator fun div(other: PxSquared): Float = value / other.value

    /**
     * Divide by a [PxCubed] to get a [PxInverse] result.
     */
    @Stable
    inline operator fun div(other: PxCubed): PxInverse =
        PxInverse(value / other.value)

    /**
     * Multiply by a scalar to get a PxSquared result.
     */
    @Stable
    inline operator fun times(other: Float): PxSquared =
        PxSquared(value = value * other)

    /**
     * Support comparing PxSquared with comparison operators.
     */
    @Stable
    override /* TODO: inline */ operator fun compareTo(other: PxSquared) =
        value.compareTo(other.value)

    @Stable
    override fun toString(): String = "$value.px^2"
}

/**
 * Holds a unit of cubed dimensions, such as `1.value * 2.value * 3.px`. [PxSquared],
 * [PxCubed], and [PxInverse] are used primarily for pixel calculations to
 * ensure resulting units are as expected. Many times, pixel calculations use scalars to
 * determine the final dimension during calculation:
 *     val width = oldWidth * stretchAmount
 * Other times, it is useful to do intermediate calculations with Dimensions directly:
 *     val width = oldWidth * newTotalWidth / oldTotalWidth
 */
@Suppress("EXPERIMENTAL_FEATURE_WARNING")
@Immutable
inline class PxCubed(val value: Float) : Comparable<PxCubed> {
    /**
     * Add two PxCubed together.
     */
    @Stable
    inline operator fun plus(dimension: PxCubed) =
        PxCubed(value = value + dimension.value)

    /**
     * Subtract a PxCubed from another one.
     */
    @Stable
    inline operator fun minus(dimension: PxCubed) =
        PxCubed(value = value - dimension.value)

    /**
     * Divide a PxCubed by a scalar.
     */
    @Stable
    inline operator fun div(other: Float): PxCubed =
        PxCubed(value = value / other)

    /**
     * Divide by a PxCubed to get a scalar result.
     */
    @Stable
    inline operator fun div(other: PxCubed): Float = value / other.value

    /**
     * Multiply by a scalar to get a PxCubed result.
     */
    @Stable
    inline operator fun times(other: Float): PxCubed =
        PxCubed(value = value * other)

    /**
     * Support comparing PxCubed with comparison operators.
     */
    @Stable
    override /* TODO: inline */ operator fun compareTo(other: PxCubed) =
        value.compareTo(other.value)

    @Stable
    override fun toString(): String = "$value.px^3"
}

/**
 * Holds a unit of an inverse dimensions, such as `1.px / (2.value * 3.px)`. [PxSquared],
 * [PxCubed], and [PxInverse] are used primarily for pixel calculations to
 * ensure resulting units are as expected. Many times, pixel calculations use scalars to
 * determine the final dimension during calculation:
 *     val width = oldWidth * stretchAmount
 * Other times, it is useful to do intermediate calculations with Dimensions directly:
 *     val width = oldWidth * newTotalWidth / oldTotalWidth
 */
@Suppress("EXPERIMENTAL_FEATURE_WARNING")
@Immutable
inline class PxInverse(val value: Float) : Comparable<PxInverse> {
    /**
     * Add two PxInverse together.
     */
    @Stable
    inline operator fun plus(dimension: PxInverse) =
        PxInverse(value = value + dimension.value)

    /**
     * Subtract a PxInverse from another one.
     */
    @Stable
    inline operator fun minus(dimension: PxInverse) =
        PxInverse(value = value - dimension.value)

    /**
     * Divide a PxInverse by a scalar.
     */
    @Stable
    inline operator fun div(other: Float): PxInverse =
        PxInverse(value = value / other)

    /**
     * Multiply by a scalar to get a PxInverse result.
     */
    @Stable
    inline operator fun times(other: Float): PxInverse =
        PxInverse(value = value * other)

    /**
     * Multiply by a [PxCubed] to get a [PxSquared] result.
     */
    @Stable
    inline operator fun times(other: PxCubed): PxSquared =
        PxSquared(value = value * other.value)

    /**
     * Support comparing PxInverse with comparison operators.
     */
    @Stable
    override /* TODO: inline */ operator fun compareTo(other: PxInverse) =
        value.compareTo(other.value)

    @Stable
    override fun toString(): String = "$value.px^-1"
}

// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
// Structures using Px
// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

/**
 * Returns the [Offset] of the center of the rect from the point of [0, 0]
 * with this [Size].
 */
@Stable
fun Size.center(): Offset {
    return Offset(width / 2f, height / 2f)
}

/**
 * Round a [Offset] down to the nearest [Int] coordinates.
 */
@Stable
inline fun Offset.round(): IntOffset = IntOffset(x.roundToInt(), y.roundToInt())

/**
 * A four dimensional bounds using pixels for units
 */
@Deprecated("Use Rect instead",
    ReplaceWith("Rect(left, top, right, bottom)",
        "androidx.compose.ui.geometry"))
@Immutable
data class PxBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    companion object
}

@Deprecated("Use Rect instead",
    ReplaceWith("Rect(topleft, size)", "androidx.compose.ui.geometry"))
@Suppress("DEPRECATION")
@Stable
inline fun PxBounds(topLeft: Offset, size: Size) =
    PxBounds(
        left = topLeft.x,
        top = topLeft.y,
        right = topLeft.x + size.width,
        bottom = topLeft.y + size.height
    )

/**
 * A width of this PxBounds in pixels.
 */
@Deprecated("Use Rect.width instead", ReplaceWith("Rect.width",
    "androidx.compose.ui.geometry"))
@Suppress("DEPRECATION")
@Stable
inline val PxBounds.width: Float get() = right - left

/**
 * A height of this PxBounds in pixels.
 */
@Deprecated("use Rect.height instead", ReplaceWith("Rect.height",
    "androidx.compose.ui.geometry"))
@Suppress("DEPRECATION")
@Stable
inline val PxBounds.height: Float get() = bottom - top

/**
 * Returns the [Offset] of the center of the [PxBounds].
 */
@Deprecated("Use Rect.center instead", ReplaceWith("Rect.center",
    "androidx.compose.ui.geometry"))
@Suppress("DEPRECATION")
@Stable
inline fun PxBounds.center(): Offset {
    return Offset((left + right) / 2f, (top + bottom) / 2f)
}

/**
 * Convert a [PxBounds] to a [Size].
 */
@Deprecated("Use Rect.size instead", ReplaceWith("Rect.size",
    "androidx.compose.ui.geometry"))
@Suppress("DEPRECATION")
@Stable
fun PxBounds.toSize(): Size {
    return Size(width, height)
}

/**
 * Convert a [Size] to a [PxBounds]. The left and top are 0.px and the right and bottom
 * are the width and height, respectively.
 */
@Deprecated("Use toRect() instead",
    ReplaceWith("Size.toRect()", "androidx.compose.ui.geometry"))
@Suppress("DEPRECATION")
@Stable
fun Size.toBounds(): PxBounds {
    return PxBounds(0f, 0f, width, height)
}

/**
 * Convert a [PxBounds] to a [Rect].
 */
@Deprecated("Use Rect instead of PxBounds", ReplaceWith("Rect(left, top, right, bottom)",
    "androidx.compose.ui.geometry"))
@Suppress("DEPRECATION")
@Stable
fun PxBounds.toRect(): Rect {
    return Rect(
        left,
        top,
        right,
        bottom
    )
}