import androidx.compose.runtime.Composable


interface ComposableContent {
    @Composable
    fun ComposableContent()

    @Composable
    fun ComposableContentWithChildren(moreContent: @Composable () -> Unit)
}

interface CollectionOfComposable {
    fun add(composable: @Composable () -> Unit)

    fun iterator(): Iterator<@Composable () -> Unit>
}
