package example.todoapp.lite.common


import androidx.compose.foundation.lazy.LazyListState

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal actual val MARGIN_SCROLLBAR: Dp = 0.dp

internal actual interface ScrollbarAdapter

@Composable
internal actual fun rememberScrollbarAdapter(scrollState: LazyListState): ScrollbarAdapter =
    object : ScrollbarAdapter {}

@Composable
internal actual fun VerticalScrollbar(
    modifier: Modifier,
    adapter: ScrollbarAdapter
) {
    // no-op
}

@Composable
internal actual fun Dialog(
    title: String,
    onCloseRequest: () -> Unit,
    content: @Composable () -> Unit
) = Unit
