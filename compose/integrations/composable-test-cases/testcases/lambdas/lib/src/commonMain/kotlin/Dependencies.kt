import androidx.compose.runtime.Composable
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
