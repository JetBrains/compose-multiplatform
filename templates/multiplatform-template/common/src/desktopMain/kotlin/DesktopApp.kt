import androidx.compose.runtime.Composable
import androidx.compose.desktop.ui.tooling.preview.Preview

actual fun getPlatformName(): String = "Desktop"

@Preview
@Composable
fun AppPreview() {
    App()
}