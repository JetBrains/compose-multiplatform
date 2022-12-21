package example.imageviewer.view

import androidx.compose.runtime.Composable

@Composable
internal actual fun Tooltip( //todo ios
    text: String,
    content: @Composable () -> Unit
) {
    content()
}
