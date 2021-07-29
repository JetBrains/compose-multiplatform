import androidx.compose.runtime.*
import androidx.compose.desktop.ui.tooling.preview.Preview

actual fun getPlatformName(): String = "Desktop"

@Preview
@Composable
fun DesktopAppPreview() {
    App()
}