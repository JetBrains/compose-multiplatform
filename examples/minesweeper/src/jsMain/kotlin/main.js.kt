import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.window.Window
import org.jetbrains.skiko.wasm.onWasmReady

fun main() {
    onWasmReady {
        Window("Minesweeper") {
            Game(
                requestWindowSize = { _, _ ->
                    // TODO(not implemented yet)
                }
            )
        }
    }
}

@Composable
actual fun loadImage(src: String): Painter {
    // TODO Bundle pics and show images properly
    val color = when (src) {
        "assets/clock.png" -> Color.Blue
        "assets/flag.png" -> Color.Green
        "assets/mine.png" -> Color.Red
        else -> Color.White
    }

    return object : Painter() {
        override val intrinsicSize: Size
            get() = Size(16f, 16f)

        override fun DrawScope.onDraw() {
            drawRect(color = color)
        }
    }
}
