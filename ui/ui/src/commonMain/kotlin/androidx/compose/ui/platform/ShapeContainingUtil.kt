/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.platform

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation

/**
 * Returns `true` if ([x], [y]) is within [outline]. For some outlines that don't require a [Path],
 * the exact point is used to calculate whether the point is inside [outline]. When a [Path] is
 * required, a 0.01 x 0.01 box around ([x], [y]) is used to intersect with the path to determine
 * the result.
 *
 * The [tmpTouchPointPath] and [tmpOpPath] are temporary Paths that are cleared after use and will
 * be used in the calculation of the intersection. These must be empty when passed as parameters or
 * can be `null` to allocate locally.
 */
internal fun isInOutline(
    outline: Outline,
    x: Float,
    y: Float,
    tmpTouchPointPath: Path? = null,
    tmpOpPath: Path? = null
): Boolean = when (outline) {
    is Outline.Rectangle -> isInRectangle(outline.rect, x, y)
    is Outline.Rounded -> isInRoundedRect(outline, x, y, tmpTouchPointPath, tmpOpPath)
    is Outline.Generic -> isInPath(outline.path, x, y, tmpTouchPointPath, tmpOpPath)
}

private fun isInRectangle(rect: Rect, x: Float, y: Float) =
    rect.left <= x && x < rect.right && rect.top <= y && y < rect.bottom

/**
 * Returns `true` if ([x], [y]) is within [outline].
 */
private fun isInRoundedRect(
    outline: Outline.Rounded,
    x: Float,
    y: Float,
    touchPointPath: Path?,
    opPath: Path?
): Boolean {
    val rrect = outline.roundRect

    // first, everything that is outside the rect
    if (x < rrect.left || x >= rrect.right || y < rrect.top || y >= rrect.bottom) {
        return false
    }

    // This algorithm assumes that the corner radius isn't greater than the size of the Rect.
    // There's a complex algorithm to handle cases beyond that, so we'll fall back to
    // the Path algorithm to handle it
    if (!rrect.cornersFit()) {
        val path = opPath ?: Path()
        path.addRoundRect(rrect)
        return isInPath(path, x, y, touchPointPath, opPath)
    }

    val topLeftX = rrect.left + rrect.topLeftCornerRadius.x
    val topLeftY = rrect.top + rrect.topLeftCornerRadius.y

    val topRightX = rrect.right - rrect.topRightCornerRadius.x
    val topRightY = rrect.top + rrect.topRightCornerRadius.y

    val bottomRightX = rrect.right - rrect.bottomRightCornerRadius.x
    val bottomRightY = rrect.bottom - rrect.bottomRightCornerRadius.y

    val bottomLeftX = rrect.bottom - rrect.bottomLeftCornerRadius.y
    val bottomLeftY = rrect.left + rrect.bottomLeftCornerRadius.x

    return if (x < topLeftX && y < topLeftY) {
        // top-left corner
        isWithinEllipse(x, y, rrect.topLeftCornerRadius, topLeftX, topLeftY)
    } else if (x < bottomLeftY && y > bottomLeftX) {
        // bottom-left corner
        isWithinEllipse(x, y, rrect.bottomLeftCornerRadius, bottomLeftY, bottomLeftX)
    } else if (x > topRightX && y < topRightY) {
        // top-right corner
        isWithinEllipse(x, y, rrect.topRightCornerRadius, topRightX, topRightY)
    } else if (x > bottomRightX && y > bottomRightY) {
        // bottom-right corner
        isWithinEllipse(x, y, rrect.bottomRightCornerRadius, bottomRightX, bottomRightY)
    } else {
        true // not at a corner, so it must be inside
    }
}

/**
 * Returns `true` if the rounded rectangle has rounded corners that fit within the sides or
 * `false` if the rounded sides add up to a greater size that a side.
 */
private fun RoundRect.cornersFit() = topLeftCornerRadius.x + topRightCornerRadius.x <= width &&
    bottomLeftCornerRadius.x + bottomRightCornerRadius.x <= width &&
    topLeftCornerRadius.y + bottomLeftCornerRadius.y <= height &&
    topRightCornerRadius.y + bottomRightCornerRadius.y <= height

/**
 * Used to determine whether a point is within a rounded corner, this returns `true` if the point
 * ([x], [y]) is within the ellipse centered at ([centerX], [centerY]) with the horizontal and
 * vertical radii given by [cornerRadius].
 */
private fun isWithinEllipse(
    x: Float,
    y: Float,
    cornerRadius: CornerRadius,
    centerX: Float,
    centerY: Float
): Boolean {
    val px = x - centerX
    val py = y - centerY
    val radiusX = cornerRadius.x
    val radiusY = cornerRadius.y
    return (px * px) / (radiusX * radiusX) + (py * py) / (radiusY * radiusY) <= 1f
}

/**
 * Returns `true` if the 0.01 x 0.01 box around ([x], [y]) has any point with [path].
 *
 * The [tmpTouchPointPath] and [tmpOpPath] are temporary Paths that are cleared after use and will
 * be used in the calculation of the intersection. These must be empty when passed as parameters or
 * can be `null` to allocate locally.
 */
private fun isInPath(
    path: Path,
    x: Float,
    y: Float,
    tmpTouchPointPath: Path?,
    tmpOpPath: Path?
): Boolean {
    val rect = Rect(x - 0.005f, y - 0.005f, x + 0.005f, y + 0.005f)
    val touchPointPath = tmpTouchPointPath ?: Path()
    touchPointPath.addRect(
        rect
    )

    val opPath = tmpOpPath ?: Path()
    opPath.op(path, touchPointPath, PathOperation.Intersect)

    val isClipped = opPath.isEmpty
    opPath.reset()
    touchPointPath.reset()
    return !isClipped
}