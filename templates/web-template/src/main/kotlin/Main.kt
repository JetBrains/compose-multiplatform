import androidx.compose.web.elements.Div
import androidx.compose.web.elements.Text
import androidx.compose.web.renderComposable

fun main() {
    renderComposable(rootElementId = "root") {
        Div {
            Text("This is a template!")
        }
    }
}