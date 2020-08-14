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

package androidx.compose.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.roundToInt

/**
 * An interface to position a point inside a 2D box. [Alignment] is often used to define
 * the alignment of a box inside a parent container.
 */
@Immutable
interface Alignment {
    // TODO(b/146346559): remove default layout direction when Rtl is supported where this function
    //  gets called
    /**
     * Returns the position of a 2D point in a container of a given size,
     * according to this [Alignment].
     */
    fun align(
        size: IntSize,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr
    ): IntOffset

    /**
     * An interface that positions a point on a 1D vertical finite line. [Alignment.Vertical] is
     * often used to define the vertical alignment of a box inside a parent container.
     */
    @Immutable
    interface Vertical {
        /**
         * Returns the position of a 1D point in a container of a given size, according to this
         * [Alignment].
         */
        fun align(size: Int): Int
    }

    /**
     * An interface that positions a point on a 1D horizontal finite line. [Alignment.Horizontal]
     * is often used to define the horizontal alignment of a box inside a parent container.
     */
    @Immutable
    interface Horizontal {
        /**
         * Returns the position of a 1D point in a container of a given size,
         * according to this [Alignment].
         */
        fun align(size: Int, layoutDirection: LayoutDirection = LayoutDirection.Ltr): Int
    }

    companion object {
        // 2D Alignments.
        @Stable
        val TopStart: Alignment = DirectionalAlignment(-1f, -1f)
        @Stable
        val TopCenter: Alignment = DirectionalAlignment(-1f, 0f)
        @Stable
        val TopEnd: Alignment = DirectionalAlignment(-1f, 1f)
        @Stable
        val CenterStart: Alignment = DirectionalAlignment(0f, -1f)
        @Stable
        val Center: Alignment = DirectionalAlignment(0f, 0f)
        @Stable
        val CenterEnd: Alignment = DirectionalAlignment(0f, 1f)
        @Stable
        val BottomStart: Alignment = DirectionalAlignment(1f, -1f)
        @Stable
        val BottomCenter: Alignment = DirectionalAlignment(1f, 0f)
        @Stable
        val BottomEnd: Alignment = DirectionalAlignment(1f, 1f)

        // 1D Alignment.Verticals.
        @Stable
        val Top: Vertical = DirectionalAlignment.Vertical(-1f)
        @Stable
        val CenterVertically: Vertical = DirectionalAlignment.Vertical(0f)
        @Stable
        val Bottom: Vertical = DirectionalAlignment.Vertical(1f)

        // 1D Alignment.Horizontals.
        @Stable
        val Start: Horizontal = DirectionalAlignment.Horizontal(-1f)
        @Stable
        val CenterHorizontally: Horizontal = DirectionalAlignment.Horizontal(0f)
        @Stable
        val End: Horizontal = DirectionalAlignment.Horizontal(1f)
    }
}

/**
 * Represents a positioning of a point inside a 2D box.
 * The coordinate space of the 2D box is the continuous [-1f, 1f] range in both dimensions,
 * and (verticalBias, horizontalBias) will be points in this space. (verticalBias=0f,
 * horizontalBias=0f) represents the center of the box, (verticalBias=-1f, horizontalBias=1f)
 * will be the top end, etc.
 */
@Immutable
private data class DirectionalAlignment(
    val verticalBias: Float,
    val horizontalBias: Float
) : Alignment {
    override fun align(
        size: IntSize,
        layoutDirection: LayoutDirection
    ): IntOffset {
        // Convert to Px first and only round at the end, to avoid rounding twice while calculating
        // the new positions
        val centerX = size.width.toFloat() / 2f
        val centerY = size.height.toFloat() / 2f
        val resolvedHorizontalBias = if (layoutDirection == LayoutDirection.Ltr) {
            horizontalBias
        } else {
            -1 * horizontalBias
        }

        val x = centerX * (1 + resolvedHorizontalBias)
        val y = centerY * (1 + verticalBias)
        return IntOffset(x.roundToInt(), y.roundToInt())
    }

    @Immutable
    data class Vertical(private val bias: Float) : Alignment.Vertical {
        override fun align(size: Int): Int {
            // Convert to Px first and only round at the end, to avoid rounding twice while
            // calculating the new positions
            val center = size.toFloat() / 2f
            return (center * (1 + bias)).roundToInt()
        }
    }

    @Immutable
    data class Horizontal(private val bias: Float) : Alignment.Horizontal {
        override fun align(size: Int, layoutDirection: LayoutDirection): Int {
            // Convert to Px first and only round at the end, to avoid rounding twice while
            // calculating the new positions
            val center = size.toFloat() / 2f
            val resolvedBias = if (layoutDirection == LayoutDirection.Ltr) bias else -1 * bias
            return (center * (1 + resolvedBias)).roundToInt()
        }
    }
}

/**
 * Represents an absolute positioning of a point inside a 2D box. The position will not be
 * automatically mirrored in Rtl context.
 */
@Immutable
data class AbsoluteAlignment internal constructor(
    private val verticalBias: Float,
    private val horizontalBias: Float
) : Alignment {
    /**
     * Returns the position of a 2D point in a container of a given size, according to this
     * [AbsoluteAlignment]. The position will not be mirrored in Rtl context.
     */
    override fun align(size: IntSize, layoutDirection: LayoutDirection): IntOffset {
        // Convert to Px first and only round at the end, to avoid rounding twice while calculating
        // the new positions
        val centerX = size.width.toFloat() / 2f
        val centerY = size.height.toFloat() / 2f

        val x = centerX * (1 + horizontalBias)
        val y = centerY * (1 + verticalBias)
        return IntOffset(x.roundToInt(), y.roundToInt())
    }

    /**
     * Represents a absolute positioning of a point on a 1D horizontal finite line. The position
     * will not be mirrored in Rtl context.
     */
    @Immutable
    data class Horizontal internal constructor(private val bias: Float) : Alignment.Horizontal {
        /**
         * Returns the position of a 2D point in a container of a given size,
         * according to this [AbsoluteAlignment.Horizontal]. This position will not be mirrored in
         * Rtl context.
         */
        override fun align(size: Int, layoutDirection: LayoutDirection): Int {
            // Convert to Px first and only round at the end, to avoid rounding twice while
            // calculating the new positions
            val center = size.toFloat() / 2f
            return (center * (1 + bias)).roundToInt()
        }
    }

    companion object {
        // 2D AbsoluteAlignments.
        @Stable
        val TopLeft = AbsoluteAlignment(-1f, -1f)
        @Stable
        val TopRight = AbsoluteAlignment(-1f, 1f)
        @Stable
        val CenterLeft = AbsoluteAlignment(0f, -1f)
        @Stable
        val CenterRight = AbsoluteAlignment(0f, 1f)
        @Stable
        val BottomLeft = AbsoluteAlignment(1f, -1f)
        @Stable
        val BottomRight = AbsoluteAlignment(1f, 1f)

        // 1D AbsoluteAlignment.Horizontals.
        @Stable
        val Left: Horizontal = Horizontal(-1f)
        @Stable
        val Right: Horizontal = Horizontal(1f)
    }
}
