import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.cache
import androidx.compose.runtime.currentComposer
import com.example.common.TextLeafNode

@Composable
fun ComposableSomeText(someText : () -> String) {
    TextLeafNode(someText())
}


val composableInt: Int
    @Composable
    get() = currentComposer.cache(false) { 100 }


@Composable
fun ComposableAlwaysReturnsNull(): String? {
    return null
}

@Composable
fun ComposableAlwaysReturnsNullUnit(): Unit? {
    val u: Unit? = null
    return u
}

@Composable
fun ComposableAlwaysReturnsUnit(): Unit? {
    return Unit
}