import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
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

actual fun hasRightClick() = true
