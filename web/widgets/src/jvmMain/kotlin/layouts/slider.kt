package org.jetbrains.compose.common.material

import androidx.compose.runtime.Composable
import org.jetbrains.compose.common.ui.Modifier
import androidx.compose.material.Slider as JSlider
import org.jetbrains.compose.common.ui.implementation

@Composable
actual fun SliderActual(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    modifier: Modifier
) {
    JSlider(
        value,
        onValueChange = onValueChange,
        modifier = modifier.implementation,
        valueRange = valueRange,
        steps = steps
    )
}
