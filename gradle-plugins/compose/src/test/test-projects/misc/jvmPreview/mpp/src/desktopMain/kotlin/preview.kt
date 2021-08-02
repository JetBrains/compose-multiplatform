import androidx.compose.runtime.Composable
import androidx.compose.desktop.ui.tooling.preview.Preview

@Preview
@Composable
fun ExamplePreview() {
    ExampleComposable()
}

actual val platformName: String
    get() = "Desktop"