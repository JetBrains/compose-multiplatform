import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import org.jetbrains.compose.resources.configureWebResources
import org.jetbrains.compose.resources.demo.shared.UseResources

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    configureWebResources {
        // Not necessary - It's the same as the default. We add it here just to present this feature.
        resourcePathMapping { path -> "./$path" }
    }
    CanvasBasedWindow("Resources demo + K/Wasm") {
        UseResources()
    }
}