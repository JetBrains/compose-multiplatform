package org.jetbrains.compose.common.material

import androidx.compose.runtime.Composable
import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgets
import org.jetbrains.compose.common.ui.Modifier

@Composable
@ExperimentalComposeWebWidgets
internal expect fun SliderActual(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    modifier: Modifier,
)
