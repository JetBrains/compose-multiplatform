package org.jetbrains.compose.splitpane

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt

private fun Constraints.maxByDirection(isHorizontal: Boolean): Int = if (isHorizontal) maxWidth else maxHeight
private fun Constraints.minByDirection(isHorizontal: Boolean): Int = if (isHorizontal) minWidth else minHeight

@Composable
internal actual fun SplitPane(
    modifier: Modifier,
    isHorizontal: Boolean,
    splitterPositionState: SplitterPositionState,
    positionBorders: PositionBorders,
    minimalSizesConfiguration: MinimalSizes,
    first: @Composable ()->Unit,
    second: @Composable ()->Unit,
    splitter: @Composable ()->Unit
) {
    Layout(
        {
            first()
            second()
            splitter()
        },
        modifier,
    ) { measurables, constraints ->
        with(minimalSizesConfiguration) {
            with(splitterPositionState) {

                val constrainedMin = constraints.minByDirection(isHorizontal) + firstPlaceableMinimalSize.value

                val constrainedMax = (constraints.maxByDirection(isHorizontal).toFloat() - secondPlaceableMinimalSize.value).let {
                    if (it <= 0 || it <= constrainedMin) {
                        constraints.maxByDirection(isHorizontal).toFloat()
                    } else { it }
                }

                if (positionBorders.minPosition != constrainedMin) {
                    positionBorders.maxPosition = constrainedMin
                }

                if (positionBorders.maxPosition != constrainedMax) {
                    if ((firstPlaceableMinimalSize + secondPlaceableMinimalSize).value < constraints.maxByDirection(isHorizontal)) {
                        positionBorders.maxPosition = constrainedMax
                    } else {
                        positionBorders.maxPosition = positionBorders.minPosition
                    }
                }

                val constrainedPosition = (constraints.maxByDirection(isHorizontal) - (firstPlaceableMinimalSize + secondPlaceableMinimalSize).value).let {
                    if (it > 0f) {
                        (it*positionPercentage).coerceIn(constrainedMin,constrainedMax).roundToInt()
                    } else {
                        constrainedMin.roundToInt()
                    }
                }


                val firstPlaceable = measurables[0].measure(
                    if(isHorizontal) {
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

                val secondPlaceableSize = (constraints.maxByDirection(isHorizontal) - constrainedPosition).coerceIn(0, constraints.maxByDirection(isHorizontal))

                val secondPlaceable = measurables[1].measure(
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

                val splitterPlaceable = measurables[2].measure(constraints)

                layout(constraints.maxWidth, constraints.maxHeight) {
                    firstPlaceable.place(0, 0)
                    if (isHorizontal) {
                        secondPlaceable.place(constrainedPosition, 0)
                        splitterPlaceable.place(constrainedPosition, 0)
                    } else {
                        secondPlaceable.place(0,constrainedPosition)
                        splitterPlaceable.place(0,constrainedPosition)
                    }
                }
            }
        }
    }
}