package org.jetbrains.compose.splitpane

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt

private fun Constraints.maxByDirection(isHorizontal: Boolean): Int = if (isHorizontal) maxWidth else maxHeight
private fun Constraints.minByDirection(isHorizontal: Boolean): Int = if (isHorizontal) minWidth else minHeight
private fun Placeable.valueByDirection(isHorizontal: Boolean): Int = if (isHorizontal) width else height

@OptIn(ExperimentalSplitPaneApi::class)
@Composable
internal actual fun SplitPane(
    modifier: Modifier,
    isHorizontal: Boolean,
    splitPaneState: SplitPaneState,
    minimalSizesConfiguration: MinimalSizes,
    first: @Composable () -> Unit,
    second: @Composable () -> Unit,
    splitter: Splitter
) {
    Layout(
        {
            first()
            splitter.measuredPart()
            second()
            splitter.handlePart()
        },
        modifier,
    ) { measurables, constraints ->
        with(minimalSizesConfiguration) {
            with(splitPaneState) {

                val constrainedMin = constraints.minByDirection(isHorizontal) + firstPlaceableMinimalSize.value

                val constrainedMax =
                    (constraints.maxByDirection(isHorizontal).toFloat() - secondPlaceableMinimalSize.value).let {
                        if (it <= 0 || it <= constrainedMin) {
                            constraints.maxByDirection(isHorizontal).toFloat()
                        } else {
                            it
                        }
                    }

                if (minPosition != constrainedMin) {
                    maxPosition = constrainedMin
                }

                if (maxPosition != constrainedMax) {
                    maxPosition =
                        if ((firstPlaceableMinimalSize + secondPlaceableMinimalSize).value < constraints.maxByDirection(isHorizontal)) {
                            constrainedMax
                        } else {
                            minPosition
                        }
                }

                val constrainedPosition =
                    (constraints.maxByDirection(isHorizontal) - (firstPlaceableMinimalSize + secondPlaceableMinimalSize).value).let {
                        if (it > 0f) {
                            (it * positionPercentage).coerceIn(constrainedMin, constrainedMax).roundToInt()
                        } else {
                            constrainedMin.roundToInt()
                        }
                    }


                val firstPlaceable = measurables[0].measure(
                    if (isHorizontal) {
                        constraints.copy(
                            minWidth = 0,
                            maxWidth = constrainedPosition
                        )
                    } else {
                        constraints.copy(
                            minHeight = 0,
                            maxHeight = constrainedPosition
                        )
                    }
                )

                val splitterPlaceable = measurables[1].measure(constraints)
                val splitterSize = splitterPlaceable.valueByDirection(isHorizontal)
                val secondPlaceablePosition = constrainedPosition + splitterSize

                val secondPlaceableSize =
                    (constraints.maxByDirection(isHorizontal) - secondPlaceablePosition).coerceIn(
                        0,
                        if (secondPlaceablePosition < constraints.maxByDirection(isHorizontal)) {
                            constraints.maxByDirection(isHorizontal) - secondPlaceablePosition
                        } else {
                            constraints.maxByDirection(isHorizontal)
                        }
                    )

                val secondPlaceable = measurables[2].measure(
                    if (isHorizontal) {
                        constraints.copy(
                            minWidth = 0,
                            maxWidth = secondPlaceableSize
                        )
                    } else {
                        constraints.copy(
                            minHeight = 0,
                            maxHeight = secondPlaceableSize
                        )
                    }
                )

                val handlePlaceable = measurables[3].measure(constraints)
                val handleSize = handlePlaceable.valueByDirection(isHorizontal)
                // TODO support RTL
                val handlePosition = when (splitter.alignment) {
                    SplitterHandleAlignment.BEFORE -> constrainedPosition + splitterSize - handleSize
                    SplitterHandleAlignment.ABOVE -> constrainedPosition + (splitterSize - handleSize) / 2
                    SplitterHandleAlignment.AFTER -> constrainedPosition
                }

                layout(constraints.maxWidth, constraints.maxHeight) {
                    firstPlaceable.place(0, 0)
                    if (isHorizontal) {
                        secondPlaceable.place(secondPlaceablePosition, 0)
                        splitterPlaceable.place(constrainedPosition, 0)
                        if (moveEnabled) {
                            handlePlaceable.place(handlePosition, 0)
                        }
                    } else {
                        secondPlaceable.place(0, secondPlaceablePosition)
                        splitterPlaceable.place(0, constrainedPosition)
                        if (moveEnabled) {
                            handlePlaceable.place(0, handlePosition)
                        }
                    }
                }
            }
        }
    }
}