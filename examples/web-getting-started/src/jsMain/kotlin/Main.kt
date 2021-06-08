import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import org.jetbrains.compose.common.core.graphics.Color
import org.jetbrains.compose.common.foundation.layout.Box
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.renderComposable
import org.jetbrains.compose.common.foundation.layout.Column
import org.jetbrains.compose.common.foundation.layout.fillMaxHeight
import org.jetbrains.compose.common.foundation.layout.fillMaxWidth
import org.jetbrains.compose.common.ui.Modifier
import org.jetbrains.compose.common.ui.unit.em

fun main() {
    var count: Int by mutableStateOf(0)

    renderComposable(rootElementId = "root") {
        Column(Modifier.fillMaxWidth().fillMaxHeight(1f)) {
            Box() {
                org.jetbrains.compose.common.material.Text(
                    "Hello Column",
                    size = 1.8f.em,
                    color = Color(218, 120, 91)
                )
            }
        }
        Div({ style { padding(25.px) } }) {
            Button(attrs = {
                onClick { count -= 1 }
            }) {
                Text("-")
            }

            Span({style { padding(15.px) }}) {
                Text("$count")
            }

            Button({
                onClick { count += 1 }
            }) {
                Text("+")
            }
        }
    }
}