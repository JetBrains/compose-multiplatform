package androidx.compose.foundation.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collection.MutableVector
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.util.fastForEachIndexed
import kotlin.math.ceil
import kotlin.math.max

/**
 * [FlowRow] is a layout that fills items from left to right (ltr) in LTR layouts
 * or right to left (rtl) in RTL layouts and when it runs out of space, moves to
 * the next "row" or "line" positioned on the bottom, and then continues filling items
 * until the items run out.
 *
 * Example:
 * @sample androidx.compose.foundation.layout.samples.SimpleFlowRow
 *
 * When a Modifier [RowScope.weight] is provided, it scales the item
 * based on the number items that fall on the row it was placed in.
 *
 * Example:
 * @sample androidx.compose.foundation.layout.samples.SimpleFlowRowWithWeights
 *
 *
 * @param modifier The modifier to be applied to the Row.
 * @param horizontalArrangement The horizontal arrangement of the layout's children.
 * @param verticalAlignment The vertical alignment of the layout's children.
 * @param maxItemsInEachRow The maximum number of items per row
 * @param content The content as a [RowScope]
 *
 * @see FlowColumn
 * @see [androidx.compose.foundation.layout.Row]
 */
@Composable
@ExperimentalLayoutApi
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    maxItemsInEachRow: Int = Int.MAX_VALUE,
    content: @Composable RowScope.() -> Unit
) {
    val measurePolicy = rowMeasurementHelper(
        horizontalArrangement,
        verticalAlignment,
        maxItemsInEachRow
    )
    Layout(
        content = { RowScopeInstance.content() },
        measurePolicy = measurePolicy,
        modifier = modifier
    )
}

/**
 * [FlowColumn] is a layout that fills items from top to bottom, and when it runs out of space
 * on the bottom, moves to the next "column" or "line"
 * on the right or left based on ltr or rtl layouts,
 * and then continues filling items from top to bottom.
 *
 * It supports ltr in LTR layouts, by placing the first column to the left, and then moving
 * to the right
 * It supports rtl in RTL layouts, by placing the first column to the right, and then moving
 * to the left
 *
 * Example:
 * @sample androidx.compose.foundation.layout.samples.SimpleFlowColumn
 *
 * When a Modifier [ColumnScope.weight] is provided, it scales the item
 * based on the number items that fall on the column it was placed in.
 *
 * Example:
 * @sample androidx.compose.foundation.layout.samples.SimpleFlowColumnWithWeights
 *
 * @param modifier The modifier to be applied to the Row.
 * @param verticalArrangement The vertical arrangement of the layout's children.
 * @param horizontalAlignment The horizontal alignment of the layout's children.
 * @param maxItemsInEachColumn The maximum number of items per column
 * @param content The content as a [ColumnScope]
 *
 * @see FlowRow
 * @see [androidx.compose.foundation.layout.Column]
 */
@Composable
@ExperimentalLayoutApi
fun FlowColumn(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    maxItemsInEachColumn: Int = Int.MAX_VALUE,
    content: @Composable ColumnScope.() -> Unit
) {
    val measurePolicy = columnMeasurementHelper(
        verticalArrangement,
        horizontalAlignment,
        maxItemsInEachColumn
    )
    Layout(
        content = { ColumnScopeInstance.content() },
        measurePolicy = measurePolicy,
        modifier = modifier
    )
}

@Composable
private fun mainAxisRowArrangement(horizontalArrangement: Arrangement.Horizontal):
        (Int, IntArray, LayoutDirection, Density, IntArray) -> Unit =
    remember(horizontalArrangement) {
        { totalSize, size, layoutDirection, density, outPosition ->
            with(horizontalArrangement) {
                density.arrange(totalSize, size, layoutDirection, outPosition)
            }
        }
    }

@Composable
private fun mainAxisColumnArrangement(verticalArrangement: Arrangement.Vertical):
        (Int, IntArray, LayoutDirection, Density, IntArray) -> Unit =
    remember(verticalArrangement) {
        { totalSize, size, _, density, outPosition ->
            with(verticalArrangement) {
                density.arrange(totalSize, size, outPosition)
            }
        }
    }

private val crossAxisRowArrangement = { totalSize: Int, size: IntArray,
    measureScope: MeasureScope,
    outPosition: IntArray ->
    with(Arrangement.Top) { measureScope.arrange(totalSize, size, outPosition) }
}

private val crossAxisColumnArrangement = { totalSize: Int,
    size: IntArray, measureScope: MeasureScope, outPosition: IntArray ->
    with(Arrangement.Start) {
        measureScope.arrange(totalSize, size, measureScope.layoutDirection, outPosition)
    }
}

@Composable
private fun rowMeasurementHelper(
    horizontalArrangement: Arrangement.Horizontal = Arrangement.End,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    maxItemsInMainAxis: Int,
): MeasurePolicy {
    val mainAxisArrangement = mainAxisRowArrangement(horizontalArrangement)
    val crossAxisAlignment = remember(verticalAlignment) {
        CrossAxisAlignment.vertical(verticalAlignment)
    }
    return remember(horizontalArrangement, verticalAlignment, maxItemsInMainAxis) {
        flowMeasurePolicy(
            orientation = LayoutOrientation.Horizontal,
            mainAxisArrangement = mainAxisArrangement,
            arrangementSpacing = horizontalArrangement.spacing,
            crossAxisAlignment = crossAxisAlignment,
            crossAxisSize = SizeMode.Wrap,
            crossAxisArrangement = crossAxisRowArrangement,
            maxItemsInMainAxis = maxItemsInMainAxis,
        )
    }
}

@Composable
private fun columnMeasurementHelper(
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    maxItemsInMainAxis: Int,
): MeasurePolicy {
    val mainAxisArrangement = mainAxisColumnArrangement(verticalArrangement)
    val crossAxisAlignment = remember(horizontalAlignment) {
        CrossAxisAlignment.horizontal(horizontalAlignment)
    }
    return remember(verticalArrangement, horizontalAlignment, maxItemsInMainAxis) {
        flowMeasurePolicy(
            orientation = LayoutOrientation.Vertical,
            mainAxisArrangement = mainAxisArrangement,
            arrangementSpacing = verticalArrangement.spacing,
            crossAxisAlignment = crossAxisAlignment,
            crossAxisArrangement = crossAxisColumnArrangement,
            maxItemsInMainAxis = maxItemsInMainAxis,
            crossAxisSize = SizeMode.Wrap
        )
    }
}

/**
 * Returns a Flow Measure Policy
 */
private fun flowMeasurePolicy(
    orientation: LayoutOrientation,
    mainAxisArrangement: (Int, IntArray, LayoutDirection, Density, IntArray) -> Unit,
    arrangementSpacing: Dp,
    crossAxisSize: SizeMode,
    crossAxisAlignment: CrossAxisAlignment,
    crossAxisArrangement: (Int, IntArray, MeasureScope, IntArray) -> Unit,
    maxItemsInMainAxis: Int,
): MeasurePolicy {

    return object : MeasurePolicy {

        override fun MeasureScope.measure(
            measurables: List<Measurable>,
            constraints: Constraints
        ): MeasureResult {
            val placeables: Array<Placeable?> = arrayOfNulls(measurables.size)
            val measureHelper = RowColumnMeasurementHelper(
                orientation,
                mainAxisArrangement,
                arrangementSpacing,
                crossAxisSize,
                crossAxisAlignment,
                measurables,
                placeables,
            )
            val orientationIndependentConstraints =
                OrientationIndependentConstraints(constraints, orientation)
            val flowResult = breakDownItems(
                measureHelper,
                orientation,
                orientationIndependentConstraints,
                maxItemsInMainAxis,
            )
            val totalCrossAxisSize = flowResult.crossAxisTotalSize
            val items = flowResult.items
            val crossAxisSizes = IntArray(items.size) { index ->
                items[index].crossAxisSize
            }
            val outPosition = IntArray(crossAxisSizes.size)
            crossAxisArrangement(
                totalCrossAxisSize,
                crossAxisSizes, this@measure, outPosition
            )

            var layoutWidth: Int
            var layoutHeight: Int
            if (orientation == LayoutOrientation.Horizontal) {
                layoutWidth = flowResult.mainAxisTotalSize
                layoutHeight = flowResult.crossAxisTotalSize
            } else {
                layoutWidth = flowResult.crossAxisTotalSize
                layoutHeight = flowResult.mainAxisTotalSize
            }
            layoutWidth = constraints.constrainWidth(layoutWidth)
            layoutHeight = constraints.constrainHeight(layoutHeight)

            return layout(layoutWidth, layoutHeight) {
                flowResult.items.forEachIndexed { currentRowOrColumnIndex,
                    measureResult ->
                    measureHelper.placeHelper(
                        this,
                        measureResult,
                        outPosition[currentRowOrColumnIndex],
                        this@measure.layoutDirection
                    )
                }
            }
        }

        override fun IntrinsicMeasureScope.minIntrinsicWidth(
            measurables: List<IntrinsicMeasurable>,
            height: Int
        ) = if (orientation == LayoutOrientation.Horizontal) {
            minIntrinsicMainAxisSize(
                measurables,
                height,
                arrangementSpacing.roundToPx(),
            )
        } else {
            intrinsicCrossAxisSize(
                measurables,
                height,
                arrangementSpacing.roundToPx(),
            )
        }

        override fun IntrinsicMeasureScope.minIntrinsicHeight(
            measurables: List<IntrinsicMeasurable>,
            width: Int
        ) = if (orientation == LayoutOrientation.Horizontal) {
            intrinsicCrossAxisSize(
                measurables,
                width,
                arrangementSpacing.roundToPx(),
            )
        } else {
            minIntrinsicMainAxisSize(
                measurables,
                width,
                arrangementSpacing.roundToPx(),
            )
        }

        override fun IntrinsicMeasureScope.maxIntrinsicHeight(
            measurables: List<IntrinsicMeasurable>,
            width: Int
        ) = if (orientation == LayoutOrientation.Horizontal) {
            intrinsicCrossAxisSize(
                measurables,
                width,
                arrangementSpacing.roundToPx(),
            )
        } else {
            maxIntrinsicMainAxisSize(
                measurables,
                width,
                arrangementSpacing.roundToPx(),
            )
        }

        override fun IntrinsicMeasureScope.maxIntrinsicWidth(
            measurables: List<IntrinsicMeasurable>,
            height: Int
        ) = if (orientation == LayoutOrientation.Horizontal) {
            maxIntrinsicMainAxisSize(
                measurables,
                height,
                arrangementSpacing.roundToPx(),
            )
        } else {
            intrinsicCrossAxisSize(
                measurables,
                height,
                arrangementSpacing.roundToPx(),
            )
        }

        fun minIntrinsicMainAxisSize(
            measurables: List<IntrinsicMeasurable>,
            crossAxisAvailable: Int,
            arrangementSpacing: Int
        ) = minIntrinsicMainAxisSize(
            measurables,
            mainAxisSize = minMainAxisIntrinsicItemSize,
            crossAxisSize = minCrossAxisIntrinsicItemSize,
            crossAxisAvailable,
            arrangementSpacing,
            maxItemsInMainAxis
        )

        fun maxIntrinsicMainAxisSize(
            measurables: List<IntrinsicMeasurable>,
            height: Int,
            arrangementSpacing: Int
        ) = maxIntrinsicMainAxisSize(
            measurables,
            maxMainAxisIntrinsicItemSize,
            height,
            arrangementSpacing,
            maxItemsInMainAxis
        )

        fun intrinsicCrossAxisSize(
            measurables: List<IntrinsicMeasurable>,
            mainAxisAvailable: Int,
            arrangementSpacing: Int
        ) = intrinsicCrossAxisSize(
            measurables,
            mainAxisSize = minMainAxisIntrinsicItemSize,
            crossAxisSize = minCrossAxisIntrinsicItemSize,
            mainAxisAvailable,
            arrangementSpacing,
            maxItemsInMainAxis
        )

        val maxMainAxisIntrinsicItemSize: IntrinsicMeasurable.(Int, Int) -> Int =
            if (orientation == LayoutOrientation.Horizontal) { _, h ->
                maxIntrinsicWidth(h)
            }
            else { _, w ->
                maxIntrinsicHeight(w)
            }

        val maxCrossAxisIntrinsicItemSize: IntrinsicMeasurable.(Int, Int) -> Int =
            if (orientation == LayoutOrientation.Horizontal) { _, w ->
                maxIntrinsicHeight(w)
            }
            else { _, h ->
                maxIntrinsicWidth(h)
            }

        val minCrossAxisIntrinsicItemSize: IntrinsicMeasurable.(Int, Int) -> Int =
            if (orientation == LayoutOrientation.Horizontal) { _, w ->
                minIntrinsicHeight(w)
            }
            else { _, h ->
                minIntrinsicWidth(h)
            }

        val minMainAxisIntrinsicItemSize: IntrinsicMeasurable.(Int, Int) -> Int =
            if (orientation == LayoutOrientation.Horizontal) { _, h ->
                minIntrinsicWidth(h)
            }
            else { _, w ->
                minIntrinsicHeight(w)
            }
    }
}

private fun maxIntrinsicMainAxisSize(
    children: List<IntrinsicMeasurable>,
    mainAxisSize: IntrinsicMeasurable.(Int, Int) -> Int,
    crossAxisAvailable: Int,
    mainAxisSpacing: Int,
    maxItemsInMainAxis: Int
): Int {
    var fixedSpace = 0
    var currentFixedSpace = 0
    var lastBreak = 0
    children.fastForEachIndexed { index, child ->
        val size = child.mainAxisSize(index, crossAxisAvailable) + mainAxisSpacing
        if (index + 1 - lastBreak == maxItemsInMainAxis || index + 1 == children.size) {
            lastBreak = index
            currentFixedSpace += size
            fixedSpace = max(fixedSpace, currentFixedSpace)
            currentFixedSpace = 0
        } else {
            currentFixedSpace += size
        }
    }
    return fixedSpace
}

/**
 * Slower algorithm but needed to determine the minimum main axis size
 * Uses a binary search to search different scenarios to see the minimum main axis size
 */
private fun minIntrinsicMainAxisSize(
    children: List<IntrinsicMeasurable>,
    mainAxisSize: IntrinsicMeasurable.(Int, Int) -> Int,
    crossAxisSize: IntrinsicMeasurable.(Int, Int) -> Int,
    crossAxisAvailable: Int,
    mainAxisSpacing: Int,
    maxItemsInMainAxis: Int
): Int {
    val mainAxisSizes = IntArray(children.size) { 0 }
    val crossAxisSizes = IntArray(children.size) { 0 }

    for (index in children.indices) {
        val child = children[index]
        val mainAxisItemSize = child.mainAxisSize(index, crossAxisAvailable)
        mainAxisSizes[index] = mainAxisItemSize
        crossAxisSizes[index] = child.crossAxisSize(index, mainAxisItemSize)
    }

    val maxMainAxisSize = mainAxisSizes.sum()
    var mainAxisUsed = maxMainAxisSize
    var crossAxisUsed = crossAxisSizes.maxOf { it }

    val minimumItemSize = mainAxisSizes.maxOf { it }
    var low = minimumItemSize
    var high = maxMainAxisSize
    while (low < high) {
        if (crossAxisUsed == crossAxisAvailable) {
            return mainAxisUsed
        }
        val mid = (low + high) / 2
        mainAxisUsed = mid
        crossAxisUsed = intrinsicCrossAxisSize(
            children,
            mainAxisSizes,
            crossAxisSizes,
            mainAxisUsed,
            mainAxisSpacing,
            maxItemsInMainAxis
        )

        if (crossAxisUsed == crossAxisAvailable) {
            return mainAxisUsed
        } else if (crossAxisUsed > crossAxisAvailable) {
            low = mid + 1
        } else {
            high = mid - 1
        }
    }

    return mainAxisUsed
}

/**
 * FlowRow: Intrinsic height (cross Axis) is based on a specified width
 * FlowColumn: Intrinsic width (crossAxis) based on a specified height
 */
private fun intrinsicCrossAxisSize(
    children: List<IntrinsicMeasurable>,
    mainAxisSizes: IntArray,
    crossAxisSizes: IntArray,
    mainAxisAvailable: Int,
    mainAxisSpacing: Int,
    maxItemsInMainAxis: Int
): Int {
    return intrinsicCrossAxisSize(
        children,
        { index, _ -> mainAxisSizes[index] },
        { index, _ -> crossAxisSizes[index] },
        mainAxisAvailable,
        mainAxisSpacing,
        maxItemsInMainAxis
    )
}

/** FlowRow: Intrinsic height (cross Axis) is based on a specified width
 ** FlowColumn: Intrinsic width (crossAxis) based on a specified height
 */
private fun intrinsicCrossAxisSize(
    children: List<IntrinsicMeasurable>,
    mainAxisSize: IntrinsicMeasurable.(Int, Int) -> Int,
    crossAxisSize: IntrinsicMeasurable.(Int, Int) -> Int,
    mainAxisAvailable: Int,
    mainAxisSpacing: Int,
    maxItemsInMainAxis: Int
): Int {
    if (children.isEmpty()) {
        return 0
    }
    var nextChild = children.getOrNull(0)
    var nextCrossAxisSize = nextChild?.crossAxisSize(0, mainAxisAvailable) ?: 0
    var nextMainAxisSize = nextChild?.mainAxisSize(0, nextCrossAxisSize) ?: 0

    var remaining = mainAxisAvailable
    var currentCrossAxisSize = 0
    var totalCrossAxisSize = 0
    var lastBreak = 0

    children.fastForEachIndexed { index, _ ->
        nextChild!!
        val childCrossAxisSize = nextCrossAxisSize
        val childMainAxisSize = nextMainAxisSize
        remaining -= childMainAxisSize
        currentCrossAxisSize = maxOf(currentCrossAxisSize, childCrossAxisSize)

        // look ahead to simplify logic
        nextChild = children.getOrNull(index + 1)
        nextCrossAxisSize = nextChild?.crossAxisSize(index + 1, mainAxisAvailable) ?: 0
        nextMainAxisSize = nextChild?.mainAxisSize(index + 1, nextCrossAxisSize)
            ?.plus(mainAxisSpacing) ?: 0

        if (remaining < 0 || index + 1 == children.size ||
            (index + 1) - lastBreak == maxItemsInMainAxis ||
            remaining - nextMainAxisSize < 0
        ) {
            totalCrossAxisSize += currentCrossAxisSize
            currentCrossAxisSize = 0
            remaining = mainAxisAvailable
            lastBreak = index + 1
            nextMainAxisSize -= mainAxisSpacing
        }
    }
    return totalCrossAxisSize
}

/**
 * Breaks down items based on space, size and maximum items in main axis.
 * When items run out of space or the maximum items to fit in the main axis is reached,
 * it moves to the next "line" and moves the next batch of items to a new list of items
 */
internal fun MeasureScope.breakDownItems(
    measureHelper: RowColumnMeasurementHelper,
    orientation: LayoutOrientation,
    constraints: OrientationIndependentConstraints,
    maxItemsInMainAxis: Int,
): FlowResult {
    val items = mutableVectorOf<RowColumnMeasureHelperResult>()
    val mainAxisMax = constraints.mainAxisMax
    val mainAxisMin = constraints.mainAxisMin
    val crossAxisMax = constraints.crossAxisMax
    val measurables = measureHelper.measurables
    val placeables = measureHelper.placeables

    val spacing = ceil(measureHelper.arrangementSpacing.toPx()).toInt()
    val subsetConstraints = OrientationIndependentConstraints(
        mainAxisMin,
        mainAxisMax,
        0,
        crossAxisMax
    )
    // nextSize of the list, pre-calculated
    var nextSize: Int? = measurables.getOrNull(0)?.measureAndCache(
        subsetConstraints, orientation
    ) { placeable ->
        placeables[0] = placeable
    }

    var startBreakLineIndex = 0
    val endBreakLineList = arrayOfNulls<Int>(measurables.size)
    var endBreakLineIndex = 0

    var leftOver = mainAxisMax
    // figure out the mainAxisTotalSize which will be minMainAxis when measuring the row/column
    var mainAxisTotalSize = mainAxisMin
    var currentLineMainAxisSize = 0
    for (index in measurables.indices) {
        val itemMainAxisSize = nextSize!!
        currentLineMainAxisSize += itemMainAxisSize
        leftOver -= itemMainAxisSize
        nextSize = measurables.getOrNull(index + 1)?.measureAndCache(
            subsetConstraints, orientation
        ) { placeable ->
            placeables[index + 1] = placeable
        }?.plus(spacing)
        if (index + 1 >= measurables.size ||
            (index + 1) - startBreakLineIndex >= maxItemsInMainAxis ||
            leftOver - (nextSize ?: 0) < 0
        ) {
            mainAxisTotalSize = maxOf(mainAxisTotalSize, currentLineMainAxisSize)
            currentLineMainAxisSize = 0
            leftOver = mainAxisMax
            startBreakLineIndex = index + 1
            endBreakLineList[endBreakLineIndex] = index + 1
            endBreakLineIndex++
            // only add spacing for next items in the row or column, not the starting indexes
            nextSize = nextSize?.minus(spacing)
        }
    }

    val subsetBoxConstraints = subsetConstraints.copy(
        mainAxisMin = mainAxisTotalSize
    ).toBoxConstraints(orientation)

    startBreakLineIndex = 0
    var crossAxisTotalSize = 0

    endBreakLineIndex = 0
    var endIndex = endBreakLineList.getOrNull(endBreakLineIndex)
    while (endIndex != null) {
        val result = measureHelper.measureWithoutPlacing(
            this,
            subsetBoxConstraints,
            startBreakLineIndex,
            endIndex
        )
        crossAxisTotalSize += result.crossAxisSize
        mainAxisTotalSize = maxOf(mainAxisTotalSize, result.mainAxisSize)
        items.add(
            result
        )
        startBreakLineIndex = endIndex
        endBreakLineIndex++
        endIndex = endBreakLineList.getOrNull(endBreakLineIndex)
    }

    crossAxisTotalSize = maxOf(crossAxisTotalSize, constraints.crossAxisMin)
    mainAxisTotalSize = maxOf(mainAxisTotalSize, constraints.mainAxisMin)
    return FlowResult(
        mainAxisTotalSize,
        crossAxisTotalSize,
        items,
    )
}

internal fun Measurable.mainAxisMin(orientation: LayoutOrientation, crossAxisSize: Int) =
    if (orientation == LayoutOrientation.Horizontal) {
        minIntrinsicWidth(crossAxisSize)
    } else {
        minIntrinsicHeight(crossAxisSize)
    }

internal fun Measurable.crossAxisMin(orientation: LayoutOrientation, mainAxisSize: Int) =
    if (orientation == LayoutOrientation.Horizontal) {
        minIntrinsicHeight(mainAxisSize)
    } else {
        minIntrinsicWidth(mainAxisSize)
    }

internal fun Placeable.mainAxisSize(orientation: LayoutOrientation) =
    if (orientation == LayoutOrientation.Horizontal) width else height

internal fun Placeable.crossAxisSize(orientation: LayoutOrientation) =
    if (orientation == LayoutOrientation.Horizontal) height else width

// We measure and cache to improve performance dramatically, instead of using intrinsics
// This only works so far for fixed size items.
// For weighted items, we continue to use their intrinsic widths.
// This is because their fixed sizes are only determined after we determine
// the number of items that can fit in the row/column it only lies on.
private fun Measurable.measureAndCache(
    constraints: OrientationIndependentConstraints,
    orientation: LayoutOrientation,
    storePlaceable: (Placeable?) -> Unit
): Int {
    val itemSize: Int = if (rowColumnParentData.weight == 0f) {
        // fixed sizes: measure once
        val placeable = measure(
            constraints.copy(
                mainAxisMin = 0,
            ).toBoxConstraints(orientation)
        ).also(storePlaceable)
        placeable.mainAxisSize(orientation)
    } else {
        mainAxisMin(orientation, Constraints.Infinity)
    }
    return itemSize
}

/**
 * FlowResult when broken down to multiple rows or columns based on [breakDownItems] algorithm
 *
 * @param mainAxisTotalSize the total size of the main axis
 * @param crossAxisTotalSize the total size of the cross axis when taken into account
 * the cross axis sizes of all items
 * @param items the row or column measurements for each row or column
 */
internal class FlowResult(
    val mainAxisTotalSize: Int,
    val crossAxisTotalSize: Int,
    val items: MutableVector<RowColumnMeasureHelperResult>,
)