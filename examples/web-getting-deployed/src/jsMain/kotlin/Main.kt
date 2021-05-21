import androidx.compose.runtime.mutableStateOf
import androidx.compose.web.css.padding
import androidx.compose.web.css.px
import androidx.compose.web.elements.Button
import androidx.compose.web.elements.Div
import androidx.compose.web.elements.Span
import androidx.compose.web.elements.Text
import androidx.compose.web.renderComposable

fun main() {
    val count = mutableStateOf(0)

    renderComposable(rootElementId = "root") {
        Div(style = { padding(25.px) }) {
            Button(
                attrs = {
                    onClick { count.value = count.value - 1 }
                }
            ) {
                Text("-")
            }

            Span(style = { padding(15.px) }) {
                Text("${count.value}")
            }

            Button(
                attrs = {
                    onClick { count.value = count.value + 1 }
                }
            ) {
                Text("+")
            }
        }
    }
}
