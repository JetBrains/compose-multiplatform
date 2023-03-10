import androidx.compose.runtime.Composable
import com.example.common.TextContainerNode
import com.example.common.TextLeafNode

class ComposableContentImpl : ComposableContent {

    @Composable
    override fun ComposableContent() {
        TextLeafNode("ComposableContent")
    }

    @Composable
    override fun ComposableContentWithChildren(moreContent: @Composable () -> Unit) {
        TextContainerNode("ComposableContent", moreContent)
    }
}

class DelegateComposableContent(delegateTo: ComposableContent): ComposableContent by delegateTo

open class OpenComposableContentImpl : ComposableContent {
    @Composable
    override fun ComposableContent() {
        TextLeafNode("OpenComposableContentImpl")
    }

    @Composable
    override fun ComposableContentWithChildren(moreContent: @Composable () -> Unit) {
        TextContainerNode("OpenComposableContentImpl", moreContent)
    }
}


class CollectionOfComposablesImpl : CollectionOfComposable {

    private val list = mutableListOf<@Composable () -> Unit>()
    override fun add(composable: @Composable () -> Unit) {
        list.add(composable)
    }

    override fun iterator(): Iterator<@Composable () -> Unit> {
        return list.iterator()
    }
}
