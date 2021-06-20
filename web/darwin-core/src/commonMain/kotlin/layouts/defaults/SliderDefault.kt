package org.jetbrains.compose.common.material

import androidx.compose.runtime.Composable
import org.jetbrains.compose.common.ui.Modifier

@Composable
fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit = {},
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    modifier: Modifier = Modifier.Companion
) {
    SliderActual(
        value,
        onValueChange,
        valueRange,
        steps,
        modifier
    )
}
