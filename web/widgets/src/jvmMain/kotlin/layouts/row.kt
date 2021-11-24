package org.jetbrains.compose.common.foundation.layout

import androidx.compose.runtime.Composable
import org.jetbrains.compose.common.ui.*
import androidx.compose.foundation.layout.Row as JRow

@Composable
@ExperimentalComposeWebWidgetsApi
@Deprecated(message = "compose.web.web-widgets API is deprecated")
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
