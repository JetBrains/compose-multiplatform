package org.jetbrains.compose.common.material

import androidx.compose.runtime.Composable
import org.jetbrains.compose.common.ui.Modifier
import androidx.compose.web.elements.Input
import androidx.compose.web.attributes.InputType

@Composable
actual fun SliderActual(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    modifier: Modifier,
) {
    val stepCount = if (steps == 0) 100 else steps
    val step = (valueRange.endInclusive - valueRange.start) / stepCount

    Input(
        type = InputType.Range,
        value = value.toString(),
        attrs = {
            attr("min", valueRange.start.toString())
            attr("max", valueRange.endInclusive.toString())
            attr("step", step.toString())
            onRangeInput {
                val value: String = it.nativeEvent.target.asDynamic().value
                onValueChange(value.toFloat())
            }
        }
    ) {}
}
