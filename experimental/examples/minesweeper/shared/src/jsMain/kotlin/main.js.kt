import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter

@Composable
fun MainView() = Game()

@Composable
actual fun loadImage(src: String): Painter = loadImageAsColoredRect(src)

actual fun isMobileDevice() = false
