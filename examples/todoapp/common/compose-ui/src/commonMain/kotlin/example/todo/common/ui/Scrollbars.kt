package example.todo.common.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

expect val MARGIN_SCROLLBAR: Dp

expect interface ScrollbarAdapter

@Composable
expect fun rememberScrollbarAdapter(
    scrollState: LazyListState,
    itemCount: Int,
    averageItemSize: Dp
): ScrollbarAdapter

@Composable
expect fun VerticalScrollbar(
    modifier: Modifier,
    adapter: ScrollbarAdapter
)
