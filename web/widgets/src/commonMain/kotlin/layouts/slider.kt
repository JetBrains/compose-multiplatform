package org.jetbrains.compose.common.material

import androidx.compose.runtime.Composable
import org.jetbrains.compose.common.ui.Modifier

@Composable
expect fun SliderActual(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    modifier: Modifier,
)
