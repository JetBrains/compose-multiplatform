package org.jetbrains.compose.splitpane

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt

private fun Constraints.maxByDirection(isHorizontal: Boolean): Int = if (isHorizontal) maxWidth else maxHeight

internal fun SplitterState.valueInPercents(): Float = (value - minValue) / (maxValue - minValue)

private val SplitterState.roundToIntValue
    get() = value.roundToInt()

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
        require(measurables.size == 3)
        with(minimalSizesConfiguration) {
            with(state) {
                // check if minimal sizes constraints are valid
                var constraintedMax = constraints.maxByDirection(isHorizontal).toFloat() - secondPlaceableMinimalSize.value
                if (constraintedMax <= 0 || constraintedMax <= firstPlaceableMinimalSize.value) {
                    constraintedMax = constraints.maxByDirection(isHorizontal).toFloat()
                }
                // apply second placeable minimal size constraint
                if (maxValue == kotlin.Float.POSITIVE_INFINITY) {
                    maxValue = constraintedMax
                }
                // check if layout size was changed
                if (constraintedMax != maxValue) {
                    if (constraints.maxByDirection(isHorizontal).toFloat() < (firstPlaceableMinimalSize + secondPlaceableMinimalSize).value) {
                        value = minValue
                        maxValue = minValue
                    } else {
                        if (maxValue != minValue) {
                            val splitterPositionPercent = valueInPercents()
                            val newMax = constraints.maxByDirection(isHorizontal) - secondPlaceableMinimalSize.value
                            val newStateValue = (newMax - minValue) * splitterPositionPercent
                            maxValue = newMax
                            value = newStateValue
                        } else {
                            maxValue = constraints.maxByDirection(isHorizontal) - secondPlaceableMinimalSize.value
                        }
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