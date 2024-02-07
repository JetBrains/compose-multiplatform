package org.jetbrains.compose.splitpane

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt

private fun Constraints.maxByDirection(isHorizontal: Boolean): Int = if (isHorizontal) maxWidth else maxHeight
private fun Placeable.valueByDirection(isHorizontal: Boolean): Int = if (isHorizontal) width else height
private fun Constraints.withUnconstrainedWidth() = copy(minWidth = 0, maxWidth = Constraints.Infinity)
private fun Constraints.withUnconstrainedHeight() = copy(minHeight = 0, maxHeight = Constraints.Infinity)


@OptIn(ExperimentalSplitPaneApi::class)
@Composable
internal actual fun SplitPane(
    modifier: Modifier,
    isHorizontal: Boolean,
    splitPaneState: SplitPaneState,
    minimalSizesConfiguration: MinimalSizes,
    first: (@Composable () -> Unit)?,
    second: (@Composable () -> Unit)?,
    splitter: Splitter
) {
    if (first == null || second == null) {
        first?.let { Box(modifier) { it() } }
        second?.let { Box(modifier) { it() } }
        return
    }
    
    Layout(
        {
            Box {
                first()
            }
            Box {
                splitter.measuredPart()
            }
            Box {
                second()
            }
            Box {
                splitter.handlePart()
            }
        },
        modifier,
    ) { measurables, constraints ->
        with(minimalSizesConfiguration) {
            val firstMinSizePx = firstPlaceableMinimalSize.value * density
            val secondMinSizePx = secondPlaceableMinimalSize.value * density

            with(splitPaneState) {
                val firstMeasurable = measurables[0]
                val splitterMeasurable = measurables[1]
                val secondMeasurable = measurables[2]
                val handleMeasurable = measurables[3]

                // Need the size of the splitter to determine the min/max position
                // Constrain the splitter only on the "other" axis
                val splitterConstraints =
                    if (isHorizontal)
                        constraints.withUnconstrainedWidth()
                    else
                        constraints.withUnconstrainedHeight()
                val splitterPlaceable = splitterMeasurable.measure(splitterConstraints)
                val splitterSize = splitterPlaceable.valueByDirection(isHorizontal)

                @Suppress("UnnecessaryVariable")
                val constrainedMin = firstMinSizePx
                val maxConstraintOnMainAxis = constraints.maxByDirection(isHorizontal)
                val constrainedMax = (maxConstraintOnMainAxis - secondMinSizePx - splitterSize)
                    .coerceAtLeast(constrainedMin)

                minPosition = constrainedMin
                maxPosition = constrainedMax

                val position = (constrainedMin * (1-positionPercentage) + constrainedMax * positionPercentage)
                    .roundToInt()

                val firstPlaceable = firstMeasurable.measure(
                    if (isHorizontal) {
                        constraints.copy(
                            minWidth = 0,
                            maxWidth = position
                        )
                    } else {
                        constraints.copy(
                            minHeight = 0,
                            maxHeight = position
                        )
                    }
                )

                val secondPlaceablePosition = position + splitterSize
                val secondAvailableSize = (maxConstraintOnMainAxis - secondPlaceablePosition).coerceAtLeast(0)

                val secondPlaceable = secondMeasurable.measure(
                    if (isHorizontal) {
                        constraints.copy(
                            minWidth = 0,
                            maxWidth = secondAvailableSize
                        )
                    } else {
                        constraints.copy(
                            minHeight = 0,
                            maxHeight = secondAvailableSize
                        )
                    }
                )

                val handlePlaceable = handleMeasurable.measure(splitterConstraints)
                val handleSize = handlePlaceable.valueByDirection(isHorizontal)
                // TODO support RTL
                val handlePosition = when (splitter.alignment) {
                    SplitterHandleAlignment.BEFORE -> position + splitterSize - handleSize
                    SplitterHandleAlignment.ABOVE -> position + (splitterSize - handleSize) / 2
                    SplitterHandleAlignment.AFTER -> position
                }

                layout(constraints.maxWidth, constraints.maxHeight) {
                    firstPlaceable.placeRelative(0, 0)
                    if (isHorizontal) {
                        secondPlaceable.placeRelative(secondPlaceablePosition, 0)
                        splitterPlaceable.placeRelative(position, 0)
                        if (moveEnabled) {
                            handlePlaceable.placeRelative(handlePosition, 0)
                        }
                    } else {
                        secondPlaceable.placeRelative(0, secondPlaceablePosition)
                        splitterPlaceable.placeRelative(0, position)
                        if (moveEnabled) {
                            handlePlaceable.placeRelative(0, handlePosition)
                        }
                    }
                }
            }
        }
    }
}