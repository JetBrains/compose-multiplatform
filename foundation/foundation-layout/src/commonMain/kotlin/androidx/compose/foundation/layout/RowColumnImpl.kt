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

package androidx.compose.foundation.layout

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.ParentDataModifier
import androidx.compose.ui.Placeable
import androidx.compose.foundation.layout.LayoutOrientation.Horizontal
import androidx.compose.foundation.layout.LayoutOrientation.Vertical
import androidx.compose.ui.AlignmentLine
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureBlock
import androidx.compose.ui.layout.Measured
import androidx.compose.ui.measureBlocksOf
import androidx.compose.ui.node.ExperimentalLayoutNodeApi
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign

@PublishedApi
@OptIn(ExperimentalLayoutNodeApi::class)
internal fun rowColumnMeasureBlocks(
    orientation: LayoutOrientation,
    arrangement: (Int, List<Int>, LayoutDirection, Density, MutableList<Int>) -> Unit,
    arrangementSpacing: Dp,
    crossAxisSize: SizeMode,
    crossAxisAlignment: CrossAxisAlignment
): LayoutNode.MeasureBlocks {
    fun Placeable.mainAxisSize() =
        if (orientation == LayoutOrientation.Horizontal) width else height

    fun Placeable.crossAxisSize() =
        if (orientation == LayoutOrientation.Horizontal) height else width

    return measureBlocksOf(
        minIntrinsicWidthMeasureBlock = MinIntrinsicWidthMeasureBlock(orientation),
        minIntrinsicHeightMeasureBlock = MinIntrinsicHeightMeasureBlock(orientation),
        maxIntrinsicWidthMeasureBlock = MaxIntrinsicWidthMeasureBlock(orientation),
        maxIntrinsicHeightMeasureBlock = MaxIntrinsicHeightMeasureBlock(orientation)
    ) { measurables, outerConstraints ->
        val constraints = OrientationIndependentConstraints(outerConstraints, orientation)
        val arrangementSpacingPx = arrangementSpacing.toIntPx()

        var totalWeight = 0f
        var fixedSpace = 0
        var crossAxisSpace = 0
        var weightChildrenCount = 0

        var anyAlignWithSiblings = false
        val placeables = arrayOfNulls<Placeable>(measurables.size)
        val rowColumnParentData = Array(measurables.size) { measurables[it].data }

        // First measure children with zero weight.
        var spaceAfterLastNoWeight = 0
        for (i in measurables.indices) {
            val child = measurables[i]
            val parentData = rowColumnParentData[i]
            val weight = parentData.weight

            if (weight > 0f) {
                totalWeight += weight
                ++weightChildrenCount
            } else {
                val mainAxisMax = constraints.mainAxisMax
                val placeable = child.measure(
                    // Ask for preferred main axis size.
                    constraints.copy(
                        mainAxisMin = 0,
                        mainAxisMax = if (mainAxisMax == Constraints.Infinity) {
                            Constraints.Infinity
                        } else {
                            mainAxisMax - fixedSpace
                        },
                        crossAxisMin = 0
                    ).toBoxConstraints(orientation)
                )
                spaceAfterLastNoWeight = min(
                    arrangementSpacingPx,
                    mainAxisMax - fixedSpace - placeable.mainAxisSize()
                )
                fixedSpace += placeable.mainAxisSize() + spaceAfterLastNoWeight
                crossAxisSpace = max(crossAxisSpace, placeable.crossAxisSize())
                anyAlignWithSiblings = anyAlignWithSiblings || parentData.isRelative
                placeables[i] = placeable
            }
        }

        var weightedSpace = 0
        if (weightChildrenCount == 0) {
            // fixedSpace contains an extra spacing after the last non-weight child.
            fixedSpace -= spaceAfterLastNoWeight
        } else {
            // Measure the rest according to their weights in the remaining main axis space.
            val targetSpace =
                if (totalWeight > 0f && constraints.mainAxisMax != Constraints.Infinity) {
                    constraints.mainAxisMax
                } else {
                    constraints.mainAxisMin
                }
            val remainingToTarget =
                targetSpace - fixedSpace - arrangementSpacingPx * (weightChildrenCount - 1)

            val weightUnitSpace = if (totalWeight > 0) remainingToTarget / totalWeight else 0f
            var remainder = remainingToTarget - rowColumnParentData.sumBy {
                (weightUnitSpace * it.weight).roundToInt()
            }

            for (i in measurables.indices) {
                if (placeables[i] == null) {
                    val child = measurables[i]
                    val parentData = rowColumnParentData[i]
                    val weight = parentData.weight
                    check(weight > 0) { "All weights <= 0 should have placeables" }
                    // After the weightUnitSpace rounding, the total space going to be occupied
                    // can be smaller or larger than remainingToTarget. Here we distribute the
                    // loss or gain remainder evenly to the first children.
                    val remainderUnit = remainder.sign
                    remainder -= remainderUnit
                    val childMainAxisSize = max(
                        0,
                        (weightUnitSpace * weight).roundToInt() + remainderUnit
                    )
                    val placeable = child.measure(
                        OrientationIndependentConstraints(
                            if (parentData.fill && childMainAxisSize != Constraints.Infinity) {
                                childMainAxisSize
                            } else {
                                0
                            },
                            childMainAxisSize,
                            0,
                            constraints.crossAxisMax
                        ).toBoxConstraints(orientation)
                    )
                    weightedSpace += placeable.mainAxisSize()
                    crossAxisSpace = max(crossAxisSpace, placeable.crossAxisSize())
                    anyAlignWithSiblings = anyAlignWithSiblings || parentData.isRelative
                    placeables[i] = placeable
                }
            }
        }

        var beforeCrossAxisAlignmentLine = 0
        var afterCrossAxisAlignmentLine = 0
        if (anyAlignWithSiblings) {
            for (i in placeables.indices) {
                val placeable = placeables[i]!!
                val parentData = rowColumnParentData[i]
                val alignmentLinePosition = parentData.crossAxisAlignment
                    ?.calculateAlignmentLinePosition(placeable)
                if (alignmentLinePosition != null) {
                    beforeCrossAxisAlignmentLine = max(
                        beforeCrossAxisAlignmentLine,
                        alignmentLinePosition.let { if (it != AlignmentLine.Unspecified) it else 0 }
                    )
                    afterCrossAxisAlignmentLine = max(
                        afterCrossAxisAlignmentLine,
                        placeable.crossAxisSize() -
                                (alignmentLinePosition.let {
                                    if (it != AlignmentLine.Unspecified) {
                                        it
                                    } else {
                                        placeable.crossAxisSize()
                                    }
                                })
                    )
                }
            }
        }

        // Compute the Row or Column size and position the children.
        val mainAxisLayoutSize =
            if (totalWeight > 0f && constraints.mainAxisMax != Constraints.Infinity) {
                constraints.mainAxisMax
            } else {
                max(fixedSpace + weightedSpace, constraints.mainAxisMin)
            }
        val crossAxisLayoutSize = if (constraints.crossAxisMax != Constraints.Infinity &&
            crossAxisSize == SizeMode.Expand
        ) {
            constraints.crossAxisMax
        } else {
            max(
                crossAxisSpace,
                max(
                    constraints.crossAxisMin,
                    beforeCrossAxisAlignmentLine + afterCrossAxisAlignmentLine
                )
            )
        }
        val layoutWidth = if (orientation == LayoutOrientation.Horizontal) {
            mainAxisLayoutSize
        } else {
            crossAxisLayoutSize
        }
        val layoutHeight = if (orientation == LayoutOrientation.Horizontal) {
            crossAxisLayoutSize
        } else {
            mainAxisLayoutSize
        }

        val mainAxisPositions = MutableList(measurables.size) { 0 }
        layout(layoutWidth, layoutHeight) {
            val childrenMainAxisSize = placeables.map { it!!.mainAxisSize() }
            arrangement(
                mainAxisLayoutSize,
                childrenMainAxisSize,
                layoutDirection,
                this@measureBlocksOf,
                mainAxisPositions
            )

            placeables.forEachIndexed { index, placeable ->
                placeable!!
                val parentData = rowColumnParentData[index]
                val childCrossAlignment = parentData.crossAxisAlignment ?: crossAxisAlignment

                val crossAxis = childCrossAlignment.align(
                    size = crossAxisLayoutSize - placeable.crossAxisSize(),
                    layoutDirection = if (orientation == LayoutOrientation.Horizontal) {
                        LayoutDirection.Ltr
                    } else {
                        layoutDirection
                    },
                    placeable = placeable,
                    beforeCrossAxisAlignmentLine = beforeCrossAxisAlignmentLine
                )

                if (orientation == LayoutOrientation.Horizontal) {
                    placeable.place(mainAxisPositions[index], crossAxis)
                } else {
                    placeable.place(crossAxis, mainAxisPositions[index])
                }
            }
        }
    }
}

/**
 * Used to specify the arrangement of the layout's children in [Row] or [Column] in the main axis
 * direction (horizontal and vertical, respectively).
 */
@Immutable
@OptIn(InternalLayoutApi::class)
object Arrangement {
    /**
     * Used to specify the horizontal arrangement of the layout's children in a [Row].
     */
    @InternalLayoutApi
    interface Horizontal {
        /**
         * Spacing that should be added between any two adjacent layout children.
         */
        val spacing get() = 0.dp

        /**
         * Horizontally places the layout children inside the [Row].
         *
         * @param totalSize Available space that can be occupied by the children.
         * @param size A list of sizes of all children.
         * @param layoutDirection A layout direction, left-to-right or right-to-left, of the parent
         * layout that should be taken into account when determining positions of the children.
         * @param density The current density.
         * @param outPosition A preallocated list that should return the calculated positions.
         */
        fun arrange(
            totalSize: Int,
            size: List<Int>,
            layoutDirection: LayoutDirection,
            density: Density,
            outPosition: MutableList<Int>
        )

        @Deprecated("Custom arrangements will not be supported anymore. Please use a provided " +
                "one instead, or Spacers.")
        fun arrange(
            totalSize: Int,
            size: List<Int>,
            layoutDirection: LayoutDirection
        ): List<Int> {
            val result = MutableList(size.size) { 0 }
            arrange(totalSize, size, layoutDirection, Density(1f), result)
            return result
        }
    }

    /**
     * Used to specify the vertical arrangement of the layout's children in a [Column].
     */
    @InternalLayoutApi
    interface Vertical {
        /**
         * Spacing that should be added between any two adjacent layout children.
         */
        val spacing get() = 0.dp

        /**
         * Vertically places the layout children inside the [Column].
         *
         * @param totalSize Available space that can be occupied by the children.
         * @param size A list of sizes of all children.
         * @param density The current density.
         * @param outPosition A preallocated list that should return the calculated positions.
         */
        fun arrange(
            totalSize: Int,
            size: List<Int>,
            density: Density,
            outPosition: MutableList<Int>
        )

        @Deprecated("Custom arrangements will not be supported anymore. Please use a provided " +
                "one instead, or Spacers.")
        fun arrange(
            totalSize: Int,
            size: List<Int>
        ): List<Int> {
            val result = MutableList(size.size) { 0 }
            arrange(totalSize, size, Density(1f), result)
            return result
        }
    }

    /**
     * Used to specify the horizontal arrangement of the layout's children in a [Row], or
     * the vertical arrangement of .
     */
    @InternalLayoutApi
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
    val Start = object : Horizontal {
        override fun arrange(
            totalSize: Int,
            size: List<Int>,
            layoutDirection: LayoutDirection,
            density: Density,
            outPosition: MutableList<Int>
        ) = if (layoutDirection == LayoutDirection.Ltr) {
            placeLeftOrTop(size, outPosition)
        } else {
            placeRightOrBottom(totalSize, size.asReversed(), outPosition)
            outPosition.reverse()
        }
    }

    /**
     * Place children horizontally such that they are as close as possible to the end of the main
     * axis.
     */
    val End = object : Horizontal {
        override fun arrange(
            totalSize: Int,
            size: List<Int>,
            layoutDirection: LayoutDirection,
            density: Density,
            outPosition: MutableList<Int>
        ) = if (layoutDirection == LayoutDirection.Ltr) {
            placeRightOrBottom(totalSize, size, outPosition)
        } else {
            placeLeftOrTop(size.asReversed(), outPosition)
            outPosition.reverse()
        }
    }

    /**
     * Place children vertically such that they are as close as possible to the top of the main
     * axis.
     */
    val Top = object : Vertical {
        override fun arrange(
            totalSize: Int,
            size: List<Int>,
            density: Density,
            outPosition: MutableList<Int>
        ) = placeLeftOrTop(size, outPosition)
    }

    /**
     * Place children vertically such that they are as close as possible to the bottom of the main
     * axis.
     */
    val Bottom = object : Vertical {
        override fun arrange(
            totalSize: Int,
            size: List<Int>,
            density: Density,
            outPosition: MutableList<Int>
        ) = placeRightOrBottom(totalSize, size, outPosition)
    }

    /**
     * Place children such that they are as close as possible to the middle of the main axis.
     */
    val Center = object : HorizontalOrVertical {
        override val spacing = 0.dp

        override fun arrange(
            totalSize: Int,
            size: List<Int>,
            layoutDirection: LayoutDirection,
            density: Density,
            outPosition: MutableList<Int>
        ) = if (layoutDirection == LayoutDirection.Ltr) {
            placeCenter(totalSize, size, outPosition)
        } else {
            placeCenter(totalSize, size.asReversed(), outPosition)
            outPosition.reverse()
        }

        override fun arrange(
            totalSize: Int,
            size: List<Int>,
            density: Density,
            outPosition: MutableList<Int>
        ) = placeCenter(totalSize, size, outPosition)
    }

    /**
     * Place children such that they are spaced evenly across the main axis, including free
     * space before the first child and after the last child.
     */
    val SpaceEvenly = object : HorizontalOrVertical {
        override val spacing = 0.dp

        override fun arrange(
            totalSize: Int,
            size: List<Int>,
            layoutDirection: LayoutDirection,
            density: Density,
            outPosition: MutableList<Int>
        ) = if (layoutDirection == LayoutDirection.Ltr) {
            placeSpaceEvenly(totalSize, size, outPosition)
        } else {
            placeSpaceEvenly(totalSize, size.asReversed(), outPosition)
            outPosition.reverse()
        }

        override fun arrange(
            totalSize: Int,
            size: List<Int>,
            density: Density,
            outPosition: MutableList<Int>
        ) = placeSpaceEvenly(totalSize, size, outPosition)
    }

    /**
     * Place children such that they are spaced evenly across the main axis, without free
     * space before the first child or after the last child.
     */
    val SpaceBetween = object : HorizontalOrVertical {
        override val spacing = 0.dp

        override fun arrange(
            totalSize: Int,
            size: List<Int>,
            layoutDirection: LayoutDirection,
            density: Density,
            outPosition: MutableList<Int>
        ) = if (layoutDirection == LayoutDirection.Ltr) {
            placeSpaceBetween(totalSize, size, outPosition)
        } else {
            placeSpaceBetween(totalSize, size.asReversed(), outPosition)
            outPosition.reverse()
        }

        override fun arrange(
            totalSize: Int,
            size: List<Int>,
            density: Density,
            outPosition: MutableList<Int>
        ) = placeSpaceBetween(totalSize, size, outPosition)
    }

    /**
     * Place children such that they are spaced evenly across the main axis, including free
     * space before the first child and after the last child, but half the amount of space
     * existing otherwise between two consecutive children.
     */
    val SpaceAround = object : HorizontalOrVertical {
        override val spacing = 0.dp

        override fun arrange(
            totalSize: Int,
            size: List<Int>,
            layoutDirection: LayoutDirection,
            density: Density,
            outPosition: MutableList<Int>
        ) = if (layoutDirection == LayoutDirection.Ltr) {
            placeSpaceAround(totalSize, size, outPosition)
        } else {
            placeSpaceAround(totalSize, size.asReversed(), outPosition)
            outPosition.reverse()
        }

        override fun arrange(
            totalSize: Int,
            size: List<Int>,
            density: Density,
            outPosition: MutableList<Int>
        ) = placeSpaceAround(totalSize, size, outPosition)
    }

    /**
     * Place children such that each two adjacent ones are spaced by a fixed [space] distance across
     * the main axis. The spacing will be subtracted from the available space that the children
     * can occupy.
     *
     * @param space The space between adjacent children.
     */
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
    fun spacedBy(space: Dp, alignment: Alignment.Horizontal): Horizontal =
        SpacedAligned(space, true) { size, layoutDirection ->
            alignment.align(size, layoutDirection)
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
    fun spacedBy(space: Dp, alignment: Alignment.Vertical): Vertical =
        SpacedAligned(space, false) { size, _ -> alignment.align(size) }

    /**
     * Place children horizontally one next to the other and align the obtained group
     * according to an [alignment].
     *
     * @param alignment The alignment of the children inside the parent.
     */
    fun aligned(alignment: Alignment.Horizontal): Horizontal =
        SpacedAligned(0.dp, true) { size, layoutDirection ->
            alignment.align(size, layoutDirection)
        }

    /**
     * Place children vertically one next to the other and align the obtained group
     * according to an [alignment].
     *
     * @param alignment The alignment of the children inside the parent.
     */
    fun aligned(alignment: Alignment.Vertical): Vertical =
        SpacedAligned(0.dp, false) { size, _ -> alignment.align(size) }

    /**
     * Arrangement with spacing between adjacent children and alignment for the spaced group.
     * Should not be instantiated directly, use [spacedBy] instead.
     */
    internal data class SpacedAligned(
        val space: Dp,
        val rtlMirror: Boolean,
        val alignment: ((Int, LayoutDirection) -> Int)?
    ) : HorizontalOrVertical {
        override val spacing = space

        override fun arrange(
            totalSize: Int,
            size: List<Int>,
            layoutDirection: LayoutDirection,
            density: Density,
            outPosition: MutableList<Int>
        ) {
            if (size.isEmpty()) return
            val spacePx = with(density) { space.toIntPx() }

            var occupied = 0
            var lastSpace = 0
            (if (layoutDirection == LayoutDirection.Ltr || !rtlMirror) size else size.asReversed())
                .fastForEachIndexed { index, it ->
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
            size: List<Int>,
            density: Density,
            outPosition: MutableList<Int>
        ) = arrange(totalSize, size, LayoutDirection.Ltr, density, outPosition)
    }

    internal fun placeRightOrBottom(
        totalSize: Int,
        size: List<Int>,
        outPosition: MutableList<Int>
    ) {
        val consumedSize = size.fold(0) { a, b -> a + b }
        var current = totalSize - consumedSize
        size.fastForEachIndexed { index, it ->
            outPosition[index] = current
            current += it
        }
    }

    internal fun placeLeftOrTop(size: List<Int>, outPosition: MutableList<Int>) {
        var current = 0
        size.fastForEachIndexed { index, it ->
            outPosition[index] = current
            current += it
        }
    }

    internal fun placeCenter(totalSize: Int, size: List<Int>, outPosition: MutableList<Int>) {
        val consumedSize = size.fold(0) { a, b -> a + b }
        var current = (totalSize - consumedSize).toFloat() / 2
        size.fastForEachIndexed { index, it ->
            outPosition[index] = current.roundToInt()
            current += it.toFloat()
        }
    }

    internal fun placeSpaceEvenly(totalSize: Int, size: List<Int>, outPosition: MutableList<Int>) {
        val consumedSize = size.fold(0) { a, b -> a + b }
        val gapSize = (totalSize - consumedSize).toFloat() / (size.size + 1)
        var current = gapSize
        size.fastForEachIndexed { index, it ->
            outPosition[index] = current.roundToInt()
            current += it.toFloat() + gapSize
        }
    }

    internal fun placeSpaceBetween(totalSize: Int, size: List<Int>, outPosition: MutableList<Int>) {
        val consumedSize = size.fold(0) { a, b -> a + b }
        val gapSize = if (size.size > 1) {
            (totalSize - consumedSize).toFloat() / (size.size - 1)
        } else {
            0f
        }
        var current = 0f
        size.fastForEachIndexed { index, it ->
            outPosition[index] = current.roundToInt()
            current += it.toFloat() + gapSize
        }
    }

    internal fun placeSpaceAround(totalSize: Int, size: List<Int>, outPosition: MutableList<Int>) {
        val consumedSize = size.fold(0) { a, b -> a + b }
        val gapSize = if (size.isNotEmpty()) {
            (totalSize - consumedSize).toFloat() / size.size
        } else {
            0f
        }
        var current = gapSize / 2
        size.fastForEachIndexed { index, it ->
            outPosition[index] = current.roundToInt()
            current += it.toFloat() + gapSize
        }
    }
}

@Immutable
@OptIn(InternalLayoutApi::class)
object AbsoluteArrangement {
    /**
     * Place children horizontally such that they are as close as possible to the left edge of
     * the [Row].
     *
     * Unlike [Arrangement.Start], when the layout direction is RTL, the children will not be
     * mirrored and as such children will appear in the order they are composed inside the [Row].
     */
    val Left = object : Arrangement.Horizontal {
        override fun arrange(
            totalSize: Int,
            size: List<Int>,
            layoutDirection: LayoutDirection,
            density: Density,
            outPosition: MutableList<Int>
        ) = Arrangement.placeLeftOrTop(size, outPosition)
    }

    /**
     * Place children such that they are as close as possible to the middle of the [Row].
     *
     * Unlike [Arrangement.Center], when the layout direction is RTL, the children will not be
     * mirrored and as such children will appear in the order they are composed inside the [Row].
     */
    val Center = object : Arrangement.Horizontal {
        override fun arrange(
            totalSize: Int,
            size: List<Int>,
            layoutDirection: LayoutDirection,
            density: Density,
            outPosition: MutableList<Int>
        ) = Arrangement.placeCenter(totalSize, size, outPosition)
    }

    /**
     * Place children horizontally such that they are as close as possible to the right edge of
     * the [Row].
     *
     * Unlike [Arrangement.End], when the layout direction is RTL, the children will not be
     * mirrored and as such children will appear in the order they are composed inside the [Row].
     */
    val Right = object : Arrangement.Horizontal {
        override fun arrange(
            totalSize: Int,
            size: List<Int>,
            layoutDirection: LayoutDirection,
            density: Density,
            outPosition: MutableList<Int>
        ) = Arrangement.placeRightOrBottom(totalSize, size, outPosition)
    }

    /**
     * Place children such that they are spaced evenly across the main axis, without free
     * space before the first child or after the last child.
     *
     * Unlike [Arrangement.SpaceBetween], when the layout direction is RTL, the children will not be
     * mirrored and as such children will appear in the order they are composed inside the [Row].
     */
    val SpaceBetween = object : Arrangement.Horizontal {
        override fun arrange(
            totalSize: Int,
            size: List<Int>,
            layoutDirection: LayoutDirection,
            density: Density,
            outPosition: MutableList<Int>
        ) = Arrangement.placeSpaceBetween(totalSize, size, outPosition)
    }

    /**
     * Place children such that they are spaced evenly across the main axis, including free
     * space before the first child and after the last child.
     *
     * Unlike [Arrangement.SpaceEvenly], when the layout direction is RTL, the children will not be
     * mirrored and as such children will appear in the order they are composed inside the [Row].
     */
    val SpaceEvenly = object : Arrangement.Horizontal {
        override fun arrange(
            totalSize: Int,
            size: List<Int>,
            layoutDirection: LayoutDirection,
            density: Density,
            outPosition: MutableList<Int>
        ) = Arrangement.placeSpaceEvenly(totalSize, size, outPosition)
    }

    /**
     * Place children such that they are spaced evenly horizontally, including free
     * space before the first child and after the last child, but half the amount of space
     * existing otherwise between two consecutive children.
     *
     * Unlike [Arrangement.SpaceAround], when the layout direction is RTL, the children will not be
     * mirrored and as such children will appear in the order they are composed inside the [Row].
     */
    val SpaceAround = object : Arrangement.Horizontal {
        override fun arrange(
            totalSize: Int,
            size: List<Int>,
            layoutDirection: LayoutDirection,
            density: Density,
            outPosition: MutableList<Int>
        ) = Arrangement.placeSpaceAround(totalSize, size, outPosition)
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
    fun spacedBy(space: Dp): Arrangement.HorizontalOrVertical =
        Arrangement.SpacedAligned(space, false, null)

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
    fun spacedBy(space: Dp, alignment: Alignment.Horizontal): Arrangement.Horizontal =
        Arrangement.SpacedAligned(space, false) { size, layoutDirection ->
            alignment.align(size, layoutDirection)
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
    fun spacedBy(space: Dp, alignment: Alignment.Vertical): Arrangement.Vertical =
        Arrangement.SpacedAligned(space, false) { size, _ -> alignment.align(size) }

    /**
     * Place children horizontally one next to the other and align the obtained group
     * according to an [alignment].
     *
     * Unlike [Arrangement.aligned], when the layout direction is RTL, the children will not be
     * mirrored and as such children will appear in the order they are composed inside the [Row].
     *
     * @param alignment The alignment of the children inside the parent.
     */
    fun aligned(alignment: Alignment.Horizontal): Arrangement.Horizontal =
        Arrangement.SpacedAligned(0.dp, false) { size, layoutDirection ->
            alignment.align(size, layoutDirection)
        }
}

/**
 * [Row] will be [Horizontal], [Column] is [Vertical].
 */
internal enum class LayoutOrientation {
    Horizontal,
    Vertical
}

/**
 * Used to specify how a layout chooses its own size when multiple behaviors are possible.
 */
// TODO(popam): remove this when Flow is reworked
enum class SizeMode {
    /**
     * Minimize the amount of free space by wrapping the children,
     * subject to the incoming layout constraints.
     */
    Wrap,
    /**
     * Maximize the amount of free space by expanding to fill the available space,
     * subject to the incoming layout constraints.
     */
    Expand
}

/**
 * Used to specify the alignment of a layout's children, in main axis direction.
 */
@OptIn(InternalLayoutApi::class)
enum class MainAxisAlignment(internal val arrangement: Arrangement.Vertical) {
    // TODO(soboleva) support RTl in Flow
    // workaround for now - use Arrangement that equals to previous Arrangement
    /**
     * Place children such that they are as close as possible to the middle of the main axis.
     */
    Center(Arrangement.Center),

    /**
     * Place children such that they are as close as possible to the start of the main axis.
     */
    Start(Arrangement.Top),

    /**
     * Place children such that they are as close as possible to the end of the main axis.
     */
    End(Arrangement.Bottom),

    /**
     * Place children such that they are spaced evenly across the main axis, including free
     * space before the first child and after the last child.
     */
    SpaceEvenly(Arrangement.SpaceEvenly),

    /**
     * Place children such that they are spaced evenly across the main axis, without free
     * space before the first child or after the last child.
     */
    SpaceBetween(Arrangement.SpaceBetween),

    /**
     * Place children such that they are spaced evenly across the main axis, including free
     * space before the first child and after the last child, but half the amount of space
     * existing otherwise between two consecutive children.
     */
    SpaceAround(Arrangement.SpaceAround);
}

/**
 * Used to specify the alignment of a layout's children, in cross axis direction.
 */
// TODO(popam): refine this API surface with modifiers - add type safety for alignment orientation.
@Immutable
sealed class CrossAxisAlignment {
    /**
     * Aligns to [size]. If this is a vertical alignment, [layoutDirection] should be
     * [LayoutDirection.Ltr].
     *
     * @param size The remaining space (total size - content size) in the container.
     * @param layoutDirection The layout direction of the content if horizontal or
     * [LayoutDirection.Ltr] if vertical.
     * @param placeable The item being aligned.
     * @param beforeCrossAxisAlignmentLine The space before the cross-axis alignment line if
     * an alignment line is being used or 0 if no alignment line is being used.
     */
    internal abstract fun align(
        size: Int,
        layoutDirection: LayoutDirection,
        placeable: Placeable,
        beforeCrossAxisAlignmentLine: Int
    ): Int

    /**
     * Returns `true` if this is [Relative].
     */
    internal open val isRelative: Boolean
        get() = false

    /**
     * Returns the alignment line position relative to the left/top of the space or `null` if
     * this alignment doesn't rely on alignment lines.
     */
    internal open fun calculateAlignmentLinePosition(placeable: Placeable): Int? = null

    companion object {
        /**
         * Place children such that their center is in the middle of the cross axis.
         */
        @Stable
        val Center: CrossAxisAlignment = CenterCrossAxisAlignment
        /**
         * Place children such that their start edge is aligned to the start edge of the cross
         * axis. TODO(popam): Consider rtl directionality.
         */
        @Stable
        val Start: CrossAxisAlignment = StartCrossAxisAlignment
        /**
         * Place children such that their end edge is aligned to the end edge of the cross
         * axis. TODO(popam): Consider rtl directionality.
         */
        @Stable
        val End: CrossAxisAlignment = EndCrossAxisAlignment

        /**
         * Align children by their baseline.
         */
        fun AlignmentLine(alignmentLine: AlignmentLine): CrossAxisAlignment =
            AlignmentLineCrossAxisAlignment(AlignmentLineProvider.Value(alignmentLine))

        /**
         * Align children relative to their siblings using the alignment line provided as a
         * parameter using [AlignmentLineProvider].
         */
        internal fun Relative(alignmentLineProvider: AlignmentLineProvider): CrossAxisAlignment =
            AlignmentLineCrossAxisAlignment(alignmentLineProvider)

        /**
         * Align children with vertical alignment.
         */
        internal fun vertical(vertical: Alignment.Vertical): CrossAxisAlignment =
            VerticalCrossAxisAlignment(vertical)

        /**
         * Align children with horizontal alignment.
         */
        internal fun horizontal(horizontal: Alignment.Horizontal): CrossAxisAlignment =
            HorizontalCrossAxisAlignment(horizontal)
    }

    private object CenterCrossAxisAlignment : CrossAxisAlignment() {
        override fun align(
            size: Int,
            layoutDirection: LayoutDirection,
            placeable: Placeable,
            beforeCrossAxisAlignmentLine: Int
        ): Int {
            return size / 2
        }
    }

    private object StartCrossAxisAlignment : CrossAxisAlignment() {
        override fun align(
            size: Int,
            layoutDirection: LayoutDirection,
            placeable: Placeable,
            beforeCrossAxisAlignmentLine: Int
        ): Int {
            return if (layoutDirection == LayoutDirection.Ltr) 0 else size
        }
    }

    private object EndCrossAxisAlignment : CrossAxisAlignment() {
        override fun align(
            size: Int,
            layoutDirection: LayoutDirection,
            placeable: Placeable,
            beforeCrossAxisAlignmentLine: Int
        ): Int {
            return if (layoutDirection == LayoutDirection.Ltr) size else 0
        }
    }

    private class AlignmentLineCrossAxisAlignment(
        val alignmentLineProvider: AlignmentLineProvider
    ) : CrossAxisAlignment() {
        override val isRelative: Boolean
            get() = true

        override fun calculateAlignmentLinePosition(placeable: Placeable): Int? {
            return alignmentLineProvider.calculateAlignmentLinePosition(placeable)
        }

        override fun align(
            size: Int,
            layoutDirection: LayoutDirection,
            placeable: Placeable,
            beforeCrossAxisAlignmentLine: Int
        ): Int {
            val alignmentLinePosition =
                alignmentLineProvider.calculateAlignmentLinePosition(placeable)
            return if (alignmentLinePosition != null) {
                val line = beforeCrossAxisAlignmentLine - alignmentLinePosition
                if (layoutDirection == LayoutDirection.Rtl) {
                    size - line
                } else {
                    line
                }
            } else {
                0
            }
        }
    }

    private class VerticalCrossAxisAlignment(
        val vertical: Alignment.Vertical
    ) : CrossAxisAlignment() {
        override fun align(
            size: Int,
            layoutDirection: LayoutDirection,
            placeable: Placeable,
            beforeCrossAxisAlignmentLine: Int
        ): Int {
            return vertical.align(size)
        }
    }

    private class HorizontalCrossAxisAlignment(
        val horizontal: Alignment.Horizontal
    ) : CrossAxisAlignment() {
        override fun align(
            size: Int,
            layoutDirection: LayoutDirection,
            placeable: Placeable,
            beforeCrossAxisAlignmentLine: Int
        ): Int {
            return horizontal.align(size, layoutDirection)
        }
    }
}

/**
 * Box [Constraints], but which abstract away width and height in favor of main axis and cross axis.
 */
internal data class OrientationIndependentConstraints(
    val mainAxisMin: Int,
    val mainAxisMax: Int,
    val crossAxisMin: Int,
    val crossAxisMax: Int
) {
    constructor(c: Constraints, orientation: LayoutOrientation) : this(
        if (orientation === LayoutOrientation.Horizontal) c.minWidth else c.minHeight,
        if (orientation === LayoutOrientation.Horizontal) c.maxWidth else c.maxHeight,
        if (orientation === LayoutOrientation.Horizontal) c.minHeight else c.minWidth,
        if (orientation === LayoutOrientation.Horizontal) c.maxHeight else c.maxWidth
    )

    // Creates a new instance with the same main axis constraints and maximum tight cross axis.
    fun stretchCrossAxis() = OrientationIndependentConstraints(
        mainAxisMin,
        mainAxisMax,
        if (crossAxisMax != Constraints.Infinity) crossAxisMax else crossAxisMin,
        crossAxisMax
    )

    // Given an orientation, resolves the current instance to traditional constraints.
    fun toBoxConstraints(orientation: LayoutOrientation) =
        if (orientation === LayoutOrientation.Horizontal) {
            Constraints(mainAxisMin, mainAxisMax, crossAxisMin, crossAxisMax)
        } else {
            Constraints(crossAxisMin, crossAxisMax, mainAxisMin, mainAxisMax)
        }

    // Given an orientation, resolves the max width constraint this instance represents.
    fun maxWidth(orientation: LayoutOrientation) =
        if (orientation === LayoutOrientation.Horizontal) {
            mainAxisMax
        } else {
            crossAxisMax
        }

    // Given an orientation, resolves the max height constraint this instance represents.
    fun maxHeight(orientation: LayoutOrientation) =
        if (orientation === LayoutOrientation.Horizontal) {
            crossAxisMax
        } else {
            mainAxisMax
        }
}

private val IntrinsicMeasurable.data: RowColumnParentData?
    get() = parentData as? RowColumnParentData

private val RowColumnParentData?.weight: Float
    get() = this?.weight ?: 0f

private val RowColumnParentData?.fill: Boolean
    get() = this?.fill ?: true

private val RowColumnParentData?.crossAxisAlignment: CrossAxisAlignment?
    get() = this?.crossAxisAlignment

private val RowColumnParentData?.isRelative: Boolean
    get() = this.crossAxisAlignment?.isRelative ?: false

private /*inline*/ fun MinIntrinsicWidthMeasureBlock(orientation: LayoutOrientation) =
    if (orientation == LayoutOrientation.Horizontal) {
        IntrinsicMeasureBlocks.HorizontalMinWidth
    } else {
        IntrinsicMeasureBlocks.VerticalMinWidth
    }

private /*inline*/ fun MinIntrinsicHeightMeasureBlock(orientation: LayoutOrientation) =
    if (orientation == LayoutOrientation.Horizontal) {
        IntrinsicMeasureBlocks.HorizontalMinHeight
    } else {
        IntrinsicMeasureBlocks.VerticalMinHeight
    }

private /*inline*/ fun MaxIntrinsicWidthMeasureBlock(orientation: LayoutOrientation) =
    if (orientation == LayoutOrientation.Horizontal) {
        IntrinsicMeasureBlocks.HorizontalMaxWidth
    } else {
        IntrinsicMeasureBlocks.VerticalMaxWidth
    }

private /*inline*/ fun MaxIntrinsicHeightMeasureBlock(orientation: LayoutOrientation) =
    if (orientation == LayoutOrientation.Horizontal) {
        IntrinsicMeasureBlocks.HorizontalMaxHeight
    } else {
        IntrinsicMeasureBlocks.VerticalMaxHeight
    }

private object IntrinsicMeasureBlocks {
    val HorizontalMinWidth: IntrinsicMeasureBlock = { measurables, availableHeight ->
        intrinsicSize(
            measurables,
            { h -> minIntrinsicWidth(h) },
            { w -> maxIntrinsicHeight(w) },
            availableHeight,
            LayoutOrientation.Horizontal,
            LayoutOrientation.Horizontal
        )
    }
    val VerticalMinWidth: IntrinsicMeasureBlock = { measurables, availableHeight ->
        intrinsicSize(
            measurables,
            { h -> minIntrinsicWidth(h) },
            { w -> maxIntrinsicHeight(w) },
            availableHeight,
            LayoutOrientation.Vertical,
            LayoutOrientation.Horizontal
        )
    }
    val HorizontalMinHeight: IntrinsicMeasureBlock = { measurables, availableWidth ->
        intrinsicSize(
            measurables,
            { w -> minIntrinsicHeight(w) },
            { h -> maxIntrinsicWidth(h) },
            availableWidth,
            LayoutOrientation.Horizontal,
            LayoutOrientation.Vertical
        )
    }
    val VerticalMinHeight: IntrinsicMeasureBlock = { measurables, availableWidth ->
        intrinsicSize(
            measurables,
            { w -> minIntrinsicHeight(w) },
            { h -> maxIntrinsicWidth(h) },
            availableWidth,
            LayoutOrientation.Vertical,
            LayoutOrientation.Vertical
        )
    }
    val HorizontalMaxWidth: IntrinsicMeasureBlock = { measurables, availableHeight ->
        intrinsicSize(
            measurables,
            { h -> maxIntrinsicWidth(h) },
            { w -> maxIntrinsicHeight(w) },
            availableHeight,
            LayoutOrientation.Horizontal,
            LayoutOrientation.Horizontal
        )
    }
    val VerticalMaxWidth: IntrinsicMeasureBlock = { measurables, availableHeight ->
        intrinsicSize(
            measurables,
            { h -> maxIntrinsicWidth(h) },
            { w -> maxIntrinsicHeight(w) },
            availableHeight,
            LayoutOrientation.Vertical,
            LayoutOrientation.Horizontal
        )
    }
    val HorizontalMaxHeight: IntrinsicMeasureBlock = { measurables, availableWidth ->
        intrinsicSize(
            measurables,
            { w -> maxIntrinsicHeight(w) },
            { h -> maxIntrinsicWidth(h) },
            availableWidth,
            LayoutOrientation.Horizontal,
            LayoutOrientation.Vertical
        )
    }
    val VerticalMaxHeight: IntrinsicMeasureBlock = { measurables, availableWidth ->
        intrinsicSize(
            measurables,
            { w -> maxIntrinsicHeight(w) },
            { h -> maxIntrinsicWidth(h) },
            availableWidth,
            LayoutOrientation.Vertical,
            LayoutOrientation.Vertical
        )
    }
}

private fun intrinsicSize(
    children: List<IntrinsicMeasurable>,
    intrinsicMainSize: IntrinsicMeasurable.(Int) -> Int,
    intrinsicCrossSize: IntrinsicMeasurable.(Int) -> Int,
    crossAxisAvailable: Int,
    layoutOrientation: LayoutOrientation,
    intrinsicOrientation: LayoutOrientation
) = if (layoutOrientation == intrinsicOrientation) {
    intrinsicMainAxisSize(children, intrinsicMainSize, crossAxisAvailable)
} else {
    intrinsicCrossAxisSize(children, intrinsicCrossSize, intrinsicMainSize, crossAxisAvailable)
}

private fun intrinsicMainAxisSize(
    children: List<IntrinsicMeasurable>,
    mainAxisSize: IntrinsicMeasurable.(Int) -> Int,
    crossAxisAvailable: Int
): Int {
    var weightUnitSpace = 0
    var fixedSpace = 0
    var totalWeight = 0f
    children.fastForEach { child ->
        val weight = child.data.weight
        val size = child.mainAxisSize(crossAxisAvailable)
        if (weight == 0f) {
            fixedSpace += size
        } else if (weight > 0f) {
            totalWeight += weight
            weightUnitSpace = max(weightUnitSpace, (size / weight).roundToInt())
        }
    }
    return (weightUnitSpace * totalWeight).roundToInt() + fixedSpace
}

private fun intrinsicCrossAxisSize(
    children: List<IntrinsicMeasurable>,
    mainAxisSize: IntrinsicMeasurable.(Int) -> Int,
    crossAxisSize: IntrinsicMeasurable.(Int) -> Int,
    mainAxisAvailable: Int
): Int {
    var fixedSpace = 0
    var crossAxisMax = 0
    var totalWeight = 0f
    children.fastForEach { child ->
        val weight = child.data.weight
        if (weight == 0f) {
            // Ask the child how much main axis space it wants to occupy. This cannot be more
            // than the remaining available space.
            val mainAxisSpace = min(
                child.mainAxisSize(Constraints.Infinity),
                mainAxisAvailable - fixedSpace
            )
            fixedSpace += mainAxisSpace
            // Now that the assigned main axis space is known, ask about the cross axis space.
            crossAxisMax = max(crossAxisMax, child.crossAxisSize(mainAxisSpace))
        } else if (weight > 0f) {
            totalWeight += weight
        }
    }

    // For weighted children, calculate how much main axis space weight=1 would represent.
    val weightUnitSpace = if (totalWeight == 0f) {
        0
    } else if (mainAxisAvailable == Constraints.Infinity) {
        Constraints.Infinity
    } else {
        (max(mainAxisAvailable - fixedSpace, 0) / totalWeight).roundToInt()
    }

    children.fastForEach { child ->
        val weight = child.data.weight
        // Now the main axis for weighted children is known, so ask about the cross axis space.
        if (weight > 0f) {
            crossAxisMax = max(
                crossAxisMax,
                child.crossAxisSize((weightUnitSpace * weight).roundToInt())
            )
        }
    }
    return crossAxisMax
}

internal data class LayoutWeightImpl(val weight: Float, val fill: Boolean) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?) =
        ((parentData as? RowColumnParentData) ?: RowColumnParentData()).also {
            it.weight = weight
            it.fill = fill
        }
}

internal sealed class SiblingsAlignedModifier : ParentDataModifier {
    abstract override fun Density.modifyParentData(parentData: Any?): Any?

    internal data class WithAlignmentLineBlock(val block: (Measured) -> Int) :
        SiblingsAlignedModifier() {
        override fun Density.modifyParentData(parentData: Any?): Any? {
            return ((parentData as? RowColumnParentData) ?: RowColumnParentData()).also {
                it.crossAxisAlignment =
                    CrossAxisAlignment.Relative(AlignmentLineProvider.Block(block))
            }
        }
    }

    internal data class WithAlignmentLine(val line: AlignmentLine) :
        SiblingsAlignedModifier() {
        override fun Density.modifyParentData(parentData: Any?): Any? {
            return ((parentData as? RowColumnParentData) ?: RowColumnParentData()).also {
                it.crossAxisAlignment =
                    CrossAxisAlignment.Relative(AlignmentLineProvider.Value(line))
            }
        }
    }
}

internal data class HorizontalGravityModifier(
    val horizontal: Alignment.Horizontal
) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?): RowColumnParentData {
        return ((parentData as? RowColumnParentData) ?: RowColumnParentData()).also {
            it.crossAxisAlignment = CrossAxisAlignment.horizontal(horizontal)
        }
    }
}

internal data class VerticalGravityModifier(
    val vertical: Alignment.Vertical
) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?): RowColumnParentData {
        return ((parentData as? RowColumnParentData) ?: RowColumnParentData()).also {
            it.crossAxisAlignment = CrossAxisAlignment.vertical(vertical)
        }
    }
}

/**
 * Parent data associated with children.
 */
internal data class RowColumnParentData(
    var weight: Float = 0f,
    var fill: Boolean = true,
    var crossAxisAlignment: CrossAxisAlignment? = null
)

/**
 * Provides the alignment line.
 */
internal sealed class AlignmentLineProvider {
    abstract fun calculateAlignmentLinePosition(placeable: Placeable): Int?
    data class Block(val lineProviderBlock: (Measured) -> Int) : AlignmentLineProvider() {
        override fun calculateAlignmentLinePosition(
            placeable: Placeable
        ): Int? {
            return lineProviderBlock(Measured(placeable))
        }
    }

    data class Value(val line: AlignmentLine) : AlignmentLineProvider() {
        override fun calculateAlignmentLinePosition(placeable: Placeable): Int? {
            return placeable[line]
        }
    }
}
