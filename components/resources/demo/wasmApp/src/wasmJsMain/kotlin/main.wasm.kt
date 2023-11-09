
import androidx.compose.ui.window.Window
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.configureWebResources
import org.jetbrains.compose.resources.urlResource

@OptIn(ExperimentalResourceApi::class)
fun main() {
    configureWebResources {
        // Not necessary - It's the same as the default. We add it here just to present this feature.
        setResourceFactory { urlResource("./$it") }
    }

    Window("Resources demo") {
        MainView()
    }
}
