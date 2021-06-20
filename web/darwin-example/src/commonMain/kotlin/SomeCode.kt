import androidx.compose.runtime.Composable
import org.jetbrains.compose.common.foundation.layout.Column
import org.jetbrains.compose.common.material.Text

object SomeCode {
    @Composable
    internal fun HelloWorld() {
        Column {
            repeat(60) {
                Text("Common Row $it")
            }
        }
    }
}