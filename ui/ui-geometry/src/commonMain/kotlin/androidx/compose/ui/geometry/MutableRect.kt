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

package androidx.compose.ui.geometry

import androidx.compose.runtime.Stable
import kotlin.math.max
import kotlin.math.min

/**
 * An mutable, 2D, axis-aligned, floating-point rectangle whose coordinates
 * are relative to a given origin.
 *
 * @param left The offset of the left edge of this rectangle from the x axis.
 * @param top The offset of the top edge of this rectangle from the y axis.
 * @param right The offset of the right edge of this rectangle from the x axis.
 * @param bottom The offset of the bottom edge of this rectangle from the y axis.
 */
class MutableRect(
    var left: Float,
    var top: Float,
    var right: Float,
    var bottom: Float
) {
    /** The distance between the left and right edges of this rectangle. */
    inline val width: Float
        get() = right - left

    /** The distance between the top and bottom edges of this rectangle. */
    inline val height: Float
        get() = bottom - top

    /**
     * The distance between the upper-left corner and the lower-right corner of
     * this rectangle.
     */
    val size: Size
        get() = Size(width, height)

    /**
     * Whether this rectangle encloses a non-zero area. Negative areas are
     * considered empty.
     */
    val isEmpty: Boolean
        get() = left >= right || top >= bottom

    /**
     * Modifies `this` to be the intersection of this and the rect formed
     * by [left], [top], [right], and [bottom].
     */
    @Stable
    fun intersect(left: Float, top: Float, right: Float, bottom: Float) {
        this.left = max(left, this.left)
        this.top = max(top, this.top)
        this.right = min(right, this.right)
        this.bottom = min(bottom, this.bottom)
    }

    /**
     * Whether the point specified by the given offset (which is assumed to be
     * relative to the origin) lies between the left and right and the top and
     * bottom edges of this rectangle.
     *
     * Rectangles include their top and left edges but exclude their bottom and
     * right edges.
     */
    operator fun contains(offset: Offset): Boolean {
        return offset.x >= left && offset.x < right && offset.y >= top && offset.y < bottom
    }

    /**
     * Sets new bounds to ([left], [top], [right], [bottom])
     */
    fun set(left: Float, top: Float, right: Float, bottom: Float) {
        this.left = left
        this.top = top
        this.right = right
        this.bottom = bottom
    }

    override fun toString() = "MutableRect(" +
        "${left.toStringAsFixed(1)}, " +
        "${top.toStringAsFixed(1)}, " +
        "${right.toStringAsFixed(1)}, " +
        "${bottom.toStringAsFixed(1)})"
}

fun MutableRect.toRect(): Rect = Rect(left, top, right, bottom)