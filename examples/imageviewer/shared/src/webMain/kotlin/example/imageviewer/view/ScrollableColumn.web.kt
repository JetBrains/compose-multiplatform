package example.imageviewer.view

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
actual fun ScrollableColumn(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    val scrollState = rememberScrollState()
    Box(modifier) {
        Column(Modifier.verticalScroll(scrollState)) {
            content()
        }
        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd)
                .padding(4.dp)
                .fillMaxHeight(),
            adapter = rememberScrollbarAdapter(scrollState),
        )
    }
}
