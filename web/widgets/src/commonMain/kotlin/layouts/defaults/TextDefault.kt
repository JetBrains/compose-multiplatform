package org.jetbrains.compose.common.material

import androidx.compose.runtime.Composable
import org.jetbrains.compose.common.ui.Modifier
import org.jetbrains.compose.common.core.graphics.Color
import org.jetbrains.compose.common.ui.unit.TextUnit

@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier.Companion,
    color: Color = Color.Black,
    size: TextUnit = TextUnit.Unspecified
) {
    TextActual(text, modifier, color, size)
}
