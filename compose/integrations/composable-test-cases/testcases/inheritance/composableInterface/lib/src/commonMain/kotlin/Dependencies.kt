import androidx.compose.runtime.Composable
import com.example.common.TextLeafNode
import kotlin.jvm.JvmInline


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

interface DefaultComposableContent {
    @Composable
    @Suppress("ABSTRACT_COMPOSABLE_DEFAULT_PARAMETER_VALUE")
    fun ComposableContent(
        any: String = "any" // default value is required to reproduce
    ) {
        TextLeafNode("DefaultComposableContent - $any")
    }
}

abstract class AbstractGreeter {
    @Composable
    protected abstract fun Greeting()

    @Composable
    fun Hi() {
        Greeting()
    }
}

class Greeter(val target: String) : AbstractGreeter() {
    @Composable
    override fun Greeting() {
        TextLeafNode("Hello, $target!")
    }
}

@JvmInline
value class ValClass(val key: Int) {
    constructor(a: Int, b: Int) : this(a + b)
}
