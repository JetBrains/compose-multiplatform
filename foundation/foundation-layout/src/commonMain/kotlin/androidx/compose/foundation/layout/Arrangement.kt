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
import kotlin.jvm.JvmDefaultWithCompatibility
/**
 * Used to specify the arrangement of the layout's children in layouts like [Row] or [Column] in
 * the main axis direction (horizontal and vertical, respectively).
 *
 * Below is an illustration of different horizontal arrangements in [Row]s:
 * ![Row arrangements](https://developer.android.com/images/reference/androidx/compose/foundation/layout/row_arrangement_visualization.gif)
 *
 * Different vertical arrangements in [Column]s:
 * ![Column arrangements](https://developer.android.com/images/reference/androidx/compose/foundation/layout/column_arrangement_visualization.gif)
 */
@Immutable
object Arrangement {
    /**
     * Used to specify the horizontal arrangement of the layout's children in layouts like [Row].
     */
    @Stable
    @JvmDefaultWithCompatibility
    interface Horizontal {
        /**
         * Spacing that should be added between any two adjacent layout children.
         */
        val spacing get() = 0.dp

        /**
         * Horizontally places the layout children.
         *
         * @param totalSize Available space that can be occupied by the children, in pixels.
         * @param sizes An array of sizes of all children, in pixels.
         * @param layoutDirection A layout direction, left-to-right or right-to-left, of the parent
         * layout that should be taken into account when determining positions of the children.
         * @param outPositions An array of the size of [sizes] that returns the calculated
         * positions relative to the left, in pixels.
         */
        fun Density.arrange(
            totalSize: Int,
            sizes: IntArray,
            layoutDirection: LayoutDirection,
            outPositions: IntArray
        )
    }

    /**
     * Used to specify the vertical arrangement of the layout's children in layouts like [Column].
     */
    @Stable
    @JvmDefaultWithCompatibility
    interface Vertical {
        /**
         * Spacing that should be added between any two adjacent layout children.
         */
        val spacing get() = 0.dp

        /**
         * Vertically places the layout children.
         *
         * @param totalSize Available space that can be occupied by the children, in pixels.
         * @param sizes An array of sizes of all children, in pixels.
         * @param outPositions An array of the size of [sizes] that returns the calculated
         * positions relative to the top, in pixels.
         */
        fun Density.arrange(
            totalSize: Int,
            sizes: IntArray,
            outPositions: IntArray
        )
    }

    /**
     * Used to specify the horizontal arrangement of the layout's children in horizontal layouts
     * like [Row], or the vertical arrangement of the layout's children in vertical layouts like
     * [Column].
     */
    @Stable
    @JvmDefaultWithCompatibility
    interface HorizontalOrVertical : Horizontal, Vertical {
        /**
         * Spacing that should be added between any two adjacent layout children.
         */
        override val spacing: Dp get() = 0.dp
    }

    /**
     * Place children horizontally such that they are as close as possible to the beginning of the
     * horizontal axis (left if the layout direction is LTR, right otherwise).
     * Visually: 123#### for LTR and ####321.
     */
    @Stable
    val Start = object : Horizontal {
        override fun Density.arrange(
            totalSize: Int,
            sizes: IntArray,
            layoutDirection: LayoutDirection,
            outPositions: IntArray
        ) = if (layoutDirection == LayoutDirection.Ltr) {
            placeLeftOrTop(sizes, outPositions, reverseInput = false)
        } else {
            placeRightOrBottom(totalSize, sizes, outPositions, reverseInput = true)
        }

        override fun toString() = "Arrangement#Start"
    }

    /**
     * Place children horizontally such that they are as close as possible to the end of the main
     * axis.
     * Visually: ####123 for LTR and 321#### for RTL.
     */
    @Stable
    val End = object : Horizontal {
        override fun Density.arrange(
            totalSize: Int,
            sizes: IntArray,
            layoutDirection: LayoutDirection,
            outPositions: IntArray
        ) = if (layoutDirection == LayoutDirection.Ltr) {
            placeRightOrBottom(totalSize, sizes, outPositions, reverseInput = false)
        } else {
            placeLeftOrTop(sizes, outPositions, reverseInput = true)
        }

        override fun toString() = "Arrangement#End"
    }

    /**
     * Place children vertically such that they are as close as possible to the top of the main
     * axis.
     * Visually: (top) 123#### (bottom)
     */
    @Stable
    val Top = object : Vertical {
        override fun Density.arrange(
            totalSize: Int,
            sizes: IntArray,
            outPositions: IntArray
        ) = placeLeftOrTop(sizes, outPositions, reverseInput = false)

        override fun toString() = "Arrangement#Top"
    }

    /**
     * Place children vertically such that they are as close as possible to the bottom of the main
     * axis.
     * Visually: (top) ####123 (bottom)
     */
    @Stable
    val Bottom = object : Vertical {
        override fun Density.arrange(
            totalSize: Int,
            sizes: IntArray,
            outPositions: IntArray
        ) = placeRightOrBottom(totalSize, sizes, outPositions, reverseInput = false)

        override fun toString() = "Arrangement#Bottom"
    }

    /**
     * Place children such that they are as close as possible to the middle of the main axis.
     * Visually: ##123## for LTR and ##321## for RTL.
     */
    @Stable
    val Center = object : HorizontalOrVertical {
        override val spacing = 0.dp

        override fun Density.arrange(
            totalSize: Int,
            sizes: IntArray,
            layoutDirection: LayoutDirection,
            outPositions: IntArray
        ) = if (layoutDirection == LayoutDirection.Ltr) {
            placeCenter(totalSize, sizes, outPositions, reverseInput = false)
        } else {
            placeCenter(totalSize, sizes, outPositions, reverseInput = true)
        }

        override fun Density.arrange(
            totalSize: Int,
            sizes: IntArray,
            outPositions: IntArray
        ) = placeCenter(totalSize, sizes, outPositions, reverseInput = false)

        override fun toString() = "Arrangement#Center"
    }

    /**
     * Place children such that they are spaced evenly across the main axis, including free
     * space before the first child and after the last child.
     * Visually: #1#2#3# for LTR and #3#2#1# for RTL.
     */
    @Stable
    val SpaceEvenly = object : HorizontalOrVertical {
        override val spacing = 0.dp

        override fun Density.arrange(
            totalSize: Int,
            sizes: IntArray,
            layoutDirection: LayoutDirection,
            outPositions: IntArray
        ) = if (layoutDirection == LayoutDirection.Ltr) {
            placeSpaceEvenly(totalSize, sizes, outPositions, reverseInput = false)
        } else {
            placeSpaceEvenly(totalSize, sizes, outPositions, reverseInput = true)
        }

        override fun Density.arrange(
            totalSize: Int,
            sizes: IntArray,
            outPositions: IntArray
        ) = placeSpaceEvenly(totalSize, sizes, outPositions, reverseInput = false)

        override fun toString() = "Arrangement#SpaceEvenly"
    }

    /**
     * Place children such that they are spaced evenly across the main axis, without free
     * space before the first child or after the last child.
     * Visually: 1##2##3 for LTR or 3##2##1 for RTL.
     */
    @Stable
    val SpaceBetween = object : HorizontalOrVertical {
        override val spacing = 0.dp

        override fun Density.arrange(
            totalSize: Int,
            sizes: IntArray,
            layoutDirection: LayoutDirection,
            outPositions: IntArray
        ) = if (layoutDirection == LayoutDirection.Ltr) {
            placeSpaceBetween(totalSize, sizes, outPositions, reverseInput = false)
        } else {
            placeSpaceBetween(totalSize, sizes, outPositions, reverseInput = true)
        }

        override fun Density.arrange(
            totalSize: Int,
            sizes: IntArray,
            outPositions: IntArray
        ) = placeSpaceBetween(totalSize, sizes, outPositions, reverseInput = false)

        override fun toString() = "Arrangement#SpaceBetween"
    }

    /**
     * Place children such that they are spaced evenly across the main axis, including free
     * space before the first child and after the last child, but half the amount of space
     * existing otherwise between two consecutive children.
     * Visually: #1##2##3# for LTR and #3##2##1# for RTL
     */
    @Stable
    val SpaceAround = object : HorizontalOrVertical {
        override val spacing = 0.dp

        override fun Density.arrange(
            totalSize: Int,
            sizes: IntArray,
            layoutDirection: LayoutDirection,
            outPositions: IntArray
        ) = if (layoutDirection == LayoutDirection.Ltr) {
            placeSpaceAround(totalSize, sizes, outPositions, reverseInput = false)
        } else {
            placeSpaceAround(totalSize, sizes, outPositions, reverseInput = true)
        }

        override fun Density.arrange(
            totalSize: Int,
            sizes: IntArray,
            outPositions: IntArray
        ) = placeSpaceAround(totalSize, sizes, outPositions, reverseInput = false)

        override fun toString() = "Arrangement#SpaceAround"
    }

    /**
     * Place children such that each two adjacent ones are spaced by a fixed [space] distance across
     * the main axis. The spacing will be subtracted from the available space that the children
     * can occupy. The [space] can be negative, in which case children will overlap.
     *
     * To change alignment of the spaced children horizontally or vertically, use [spacedBy]
     * overloads with `alignment` parameter.
     *
     * @param space The space between adjacent children.
     */
    @Stable
    fun spacedBy(space: Dp): HorizontalOrVertical =
        SpacedAligned(space, true) { size, layoutDirection ->
            Alignment.Start.align(0, size, layoutDirection)
        }

    /**
     * Place children horizontally such that each two adjacent ones are spaced by a fixed [space]
     * distance. The spacing will be subtracted from the available width that the children
     * can occupy. An [alignment] can be specified to align the spaced children horizontally
     * inside the parent, in case there is empty width remaining. The [space] can be negative,
     * in which case children will overlap.
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
     * inside the parent, in case there is empty height remaining. The [space] can be negative,
     * in which case children will overlap.
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
    object Absolute {
        /**
         * Place children horizontally such that they are as close as possible to the left edge of
         * the [Row].
         *
         * Unlike [Arrangement.Start], when the layout direction is RTL, the children will not be
         * mirrored and as such children will appear in the order they are composed inside the [Row].
         *
         * Visually: 123####
         */
        @Stable
        val Left = object : Horizontal {
            override fun Density.arrange(
                totalSize: Int,
                sizes: IntArray,
                layoutDirection: LayoutDirection,
                outPositions: IntArray
            ) = placeLeftOrTop(sizes, outPositions, reverseInput = false)

            override fun toString() = "AbsoluteArrangement#Left"
        }

        /**
         * Place children such that they are as close as possible to the middle of the [Row].
         *
         * Unlike [Arrangement.Center], when the layout direction is RTL, the children will not be
         * mirrored and as such children will appear in the order they are composed inside the [Row].
         *
         * Visually: ##123##
         */
        @Stable
        val Center = object : Horizontal {
            override fun Density.arrange(
                totalSize: Int,
                sizes: IntArray,
                layoutDirection: LayoutDirection,
                outPositions: IntArray
            ) = placeCenter(totalSize, sizes, outPositions, reverseInput = false)

            override fun toString() = "AbsoluteArrangement#Center"
        }

        /**
         * Place children horizontally such that they are as close as possible to the right edge of
         * the [Row].
         *
         * Unlike [Arrangement.End], when the layout direction is RTL, the children will not be
         * mirrored and as such children will appear in the order they are composed inside the [Row].
         *
         * Visually: ####123
         */
        @Stable
        val Right = object : Horizontal {
            override fun Density.arrange(
                totalSize: Int,
                sizes: IntArray,
                layoutDirection: LayoutDirection,
                outPositions: IntArray
            ) = placeRightOrBottom(totalSize, sizes, outPositions, reverseInput = false)

            override fun toString() = "AbsoluteArrangement#Right"
        }

        /**
         * Place children such that they are spaced evenly across the main axis, without free
         * space before the first child or after the last child.
         *
         * Unlike [Arrangement.SpaceBetween], when the layout direction is RTL, the children will not be
         * mirrored and as such children will appear in the order they are composed inside the [Row].
         *
         * Visually: 1##2##3
         */
        @Stable
        val SpaceBetween = object : Horizontal {
            override fun Density.arrange(
                totalSize: Int,
                sizes: IntArray,
                layoutDirection: LayoutDirection,
                outPositions: IntArray
            ) = placeSpaceBetween(totalSize, sizes, outPositions, reverseInput = false)

            override fun toString() = "AbsoluteArrangement#SpaceBetween"
        }

        /**
         * Place children such that they are spaced evenly across the main axis, including free
         * space before the first child and after the last child.
         *
         * Unlike [Arrangement.SpaceEvenly], when the layout direction is RTL, the children will not be
         * mirrored and as such children will appear in the order they are composed inside the [Row].
         *
         * Visually: #1#2#3#
         */
        @Stable
        val SpaceEvenly = object : Horizontal {
            override fun Density.arrange(
                totalSize: Int,
                sizes: IntArray,
                layoutDirection: LayoutDirection,
                outPositions: IntArray
            ) = placeSpaceEvenly(totalSize, sizes, outPositions, reverseInput = false)

            override fun toString() = "AbsoluteArrangement#SpaceEvenly"
        }

        /**
         * Place children such that they are spaced evenly horizontally, including free
         * space before the first child and after the last child, but half the amount of space
         * existing otherwise between two consecutive children.
         *
         * Unlike [Arrangement.SpaceAround], when the layout direction is RTL, the children will not be
         * mirrored and as such children will appear in the order they are composed inside the [Row].
         *
         * Visually: #1##2##3##4#
         */
        @Stable
        val SpaceAround = object : Horizontal {
            override fun Density.arrange(
                totalSize: Int,
                sizes: IntArray,
                layoutDirection: LayoutDirection,
                outPositions: IntArray
            ) = placeSpaceAround(totalSize, sizes, outPositions, reverseInput = false)

            override fun toString() = "AbsoluteArrangement#SpaceAround"
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

        override fun Density.arrange(
            totalSize: Int,
            sizes: IntArray,
            layoutDirection: LayoutDirection,
            outPositions: IntArray
        ) {
            if (sizes.isEmpty()) return
            val spacePx = space.roundToPx()

            var occupied = 0
            var lastSpace = 0
            val reversed = rtlMirror && layoutDirection == LayoutDirection.Rtl
            sizes.forEachIndexed(reversed) { index, it ->
                outPositions[index] = min(occupied, totalSize - it)
                lastSpace = min(spacePx, totalSize - outPositions[index] - it)
                occupied = outPositions[index] + it + lastSpace
            }
            occupied -= lastSpace

            if (alignment != null && occupied < totalSize) {
                val groupPosition = alignment.invoke(totalSize - occupied, layoutDirection)
                for (index in outPositions.indices) {
                    outPositions[index] += groupPosition
                }
            }
        }

        override fun Density.arrange(
            totalSize: Int,
            sizes: IntArray,
            outPositions: IntArray
        ) = arrange(totalSize, sizes, LayoutDirection.Ltr, outPositions)

        override fun toString() =
            "${if (rtlMirror) "" else "Absolute"}Arrangement#spacedAligned($space, $alignment)"
    }

    internal fun placeRightOrBottom(
        totalSize: Int,
        size: IntArray,
        outPosition: IntArray,
        reverseInput: Boolean
    ) {
        val consumedSize = size.fold(0) { a, b -> a + b }
        var current = totalSize - consumedSize
        size.forEachIndexed(reverseInput) { index, it ->
            outPosition[index] = current
            current += it
        }
    }

    internal fun placeLeftOrTop(size: IntArray, outPosition: IntArray, reverseInput: Boolean) {
        var current = 0
        size.forEachIndexed(reverseInput) { index, it ->
            outPosition[index] = current
            current += it
        }
    }

    internal fun placeCenter(
        totalSize: Int,
        size: IntArray,
        outPosition: IntArray,
        reverseInput: Boolean
    ) {
        val consumedSize = size.fold(0) { a, b -> a + b }
        var current = (totalSize - consumedSize).toFloat() / 2
        size.forEachIndexed(reverseInput) { index, it ->
            outPosition[index] = current.roundToInt()
            current += it.toFloat()
        }
    }

    internal fun placeSpaceEvenly(
        totalSize: Int,
        size: IntArray,
        outPosition: IntArray,
        reverseInput: Boolean
    ) {
        val consumedSize = size.fold(0) { a, b -> a + b }
        val gapSize = (totalSize - consumedSize).toFloat() / (size.size + 1)
        var current = gapSize
        size.forEachIndexed(reverseInput) { index, it ->
            outPosition[index] = current.roundToInt()
            current += it.toFloat() + gapSize
        }
    }

    internal fun placeSpaceBetween(
        totalSize: Int,
        size: IntArray,
        outPosition: IntArray,
        reverseInput: Boolean
    ) {
        val consumedSize = size.fold(0) { a, b -> a + b }
        val gapSize = if (size.size > 1) {
            (totalSize - consumedSize).toFloat() / (size.size - 1)
        } else {
            0f
        }
        var current = 0f
        size.forEachIndexed(reverseInput) { index, it ->
            outPosition[index] = current.roundToInt()
            current += it.toFloat() + gapSize
        }
    }

    internal fun placeSpaceAround(
        totalSize: Int,
        size: IntArray,
        outPosition: IntArray,
        reverseInput: Boolean
    ) {
        val consumedSize = size.fold(0) { a, b -> a + b }
        val gapSize = if (size.isNotEmpty()) {
            (totalSize - consumedSize).toFloat() / size.size
        } else {
            0f
        }
        var current = gapSize / 2
        size.forEachIndexed(reverseInput) { index, it ->
            outPosition[index] = current.roundToInt()
            current += it.toFloat() + gapSize
        }
    }

    private inline fun IntArray.forEachIndexed(reversed: Boolean, action: (Int, Int) -> Unit) {
        if (!reversed) {
            forEachIndexed(action)
        } else {
            for (i in (size - 1) downTo 0) {
                action(i, get(i))
            }
        }
    }
}
