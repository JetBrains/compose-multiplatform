@file:JvmName("Utils")

package example.todoapp.lite.common

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal actual val MARGIN_SCROLLBAR: Dp = 8.dp

@Suppress("ACTUAL_WITHOUT_EXPECT") // Workaround https://youtrack.jetbrains.com/issue/KT-37316
internal actual typealias ScrollbarAdapter = androidx.compose.foundation.v2.ScrollbarAdapter

@Composable
internal actual fun rememberScrollbarAdapter(scrollState: LazyListState): ScrollbarAdapter =
    androidx.compose.foundation.rememberScrollbarAdapter(scrollState)

@Composable
internal actual fun VerticalScrollbar(
    modifier: Modifier,
    adapter: ScrollbarAdapter
) {
    androidx.compose.foundation.VerticalScrollbar(
        modifier = modifier,
        adapter = adapter
    )
}
