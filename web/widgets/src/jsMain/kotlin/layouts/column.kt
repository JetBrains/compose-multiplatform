package org.jetbrains.compose.common.foundation.layout

import org.jetbrains.compose.common.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.web.elements.Div
import org.jetbrains.compose.web.ui.Styles

@Composable
internal actual fun ColumnActual(modifier: Modifier, content: @Composable () -> Unit) {
    Div(
        attrs = {
            classes(Styles.columnClass)
        }
    ) {
        content()
    }
}
