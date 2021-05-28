import androidx.compose.desktop.Window
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.IntSize
import fallingBalls.Game
import fallingBalls.fallingBalls

class JvmGame : Game() {
    override fun now() = System.nanoTime()
}

fun main() {
    Window(title = "Demo", size = IntSize(600, 400)) {
        fallingBalls(
            remember {
                JvmGame().apply {
                    width = 600
                    height = 400
                }
            }
        )
    }
}
