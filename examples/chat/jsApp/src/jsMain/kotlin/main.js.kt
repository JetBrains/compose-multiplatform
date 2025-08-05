import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeViewport
import androidx.compose.ui.ExperimentalComposeUiApi
import org.jetbrains.skiko.wasm.onWasmReady

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    onWasmReady {
        ComposeViewport("composeApp") {
            Column(modifier = Modifier.fillMaxSize()) {
                MainView()
            }
        }
    }
}

