import androidx.compose.runtime.mutableStateOf
import com.sample.content.AddItems
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.renderComposable

fun main() {
    val count = mutableStateOf(0)

    renderComposable("root") {
        Button(attrs = {
            onClick {
                count.value = if (count.value == 500) 0 else 500
            }
        }) {
            Text(if (count.value == 0) "add 500 items" else "delete items")
        }

        AddItems(count.value)
    }
}
