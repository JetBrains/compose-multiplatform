package org.jetbrains.compose.common.material

import androidx.compose.runtime.Composable
import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgetsApi
import org.jetbrains.compose.common.ui.Modifier
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.value

@Composable
@ExperimentalComposeWebWidgetsApi
@Deprecated(message = "compose.web.web-widgets API is deprecated")
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
        attrs = {
            value(value.toString())
            attr("min", valueRange.start.toString())
            attr("max", valueRange.endInclusive.toString())
            attr("step", step.toString())
            onInput {
                onValueChange(it.value?.toFloat() ?: 0f)
            }
        }
    )
}
