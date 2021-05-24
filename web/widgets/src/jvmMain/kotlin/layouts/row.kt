package org.jetbrains.compose.common.foundation.layout

import org.jetbrains.compose.common.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Row as JRow
import org.jetbrains.compose.common.ui.implementation
import org.jetbrains.compose.common.ui.implementation
import org.jetbrains.compose.common.ui.Alignment

@Composable
internal actual fun RowActual(
    modifier: Modifier,
    horizontalArrangement: Arrangement.Horizontal,
    verticalAlignment: Alignment.Vertical,
    content: @Composable () -> Unit
) {
    JRow(
        modifier.implementation,
        horizontalArrangement.implementation,
        verticalAlignment.implementation
    ) {
        content.invoke()
    }
}