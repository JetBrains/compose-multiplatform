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
 * An interface to calculate the position of a sized box inside an available space. [Alignment] is
 * often used to define the alignment of a layout inside a parent layout.
 *
 * @see AbsoluteAlignment
 * @see BiasAlignment
 * @see BiasAbsoluteAlignment
 */
@Stable
fun interface Alignment {
    /**
     * Calculates the position of a box of size [size] relative to the top left corner of an area
     * of size [space]. The returned offset can be negative or larger than `space - size`,
     * meaning that the box will be positioned partially or completely outside the area.
     */
    fun align(size: IntSize, space: IntSize, layoutDirection: LayoutDirection): IntOffset

    /**
     * An interface to calculate the position of box of a certain width inside an available width.
     * [Alignment.Horizontal] is often used to define the horizontal alignment of a layout inside a
     * parent layout.
     */
    @Stable
    fun interface Horizontal {
        /**
         * Calculates the horizontal position of a box of width [size] relative to the left
         * side of an area of width [space]. The returned offset can be negative or larger than
         * `space - size` meaning that the box will be positioned partially or completely outside
         * the area.
         */
        fun align(size: Int, space: Int, layoutDirection: LayoutDirection): Int
    }

    /**
     * An interface to calculate the position of a box of a certain height inside an available
     * height. [Alignment.Vertical] is often used to define the vertical alignment of a
     * layout inside a parent layout.
     */
    @Stable
    fun interface Vertical {
        /**
         * Calculates the vertical position of a box of height [size] relative to the top edge of
         * an area of height [space]. The returned offset can be negative or larger than
         * `space - size` meaning that the box will be positioned partially or completely outside
         * the area.
         */
        fun align(size: Int, space: Int): Int
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
        space: IntSize,
        layoutDirection: LayoutDirection
    ): IntOffset {
        // Convert to Px first and only round at the end, to avoid rounding twice while calculating
        // the new positions
        val centerX = (space.width - size.width).toFloat() / 2f
        val centerY = (space.height - size.height).toFloat() / 2f
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
        override fun align(size: Int, space: Int, layoutDirection: LayoutDirection): Int {
            // Convert to Px first and only round at the end, to avoid rounding twice while
            // calculating the new positions
            val center = (space - size).toFloat() / 2f
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
        override fun align(size: Int, space: Int): Int {
            // Convert to Px first and only round at the end, to avoid rounding twice while
            // calculating the new positions
            val center = (space - size).toFloat() / 2f
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
data class BiasAbsoluteAlignment(
    private val horizontalBias: Float,
    private val verticalBias: Float
) : Alignment {
    /**
     * Returns the position of a 2D point in a container of a given size, according to this
     * [BiasAbsoluteAlignment]. The position will not be mirrored in Rtl context.
     */
    override fun align(size: IntSize, space: IntSize, layoutDirection: LayoutDirection): IntOffset {
        // Convert to Px first and only round at the end, to avoid rounding twice while calculating
        // the new positions
        val remaining = IntSize(space.width - size.width, space.height - size.height)
        val centerX = remaining.width.toFloat() / 2f
        val centerY = remaining.height.toFloat() / 2f

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
        override fun align(size: Int, space: Int, layoutDirection: LayoutDirection): Int {
            // Convert to Px first and only round at the end, to avoid rounding twice while
            // calculating the new positions
            val center = (space - size).toFloat() / 2f
            return (center * (1 + bias)).roundToInt()
        }
    }
}
