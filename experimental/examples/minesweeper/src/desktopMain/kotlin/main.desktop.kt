import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    val windowState = rememberWindowState()

    Window(
        onCloseRequest = ::exitApplication,
        resizable = false,
        title = "Minesweeper",
        icon = painterResource("assets/mine.png"),
        state = windowState
    ) {
        MaterialTheme {
            Game(
                requestWindowSize = { w, h ->
                    windowState.size = windowState.size.copy(width = w, height = h)
                }
            )
        }
    }
}

@Composable
actual fun loadImage(src: String): Painter = painterResource(src)

actual fun isMobileDevice() = false
