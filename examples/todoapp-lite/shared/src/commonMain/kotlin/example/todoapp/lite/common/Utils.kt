package example.todoapp.lite.common

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
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

internal fun Modifier.onKeyUp(key: Key, action: () -> Unit): Modifier =
    onKeyEvent { event ->
        if ((event.type == KeyEventType.KeyUp) && (event.key == key)) {
            action()
            true
        } else {
            false
        }
    }
