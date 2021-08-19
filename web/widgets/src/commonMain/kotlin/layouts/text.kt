package org.jetbrains.compose.common.material

import androidx.compose.runtime.Composable
import org.jetbrains.compose.common.ui.Modifier
import org.jetbrains.compose.common.core.graphics.Color
import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgetsApi
import org.jetbrains.compose.common.ui.unit.TextUnit

@Composable
@ExperimentalComposeWebWidgetsApi
internal expect fun TextActual(
    text: String,
    modifier: Modifier,
    color: Color,
    size: TextUnit
)
