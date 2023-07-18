import androidx.compose.runtime.Composable
import com.example.common.TextLeafNode


interface ComposableContent {
    @Composable
    fun ComposableContent()

    @Composable
    fun ComposableContentWithChildren(moreContent: @Composable () -> Unit)
}

interface DefaultComposableContent {
    @Composable
    fun ComposableContent(
        any: String = "any"
    ) {
        TextLeafNode("DefaultComposableContent - $any")
    }
}

interface CollectionOfComposable {
    fun add(composable: @Composable () -> Unit)

    fun iterator(): Iterator<@Composable () -> Unit>
}
