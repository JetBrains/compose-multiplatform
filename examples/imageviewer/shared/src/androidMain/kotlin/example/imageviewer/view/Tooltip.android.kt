package example.imageviewer.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun Tooltip(
    text: String,
    content: @Composable () -> Unit
) {
    // No Tooltip for Android
    content()
}
