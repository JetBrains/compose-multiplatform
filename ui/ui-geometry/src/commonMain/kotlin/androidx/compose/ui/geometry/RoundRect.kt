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
import androidx.compose.ui.util.toStringAsFixed
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
    val topLeftRadius: Radius = Radius.Zero,

    /** The top-right radius */
    val topRightRadius: Radius = Radius.Zero,

    /** The bottom-right radius */
    val bottomRightRadius: Radius = Radius.Zero,

    /** The bottom-left radius */
    val bottomLeftRadius: Radius = Radius.Zero
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
        scale = minRadius(scale, bottomLeftRadius.y, topLeftRadius.y, height)
        scale = minRadius(scale, topLeftRadius.x, topRightRadius.x, width)
        scale = minRadius(scale, topRightRadius.y, bottomRightRadius.y, height)
        scale = minRadius(scale, bottomRightRadius.x, bottomLeftRadius.x, width)

        RoundRect(
            left = left * scale,
            top = top * scale,
            right = right * scale,
            bottom = bottom * scale,
            topLeftRadius = Radius(topLeftRadius.x * scale, topLeftRadius.y * scale),
            topRightRadius = Radius(topRightRadius.x * scale, topRightRadius.y * scale),
            bottomRightRadius = Radius(bottomRightRadius.x * scale, bottomRightRadius.y * scale),
            bottomLeftRadius = Radius(bottomLeftRadius.x * scale, bottomLeftRadius.y * scale)
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
    fun contains(point: Offset): Boolean {
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
        if (point.x < left + scaled.topLeftRadius.x &&
            point.y < top + scaled.topLeftRadius.y
        ) {
            x = point.x - left - scaled.topLeftRadius.x
            y = point.y - top - scaled.topLeftRadius.y
            radiusX = scaled.topLeftRadius.x
            radiusY = scaled.topLeftRadius.y
        } else if (point.x > right - scaled.topRightRadius.x &&
            point.y < top + scaled.topRightRadius.y
        ) {
            x = point.x - right + scaled.topRightRadius.x
            y = point.y - top - scaled.topRightRadius.y
            radiusX = scaled.topRightRadius.x
            radiusY = scaled.topRightRadius.y
        } else if (point.x > right - scaled.bottomRightRadius.x &&
            point.y > bottom - scaled.bottomRightRadius.y
        ) {
            x = point.x - right + scaled.bottomRightRadius.x
            y = point.y - bottom + scaled.bottomRightRadius.y
            radiusX = scaled.bottomRightRadius.x
            radiusY = scaled.bottomRightRadius.y
        } else if (point.x < left + scaled.bottomLeftRadius.x &&
            point.y > bottom - scaled.bottomLeftRadius.y
        ) {
            x = point.x - left - scaled.bottomLeftRadius.x
            y = point.y - bottom + scaled.bottomLeftRadius.y
            radiusX = scaled.bottomLeftRadius.x
            radiusY = scaled.bottomLeftRadius.y
        } else {
            return true; // inside and not within the rounded corner area
        }

        val newX = x / radiusX
        val newY = y / radiusY

        // check if the point is inside the unit circle
        return newX * newX + newY * newY <= 1.0f
    }

    override fun toString(): String {
        val tlRadius = topLeftRadius
        val trRadius = topRightRadius
        val brRadius = bottomRightRadius
        val blRadius = bottomLeftRadius
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
        val Zero = RoundRect(0.0f, 0.0f, 0.0f, 0.0f, Radius.Zero)
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
    val radius = Radius(radiusX, radiusY)
    return RoundRect(
        left = left,
        top = top,
        right = right,
        bottom = bottom,
        topLeftRadius = radius,
        topRightRadius = radius,
        bottomRightRadius = radius,
        bottomLeftRadius = radius
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
    radius: Radius
) = RoundRect(
    left,
    top,
    right,
    bottom,
    radius.x,
    radius.y
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
    radius: Radius
): RoundRect = RoundRect(
    rect = rect,
    radiusX = radius.x,
    radiusY = radius.y
)

/**
 * Construct a rounded rectangle from its bounding box and topLeft,
 * topRight, bottomRight, and bottomLeft radii.
 *
 * The corner radii default to [Radius.Zero], i.e. right-angled corners
 */
fun RoundRect(
    rect: Rect,
    topLeft: Radius = Radius.Zero,
    topRight: Radius = Radius.Zero,
    bottomRight: Radius = Radius.Zero,
    bottomLeft: Radius = Radius.Zero
): RoundRect = RoundRect(
    left = rect.left,
    top = rect.top,
    right = rect.right,
    bottom = rect.bottom,
    topLeftRadius = topLeft,
    topRightRadius = topRight,
    bottomRightRadius = bottomRight,
    bottomLeftRadius = bottomLeft
)

/** The top-left [Radius]. */
@Deprecated(
    "use topLeftRadius instead",
    ReplaceWith("topLeftRadius", "androidx.compose.ui.geometry")
)
fun RoundRect.topLeftRadius(): Radius = topLeftRadius

/**  The top-right [Radius]. */
@Deprecated(
    "Use topRightRadius instead",
    ReplaceWith("topRightRadius", "androidx.compose.ui.geometry")
)
fun RoundRect.topRightRadius(): Radius = topRightRadius

/**  The bottom-right [Radius]. */
@Deprecated(
    "Use bottomRightRadius instead",
    ReplaceWith("bottomRightRadius", "androidx.compose.ui.geometry")
)
fun RoundRect.bottomRightRadius(): Radius = bottomRightRadius

/**  The bottom-right [Radius]. */
@Deprecated(
    "Use bottomLeftRadius instead",
    ReplaceWith("bottomLeftRadius", "androidx.compose.ui.geometry")
)
/** The bottom-left [Radius]. */
fun RoundRect.bottomLeftRadius(): Radius = bottomLeftRadius

/** Returns a new [RoundRect] translated by the given offset. */
@Deprecated(
    "Use translate(offset) instead",
    ReplaceWith("translate(offset)", "androidx.compose.ui.RoundRect")
)
fun RoundRect.shift(offset: Offset): RoundRect = translate(offset)

/** Returns a new [RoundRect] translated by the given offset. */
fun RoundRect.translate(offset: Offset): RoundRect = RoundRect(
    left = left + offset.x,
    top = top + offset.y,
    right = right + offset.x,
    bottom = bottom + offset.y,
    topLeftRadius = topLeftRadius,
    topRightRadius = topRightRadius,
    bottomRightRadius = bottomRightRadius,
    bottomLeftRadius = bottomLeftRadius
)

@Deprecated(
    "Use outerRect instead",
    ReplaceWith("boundingRect", "androidx.compose.ui.RoundRect")
)
/** The bounding box of this rounded rectangle (the rectangle with no rounded corners). */
fun RoundRect.outerRect(): Rect = boundingRect

/** The bounding box of this rounded rectangle (the rectangle with no rounded corners). */
val RoundRect.boundingRect: Rect get() = Rect(left, top, right, bottom)

/**
 * The non-rounded rectangle that is constrained by the smaller of the two
 * diagonals, with each diagonal traveling through the middle of the curve
 * corners. The middle of a corner is the intersection of the curve with its
 * respective quadrant bisector.
 */
@Deprecated(
    "Use safeInnerRect instead",
    ReplaceWith("safeInnerRect", "androidx.compose.ui.RoundRect")
)
fun RoundRect.safeInnerRect(): Rect = safeInnerRect

/**
 * The non-rounded rectangle that is constrained by the smaller of the two
 * diagonals, with each diagonal traveling through the middle of the curve
 * corners. The middle of a corner is the intersection of the curve with its
 * respective quadrant bisector.
 */
val RoundRect.safeInnerRect: Rect
    get() {
        val insetFactor = 0.29289321881f // 1-cos(pi/4)

        val leftRadius = max(bottomLeftRadius.x, topLeftRadius.x)
        val topRadius = max(topLeftRadius.y, topRightRadius.y)
        val rightRadius = max(topRightRadius.x, bottomRightRadius.x)
        val bottomRadius = max(bottomRightRadius.y, bottomLeftRadius.y)

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
val RoundRect.isRect get(): Boolean = (topLeftRadius.x == 0.0f || topLeftRadius.y == 0.0f) &&
    (topRightRadius.x == 0.0f || topRightRadius.y == 0.0f) &&
    (bottomLeftRadius.x == 0.0f || bottomLeftRadius.y == 0.0f) &&
    (bottomRightRadius.x == 0.0f || bottomRightRadius.y == 0.0f)

/** Whether this rounded rectangle has no side with a straight section. */
val RoundRect.isEllipse get(): Boolean =
    topLeftRadius.x == topRightRadius.x &&
        topLeftRadius.y == topRightRadius.y &&
        topRightRadius.x == bottomRightRadius.x &&
        topRightRadius.y == bottomRightRadius.y &&
        bottomRightRadius.x == bottomLeftRadius.x &&
        bottomRightRadius.y == bottomLeftRadius.y &&
        width <= 2.0 * topLeftRadius.x &&
        height <= 2.0 * topLeftRadius.y

/** Whether this rounded rectangle would draw as a circle. */
val RoundRect.isCircle get() = width == height && isEllipse

/**
 * The lesser of the magnitudes of the [RoundRect.width] and the [RoundRect.height] of this
 * rounded rectangle.
 */
@Deprecated(
    "Use minDimension instead",
    ReplaceWith("minDimension", "androidx.compose.ui.RoundRect")
)
val RoundRect.shortestSide get(): Float = minDimension

/**
 * The lesser of the magnitudes of the [RoundRect.width] and the [RoundRect.height] of this
 * rounded rectangle.
 */
val RoundRect.minDimension get(): Float = min(width.absoluteValue, height.absoluteValue)

/**
 * The greater of the magnitudes of the [RoundRect.width] and the [RoundRect.height] of this
 * rounded rectangle.
 */
@Deprecated(
    "Use maxDimension instead",
    ReplaceWith("maxDimension", "androidx.compose.ui.RoundRect")
)
val RoundRect.longestSide get(): Float = maxDimension

val RoundRect.maxDimension get(): Float = max(width.absoluteValue, height.absoluteValue)

/**
 * The offset to the point halfway between the left and right and the top and
 * bottom edges of this rectangle.
 */
@Deprecated(
    "Use center instead",
    ReplaceWith("center", "androidx.compose.ui.RoundRect")
)
fun RoundRect.center(): Offset = Offset((left + width / 2.0f), (top + height / 2.0f))

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
    get() = topLeftRadius.x == topLeftRadius.y &&
        topLeftRadius.x == topRightRadius.x &&
        topLeftRadius.x == topRightRadius.y &&
        topLeftRadius.x == bottomRightRadius.x &&
        topLeftRadius.x == bottomRightRadius.y &&
        topLeftRadius.x == bottomLeftRadius.x &&
        topLeftRadius.x == bottomLeftRadius.y

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
        topLeftRadius = lerp(
            start.topLeftRadius,
            stop.topLeftRadius,
            fraction
        ),
        topRightRadius = lerp(
            start.topRightRadius,
            stop.topRightRadius,
            fraction
        ),
        bottomRightRadius = lerp(
            start.bottomRightRadius,
            stop.bottomRightRadius,
            fraction
        ),
        bottomLeftRadius = lerp(
            start.bottomLeftRadius,
            stop.bottomLeftRadius,
            fraction
        )
    )
