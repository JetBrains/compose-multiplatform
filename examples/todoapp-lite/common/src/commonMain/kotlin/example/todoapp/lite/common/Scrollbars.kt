package example.todoapp.lite.common

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

internal expect val MARGIN_SCROLLBAR: Dp

internal expect interface ScrollbarAdapter

@Composable
internal expect fun rememberScrollbarAdapter(scrollState: LazyListState): ScrollbarAdapter

@Composable
internal expect fun VerticalScrollbar(
    modifier: Modifier,
    adapter: ScrollbarAdapter
)
