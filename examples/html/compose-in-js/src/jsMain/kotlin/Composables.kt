import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.renderComposable

@JsExport
abstract class ComposeCounterAppController {
    abstract fun setCount(newCount: Int)
    abstract fun dispose()
}

/**
 * @param rootId - an id of an HTML element which is already added into the DOM.
 * Compose will manage the content of this element.
 *
 * @param onCountChange - a callback to receive state updates in non-compose code.
 *
 * @return instance of [ComposeCounterAppController] to control the composition in non-compose code.
 */
@JsExport
fun ComposeCounterApp(rootId: String, onCountChange: (Int) -> Unit = {}): ComposeCounterAppController {
    var count: Int by mutableStateOf(0)

    val composition = renderComposable(rootElementId = rootId) {
        Counter(count) {
            count = it
            onCountChange(count)
        }
    }

    return object : ComposeCounterAppController() {
        override fun setCount(newCount: Int) {
            count = newCount
        }

        override fun dispose() {
            composition.dispose()
        }
    }
}

@Composable
private fun Counter(count: Int, onCountChange: (Int) -> Unit) {
    Div({ style { padding(25.px) } }) {
        Button(attrs = {
            onClick {
                onCountChange(count - 1)
            }
        }) {
            Text("-")
        }

        Span({ style { padding(15.px) } }) {
            Text("$count")
        }

        Button(attrs = {
            onClick {
                onCountChange(count + 1)
            }
        }) {
            Text("+")
        }
    }
}
