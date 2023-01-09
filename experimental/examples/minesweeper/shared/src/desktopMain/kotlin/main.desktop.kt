import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.WindowState

@Composable
fun MainView(windowState: WindowState) =
    MaterialTheme {
        Game(
            requestWindowSize = { w, h ->
                windowState.size = windowState.size.copy(width = w, height = h)
            }
        )
    }

@Composable
actual fun loadImage(src: String): Painter = painterResource(src)

actual fun isMobileDevice() = false
