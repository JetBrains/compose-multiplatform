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

import androidx.compose.foundation.layout.LayoutOrientation.Horizontal
import androidx.compose.foundation.layout.LayoutOrientation.Vertical
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Measured
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.InspectorValueInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastForEach
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign

internal fun rowColumnMeasurePolicy(
    orientation: LayoutOrientation,
    arrangement: (Int, IntArray, LayoutDirection, Density, IntArray) -> Unit,
    arrangementSpacing: Dp,
    crossAxisSize: SizeMode,
    crossAxisAlignment: CrossAxisAlignment
): MeasurePolicy {
    fun Placeable.mainAxisSize() =
        if (orientation == LayoutOrientation.Horizontal) width else height

    fun Placeable.crossAxisSize() =
        if (orientation == LayoutOrientation.Horizontal) height else width

    return object : MeasurePolicy {
        override fun MeasureScope.measure(
            measurables: List<Measurable>,
            constraints: Constraints
        ): MeasureResult {
            @Suppress("NAME_SHADOWING")
            val constraints = OrientationIndependentConstraints(constraints, orientation)
            val arrangementSpacingPx = arrangementSpacing.roundToPx()

            var totalWeight = 0f
            var fixedSpace = 0
            var crossAxisSpace = 0
            var weightChildrenCount = 0

            var anyAlignBy = false
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
                    anyAlignBy = anyAlignBy || parentData.isRelative
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
                var remainder = remainingToTarget - rowColumnParentData.sumOf {
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
                        anyAlignBy = anyAlignBy || parentData.isRelative
                        placeables[i] = placeable
                    }
                }
                weightedSpace = (weightedSpace + arrangementSpacingPx * (weightChildrenCount - 1))
                    .coerceAtMost(constraints.mainAxisMax - fixedSpace)
            }

            var beforeCrossAxisAlignmentLine = 0
            var afterCrossAxisAlignmentLine = 0
            if (anyAlignBy) {
                for (i in placeables.indices) {
                    val placeable = placeables[i]!!
                    val parentData = rowColumnParentData[i]
                    val alignmentLinePosition = parentData.crossAxisAlignment
                        ?.calculateAlignmentLinePosition(placeable)
                    if (alignmentLinePosition != null) {
                        beforeCrossAxisAlignmentLine = max(
                            beforeCrossAxisAlignmentLine,
                            alignmentLinePosition.let {
                                if (it != AlignmentLine.Unspecified) it else 0
                            }
                        )
                        afterCrossAxisAlignmentLine = max(
                            afterCrossAxisAlignmentLine,
                            placeable.crossAxisSize() -
                                (
                                    alignmentLinePosition.let {
                                        if (it != AlignmentLine.Unspecified) {
                                            it
                                        } else {
                                            placeable.crossAxisSize()
                                        }
                                    }
                                    )
                        )
                    }
                }
            }

            // Compute the Row or Column size and position the children.
            val mainAxisLayoutSize = max(fixedSpace + weightedSpace, constraints.mainAxisMin)
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
            val layoutWidth = if (orientation == Horizontal) {
                mainAxisLayoutSize
            } else {
                crossAxisLayoutSize
            }
            val layoutHeight = if (orientation == Horizontal) {
                crossAxisLayoutSize
            } else {
                mainAxisLayoutSize
            }

            val mainAxisPositions = IntArray(measurables.size) { 0 }
            return layout(layoutWidth, layoutHeight) {
                val childrenMainAxisSize = IntArray(measurables.size) { index ->
                    placeables[index]!!.mainAxisSize()
                }
                arrangement(
                    mainAxisLayoutSize,
                    childrenMainAxisSize,
                    layoutDirection,
                    this@measure,
                    mainAxisPositions
                )

                placeables.forEachIndexed { index, placeable ->
                    placeable!!
                    val parentData = rowColumnParentData[index]
                    val childCrossAlignment = parentData.crossAxisAlignment ?: crossAxisAlignment

                    val crossAxis = childCrossAlignment.align(
                        size = crossAxisLayoutSize - placeable.crossAxisSize(),
                        layoutDirection = if (orientation == Horizontal) {
                            LayoutDirection.Ltr
                        } else {
                            layoutDirection
                        },
                        placeable = placeable,
                        beforeCrossAxisAlignmentLine = beforeCrossAxisAlignmentLine
                    )

                    if (orientation == Horizontal) {
                        placeable.place(mainAxisPositions[index], crossAxis)
                    } else {
                        placeable.place(crossAxis, mainAxisPositions[index])
                    }
                }
            }
        }

        override fun IntrinsicMeasureScope.minIntrinsicWidth(
            measurables: List<IntrinsicMeasurable>,
            height: Int
        ) = MinIntrinsicWidthMeasureBlock(orientation)(
            measurables,
            height,
            arrangementSpacing.roundToPx()
        )

        override fun IntrinsicMeasureScope.minIntrinsicHeight(
            measurables: List<IntrinsicMeasurable>,
            width: Int
        ) = MinIntrinsicHeightMeasureBlock(orientation)(
            measurables,
            width,
            arrangementSpacing.roundToPx()
        )

        override fun IntrinsicMeasureScope.maxIntrinsicWidth(
            measurables: List<IntrinsicMeasurable>,
            height: Int
        ) = MaxIntrinsicWidthMeasureBlock(orientation)(
            measurables,
            height,
            arrangementSpacing.roundToPx()
        )

        override fun IntrinsicMeasureScope.maxIntrinsicHeight(
            measurables: List<IntrinsicMeasurable>,
            width: Int
        ) = MaxIntrinsicHeightMeasureBlock(orientation)(
            measurables,
            width,
            arrangementSpacing.roundToPx()
        )
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
 * Used to specify the alignment of a layout's children, in cross axis direction.
 */
@Immutable
internal sealed class CrossAxisAlignment {
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

        override fun calculateAlignmentLinePosition(placeable: Placeable): Int {
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
            return if (alignmentLinePosition != AlignmentLine.Unspecified) {
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
            return vertical.align(0, size)
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
            return horizontal.align(0, size, layoutDirection)
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

private fun MinIntrinsicWidthMeasureBlock(orientation: LayoutOrientation) =
    if (orientation == LayoutOrientation.Horizontal) {
        IntrinsicMeasureBlocks.HorizontalMinWidth
    } else {
        IntrinsicMeasureBlocks.VerticalMinWidth
    }

private fun MinIntrinsicHeightMeasureBlock(orientation: LayoutOrientation) =
    if (orientation == LayoutOrientation.Horizontal) {
        IntrinsicMeasureBlocks.HorizontalMinHeight
    } else {
        IntrinsicMeasureBlocks.VerticalMinHeight
    }

private fun MaxIntrinsicWidthMeasureBlock(orientation: LayoutOrientation) =
    if (orientation == LayoutOrientation.Horizontal) {
        IntrinsicMeasureBlocks.HorizontalMaxWidth
    } else {
        IntrinsicMeasureBlocks.VerticalMaxWidth
    }

private fun MaxIntrinsicHeightMeasureBlock(orientation: LayoutOrientation) =
    if (orientation == LayoutOrientation.Horizontal) {
        IntrinsicMeasureBlocks.HorizontalMaxHeight
    } else {
        IntrinsicMeasureBlocks.VerticalMaxHeight
    }

private object IntrinsicMeasureBlocks {
    val HorizontalMinWidth: (List<IntrinsicMeasurable>, Int, Int) -> Int =
        { measurables, availableHeight, mainAxisSpacing ->
            intrinsicSize(
                measurables,
                { h -> minIntrinsicWidth(h) },
                { w -> maxIntrinsicHeight(w) },
                availableHeight,
                mainAxisSpacing,
                LayoutOrientation.Horizontal,
                LayoutOrientation.Horizontal
            )
        }
    val VerticalMinWidth: (List<IntrinsicMeasurable>, Int, Int) -> Int =
        { measurables, availableHeight, mainAxisSpacing ->
            intrinsicSize(
                measurables,
                { h -> minIntrinsicWidth(h) },
                { w -> maxIntrinsicHeight(w) },
                availableHeight,
                mainAxisSpacing,
                LayoutOrientation.Vertical,
                LayoutOrientation.Horizontal
            )
        }
    val HorizontalMinHeight: (List<IntrinsicMeasurable>, Int, Int) -> Int =
        { measurables, availableWidth, mainAxisSpacing ->
            intrinsicSize(
                measurables,
                { w -> minIntrinsicHeight(w) },
                { h -> maxIntrinsicWidth(h) },
                availableWidth,
                mainAxisSpacing,
                LayoutOrientation.Horizontal,
                LayoutOrientation.Vertical
            )
        }
    val VerticalMinHeight: (List<IntrinsicMeasurable>, Int, Int) -> Int =
        { measurables, availableWidth, mainAxisSpacing ->
            intrinsicSize(
                measurables,
                { w -> minIntrinsicHeight(w) },
                { h -> maxIntrinsicWidth(h) },
                availableWidth,
                mainAxisSpacing,
                LayoutOrientation.Vertical,
                LayoutOrientation.Vertical
            )
        }
    val HorizontalMaxWidth: (List<IntrinsicMeasurable>, Int, Int) -> Int =
        { measurables, availableHeight, mainAxisSpacing ->
            intrinsicSize(
                measurables,
                { h -> maxIntrinsicWidth(h) },
                { w -> maxIntrinsicHeight(w) },
                availableHeight,
                mainAxisSpacing,
                LayoutOrientation.Horizontal,
                LayoutOrientation.Horizontal
            )
        }
    val VerticalMaxWidth: (List<IntrinsicMeasurable>, Int, Int) -> Int =
        { measurables, availableHeight, mainAxisSpacing ->
            intrinsicSize(
                measurables,
                { h -> maxIntrinsicWidth(h) },
                { w -> maxIntrinsicHeight(w) },
                availableHeight,
                mainAxisSpacing,
                LayoutOrientation.Vertical,
                LayoutOrientation.Horizontal
            )
        }
    val HorizontalMaxHeight: (List<IntrinsicMeasurable>, Int, Int) -> Int =
        { measurables, availableWidth, mainAxisSpacing ->
            intrinsicSize(
                measurables,
                { w -> maxIntrinsicHeight(w) },
                { h -> maxIntrinsicWidth(h) },
                availableWidth,
                mainAxisSpacing,
                LayoutOrientation.Horizontal,
                LayoutOrientation.Vertical
            )
        }
    val VerticalMaxHeight: (List<IntrinsicMeasurable>, Int, Int) -> Int =
        { measurables, availableWidth, mainAxisSpacing ->
            intrinsicSize(
                measurables,
                { w -> maxIntrinsicHeight(w) },
                { h -> maxIntrinsicWidth(h) },
                availableWidth,
                mainAxisSpacing,
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
    mainAxisSpacing: Int,
    layoutOrientation: LayoutOrientation,
    intrinsicOrientation: LayoutOrientation
) = if (layoutOrientation == intrinsicOrientation) {
    intrinsicMainAxisSize(children, intrinsicMainSize, crossAxisAvailable, mainAxisSpacing)
} else {
    intrinsicCrossAxisSize(
        children,
        intrinsicCrossSize,
        intrinsicMainSize,
        crossAxisAvailable,
        mainAxisSpacing
    )
}

private fun intrinsicMainAxisSize(
    children: List<IntrinsicMeasurable>,
    mainAxisSize: IntrinsicMeasurable.(Int) -> Int,
    crossAxisAvailable: Int,
    mainAxisSpacing: Int
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
    return (weightUnitSpace * totalWeight).roundToInt() + fixedSpace +
        (children.size - 1) * mainAxisSpacing
}

private fun intrinsicCrossAxisSize(
    children: List<IntrinsicMeasurable>,
    mainAxisSize: IntrinsicMeasurable.(Int) -> Int,
    crossAxisSize: IntrinsicMeasurable.(Int) -> Int,
    mainAxisAvailable: Int,
    mainAxisSpacing: Int
): Int {
    var fixedSpace = min((children.size - 1) * mainAxisSpacing, mainAxisAvailable)
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
                child.crossAxisSize(
                    if (weightUnitSpace != Constraints.Infinity) {
                        (weightUnitSpace * weight).roundToInt()
                    } else {
                        Constraints.Infinity
                    }
                )
            )
        }
    }
    return crossAxisMax
}

internal class LayoutWeightImpl(
    val weight: Float,
    val fill: Boolean,
    inspectorInfo: InspectorInfo.() -> Unit
) : ParentDataModifier, InspectorValueInfo(inspectorInfo) {
    override fun Density.modifyParentData(parentData: Any?) =
        ((parentData as? RowColumnParentData) ?: RowColumnParentData()).also {
            it.weight = weight
            it.fill = fill
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherModifier = other as? LayoutWeightImpl ?: return false
        return weight == otherModifier.weight &&
            fill == otherModifier.fill
    }

    override fun hashCode(): Int {
        var result = weight.hashCode()
        result = 31 * result + fill.hashCode()
        return result
    }

    override fun toString(): String =
        "LayoutWeightImpl(weight=$weight, fill=$fill)"
}

internal sealed class SiblingsAlignedModifier(
    inspectorInfo: InspectorInfo.() -> Unit
) : ParentDataModifier, InspectorValueInfo(inspectorInfo) {
    abstract override fun Density.modifyParentData(parentData: Any?): Any?

    internal class WithAlignmentLineBlock(
        val block: (Measured) -> Int,
        inspectorInfo: InspectorInfo.() -> Unit
    ) : SiblingsAlignedModifier(inspectorInfo) {
        override fun Density.modifyParentData(parentData: Any?): Any {
            return ((parentData as? RowColumnParentData) ?: RowColumnParentData()).also {
                it.crossAxisAlignment =
                    CrossAxisAlignment.Relative(AlignmentLineProvider.Block(block))
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            val otherModifier = other as? WithAlignmentLineBlock ?: return false
            return block == otherModifier.block
        }

        override fun hashCode(): Int = block.hashCode()

        override fun toString(): String = "WithAlignmentLineBlock(block=$block)"
    }

    internal class WithAlignmentLine(
        val alignmentLine: AlignmentLine,
        inspectorInfo: InspectorInfo.() -> Unit
    ) : SiblingsAlignedModifier(inspectorInfo) {
        override fun Density.modifyParentData(parentData: Any?): Any {
            return ((parentData as? RowColumnParentData) ?: RowColumnParentData()).also {
                it.crossAxisAlignment =
                    CrossAxisAlignment.Relative(AlignmentLineProvider.Value(alignmentLine))
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            val otherModifier = other as? WithAlignmentLine ?: return false
            return alignmentLine == otherModifier.alignmentLine
        }

        override fun hashCode(): Int = alignmentLine.hashCode()

        override fun toString(): String = "WithAlignmentLine(line=$alignmentLine)"
    }
}

internal class HorizontalAlignModifier(
    val horizontal: Alignment.Horizontal,
    inspectorInfo: InspectorInfo.() -> Unit
) : ParentDataModifier, InspectorValueInfo(inspectorInfo) {
    override fun Density.modifyParentData(parentData: Any?): RowColumnParentData {
        return ((parentData as? RowColumnParentData) ?: RowColumnParentData()).also {
            it.crossAxisAlignment = CrossAxisAlignment.horizontal(horizontal)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherModifier = other as? HorizontalAlignModifier ?: return false
        return horizontal == otherModifier.horizontal
    }

    override fun hashCode(): Int = horizontal.hashCode()

    override fun toString(): String =
        "HorizontalAlignModifier(horizontal=$horizontal)"
}

internal class VerticalAlignModifier(
    val vertical: Alignment.Vertical,
    inspectorInfo: InspectorInfo.() -> Unit
) : ParentDataModifier, InspectorValueInfo(inspectorInfo) {
    override fun Density.modifyParentData(parentData: Any?): RowColumnParentData {
        return ((parentData as? RowColumnParentData) ?: RowColumnParentData()).also {
            it.crossAxisAlignment = CrossAxisAlignment.vertical(vertical)
        }
    }
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherModifier = other as? VerticalAlignModifier ?: return false
        return vertical == otherModifier.vertical
    }

    override fun hashCode(): Int = vertical.hashCode()

    override fun toString(): String =
        "VerticalAlignModifier(vertical=$vertical)"
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
    abstract fun calculateAlignmentLinePosition(placeable: Placeable): Int
    data class Block(val lineProviderBlock: (Measured) -> Int) : AlignmentLineProvider() {
        override fun calculateAlignmentLinePosition(
            placeable: Placeable
        ): Int {
            return lineProviderBlock(placeable)
        }
    }

    data class Value(val alignmentLine: AlignmentLine) : AlignmentLineProvider() {
        override fun calculateAlignmentLinePosition(placeable: Placeable): Int {
            return placeable[alignmentLine]
        }
    }
}

/**
 * Used to specify how a layout chooses its own size when multiple behaviors are possible.
 */
// TODO(popam): remove this when Flow is reworked
internal enum class SizeMode {
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
