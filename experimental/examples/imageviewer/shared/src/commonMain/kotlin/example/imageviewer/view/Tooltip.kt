package example.imageviewer.view

import androidx.compose.runtime.Composable

@Composable
expect fun Tooltip(
    text: String = "Tooltip",
    content: @Composable () -> Unit
)
