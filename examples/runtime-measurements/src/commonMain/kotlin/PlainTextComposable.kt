import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode

@Composable
fun PlainText(value: String, content: @Composable () -> Unit = {}) {
    ComposeNode<Node, ListApplier>(
        factory = { Node() },
        update = {
            set(value) { value -> this.text = value }
        },
        content = content
    )
}
