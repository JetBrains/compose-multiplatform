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
actual fun loadImage(src: String): Painter = loadImageAsColoredRect(src)

actual fun isMobileDevice() = false
