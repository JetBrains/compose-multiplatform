package example.imageviewer.view

import androidx.compose.runtime.Composable

@Composable
actual fun Tooltip(
    text: String,
    content: @Composable () -> Unit
) {
    //No tooltip for web
    content()
}
