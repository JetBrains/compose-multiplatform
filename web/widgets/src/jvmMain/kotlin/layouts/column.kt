package org.jetbrains.compose.common.foundation.layout

import org.jetbrains.compose.common.ui.Modifier
import androidx.compose.runtime.Composable
import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgetsApi
import androidx.compose.foundation.layout.Column as JColumn
import org.jetbrains.compose.common.ui.implementation

@Composable
@ExperimentalComposeWebWidgetsApi
internal actual fun ColumnActual(modifier: Modifier, content: @Composable () -> Unit) {
    JColumn(modifier = modifier.implementation) {
        content.invoke()
    }
}
