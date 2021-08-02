import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*

@Composable
fun App() {
    MaterialTheme {
        Button(onClick = {}) {
            Text("Hello, ${getPlatformName()}!")
        }
    }
}

expect fun getPlatformName(): String