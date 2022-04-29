import androidx.compose.runtime.*
import kotlinx.coroutines.*

fun main() {
    runBlocking(UIDispatcher) {
        val container = ComposeContainer()

        launch {
            while (true) {
                println("=== CURRENT UI:")
                container.render()
                delay(1000)
            }
        }

        container.setContent {
            var text1 by remember { mutableStateOf("text1") }
            var text2 by remember { mutableStateOf("text2") }
            ComposeExternalTextField(text1, onChange = { text1 = it })
            ComposeExternalTextField(text2, onChange = { text2 = it })

            LaunchedEffect(Unit) {
                while (true) {
                    text1 = "text1 " + System.currentTimeMillis()
                    delay(100)
                }
            }
        }
    }
}
