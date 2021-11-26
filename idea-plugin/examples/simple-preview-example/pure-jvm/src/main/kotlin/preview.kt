import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.singleWindowApplication

@Preview
@Composable
fun ExamplePreview() {
    var text by remember { mutableStateOf("Hello, World!") }
    val buttonColors = ButtonDefaults.buttonColors(
        backgroundColor = Color.Blue,
        contentColor = Color.White
    )
    Button(
        colors = buttonColors,
        modifier = Modifier.padding(5.dp),
        onClick = { text = "Hello, Desktop!" }
    ) {
        Row {
            Image(
                painterResource("compose-logo-white.png"),
                "compose-logo",
                modifier = Modifier.height(32.dp).width(32.dp),
            )
            Text(text, modifier = Modifier.padding(top = 8.dp, start = 5.dp))
        }
    }
}

fun main() = singleWindowApplication {
    ExamplePreview()
}