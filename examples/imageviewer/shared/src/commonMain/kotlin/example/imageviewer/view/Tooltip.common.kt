package example.imageviewer.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun Tooltip(
    text: String,
    content: @Composable () -> Unit
)
