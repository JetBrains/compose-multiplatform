package org.jetbrains.compose.common.foundation.layout

import org.jetbrains.compose.common.ui.Modifier
import androidx.compose.runtime.Composable
import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgetsApi
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.ui.Styles

@Composable
@ExperimentalComposeWebWidgetsApi
@Deprecated(message = "compose.web.web-widgets API is deprecated")
internal actual fun ColumnActual(modifier: Modifier, content: @Composable () -> Unit) {
    Div(
        attrs = {
            classes(Styles.columnClass)
        }
    ) {
        content()
    }
}
