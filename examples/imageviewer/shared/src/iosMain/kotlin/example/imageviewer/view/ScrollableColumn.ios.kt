package example.imageviewer.view

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun ScrollableColumn(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) =
    TouchScrollableColumn(modifier, content)
