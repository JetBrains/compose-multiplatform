import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

@Composable
fun App() {
    MaterialTheme {
        var message by remember { mutableStateOf("Press the button!") }

        Button(
            onClick = { message = "Welcome to Compose Multiplatform!" }
        ) {
            Text(message)
        }
    }
}