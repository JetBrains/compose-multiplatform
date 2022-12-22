package example.imageviewer.view

import androidx.compose.runtime.Composable

@Composable
internal expect fun Tooltip(
    text: String,
    content: @Composable () -> Unit
)
