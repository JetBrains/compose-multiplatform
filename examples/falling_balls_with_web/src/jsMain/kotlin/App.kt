import androidx.compose.runtime.remember
import androidx.compose.web.css.Style
import androidx.compose.web.renderComposable
import fallingBalls.Game
import fallingBalls.fallingBalls
import kotlinx.browser.document
import kotlinx.browser.window
import org.jetbrains.compose.web.ui.Styles
import org.w3c.dom.HTMLElement

class JsGame : Game() {
    override fun now() = window.performance.now().toLong()
}

fun main() {
    val root = document.getElementById("root") as HTMLElement

    renderComposable(root = root) {
        Style(Styles)
        val game = remember {
            JsGame().apply {
                width = root.offsetWidth
                height = root.offsetHeight
            }
        }
        fallingBalls(game)
    }
}
