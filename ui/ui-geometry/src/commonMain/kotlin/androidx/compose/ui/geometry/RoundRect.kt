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
    /** The top-left horizontal radius */
    val topLeftRadiusX: Float,
    /** The top-left vertical radius */
    val topLeftRadiusY: Float,
    /** The top-right horizontal radius */
    val topRightRadiusX: Float,
    /** The top-right vertical radius */
    val topRightRadiusY: Float,
    /** The bottom-right horizontal radius */
    val bottomRightRadiusX: Float,
    /** The bottom-right vertical radius */
    val bottomRightRadiusY: Float,
    /** The bottom-left horizontal radius */
    val bottomLeftRadiusX: Float,
    /** The bottom-left vertical radius */
    val bottomLeftRadiusY: Float
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
        scale = minRadius(scale, bottomLeftRadiusY, topLeftRadiusY, height)
        scale = minRadius(scale, topLeftRadiusX, topRightRadiusX, width)
        scale = minRadius(scale, topRightRadiusY, bottomRightRadiusY, height)
        scale = minRadius(scale, bottomRightRadiusX, bottomLeftRadiusX, width)

        RoundRect(
            left = left * scale,
            top = top * scale,
            right = right * scale,
            bottom = bottom * scale,
            topLeftRadiusX = topLeftRadiusX * scale,
            topLeftRadiusY = topLeftRadiusY * scale,
            topRightRadiusX = topRightRadiusX * scale,
            topRightRadiusY = topRightRadiusY * scale,
            bottomRightRadiusX = bottomRightRadiusX * scale,
            bottomRightRadiusY = bottomRightRadiusY * scale,
            bottomLeftRadiusX = bottomLeftRadiusX * scale,
            bottomLeftRadiusY = bottomLeftRadiusY * scale
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
        if (point.x < left + scaled.topLeftRadiusX &&
            point.y < top + scaled.topLeftRadiusY
        ) {
            x = point.x - left - scaled.topLeftRadiusX
            y = point.y - top - scaled.topLeftRadiusY
            radiusX = scaled.topLeftRadiusX
            radiusY = scaled.topLeftRadiusY
        } else if (point.x > right - scaled.topRightRadiusX &&
            point.y < top + scaled.topRightRadiusY
        ) {
            x = point.x - right + scaled.topRightRadiusX
            y = point.y - top - scaled.topRightRadiusY
            radiusX = scaled.topRightRadiusX
            radiusY = scaled.topRightRadiusY
        } else if (point.x > right - scaled.bottomRightRadiusX &&
            point.y > bottom - scaled.bottomRightRadiusY
        ) {
            x = point.x - right + scaled.bottomRightRadiusX
            y = point.y - bottom + scaled.bottomRightRadiusY
            radiusX = scaled.bottomRightRadiusX
            radiusY = scaled.bottomRightRadiusY
        } else if (point.x < left + scaled.bottomLeftRadiusX &&
            point.y > bottom - scaled.bottomLeftRadiusY
        ) {
            x = point.x - left - scaled.bottomLeftRadiusX
            y = point.y - bottom + scaled.bottomLeftRadiusY
            radiusX = scaled.bottomLeftRadiusX
            radiusY = scaled.bottomLeftRadiusY
        } else {
            return true; // inside and not within the rounded corner area
        }

        val newX = x / radiusX
        val newY = y / radiusY

        // check if the point is inside the unit circle
        return newX * newX + newY * newY <= 1.0f
    }

    override fun toString(): String {
        val tlRadius = topLeftRadius()
        val trRadius = topRightRadius()
        val brRadius = bottomRightRadius()
        val blRadius = bottomLeftRadius()
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
        val Zero = RoundRect(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f)
    }
}

@Deprecated("Use RoundRect constructor instead",
    ReplaceWith("RoundRect(" +
            "left, " +
            "top, " +
            "right, " +
            "bottom, " +
            "topLeftRadiusX, " +
            "topLeftRadiusY, " +
            "topRightRadiusX, " +
            "topRightRadiusY, " +
            "bottomRightRadiusX, " +
            "bottomRightRadiusY, " +
            "bottomLeftRadiusX, " +
            "bottomLeftRadiusY" +
            ")",
            "androidx.compose.ui.geometry"
    )
)
fun RRect(
    /** The offset of the left edge of this rectangle from the x axis */
    left: Float,
    /** The offset of the top edge of this rectangle from the y axis */
    top: Float,
    /** The offset of the right edge of this rectangle from the x axis */
    right: Float,
    /** The offset of the bottom edge of this rectangle from the y axis */
    bottom: Float,
    /** The top-left horizontal radius */
    topLeftRadiusX: Float,
    /** The top-left vertical radius */
    topLeftRadiusY: Float,
    /** The top-right horizontal radius */
    topRightRadiusX: Float,
    /** The top-right vertical radius */
    topRightRadiusY: Float,
    /** The bottom-right horizontal radius */
    bottomRightRadiusX: Float,
    /** The bottom-right vertical radius */
    bottomRightRadiusY: Float,
    /** The bottom-left horizontal radius */
    bottomLeftRadiusX: Float,
    /** The bottom-left vertical radius */
    bottomLeftRadiusY: Float
): RoundRect =
    RoundRect(
        left,
        top,
        right,
        bottom,
        topLeftRadiusX,
        topLeftRadiusY,
        topRightRadiusX,
        topRightRadiusY,
        bottomRightRadiusX,
        bottomRightRadiusY,
        bottomLeftRadiusX,
        bottomLeftRadiusY
    )

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
) = RoundRect(
    left = left,
    top = top,
    right = right,
    bottom = bottom,
    topLeftRadiusX = radiusX,
    topLeftRadiusY = radiusY,
    topRightRadiusX = radiusX,
    topRightRadiusY = radiusY,
    bottomRightRadiusX = radiusX,
    bottomRightRadiusY = radiusY,
    bottomLeftRadiusX = radiusX,
    bottomLeftRadiusY = radiusY
)

/**
 * Construct a rounded rectangle from its left, top, right, and bottom edges,
 * and the same radii along its horizontal axis and its vertical axis.
 */
@Deprecated("Use RoundRect(left, top, right, bottom, radiusX, radiusY) instead",
    ReplaceWith(
        "RoundRect(left, top, right, bottom, radiusX, radiusY)",
        "androidx.compose.ui.geometry"
    ))
fun RRect(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    radiusX: Float,
    radiusY: Float
) = RoundRect(
    left = left,
    top = top,
    right = right,
    bottom = bottom,
    topLeftRadiusX = radiusX,
    topLeftRadiusY = radiusY,
    topRightRadiusX = radiusX,
    topRightRadiusY = radiusY,
    bottomRightRadiusX = radiusX,
    bottomRightRadiusY = radiusY,
    bottomLeftRadiusX = radiusX,
    bottomLeftRadiusY = radiusY
)

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
 * Construct a rounded rectangle from its left, top, right, and bottom edges,
 * and the same radius in each corner.
 */
@Deprecated("Use RoundRect(left, top, right, bottom, radius) instead",
    ReplaceWith(
        "RoundRect(left, top, right, bottom, radius)",
        "androidx.compose.ui.geometry")
)
fun RRect(
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
 * Construct a rounded rectangle from its bounding box and the same radii
 * along its horizontal axis and its vertical axis.
 */
@Deprecated("Use RoundRect(rect, radiusX, radiusY), instead",
    ReplaceWith(
        "RoundRect(rect, radiusX, radiusY)",
        "androidx.compose.ui.geometry")
)
fun RRect(
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
 * Construct a rounded rectangle from its bounding box and a radius that is
 * the same in each corner.
 */
@Deprecated("Use RoundRect(rect, radius) instead",
    ReplaceWith("RoundRect(rect, radius)", "androidx.compose.ui.geometry")
)
fun RRect(
    rect: Rect,
    radius: Radius
): RoundRect = RoundRect(
    rect = rect,
    radiusX = radius.x,
    radiusY = radius.y
)

/**
 * Construct a rounded rectangle from its left, top, right, and bottom edges,
 * and topLeft, topRight, bottomRight, and bottomLeft radii.
 *
 * The corner radii default to [Radius.Zero], i.e. right-angled corners.
 */
fun RoundRect(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    topLeft: Radius = Radius.Zero,
    topRight: Radius = Radius.Zero,
    bottomRight: Radius = Radius.Zero,
    bottomLeft: Radius = Radius.Zero
): RoundRect = RoundRect(
    left = left,
    top = top,
    right = right,
    bottom = bottom,
    topLeftRadiusX = topLeft.x,
    topLeftRadiusY = topLeft.y,
    topRightRadiusX = topRight.x,
    topRightRadiusY = topRight.y,
    bottomRightRadiusX = bottomRight.x,
    bottomRightRadiusY = bottomRight.y,
    bottomLeftRadiusX = bottomLeft.x,
    bottomLeftRadiusY = bottomLeft.y
)

/**
 * Construct a rounded rectangle from its left, top, right, and bottom edges,
 * and topLeft, topRight, bottomRight, and bottomLeft radii.
 *
 * The corner radii default to [Radius.Zero], i.e. right-angled corners.
 */
@Deprecated(
    "Use RoundRect(left, top, right, bottom, topLeft, topRight, bottomRight, bottomLeft)",
    ReplaceWith(
        "RoundRect(left, top, right, bottom, topLeft, topRight, bottomRight, bottomLeft)",
        "androidx.compose.ui.geometry"
    )
)
fun RRect(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    topLeft: Radius = Radius.Zero,
    topRight: Radius = Radius.Zero,
    bottomRight: Radius = Radius.Zero,
    bottomLeft: Radius = Radius.Zero
): RoundRect = RoundRect(
    left = left,
    top = top,
    right = right,
    bottom = bottom,
    topLeftRadiusX = topLeft.x,
    topLeftRadiusY = topLeft.y,
    topRightRadiusX = topRight.x,
    topRightRadiusY = topRight.y,
    bottomRightRadiusX = bottomRight.x,
    bottomRightRadiusY = bottomRight.y,
    bottomLeftRadiusX = bottomLeft.x,
    bottomLeftRadiusY = bottomLeft.y
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
    topLeftRadiusX = topLeft.x,
    topLeftRadiusY = topLeft.y,
    topRightRadiusX = topRight.x,
    topRightRadiusY = topRight.y,
    bottomRightRadiusX = bottomRight.x,
    bottomRightRadiusY = bottomRight.y,
    bottomLeftRadiusX = bottomLeft.x,
    bottomLeftRadiusY = bottomLeft.y
)

/**
 * Construct a rounded rectangle from its bounding box and and topLeft,
 * topRight, bottomRight, and bottomLeft radii.
 *
 * The corner radii default to [Radius.Zero], i.e. right-angled corners
 */
@Deprecated("Use RoundRect(rect, topLeft, topRight, bottomRight, bottomLeft) instead",
    ReplaceWith(
        "RoundRect(rect, topLeft, topRight, bottomRight, bottomLeft)",
        "androidx.compose.ui.geometry"
    )
)
fun RRect(
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
    topLeftRadiusX = topLeft.x,
    topLeftRadiusY = topLeft.y,
    topRightRadiusX = topRight.x,
    topRightRadiusY = topRight.y,
    bottomRightRadiusX = bottomRight.x,
    bottomRightRadiusY = bottomRight.y,
    bottomLeftRadiusX = bottomLeft.x,
    bottomLeftRadiusY = bottomLeft.y
)

/** The top-left [Radius]. */
fun RoundRect.topLeftRadius(): Radius = Radius(topLeftRadiusX, topLeftRadiusY)

/**  The top-right [Radius]. */
fun RoundRect.topRightRadius(): Radius = Radius(topRightRadiusX, topRightRadiusY)

/**  The bottom-right [Radius]. */
fun RoundRect.bottomRightRadius(): Radius = Radius(bottomRightRadiusX, bottomRightRadiusY)

/** The bottom-left [Radius]. */
fun RoundRect.bottomLeftRadius(): Radius = Radius(bottomLeftRadiusX, bottomLeftRadiusY)

/** Returns a new [RoundRect] translated by the given offset. */
fun RoundRect.shift(offset: Offset): RoundRect = RoundRect(
    left = left + offset.x,
    top = top + offset.y,
    right = right + offset.x,
    bottom = bottom + offset.y,
    topLeft = Radius(topLeftRadiusX, topLeftRadiusY),
    topRight = Radius(topRightRadiusX, topRightRadiusY),
    bottomRight = Radius(bottomRightRadiusX, bottomRightRadiusY),
    bottomLeft = Radius(bottomLeftRadiusX, bottomLeftRadiusY)
)

/**
 * Returns a new [RoundRect] with edges and radii moved outwards by the given
 * delta.
 */
fun RoundRect.grow(delta: Float): RoundRect = RoundRect(
    left = left - delta,
    top = top - delta,
    right = right + delta,
    bottom = bottom + delta,
    topLeft = Radius(topLeftRadiusX + delta, topLeftRadiusY + delta),
    topRight = Radius(topRightRadiusX + delta, topRightRadiusY + delta),
    bottomRight = Radius(bottomRightRadiusX + delta, bottomRightRadiusY + delta),
    bottomLeft = Radius(bottomLeftRadiusX + delta, bottomLeftRadiusY + delta)
)

fun RoundRect.withRadius(radius: Radius): RoundRect = RoundRect(
    left = left,
    top = top,
    right = right,
    bottom = bottom,
    topLeft = radius,
    topRight = radius,
    bottomLeft = radius,
    bottomRight = radius
)

/** Returns a new [RoundRect] with edges and radii moved inwards by the given delta. */
fun RoundRect.shrink(delta: Float): RoundRect = grow(-delta)

/** The bounding box of this rounded rectangle (the rectangle with no rounded corners). */
fun RoundRect.outerRect(): Rect = Rect(left, top, right, bottom)

/**
 * The non-rounded rectangle that is constrained by the smaller of the two
 * diagonals, with each diagonal traveling through the middle of the curve
 * corners. The middle of a corner is the intersection of the curve with its
 * respective quadrant bisector.
 */
fun RoundRect.safeInnerRect(): Rect {
    val insetFactor = 0.29289321881f // 1-cos(pi/4)

    val leftRadius = max(bottomLeftRadiusX, topLeftRadiusX)
    val topRadius = max(topLeftRadiusY, topRightRadiusY)
    val rightRadius = max(topRightRadiusX, bottomRightRadiusX)
    val bottomRadius = max(bottomRightRadiusY, bottomLeftRadiusY)

    return Rect(
        left + leftRadius * insetFactor,
        top + topRadius * insetFactor,
        right - rightRadius * insetFactor,
        bottom - bottomRadius * insetFactor
    )
}

/**
 * The rectangle that would be formed using the axis-aligned intersection of
 * the sides of the rectangle, i.e., the rectangle formed from the
 * inner-most centers of the ellipses that form the corners. This is the
 * intersection of the [wideMiddleRect] and the [tallMiddleRect]. If any of
 * the intersections are void, the resulting [Rect] will have negative width
 * or height.
 */
fun RoundRect.middleRect(): Rect {
    val leftRadius = max(bottomLeftRadiusX, topLeftRadiusX)
    val topRadius = max(topLeftRadiusY, topRightRadiusY)
    val rightRadius = max(topRightRadiusX, bottomRightRadiusX)
    val bottomRadius = max(bottomRightRadiusY, bottomLeftRadiusY)
    return Rect(
        left + leftRadius,
        top + topRadius,
        right - rightRadius,
        bottom - bottomRadius
    )
}

/**
 * The biggest rectangle that is entirely inside the rounded rectangle and
 * has the full width of the rounded rectangle. If the rounded rectangle does
 * not have an axis-aligned intersection of its left and right side, the
 * resulting [Rect] will have negative width or height.
 */
fun RoundRect.wideMiddleRect(): Rect {
    val topRadius = max(topLeftRadiusY, topRightRadiusY)
    val bottomRadius = max(bottomRightRadiusY, bottomLeftRadiusY)
    return Rect(
        left,
        top + topRadius,
        right,
        bottom - bottomRadius
    )
}

/**
 * The biggest rectangle that is entirely inside the rounded rectangle and
 * has the full height of the rounded rectangle. If the rounded rectangle
 * does not have an axis-aligned intersection of its top and bottom side, the
 * resulting [Rect] will have negative width or height.
 */
fun RoundRect.tallMiddleRect(): Rect {
    val leftRadius = max(bottomLeftRadiusX, topLeftRadiusX)
    val rightRadius = max(topRightRadiusX, bottomRightRadiusX)
    return Rect(
        left + leftRadius,
        top,
        right - rightRadius,
        bottom
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
val RoundRect.isRect get(): Boolean = (topLeftRadiusX == 0.0f || topLeftRadiusY == 0.0f) &&
        (topRightRadiusX == 0.0f || topRightRadiusY == 0.0f) &&
        (bottomLeftRadiusX == 0.0f || bottomLeftRadiusY == 0.0f) &&
        (bottomRightRadiusX == 0.0f || bottomRightRadiusY == 0.0f)

/** Whether this rounded rectangle has a side with no straight section. */
val RoundRect.isStadium get(): Boolean =
    topLeftRadiusX == topRightRadiusX && topLeftRadiusY == topRightRadiusY &&
            topRightRadiusX == bottomRightRadiusX && topRightRadiusY == bottomRightRadiusY &&
            bottomRightRadiusX == bottomLeftRadiusX && bottomRightRadiusY == bottomLeftRadiusY &&
            (width <= 2.0 * topLeftRadiusX || height <= 2.0 * topLeftRadiusY)

/** Whether this rounded rectangle has no side with a straight section. */
val RoundRect.isEllipse get(): Boolean =
    topLeftRadiusX == topRightRadiusX && topLeftRadiusY == topRightRadiusY &&
            topRightRadiusX == bottomRightRadiusX && topRightRadiusY == bottomRightRadiusY &&
            bottomRightRadiusX == bottomLeftRadiusX && bottomRightRadiusY == bottomLeftRadiusY &&
            width <= 2.0 * topLeftRadiusX &&
            height <= 2.0 * topLeftRadiusY

/** Whether this rounded rectangle would draw as a circle. */
val RoundRect.isCircle get() = width == height && isEllipse

/**
 * The lesser of the magnitudes of the [RoundRect.width] and the [RoundRect.height] of this
 * rounded rectangle.
 */
val RoundRect.shortestSide get(): Float = min(width.absoluteValue, height.absoluteValue)

/**
 * The greater of the magnitudes of the [RoundRect.width] and the [RoundRect.height] of this
 * rounded rectangle.
 */
val RoundRect.longestSide get(): Float = max(width.absoluteValue, height.absoluteValue)

/**
 * The offset to the point halfway between the left and right and the top and
 * bottom edges of this rectangle.
 */
fun RoundRect.center(): Offset = Offset((left + width / 2.0f), (top + height / 2.0f))

/**
 * Returns `true` if the rounded rectangle have the same radii in both the horizontal and vertical
 * direction for all corners.
 */
val RoundRect.isSimple: Boolean
    get() = topLeftRadiusX == topLeftRadiusY &&
            topLeftRadiusX == topRightRadiusX &&
            topLeftRadiusX == topRightRadiusY &&
            topLeftRadiusX == bottomRightRadiusX &&
            topLeftRadiusX == bottomRightRadiusY &&
            topLeftRadiusX == bottomLeftRadiusX &&
            topLeftRadiusX == bottomLeftRadiusY

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
        topLeftRadiusX = lerp(
            start.topLeftRadiusX,
            stop.topLeftRadiusX,
            fraction
        ),
        topLeftRadiusY = lerp(
            start.topLeftRadiusY,
            stop.topLeftRadiusY,
            fraction
        ),
        topRightRadiusX = lerp(
            start.topRightRadiusX,
            stop.topRightRadiusX,
            fraction
        ),
        topRightRadiusY = lerp(
            start.topRightRadiusY,
            stop.topRightRadiusY,
            fraction
        ),
        bottomRightRadiusX = lerp(
            start.bottomRightRadiusX,
            stop.bottomRightRadiusX,
            fraction
        ),
        bottomRightRadiusY = lerp(
            start.bottomRightRadiusY,
            stop.bottomRightRadiusY,
            fraction
        ),
        bottomLeftRadiusX = lerp(
            start.bottomLeftRadiusX,
            stop.bottomLeftRadiusX,
            fraction
        ),
        bottomLeftRadiusY = lerp(
            start.bottomLeftRadiusY,
            stop.bottomLeftRadiusY,
            fraction
        )
    )
