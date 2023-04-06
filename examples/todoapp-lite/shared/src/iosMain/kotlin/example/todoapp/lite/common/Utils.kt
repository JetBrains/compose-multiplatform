package example.todoapp.lite.common


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.*

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable {
                onCloseRequest()
            }
    ) {
        Box(
            Modifier
                .padding(30.dp)
                .background(Color.White)
                .align(Alignment.TopCenter)
                .clickable(enabled = false){}
        ) {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                ProvideTextStyle(MaterialTheme.typography.subtitle1) {
                    Text(text = title)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(modifier = Modifier.fillMaxHeight(0.5f)) {
                    content()
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onCloseRequest,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(text = "Done")
                }
            }
        }
    }
}
