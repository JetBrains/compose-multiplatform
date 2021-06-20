package org.jetbrains.compose.common.material

import androidx.compose.runtime.Composable
import androidx.compose.material.Text as JText
import org.jetbrains.compose.common.ui.Modifier
import org.jetbrains.compose.common.ui.implementation
import org.jetbrains.compose.common.core.graphics.Color
import org.jetbrains.compose.common.core.graphics.implementation
import org.jetbrains.compose.common.ui.unit.TextUnit
import org.jetbrains.compose.common.ui.unit.implementation

@Composable
actual fun TextActual(
    text: String,
    modifier: Modifier,
    color: Color,
    size: TextUnit
) {
    JText(
        text,
        modifier = modifier.implementation,
        color = color.implementation,
        fontSize = size.implementation
    )
}
