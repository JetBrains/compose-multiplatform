package example.imageviewer.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun ScrollableColumn(modifier: Modifier, content: @Composable ColumnScope.() -> Unit)

@Composable
fun TouchScrollableColumn(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    val scrollState = rememberScrollState()
    Column(modifier.verticalScroll(scrollState)) {
        content()
    }
}
