package example.imageviewer.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal actual fun Tooltip(
    text: String,
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    // No Tooltip for Android
    content()
}
