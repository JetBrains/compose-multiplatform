package example.imageviewer.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal actual fun ScrollableColumn(modifier: Modifier, content: @Composable () -> Unit) =
    TouchScrollableColumn(modifier, content)
