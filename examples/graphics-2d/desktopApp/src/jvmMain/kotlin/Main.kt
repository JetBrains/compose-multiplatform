import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState


private val INIT_SIZE = DpSize(800.dp, 800.dp)

fun main() =
    application {
        val windowState = rememberWindowState(width = 800.dp, height = 800.dp)

        Window(
            onCloseRequest = ::exitApplication,
            resizable = false,
            title = "Graphics2D",
            state = windowState,
        ) {
            Graphics2D(
                requestWindowSize = { w, h ->
                    windowState.size = windowState.size.copy(
                        width = w.coerceIn(INIT_SIZE.width, Float.MAX_VALUE.dp),
                        height = h.coerceIn(INIT_SIZE.height, Float.MAX_VALUE.dp)
                    )
                }
            )
        }
    }
