package example.imageviewer.view

import androidx.compose.runtime.Composable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier

@Composable
fun Clickable(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    children: @Composable () -> Unit = { }
) {
    Box(
        modifier = modifier.clickable {
            onClick?.invoke()
        }
    ) {
        children()
    }
}