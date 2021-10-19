package org.jetbrains.compose.codeeditor.editor.tooltip

import org.jetbrains.compose.codeeditor.AppTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider

@Composable
internal fun Tooltip(
    message: String,
    positionProvider: PopupPositionProvider,
    onDismissRequest: (() -> Unit)? = null
) = Popup(
    popupPositionProvider = positionProvider,
    onDismissRequest = onDismissRequest
) {
    Surface(
        elevation = 2.dp,
        border = BorderStroke(Dp.Hairline, AppTheme.colors.borderLight)
    ) {
        Box(
            Modifier.padding(5.dp)
        ) {
            Text(
                text = message,
                modifier = Modifier.align(Alignment.CenterStart),
                style = MaterialTheme.typography.caption
            )
        }
    }
}
