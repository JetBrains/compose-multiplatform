package org.jetbrains.compose.splitpane

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import org.jetbrains.compose.movable.SplitterState
import kotlin.math.roundToInt

private fun Constraints.maxByDirection(isHorizontal: Boolean): Int = if (isHorizontal) maxWidth else maxHeight
private fun Constraints.minByDirection(isHorizontal: Boolean): Int = if (isHorizontal) minWidth else minHeight

private val SplitterState.roundToIntValue
    get() = position.roundToInt()

@Composable
actual fun SplitPane(
    modifier: Modifier,
    isHorizontal: Boolean,
    state: SplitterState,
    minimalSizesConfiguration: MinimalSizes,
    first: @Composable ()->Unit,
    second: @Composable ()->Unit,
    separator: @Composable ()->Unit
) {
    Layout(
        {
            first()
            second()
            separator()
        },
        modifier,
    ) { measurables, constraints ->
        with(minimalSizesConfiguration) {
            with(state) {

                val constrainedMin = constraints.minByDirection(isHorizontal) + firstPlaceableMinimalSize.value

                if (constrainedMin != minPosition) {
                    minPosition = constrainedMin
                }

                val constrainedMax = (constraints.maxByDirection(isHorizontal).toFloat() - secondPlaceableMinimalSize.value).let {
                    if (it <= 0 || it <= constrainedMin) {
                        constraints.maxByDirection(isHorizontal).toFloat()
                    } else { it }
                }

                if (constrainedMax != maxPosition) {
                    maxPosition = if (constraints.maxByDirection(isHorizontal).toFloat() <
                        (firstPlaceableMinimalSize + secondPlaceableMinimalSize).value) {
                        minPosition
                    } else {
                        constrainedMax
                    }
                }

                val firstPlaceable = measurables[0].measure(
                    if(isHorizontal) {
                        constraints.copy(
                            minWidth = 0,
                            maxWidth = roundToIntValue
                        )
                    } else {
                        constraints.copy(
                            minHeight = 0,
                            maxHeight = roundToIntValue
                        )
                    }
                )

                val secondPlaceableSize = (constraints.maxByDirection(isHorizontal) - roundToIntValue).coerceIn(0, constraints.maxByDirection(isHorizontal))

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
                        secondPlaceable.place(roundToIntValue, 0)
                        splitterPlaceable.place(roundToIntValue, 0)
                    } else {
                        secondPlaceable.place(0,roundToIntValue)
                        splitterPlaceable.place(0,roundToIntValue)
                    }
                }
            }
        }
    }
}