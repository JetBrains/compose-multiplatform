package example.imageviewer.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TopLayout(
    alignLeftContent: @Composable () -> Unit = {},
    alignRightContent: @Composable () -> Unit = {},
) {
    Box(
        Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(12.dp)
    ) {
        Row(Modifier.align(Alignment.CenterStart)) {
            alignLeftContent()
        }
        Row(Modifier.align(Alignment.CenterEnd)) {
            alignRightContent()
        }
    }
}
