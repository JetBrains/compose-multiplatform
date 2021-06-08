package org.jetbrains.compose.common.material

import androidx.compose.runtime.Composable
import org.jetbrains.compose.common.ui.Modifier
import org.jetbrains.compose.common.core.graphics.Color
import org.jetbrains.compose.common.ui.unit.TextUnit

@Composable
fun EditText(
    text: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier.Companion,
    color: Color = Color.Black,
    size: TextUnit = TextUnit.Unspecified
) {
    EditTextActual(text, onValueChange, modifier, color, size)
}
