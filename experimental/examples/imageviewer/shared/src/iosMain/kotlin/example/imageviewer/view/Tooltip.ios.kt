package example.imageviewer.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal actual fun Tooltip(
    text: String,
    content: @Composable () -> Unit
) {
    //No tooltip for iOS
    content()
}
