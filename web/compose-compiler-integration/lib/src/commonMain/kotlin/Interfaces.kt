import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text

interface ComposableCollection {
    val list: List<@Composable () -> Unit>
    fun add(c: @Composable () -> Unit)
}


interface ComposableContent {

    @Composable
    fun ComposableContent()
}

abstract class AbstrComposableContent : ComposableContent {

    @Composable
    override fun ComposableContent() {
        Div { Text("AbstrComposableContent") }
    }
}
