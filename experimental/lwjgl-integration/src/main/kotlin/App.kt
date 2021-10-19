import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun App() {
    Column {
        var text by remember { mutableStateOf("Text") }
        TextField(text, { text = it })

        Button({}) {
            Text("Hello!")
        }

        Box(Modifier.weight(1f)) {
            val state = rememberLazyListState()

            LazyColumn(state = state, modifier = Modifier.width(200.dp).fillMaxHeight()) {
                items(100) {
                    Text("Item $it")
                }
            }

            VerticalScrollbar(
                rememberScrollbarAdapter(state),
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
            )
        }
    }
}