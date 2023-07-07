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
import androidx.compose.ui.util.lerp
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

/**
 * An immutable rounded rectangle with custom radii for all four corners.
 */
@Immutable
data class RoundRect(
    /** The offset of the left edge of this rectangle from the x axis */
    val left: Float,
    /** The offset of the top edge of this rectangle from the y axis */
    val top: Float,
    /** The offset of the right edge of this rectangle from the x axis */
    val right: Float,
    /** The offset of the bottom edge of this rectangle from the y axis */
    val bottom: Float,
    /** The top-left radius */
    val topLeftCornerRadius: CornerRadius = CornerRadius.Zero,

    /** The top-right radius */
    val topRightCornerRadius: CornerRadius = CornerRadius.Zero,

    /** The bottom-right radius */
    val bottomRightCornerRadius: CornerRadius = CornerRadius.Zero,

    /** The bottom-left radius */
    val bottomLeftCornerRadius: CornerRadius = CornerRadius.Zero
) {
    /** The distance between the left and right edges of this rectangle. */
    val width: Float
        get() = right - left

    /** The distance between the top and bottom edges of this rectangle. */
    val height: Float
        get() = bottom - top

    /**
     * Same RoundRect with scaled radii per side. If you need this call [scaledRadiiRect] instead.
     * Not @Volatile since the computed result will always be the same even if we race
     * and duplicate creation/computation in [scaledRadiiRect].
     */
    private var _scaledRadiiRect: RoundRect? = null

    /**
     * Scales all radii so that on each side their sum will not pass the size of
     * the width/height.
     */
    private fun scaledRadiiRect(): RoundRect = _scaledRadiiRect ?: run {
        var scale = 1.0f
        scale = minRadius(scale, bottomLeftCornerRadius.y, topLeftCornerRadius.y, height)
        scale = minRadius(scale, topLeftCornerRadius.x, topRightCornerRadius.x, width)
        scale = minRadius(scale, topRightCornerRadius.y, bottomRightCornerRadius.y, height)
        scale = minRadius(scale, bottomRightCornerRadius.x, bottomLeftCornerRadius.x, width)

        RoundRect(
            left = left * scale,
            top = top * scale,
            right = right * scale,
            bottom = bottom * scale,
            topLeftCornerRadius = CornerRadius(
                topLeftCornerRadius.x * scale,
                topLeftCornerRadius.y * scale
            ),
            topRightCornerRadius = CornerRadius(
                topRightCornerRadius.x * scale,
                topRightCornerRadius.y * scale
            ),
            bottomRightCornerRadius = CornerRadius(
                bottomRightCornerRadius.x * scale,
                bottomRightCornerRadius.y * scale
            ),
            bottomLeftCornerRadius = CornerRadius(
                bottomLeftCornerRadius.x * scale,
                bottomLeftCornerRadius.y * scale
            )
        )
    }.also {
        // This might happen racey on different threads, we don't care, it'll be the same results.
        _scaledRadiiRect = it
    }

    /**
     * Returns the minimum between min and scale to which radius1 and radius2
     * should be scaled with in order not to exceed the limit.
     */
    private fun minRadius(min: Float, radius1: Float, radius2: Float, limit: Float): Float {
        val sum = radius1 + radius2
        return if (sum > limit && sum != 0.0f) {
            min(min, limit / sum)
        } else {
            min
        }
    }

    /**
     * Whether the point specified by the given offset (which is assumed to be
     * relative to the origin) lies inside the rounded rectangle.
     *
     * This method may allocate (and cache) a copy of the object with normalized
     * radii the first time it is called on a particular [RoundRect] instance. When
     * using this method, prefer to reuse existing [RoundRect]s rather than
     * recreating the object each time.
     */
    operator fun contains(point: Offset): Boolean {
        if (point.x < left || point.x >= right || point.y < top || point.y >= bottom) {
            return false; // outside bounding box
        }

        val scaled = scaledRadiiRect()

        val x: Float
        val y: Float
        val radiusX: Float
        val radiusY: Float
        // check whether point is in one of the rounded corner areas
        // x, y -> translate to ellipse center
        if (point.x < left + scaled.topLeftCornerRadius.x &&
            point.y < top + scaled.topLeftCornerRadius.y
        ) {
            x = point.x - left - scaled.topLeftCornerRadius.x
            y = point.y - top - scaled.topLeftCornerRadius.y
            radiusX = scaled.topLeftCornerRadius.x
            radiusY = scaled.topLeftCornerRadius.y
        } else if (point.x > right - scaled.topRightCornerRadius.x &&
            point.y < top + scaled.topRightCornerRadius.y
        ) {
            x = point.x - right + scaled.topRightCornerRadius.x
            y = point.y - top - scaled.topRightCornerRadius.y
            radiusX = scaled.topRightCornerRadius.x
            radiusY = scaled.topRightCornerRadius.y
        } else if (point.x > right - scaled.bottomRightCornerRadius.x &&
            point.y > bottom - scaled.bottomRightCornerRadius.y
        ) {
            x = point.x - right + scaled.bottomRightCornerRadius.x
            y = point.y - bottom + scaled.bottomRightCornerRadius.y
            radiusX = scaled.bottomRightCornerRadius.x
            radiusY = scaled.bottomRightCornerRadius.y
        } else if (point.x < left + scaled.bottomLeftCornerRadius.x &&
            point.y > bottom - scaled.bottomLeftCornerRadius.y
        ) {
            x = point.x - left - scaled.bottomLeftCornerRadius.x
            y = point.y - bottom + scaled.bottomLeftCornerRadius.y
            radiusX = scaled.bottomLeftCornerRadius.x
            radiusY = scaled.bottomLeftCornerRadius.y
        } else {
            return true; // inside and not within the rounded corner area
        }

        val newX = x / radiusX
        val newY = y / radiusY

        // check if the point is inside the unit circle
        return newX * newX + newY * newY <= 1.0f
    }

    override fun toString(): String {
        val tlRadius = topLeftCornerRadius
        val trRadius = topRightCornerRadius
        val brRadius = bottomRightCornerRadius
        val blRadius = bottomLeftCornerRadius
        val rect =
            "${left.toStringAsFixed(1)}, " +
                "${top.toStringAsFixed(1)}, " +
                "${right.toStringAsFixed(1)}, " +
                bottom.toStringAsFixed(1)
        if (tlRadius == trRadius &&
            trRadius == brRadius &&
            brRadius == blRadius
        ) {
            if (tlRadius.x == tlRadius.y) {
                return "RoundRect(rect=$rect, radius=${tlRadius.x.toStringAsFixed(1)})"
            }
            return "RoundRect(rect=$rect, x=${tlRadius.x.toStringAsFixed(1)}, " +
                "y=${tlRadius.y.toStringAsFixed(1)})"
        }
        return "RoundRect(" +
            "rect=$rect, " +
            "topLeft=$tlRadius, " +
            "topRight=$trRadius, " +
            "bottomRight=$brRadius, " +
            "bottomLeft=$blRadius)"
    }

    companion object {
        /** A rounded rectangle with all the values set to zero. */
        @kotlin.jvm.JvmStatic
        val Zero = RoundRect(0.0f, 0.0f, 0.0f, 0.0f, CornerRadius.Zero)
    }
}

/**
 * Construct a rounded rectangle from its left, top, right, and bottom edges,
 * and the same radii along its horizontal axis and its vertical axis.
 */
fun RoundRect(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    radiusX: Float,
    radiusY: Float
): RoundRect {
    val radius = CornerRadius(radiusX, radiusY)
    return RoundRect(
        left = left,
        top = top,
        right = right,
        bottom = bottom,
        topLeftCornerRadius = radius,
        topRightCornerRadius = radius,
        bottomRightCornerRadius = radius,
        bottomLeftCornerRadius = radius
    )
}

/**
 * Construct a rounded rectangle from its left, top, right, and bottom edges,
 * and the same radius in each corner.
 */
fun RoundRect(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    cornerRadius: CornerRadius
) = RoundRect(
    left,
    top,
    right,
    bottom,
    cornerRadius.x,
    cornerRadius.y
)

/**
 * Construct a rounded rectangle from its bounding box and the same radii
 * along its horizontal axis and its vertical axis.
 */
fun RoundRect(
    rect: Rect,
    radiusX: Float,
    radiusY: Float
): RoundRect = RoundRect(
    left = rect.left,
    top = rect.top,
    right = rect.right,
    bottom = rect.bottom,
    radiusX = radiusX,
    radiusY = radiusY
)

/**
 * Construct a rounded rectangle from its bounding box and a radius that is
 * the same in each corner.
 */
fun RoundRect(
    rect: Rect,
    cornerRadius: CornerRadius
): RoundRect = RoundRect(
    rect = rect,
    radiusX = cornerRadius.x,
    radiusY = cornerRadius.y
)

/**
 * Construct a rounded rectangle from its bounding box and topLeft,
 * topRight, bottomRight, and bottomLeft radii.
 *
 * The corner radii default to [CornerRadius.Zero], i.e. right-angled corners
 */
fun RoundRect(
    rect: Rect,
    topLeft: CornerRadius = CornerRadius.Zero,
    topRight: CornerRadius = CornerRadius.Zero,
    bottomRight: CornerRadius = CornerRadius.Zero,
    bottomLeft: CornerRadius = CornerRadius.Zero
): RoundRect = RoundRect(
    left = rect.left,
    top = rect.top,
    right = rect.right,
    bottom = rect.bottom,
    topLeftCornerRadius = topLeft,
    topRightCornerRadius = topRight,
    bottomRightCornerRadius = bottomRight,
    bottomLeftCornerRadius = bottomLeft
)

/** Returns a new [RoundRect] translated by the given offset. */
fun RoundRect.translate(offset: Offset): RoundRect = RoundRect(
    left = left + offset.x,
    top = top + offset.y,
    right = right + offset.x,
    bottom = bottom + offset.y,
    topLeftCornerRadius = topLeftCornerRadius,
    topRightCornerRadius = topRightCornerRadius,
    bottomRightCornerRadius = bottomRightCornerRadius,
    bottomLeftCornerRadius = bottomLeftCornerRadius
)

/** The bounding box of this rounded rectangle (the rectangle with no rounded corners). */
val RoundRect.boundingRect: Rect get() = Rect(left, top, right, bottom)

/**
 * The non-rounded rectangle that is constrained by the smaller of the two
 * diagonals, with each diagonal traveling through the middle of the curve
 * corners. The middle of a corner is the intersection of the curve with its
 * respective quadrant bisector.
 */
val RoundRect.safeInnerRect: Rect
    get() {
        val insetFactor = 0.29289321881f // 1-cos(pi/4)

        val leftRadius = max(bottomLeftCornerRadius.x, topLeftCornerRadius.x)
        val topRadius = max(topLeftCornerRadius.y, topRightCornerRadius.y)
        val rightRadius = max(topRightCornerRadius.x, bottomRightCornerRadius.x)
        val bottomRadius = max(bottomRightCornerRadius.y, bottomLeftCornerRadius.y)

        return Rect(
            left + leftRadius * insetFactor,
            top + topRadius * insetFactor,
            right - rightRadius * insetFactor,
            bottom - bottomRadius * insetFactor
        )
    }

/**
 * Whether this rounded rectangle encloses a non-zero area.
 * Negative areas are considered empty.
 */
val RoundRect.isEmpty get() = left >= right || top >= bottom

/** Whether all coordinates of this rounded rectangle are finite. */
val RoundRect.isFinite get() =
    left.isFinite() && top.isFinite() && right.isFinite() && bottom.isFinite()

/**
 * Whether this rounded rectangle is a simple rectangle with zero
 * corner radii.
 */
val RoundRect.isRect get(): Boolean =
    (topLeftCornerRadius.x == 0.0f || topLeftCornerRadius.y == 0.0f) &&
        (topRightCornerRadius.x == 0.0f || topRightCornerRadius.y == 0.0f) &&
        (bottomLeftCornerRadius.x == 0.0f || bottomLeftCornerRadius.y == 0.0f) &&
        (bottomRightCornerRadius.x == 0.0f || bottomRightCornerRadius.y == 0.0f)

/** Whether this rounded rectangle has no side with a straight section. */
val RoundRect.isEllipse get(): Boolean =
    topLeftCornerRadius.x == topRightCornerRadius.x &&
        topLeftCornerRadius.y == topRightCornerRadius.y &&
        topRightCornerRadius.x == bottomRightCornerRadius.x &&
        topRightCornerRadius.y == bottomRightCornerRadius.y &&
        bottomRightCornerRadius.x == bottomLeftCornerRadius.x &&
        bottomRightCornerRadius.y == bottomLeftCornerRadius.y &&
        width <= 2.0 * topLeftCornerRadius.x &&
        height <= 2.0 * topLeftCornerRadius.y

/** Whether this rounded rectangle would draw as a circle. */
val RoundRect.isCircle get() = width == height && isEllipse

/**
 * The lesser of the magnitudes of the [RoundRect.width] and the [RoundRect.height] of this
 * rounded rectangle.
 */
val RoundRect.minDimension get(): Float = min(width.absoluteValue, height.absoluteValue)

val RoundRect.maxDimension get(): Float = max(width.absoluteValue, height.absoluteValue)

/**
 * The offset to the point halfway between the left and right and the top and
 * bottom edges of this rectangle.
 */
val RoundRect.center: Offset get() = Offset((left + width / 2.0f), (top + height / 2.0f))

/**
 * Returns `true` if the rounded rectangle have the same radii in both the horizontal and vertical
 * direction for all corners.
 */
val RoundRect.isSimple: Boolean
    get() = topLeftCornerRadius.x == topLeftCornerRadius.y &&
        topLeftCornerRadius.x == topRightCornerRadius.x &&
        topLeftCornerRadius.x == topRightCornerRadius.y &&
        topLeftCornerRadius.x == bottomRightCornerRadius.x &&
        topLeftCornerRadius.x == bottomRightCornerRadius.y &&
        topLeftCornerRadius.x == bottomLeftCornerRadius.x &&
        topLeftCornerRadius.x == bottomLeftCornerRadius.y

/**
 * Linearly interpolate between two rounded rectangles.
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
fun lerp(start: RoundRect, stop: RoundRect, fraction: Float): RoundRect =
    RoundRect(
        left = lerp(start.left, stop.left, fraction),
        top = lerp(start.top, stop.top, fraction),
        right = lerp(start.right, stop.right, fraction),
        bottom = lerp(start.bottom, stop.bottom, fraction),
        topLeftCornerRadius = lerp(
            start.topLeftCornerRadius,
            stop.topLeftCornerRadius,
            fraction
        ),
        topRightCornerRadius = lerp(
            start.topRightCornerRadius,
            stop.topRightCornerRadius,
            fraction
        ),
        bottomRightCornerRadius = lerp(
            start.bottomRightCornerRadius,
            stop.bottomRightCornerRadius,
            fraction
        ),
        bottomLeftCornerRadius = lerp(
            start.bottomLeftCornerRadius,
            stop.bottomLeftCornerRadius,
            fraction
        )
    )
