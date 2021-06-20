package org.jetbrains.compose.common.material

import androidx.compose.runtime.Composable
import androidx.compose.material.TextField as JTextField
import org.jetbrains.compose.common.ui.Modifier
import org.jetbrains.compose.common.ui.implementation
import org.jetbrains.compose.common.core.graphics.Color
import org.jetbrains.compose.common.core.graphics.implementation
import org.jetbrains.compose.common.ui.unit.TextUnit
import org.jetbrains.compose.common.ui.unit.implementation

@Composable
actual fun EditTextActual(
    text: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    color: Color,
    size: TextUnit
) {
    JTextField(
        value = text,
        onValueChange = onValueChange,
        modifier = modifier.implementation,
        label = {"HEy"}/*,
        color = color.implementation,
        fontSize = size.implementation*/
    )
}
