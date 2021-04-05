import androidx.compose.material.Text
import androidx.compose.material.Button
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.desktop.preview.Preview

@Preview
fun exapmplePreview() = @Composable {
    var text by remember { mutableStateOf("Hello, World!") }

    Button(onClick = {
        text = "Hello, Desktop!"
    }) {
        Text(text)
    }
}