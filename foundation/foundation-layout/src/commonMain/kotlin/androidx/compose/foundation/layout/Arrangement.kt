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

package androidx.compose.foundation.layout

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Used to specify the arrangement of the layout's children in layouts like [Row] or [Column] in
 * the main axis direction (horizontal and vertical, respectively).
 */
@Immutable
@OptIn(InternalLayoutApi::class)
object Arrangement {
    /**
     * Used to specify the horizontal arrangement of the layout's children in layouts like [Row].
     */
    @InternalLayoutApi
    @Immutable
    interface Horizontal {
        /**
         * Spacing that should be added between any two adjacent layout children.
         */
        val spacing get() = 0.dp

        /**
         * Horizontally places the layout children.
         *
         * @param totalSize Available space that can be occupied by the children.
         * @param size An array of sizes of all children.
         * @param layoutDirection A layout direction, left-to-right or right-to-left, of the parent
         * layout that should be taken into account when determining positions of the children.
         * @param density The current density.
         * @param outPosition An array of the size of [size] that returns the calculated positions.
         */
        fun arrange(
            totalSize: Int,
            size: IntArray,
            layoutDirection: LayoutDirection,
            density: Density,
            outPosition: IntArray
        )
    }

    /**
     * Used to specify the vertical arrangement of the layout's children in layouts like [Column].
     */
    @InternalLayoutApi
    @Immutable
    interface Vertical {
        /**
         * Spacing that should be added between any two adjacent layout children.
         */
        val spacing get() = 0.dp

        /**
         * Vertically places the layout children.
         *
         * @param totalSize Available space that can be occupied by the children.
         * @param size An array of sizes of all children.
         * @param density The current density.
         * @param outPosition An array of the size of [size] that returns the calculated positions.
         */
        fun arrange(
            totalSize: Int,
            size: IntArray,
            density: Density,
            outPosition: IntArray
        )
    }

    /**
     * Used to specify the horizontal arrangement of the layout's children in horizontal layouts
     * like [Row], or the vertical arrangement of the layout's children in vertical layouts like
     * [Column].
     */
    @InternalLayoutApi
    @Immutable
    interface HorizontalOrVertical : Horizontal, Vertical {
        /**
         * Spacing that should be added between any two adjacent layout children.
         */
        override val spacing: Dp get() = 0.dp
    }

    /**
     * Place children horizontally such that they are as close as possible to the beginning of the
     * main axis.
     */
    @Stable
    val Start = object : Horizontal {
        override fun arrange(
            totalSize: Int,
            size: IntArray,
            layoutDirection: LayoutDirection,
            density: Density,
            outPosition: IntArray
        ) = if (layoutDirection == LayoutDirection.Ltr) {
            placeLeftOrTop(size, outPosition)
        } else {
            size.reverse()
            placeRightOrBottom(totalSize, size, outPosition)
            outPosition.reverse()
        }
    }

    /**
     * Place children horizontally such that they are as close as possible to the end of the main
     * axis.
     */
    @Stable
    val End = object : Horizontal {
        override fun arrange(
            totalSize: Int,
            size: IntArray,
            layoutDirection: LayoutDirection,
            density: Density,
            outPosition: IntArray
        ) = if (layoutDirection == LayoutDirection.Ltr) {
            placeRightOrBottom(totalSize, size, outPosition)
        } else {
            size.reverse()
            placeLeftOrTop(size, outPosition)
            outPosition.reverse()
        }
    }

    /**
     * Place children vertically such that they are as close as possible to the top of the main
     * axis.
     */
    @Stable
    val Top = object : Vertical {
        override fun arrange(
            totalSize: Int,
            size: IntArray,
            density: Density,
            outPosition: IntArray
        ) = placeLeftOrTop(size, outPosition)
    }

    /**
     * Place children vertically such that they are as close as possible to the bottom of the main
     * axis.
     */
    @Stable
    val Bottom = object : Vertical {
        override fun arrange(
            totalSize: Int,
            size: IntArray,
            density: Density,
            outPosition: IntArray
        ) = placeRightOrBottom(totalSize, size, outPosition)
    }

    /**
     * Place children such that they are as close as possible to the middle of the main axis.
     */
    @Stable
    val Center = object : HorizontalOrVertical {
        override val spacing = 0.dp

        override fun arrange(
            totalSize: Int,
            size: IntArray,
            layoutDirection: LayoutDirection,
            density: Density,
            outPosition: IntArray
        ) = if (layoutDirection == LayoutDirection.Ltr) {
            placeCenter(totalSize, size, outPosition)
        } else {
            size.reverse()
            placeCenter(totalSize, size, outPosition)
            outPosition.reverse()
        }

        override fun arrange(
            totalSize: Int,
            size: IntArray,
            density: Density,
            outPosition: IntArray
        ) = placeCenter(totalSize, size, outPosition)
    }

    /**
     * Place children such that they are spaced evenly across the main axis, including free
     * space before the first child and after the last child.
     */
    @Stable
    val SpaceEvenly = object : HorizontalOrVertical {
        override val spacing = 0.dp

        override fun arrange(
            totalSize: Int,
            size: IntArray,
            layoutDirection: LayoutDirection,
            density: Density,
            outPosition: IntArray
        ) = if (layoutDirection == LayoutDirection.Ltr) {
            placeSpaceEvenly(totalSize, size, outPosition)
        } else {
            size.reverse()
            placeSpaceEvenly(totalSize, size, outPosition)
            outPosition.reverse()
        }

        override fun arrange(
            totalSize: Int,
            size: IntArray,
            density: Density,
            outPosition: IntArray
        ) = placeSpaceEvenly(totalSize, size, outPosition)
    }

    /**
     * Place children such that they are spaced evenly across the main axis, without free
     * space before the first child or after the last child.
     */
    @Stable
    val SpaceBetween = object : HorizontalOrVertical {
        override val spacing = 0.dp

        override fun arrange(
            totalSize: Int,
            size: IntArray,
            layoutDirection: LayoutDirection,
            density: Density,
            outPosition: IntArray
        ) = if (layoutDirection == LayoutDirection.Ltr) {
            placeSpaceBetween(totalSize, size, outPosition)
        } else {
            size.reverse()
            placeSpaceBetween(totalSize, size, outPosition)
            outPosition.reverse()
        }

        override fun arrange(
            totalSize: Int,
            size: IntArray,
            density: Density,
            outPosition: IntArray
        ) = placeSpaceBetween(totalSize, size, outPosition)
    }

    /**
     * Place children such that they are spaced evenly across the main axis, including free
     * space before the first child and after the last child, but half the amount of space
     * existing otherwise between two consecutive children.
     */
    @Stable
    val SpaceAround = object : HorizontalOrVertical {
        override val spacing = 0.dp

        override fun arrange(
            totalSize: Int,
            size: IntArray,
            layoutDirection: LayoutDirection,
            density: Density,
            outPosition: IntArray
        ) = if (layoutDirection == LayoutDirection.Ltr) {
            placeSpaceAround(totalSize, size, outPosition)
        } else {
            size.reverse()
            placeSpaceAround(totalSize, size, outPosition)
            outPosition.reverse()
        }

        override fun arrange(
            totalSize: Int,
            size: IntArray,
            density: Density,
            outPosition: IntArray
        ) = placeSpaceAround(totalSize, size, outPosition)
    }

    /**
     * Place children such that each two adjacent ones are spaced by a fixed [space] distance across
     * the main axis. The spacing will be subtracted from the available space that the children
     * can occupy.
     *
     * @param space The space between adjacent children.
     */
    @Stable
    fun spacedBy(space: Dp): HorizontalOrVertical =
        SpacedAligned(space, true, null)

    /**
     * Place children horizontally such that each two adjacent ones are spaced by a fixed [space]
     * distance. The spacing will be subtracted from the available width that the children
     * can occupy. An [alignment] can be specified to align the spaced children horizontally
     * inside the parent, in case there is empty width remaining.
     *
     * @param space The space between adjacent children.
     * @param alignment The alignment of the spaced children inside the parent.
     */
    @Stable
    fun spacedBy(space: Dp, alignment: Alignment.Horizontal): Horizontal =
        SpacedAligned(space, true) { size, layoutDirection ->
            alignment.align(0, size, layoutDirection)
        }

    /**
     * Place children vertically such that each two adjacent ones are spaced by a fixed [space]
     * distance. The spacing will be subtracted from the available height that the children
     * can occupy. An [alignment] can be specified to align the spaced children vertically
     * inside the parent, in case there is empty height remaining.
     *
     * @param space The space between adjacent children.
     * @param alignment The alignment of the spaced children inside the parent.
     */
    @Stable
    fun spacedBy(space: Dp, alignment: Alignment.Vertical): Vertical =
        SpacedAligned(space, false) { size, _ -> alignment.align(0, size) }

    /**
     * Place children horizontally one next to the other and align the obtained group
     * according to an [alignment].
     *
     * @param alignment The alignment of the children inside the parent.
     */
    @Stable
    fun aligned(alignment: Alignment.Horizontal): Horizontal =
        SpacedAligned(0.dp, true) { size, layoutDirection ->
            alignment.align(0, size, layoutDirection)
        }

    /**
     * Place children vertically one next to the other and align the obtained group
     * according to an [alignment].
     *
     * @param alignment The alignment of the children inside the parent.
     */
    @Stable
    fun aligned(alignment: Alignment.Vertical): Vertical =
        SpacedAligned(0.dp, false) { size, _ -> alignment.align(0, size) }

    @Immutable
    @OptIn(InternalLayoutApi::class)
    object Absolute {
        /**
         * Place children horizontally such that they are as close as possible to the left edge of
         * the [Row].
         *
         * Unlike [Arrangement.Start], when the layout direction is RTL, the children will not be
         * mirrored and as such children will appear in the order they are composed inside the [Row].
         */
        @Stable
        val Left = object : Horizontal {
            override fun arrange(
                totalSize: Int,
                size: IntArray,
                layoutDirection: LayoutDirection,
                density: Density,
                outPosition: IntArray
            ) = placeLeftOrTop(size, outPosition)
        }

        /**
         * Place children such that they are as close as possible to the middle of the [Row].
         *
         * Unlike [Arrangement.Center], when the layout direction is RTL, the children will not be
         * mirrored and as such children will appear in the order they are composed inside the [Row].
         */
        @Stable
        val Center = object : Horizontal {
            override fun arrange(
                totalSize: Int,
                size: IntArray,
                layoutDirection: LayoutDirection,
                density: Density,
                outPosition: IntArray
            ) = placeCenter(totalSize, size, outPosition)
        }

        /**
         * Place children horizontally such that they are as close as possible to the right edge of
         * the [Row].
         *
         * Unlike [Arrangement.End], when the layout direction is RTL, the children will not be
         * mirrored and as such children will appear in the order they are composed inside the [Row].
         */
        @Stable
        val Right = object : Horizontal {
            override fun arrange(
                totalSize: Int,
                size: IntArray,
                layoutDirection: LayoutDirection,
                density: Density,
                outPosition: IntArray
            ) = placeRightOrBottom(totalSize, size, outPosition)
        }

        /**
         * Place children such that they are spaced evenly across the main axis, without free
         * space before the first child or after the last child.
         *
         * Unlike [Arrangement.SpaceBetween], when the layout direction is RTL, the children will not be
         * mirrored and as such children will appear in the order they are composed inside the [Row].
         */
        @Stable
        val SpaceBetween = object : Horizontal {
            override fun arrange(
                totalSize: Int,
                size: IntArray,
                layoutDirection: LayoutDirection,
                density: Density,
                outPosition: IntArray
            ) = placeSpaceBetween(totalSize, size, outPosition)
        }

        /**
         * Place children such that they are spaced evenly across the main axis, including free
         * space before the first child and after the last child.
         *
         * Unlike [Arrangement.SpaceEvenly], when the layout direction is RTL, the children will not be
         * mirrored and as such children will appear in the order they are composed inside the [Row].
         */
        @Stable
        val SpaceEvenly = object : Horizontal {
            override fun arrange(
                totalSize: Int,
                size: IntArray,
                layoutDirection: LayoutDirection,
                density: Density,
                outPosition: IntArray
            ) = placeSpaceEvenly(totalSize, size, outPosition)
        }

        /**
         * Place children such that they are spaced evenly horizontally, including free
         * space before the first child and after the last child, but half the amount of space
         * existing otherwise between two consecutive children.
         *
         * Unlike [Arrangement.SpaceAround], when the layout direction is RTL, the children will not be
         * mirrored and as such children will appear in the order they are composed inside the [Row].
         */
        @Stable
        val SpaceAround = object : Horizontal {
            override fun arrange(
                totalSize: Int,
                size: IntArray,
                layoutDirection: LayoutDirection,
                density: Density,
                outPosition: IntArray
            ) = placeSpaceAround(totalSize, size, outPosition)
        }

        /**
         * Place children such that each two adjacent ones are spaced by a fixed [space] distance across
         * the main axis. The spacing will be subtracted from the available space that the children
         * can occupy.
         *
         * Unlike [Arrangement.spacedBy], when the layout direction is RTL, the children will not be
         * mirrored and as such children will appear in the order they are composed inside the [Row].
         *
         * @param space The space between adjacent children.
         */
        @Stable
        fun spacedBy(space: Dp): HorizontalOrVertical =
            SpacedAligned(space, false, null)

        /**
         * Place children horizontally such that each two adjacent ones are spaced by a fixed [space]
         * distance. The spacing will be subtracted from the available width that the children
         * can occupy. An [alignment] can be specified to align the spaced children horizontally
         * inside the parent, in case there is empty width remaining.
         *
         * Unlike [Arrangement.spacedBy], when the layout direction is RTL, the children will not be
         * mirrored and as such children will appear in the order they are composed inside the [Row].
         *
         * @param space The space between adjacent children.
         * @param alignment The alignment of the spaced children inside the parent.
         */
        @Stable
        fun spacedBy(space: Dp, alignment: Alignment.Horizontal): Horizontal =
            SpacedAligned(space, false) { size, layoutDirection ->
                alignment.align(0, size, layoutDirection)
            }

        /**
         * Place children vertically such that each two adjacent ones are spaced by a fixed [space]
         * distance. The spacing will be subtracted from the available height that the children
         * can occupy. An [alignment] can be specified to align the spaced children vertically
         * inside the parent, in case there is empty height remaining.
         *
         * Unlike [Arrangement.spacedBy], when the layout direction is RTL, the children will not be
         * mirrored and as such children will appear in the order they are composed inside the [Row].
         *
         * @param space The space between adjacent children.
         * @param alignment The alignment of the spaced children inside the parent.
         */
        @Stable
        fun spacedBy(space: Dp, alignment: Alignment.Vertical): Vertical =
            SpacedAligned(space, false) { size, _ -> alignment.align(0, size) }

        /**
         * Place children horizontally one next to the other and align the obtained group
         * according to an [alignment].
         *
         * Unlike [Arrangement.aligned], when the layout direction is RTL, the children will not be
         * mirrored and as such children will appear in the order they are composed inside the [Row].
         *
         * @param alignment The alignment of the children inside the parent.
         */
        @Stable
        fun aligned(alignment: Alignment.Horizontal): Horizontal =
            SpacedAligned(0.dp, false) { size, layoutDirection ->
                alignment.align(0, size, layoutDirection)
            }
    }

    /**
     * Arrangement with spacing between adjacent children and alignment for the spaced group.
     * Should not be instantiated directly, use [spacedBy] instead.
     */
    @Immutable
    internal data class SpacedAligned(
        val space: Dp,
        val rtlMirror: Boolean,
        val alignment: ((Int, LayoutDirection) -> Int)?
    ) : HorizontalOrVertical {

        override val spacing = space

        override fun arrange(
            totalSize: Int,
            size: IntArray,
            layoutDirection: LayoutDirection,
            density: Density,
            outPosition: IntArray
        ) {
            if (size.isEmpty()) return
            val spacePx = with(density) { space.toIntPx() }

            var occupied = 0
            var lastSpace = 0
            if (layoutDirection == LayoutDirection.Rtl && rtlMirror) size.reverse()
            size.forEachIndexed { index, it ->
                outPosition[index] = min(occupied, totalSize - it)
                lastSpace = min(spacePx, totalSize - outPosition[index] - it)
                occupied = outPosition[index] + it + lastSpace
            }
            occupied -= lastSpace

            if (alignment != null && occupied < totalSize) {
                val groupPosition = alignment.invoke(totalSize - occupied, layoutDirection)
                for (index in outPosition.indices) {
                    outPosition[index] += groupPosition
                }
            }

            if (layoutDirection == LayoutDirection.Rtl && rtlMirror) outPosition.reverse()
        }
        override fun arrange(
            totalSize: Int,
            size: IntArray,
            density: Density,
            outPosition: IntArray
        ) = arrange(totalSize, size, LayoutDirection.Ltr, density, outPosition)
    }

    internal fun placeRightOrBottom(
        totalSize: Int,
        size: IntArray,
        outPosition: IntArray
    ) {
        val consumedSize = size.fold(0) { a, b -> a + b }
        var current = totalSize - consumedSize
        size.forEachIndexed { index, it ->
            outPosition[index] = current
            current += it
        }
    }

    internal fun placeLeftOrTop(size: IntArray, outPosition: IntArray) {
        var current = 0
        size.forEachIndexed { index, it ->
            outPosition[index] = current
            current += it
        }
    }

    internal fun placeCenter(totalSize: Int, size: IntArray, outPosition: IntArray) {
        val consumedSize = size.fold(0) { a, b -> a + b }
        var current = (totalSize - consumedSize).toFloat() / 2
        size.forEachIndexed { index, it ->
            outPosition[index] = current.roundToInt()
            current += it.toFloat()
        }
    }

    internal fun placeSpaceEvenly(totalSize: Int, size: IntArray, outPosition: IntArray) {
        val consumedSize = size.fold(0) { a, b -> a + b }
        val gapSize = (totalSize - consumedSize).toFloat() / (size.size + 1)
        var current = gapSize
        size.forEachIndexed { index, it ->
            outPosition[index] = current.roundToInt()
            current += it.toFloat() + gapSize
        }
    }

    internal fun placeSpaceBetween(totalSize: Int, size: IntArray, outPosition: IntArray) {
        val consumedSize = size.fold(0) { a, b -> a + b }
        val gapSize = if (size.size > 1) {
            (totalSize - consumedSize).toFloat() / (size.size - 1)
        } else {
            0f
        }
        var current = 0f
        size.forEachIndexed { index, it ->
            outPosition[index] = current.roundToInt()
            current += it.toFloat() + gapSize
        }
    }
    internal fun placeSpaceAround(totalSize: Int, size: IntArray, outPosition: IntArray) {
        val consumedSize = size.fold(0) { a, b -> a + b }
        val gapSize = if (size.isNotEmpty()) {
            (totalSize - consumedSize).toFloat() / size.size
        } else {
            0f
        }
        var current = gapSize / 2
        size.forEachIndexed { index, it ->
            outPosition[index] = current.roundToInt()
            current += it.toFloat() + gapSize
        }
    }
}

@Immutable
@OptIn(InternalLayoutApi::class)
@Deprecated(
    "AbsoluteArrangement was deprecated in favor of Arrangement.Absolute",
    ReplaceWith("Arrangement.Absolute", "androidx.compose.foundation.layout.Arrangement")
)
object AbsoluteArrangement {
    /**
     * Place children horizontally such that they are as close as possible to the left edge of
     * the [Row].
     *
     * Unlike [Arrangement.Start], when the layout direction is RTL, the children will not be
     * mirrored and as such children will appear in the order they are composed inside the [Row].
     */
    @Stable
    @Deprecated(
        "AbsoluteArrangement was deprecated in favor of Arrangement.Absolute",
        ReplaceWith("Arrangement.Absolute.Left", "androidx.compose.foundation.layout.Arrangement")
    )
    val Left = Arrangement.Absolute.Left

    /**
     * Place children such that they are as close as possible to the middle of the [Row].
     *
     * Unlike [Arrangement.Center], when the layout direction is RTL, the children will not be
     * mirrored and as such children will appear in the order they are composed inside the [Row].
     */
    @Stable
    @Deprecated(
        "AbsoluteArrangement was deprecated in favor of Arrangement.Absolute",
        ReplaceWith("Arrangement.Center.Left", "androidx.compose.foundation.layout.Arrangement")
    )
    val Center = Arrangement.Absolute.Center

    /**
     * Place children horizontally such that they are as close as possible to the right edge of
     * the [Row].
     *
     * Unlike [Arrangement.End], when the layout direction is RTL, the children will not be
     * mirrored and as such children will appear in the order they are composed inside the [Row].
     */
    @Stable
    @Deprecated(
        "AbsoluteArrangement was deprecated in favor of Arrangement.Absolute",
        ReplaceWith("Arrangement.Absolute.Right", "androidx.compose.foundation.layout.Arrangement")
    )
    val Right = Arrangement.Absolute.Right

    /**
     * Place children such that they are spaced evenly across the main axis, without free
     * space before the first child or after the last child.
     *
     * Unlike [Arrangement.SpaceBetween], when the layout direction is RTL, the children will not be
     * mirrored and as such children will appear in the order they are composed inside the [Row].
     */
    @Stable
    @Deprecated(
        "AbsoluteArrangement was deprecated in favor of Arrangement.Absolute",
        ReplaceWith(
            "Arrangement.Absolute.SpaceBetween",
            "androidx.compose.foundation.layout.Arrangement"
        )
    )
    val SpaceBetween = Arrangement.Absolute.SpaceBetween

    /**
     * Place children such that they are spaced evenly across the main axis, including free
     * space before the first child and after the last child.
     *
     * Unlike [Arrangement.SpaceEvenly], when the layout direction is RTL, the children will not be
     * mirrored and as such children will appear in the order they are composed inside the [Row].
     */
    @Stable
    @Deprecated(
        "AbsoluteArrangement was deprecated in favor of Arrangement.Absolute",
        ReplaceWith(
            "Arrangement.Absolute.SpaceEvenly",
            "androidx.compose.foundation.layout.Arrangement"
        )
    )
    val SpaceEvenly = Arrangement.Absolute.SpaceEvenly

    /**
     * Place children such that they are spaced evenly horizontally, including free
     * space before the first child and after the last child, but half the amount of space
     * existing otherwise between two consecutive children.
     *
     * Unlike [Arrangement.SpaceAround], when the layout direction is RTL, the children will not be
     * mirrored and as such children will appear in the order they are composed inside the [Row].
     */
    @Stable
    @Deprecated(
        "AbsoluteArrangement was deprecated in favor of Arrangement.Absolute",
        ReplaceWith(
            "Arrangement.Absolute.SpaceAround",
            "androidx.compose.foundation.layout.Arrangement"
        )
    )
    val SpaceAround = Arrangement.Absolute.SpaceAround

    /**
     * Place children such that each two adjacent ones are spaced by a fixed [space] distance across
     * the main axis. The spacing will be subtracted from the available space that the children
     * can occupy.
     *
     * Unlike [Arrangement.spacedBy], when the layout direction is RTL, the children will not be
     * mirrored and as such children will appear in the order they are composed inside the [Row].
     *
     * @param space The space between adjacent children.
     */
    @Stable
    @Deprecated(
        "AbsoluteArrangement was deprecated in favor of Arrangement.Absolute",
        ReplaceWith(
            "Arrangement.Absolute.spacedBy(space)",
            "androidx.compose.foundation.layout.Arrangement"
        )
    )
    fun spacedBy(space: Dp): Arrangement.HorizontalOrVertical = Arrangement.Absolute.spacedBy(space)

    /**
     * Place children horizontally such that each two adjacent ones are spaced by a fixed [space]
     * distance. The spacing will be subtracted from the available width that the children
     * can occupy. An [alignment] can be specified to align the spaced children horizontally
     * inside the parent, in case there is empty width remaining.
     *
     * Unlike [Arrangement.spacedBy], when the layout direction is RTL, the children will not be
     * mirrored and as such children will appear in the order they are composed inside the [Row].
     *
     * @param space The space between adjacent children.
     * @param alignment The alignment of the spaced children inside the parent.
     */
    @Stable
    @Deprecated(
        "AbsoluteArrangement was deprecated in favor of Arrangement.Absolute",
        ReplaceWith(
            "Arrangement.Absolute.spacedBy(space, alignment)",
            "androidx.compose.foundation.layout.Arrangement"
        )
    )
    fun spacedBy(space: Dp, alignment: Alignment.Horizontal): Arrangement.Horizontal =
        Arrangement.Absolute.spacedBy(space, alignment)

    /**
     * Place children vertically such that each two adjacent ones are spaced by a fixed [space]
     * distance. The spacing will be subtracted from the available height that the children
     * can occupy. An [alignment] can be specified to align the spaced children vertically
     * inside the parent, in case there is empty height remaining.
     *
     * Unlike [Arrangement.spacedBy], when the layout direction is RTL, the children will not be
     * mirrored and as such children will appear in the order they are composed inside the [Row].
     *
     * @param space The space between adjacent children.
     * @param alignment The alignment of the spaced children inside the parent.
     */
    @Stable
    @Deprecated(
        "AbsoluteArrangement was deprecated in favor of Arrangement.Absolute",
        ReplaceWith(
            "Arrangement.Absolute.spacedBy(space, alignment)",
            "androidx.compose.foundation.layout.Arrangement"
        )
    )
    fun spacedBy(space: Dp, alignment: Alignment.Vertical): Arrangement.Vertical =
        Arrangement.Absolute.spacedBy(space, alignment)

    /**
     * Place children horizontally one next to the other and align the obtained group
     * according to an [alignment].
     *
     * Unlike [Arrangement.aligned], when the layout direction is RTL, the children will not be
     * mirrored and as such children will appear in the order they are composed inside the [Row].
     *
     * @param alignment The alignment of the children inside the parent.
     */
    @Stable
    @Deprecated(
        "AbsoluteArrangement was deprecated in favor of Arrangement.Absolute",
        ReplaceWith(
            "Arrangement.Absolute.aligned(alignment)",
            "androidx.compose.foundation.layout.Arrangement"
        )
    )
    fun aligned(alignment: Alignment.Horizontal): Arrangement.Horizontal =
        Arrangement.Absolute.aligned(alignment)
}