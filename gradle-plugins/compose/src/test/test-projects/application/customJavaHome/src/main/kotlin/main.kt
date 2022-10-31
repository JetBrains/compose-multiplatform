import androidx.compose.material.Text
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.window.singleWindowApplication
import androidx.compose.runtime.*

fun main() = singleWindowApplication {
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("Hello, World!") }

    MaterialTheme {
        Button(onClick = {
            text = "Hello, Desktop!"
        }) {
            Text(text)
        }
    }
}