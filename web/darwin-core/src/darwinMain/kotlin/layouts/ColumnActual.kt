package org.jetbrains.compose.common.foundation.layout

import androidx.compose.runtime.Composable
import org.jetbrains.compose.common.ui.Modifier
import org.jetbrains.compose.web.VStack

@Composable
internal actual fun ColumnActual(
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    VStack(content = content)
}