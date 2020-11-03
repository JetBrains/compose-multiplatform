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
 *
 * @see AbsoluteAlignment
 * @see BiasAlignment
 * @see BiasAbsoluteAlignment
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

    /**
     * A collection of common [Alignment]s aware of layout direction.
     */
    companion object {
        // 2D Alignments.
        @Stable
        val TopStart: Alignment = BiasAlignment(-1f, -1f)
        @Stable
        val TopCenter: Alignment = BiasAlignment(0f, -1f)
        @Stable
        val TopEnd: Alignment = BiasAlignment(1f, -1f)
        @Stable
        val CenterStart: Alignment = BiasAlignment(-1f, 0f)
        @Stable
        val Center: Alignment = BiasAlignment(0f, 0f)
        @Stable
        val CenterEnd: Alignment = BiasAlignment(1f, 0f)
        @Stable
        val BottomStart: Alignment = BiasAlignment(-1f, 1f)
        @Stable
        val BottomCenter: Alignment = BiasAlignment(0f, 1f)
        @Stable
        val BottomEnd: Alignment = BiasAlignment(1f, 1f)

        // 1D Alignment.Verticals.
        @Stable
        val Top: Vertical = BiasAlignment.Vertical(-1f)
        @Stable
        val CenterVertically: Vertical = BiasAlignment.Vertical(0f)
        @Stable
        val Bottom: Vertical = BiasAlignment.Vertical(1f)

        // 1D Alignment.Horizontals.
        @Stable
        val Start: Horizontal = BiasAlignment.Horizontal(-1f)
        @Stable
        val CenterHorizontally: Horizontal = BiasAlignment.Horizontal(0f)
        @Stable
        val End: Horizontal = BiasAlignment.Horizontal(1f)
    }
}

/**
 * A collection of common [Alignment]s unaware of the layout direction.
 */
object AbsoluteAlignment {
    // 2D AbsoluteAlignments.
    @Stable
    val TopLeft: Alignment = BiasAbsoluteAlignment(-1f, -1f)
    @Stable
    val TopRight: Alignment = BiasAbsoluteAlignment(1f, -1f)
    @Stable
    val CenterLeft: Alignment = BiasAbsoluteAlignment(-1f, 0f)
    @Stable
    val CenterRight: Alignment = BiasAbsoluteAlignment(1f, 0f)
    @Stable
    val BottomLeft: Alignment = BiasAbsoluteAlignment(-1f, 1f)
    @Stable
    val BottomRight: Alignment = BiasAbsoluteAlignment(1f, 1f)

    // 1D BiasAbsoluteAlignment.Horizontals.
    @Stable
    val Left: Alignment.Horizontal = BiasAbsoluteAlignment.Horizontal(-1f)
    @Stable
    val Right: Alignment.Horizontal = BiasAbsoluteAlignment.Horizontal(1f)
}

/**
 * An [Alignment] specified by bias: for example, a bias of -1 represents alignment to the
 * start/top, a bias of 0 will represent centering, and a bias of 1 will represent end/bottom.
 * Any value can be specified to obtain an alignment. Inside the [-1, 1] range, the obtained
 * alignment will position the aligned size fully inside the available space, while outside the
 * range it will the aligned size will be positioned partially or completely outside.
 *
 * @see BiasAbsoluteAlignment
 * @see Alignment
 */
@Immutable
data class BiasAlignment(
    val horizontalBias: Float,
    val verticalBias: Float
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

    /**
     * An [Alignment.Horizontal] specified by bias: for example, a bias of -1 represents alignment
     * to the start, a bias of 0 will represent centering, and a bias of 1 will represent end.
     * Any value can be specified to obtain an alignment. Inside the [-1, 1] range, the obtained
     * alignment will position the aligned size fully inside the available space, while outside the
     * range it will the aligned size will be positioned partially or completely outside.
     *
     * @see BiasAbsoluteAlignment.Horizontal
     * @see Vertical
     */
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

    /**
     * An [Alignment.Vertical] specified by bias: for example, a bias of -1 represents alignment
     * to the top, a bias of 0 will represent centering, and a bias of 1 will represent bottom.
     * Any value can be specified to obtain an alignment. Inside the [-1, 1] range, the obtained
     * alignment will position the aligned size fully inside the available space, while outside the
     * range it will the aligned size will be positioned partially or completely outside.
     *
     * @see Horizontal
     */
    @Immutable
    data class Vertical(private val bias: Float) : Alignment.Vertical {
        override fun align(size: Int): Int {
            // Convert to Px first and only round at the end, to avoid rounding twice while
            // calculating the new positions
            val center = size.toFloat() / 2f
            return (center * (1 + bias)).roundToInt()
        }
    }
}

/**
 * An [Alignment] specified by bias: for example, a bias of -1 represents alignment to the
 * left/top, a bias of 0 will represent centering, and a bias of 1 will represent right/bottom.
 * Any value can be specified to obtain an alignment. Inside the [-1, 1] range, the obtained
 * alignment will position the aligned size fully inside the available space, while outside the
 * range it will the aligned size will be positioned partially or completely outside.
 *
 * @see AbsoluteAlignment
 * @see Alignment
 */
@Immutable
data class BiasAbsoluteAlignment internal constructor(
    private val horizontalBias: Float,
    private val verticalBias: Float
) : Alignment {
    /**
     * Returns the position of a 2D point in a container of a given size, according to this
     * [BiasAbsoluteAlignment]. The position will not be mirrored in Rtl context.
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
     * An [Alignment.Horizontal] specified by bias: for example, a bias of -1 represents alignment
     * to the left, a bias of 0 will represent centering, and a bias of 1 will represent right.
     * Any value can be specified to obtain an alignment. Inside the [-1, 1] range, the obtained
     * alignment will position the aligned size fully inside the available space, while outside the
     * range it will the aligned size will be positioned partially or completely outside.
     *
     * @see BiasAlignment.Horizontal
     */
    @Immutable
    data class Horizontal(private val bias: Float) : Alignment.Horizontal {
        /**
         * Returns the position of a 2D point in a container of a given size,
         * according to this [BiasAbsoluteAlignment.Horizontal]. This position will not be
         * mirrored in Rtl context.
         */
        override fun align(size: Int, layoutDirection: LayoutDirection): Int {
            // Convert to Px first and only round at the end, to avoid rounding twice while
            // calculating the new positions
            val center = size.toFloat() / 2f
            return (center * (1 + bias)).roundToInt()
        }
    }
}
