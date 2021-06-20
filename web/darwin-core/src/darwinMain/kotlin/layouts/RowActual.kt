package org.jetbrains.compose.common.foundation.layout

import androidx.compose.runtime.Composable
import org.jetbrains.compose.common.ui.Alignment
import org.jetbrains.compose.common.ui.Modifier
import org.jetbrains.compose.web.HStack

@Composable
internal actual fun RowActual(
    modifier: Modifier,
    horizontalArrangement: Arrangement.Horizontal,
    verticalAlignment: Alignment.Vertical,
    content: @Composable () -> Unit
) {
    HStack(content = content)
}